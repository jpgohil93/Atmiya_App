const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

/**
 * Triggered when a new Wall Post is created.
 * Sends a notification to the "all_posts" topic.
 */
exports.notifyOnNewWallPost = functions.firestore
    .document('wallPosts/{postId}')
    .onCreate(async (snap, context) => {
        const newValue = snap.data();
        const postId = context.params.postId;

        // Don't notify if the post is inactive (usually false on create, but good practice)
        if (newValue.active === false) {
            return null;
        }

        const authorName = newValue.authorName || 'Someone';
        const postType = newValue.postType || 'Post'; // generic, funding_call etc
        const mediaType = newValue.mediaType || 'none';
        const attachments = newValue.attachments || [];
        const content = newValue.content || '';

        let title = authorName;
        let body = content;
        let imageUrl = null;

        // --- Formatting Logic ---

        // Check for Video (Priority over Image)
        const hasVideo = mediaType === 'video' || attachments.some(a => a.type === 'video');
        const hasImage = mediaType === 'image' || attachments.some(a => a.type === 'image');

        if (hasVideo) {
            // C. Video Post
            title = `${authorName} uploaded a video`;
            body = content ? `▶️ ${content}` : `▶️ Check out this video`;

            // Try to find a thumbnail
            if (newValue.thumbnailUrl) imageUrl = newValue.thumbnailUrl;
            else {
                const vidAttach = attachments.find(a => a.type === 'video');
                if (vidAttach && vidAttach.thumbnailUrl) imageUrl = vidAttach.thumbnailUrl;
            }

        } else if (hasImage) {
            // B. Image Post
            title = `${authorName} added a new photo`;
            body = content ? content : "Check out this new photo.";

            // Try to find image URL
            if (newValue.mediaUrl) imageUrl = newValue.mediaUrl;
            else {
                const imgAttach = attachments.find(a => a.type === 'image');
                if (imgAttach) imageUrl = imgAttach.url;
            }

        } else {
            // A. Text Post
            title = authorName; // Just the name
            body = content;
        }

        // Truncate Body
        if (body.length > 100) {
            body = body.substring(0, 97) + '...';
        }

        const timestamp = admin.firestore.FieldValue.serverTimestamp();

        // 1. Create Notification Document for History
        const notificationData = {
            title: title,
            body: body,
            type: 'wall_post', // could be specific like 'wall_video' if needed by UI
            targetId: postId,
            createdAt: timestamp,
            imageUrl: imageUrl // Save image to history too
        };

        // Write to global_notifications
        await admin.firestore().collection('global_notifications').add(notificationData);

        // 2. Prepare FCM Payload
        const payload = {
            notification: {
                title: title,
                body: body,
            },
            data: {
                type: 'wall_post',
                postId: postId,
                authorId: newValue.authorUserId || '', // Add authorId for client-side filtering
                click_action: 'FLUTTER_NOTIFICATION_CLICK'
            },
            topic: 'all_posts'
        };

        // Add Image to FCM if exists (Android/iOS support)
        if (imageUrl) {
            payload.notification.image = imageUrl;
        }

        // 3. Send FCM message
        try {
            const response = await admin.messaging().send(payload);
            console.log('Successfully sent message:', response);
            return response;
        } catch (error) {
            console.log('Error sending message:', error);
            return null;
        }
    });

/**
 * Triggered when a Funding Call is updated.
 * Notifies startups about deadline extensions or closure.
 */
