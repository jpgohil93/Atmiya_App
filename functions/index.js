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
        const postType = newValue.postType || 'Post';
        const timestamp = admin.firestore.FieldValue.serverTimestamp();

        // 1. Create Notification Document for History
        const notificationData = {
            title: `New ${postType} by ${authorName}`,
            body: newValue.content || 'Check out the new post on the wall!',
            type: 'wall_post',
            targetId: postId,
            createdAt: timestamp
        };

        // Write to global_notifications
        // We use .add() to auto-generate an ID
        await admin.firestore().collection('global_notifications').add(notificationData);

        // 2. Prepare FCM Payload
        const payload = {
            notification: {
                title: notificationData.title,
                body: notificationData.body,
            },
            data: {
                type: 'wall_post',
                postId: postId,
                click_action: 'FLUTTER_NOTIFICATION_CLICK'
            },
            topic: 'all_posts'
        };

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