exports.notifyOnFundingCallUpdate = functions.firestore
    .document('fundingCalls/{callId}')
    .onUpdate(async (change, context) => {
        const newValue = change.after.data();
        const oldValue = change.before.data();
        const callId = context.params.callId;

        const title = newValue.title || 'Funding Opportunity';
        const investorName = newValue.investorName || 'Investor';

        // Check for Deadline Extension
        const oldDeadline = oldValue && oldValue.applicationDeadline ? oldValue.applicationDeadline.toDate().getTime() : 0;
        const newDeadline = newValue && newValue.applicationDeadline ? newValue.applicationDeadline.toDate().getTime() : 0;

        let notificationTitle = null;
        let notificationBody = null;

        if (newDeadline > oldDeadline) {
            // Deadline Extended
            notificationTitle = "Funding Deadline Extended! ⏳";
            notificationBody = `${investorName} has extended the deadline for "${title}". Check it out!`;
        } else if (newValue.isActive === false && (oldValue.isActive === true || oldValue.isActive === undefined)) {
            // Call Disabled/Closed
            // Trigger if it is now false, and previously was true or undefined (assumed active)
            notificationTitle = "Funding Call Closed 🔒";
            notificationBody = `The funding opportunity "${title}" by ${investorName} has been closed.`;
        }

        if (!notificationTitle) {
            return null; // No relevant change
        }

        // Notification Payload
        const payload = {
            notification: {
                title: notificationTitle,
                body: notificationBody,
            },
            data: {
                type: 'funding_call_update',
                callId: callId,
                click_action: 'FLUTTER_NOTIFICATION_CLICK'
            },
            topic: 'startups' // Assuming startups are subscribed to this topic
        };

        // Send to 'startups' topic
        try {
            const response = await admin.messaging().send(payload);
            console.log('Successfully sent funding update:', response);

            // Also add to global notifications for history
            const historyData = {
                title: notificationTitle,
                body: notificationBody,
                type: 'funding_call_update',
                targetId: callId,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                imageUrl: null
            };
            await admin.firestore().collection('global_notifications').add(historyData);

            return response;
        } catch (error) {
            console.log('Error sending funding update:', error);
            return null;
        }
    });

/**
 * Triggered when a new Connection Request is created.
 * Notifies the receiver.
 */
exports.notifyOnNewConnectionRequest = functions.firestore
    .document('connectionRequests/{requestId}')
    .onCreate(async (snap, context) => {
        const newData = snap.data();
        const receiverId = newData.receiverId;
        const senderName = newData.senderName;
        const senderRole = newData.senderRole;

        // Get Receiver's FCM Token
        const receiverDoc = await admin.firestore().collection('users').doc(receiverId).get();
        const receiverData = receiverDoc.data();
        const fcmToken = receiverData ? receiverData.fcmToken : null;

        if (!fcmToken) {
            console.log('No FCM token for receiver:', receiverId);
            return null;
        }

        const title = "New Connection Request";
        const body = `${senderName} wants to connect with you.`;

        const payload = {
            notification: {
                title: title,
                body: body,
            },
            data: {
                type: 'connection_request',
                senderRole: senderRole || 'unknown',
                click_action: 'FLUTTER_NOTIFICATION_CLICK'
            },
            token: fcmToken
        };

        try {
            const response = await admin.messaging().send(payload);
            console.log('Sent connection request notification:', response);
            return response;
        } catch (error) {
            console.log('Error sending notification:', error);
            return null;
        }
    });

/**
 * Triggered when a Connection Request is accepted.
 * Notifies the sender.
 */
exports.notifyOnConnectionAccepted = functions.firestore
    .document('connectionRequests/{requestId}')
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const oldData = change.before.data();

        // Only trigger if status changed to 'accepted'
        if (newData.status === 'accepted' && oldData.status !== 'accepted') {
            const senderId = newData.senderId;
            const receiverId = newData.receiverId;
            const receiverName = newData.receiverName;

            // Get Sender's FCM Token
            const senderDoc = await admin.firestore().collection('users').doc(senderId).get();
            const senderData = senderDoc.data();
            const fcmToken = senderData ? senderData.fcmToken : null;

            if (!fcmToken) {
                console.log('No FCM token for sender:', senderId);
                return null;
            }

            const title = "Connection Accepted! 🎉";
            const body = `${receiverName} accepted your connection request.`;

            const payload = {
                notification: {
                    title: title,
                    body: body,
                },
                data: {
                    type: 'connection_accepted',
                    senderRole: newData.receiverRole || 'unknown',
                    authorId: receiverId, // ID to navigate to
                    click_action: 'FLUTTER_NOTIFICATION_CLICK'
                },
                token: fcmToken
            };

            try {
                const response = await admin.messaging().send(payload);
                console.log('Sent connection accepted notification:', response);
                return response;
            } catch (error) {
                console.log('Error sending notification:', error);
                return null;
            }
        }
        return null;
    });
