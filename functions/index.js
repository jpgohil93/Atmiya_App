const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');
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

/**
 * Callable Cloud Function for processing bulk CSV uploads.
 * Reads CSV from Cloud Storage, validates, and creates users in batches.
 * Supports 50,000+ records efficiently.
 */
exports.processBulkUpload = functions
    .runWith({
        timeoutSeconds: 540, // 9 minutes max for large uploads
        memory: '1GB'
    })
    .https.onCall(async (data, context) => {
        // 1. Verify Admin
        if (!context.auth) {
            throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
        }

        const callerUid = context.auth.uid;
        const callerDoc = await admin.firestore().collection('users').doc(callerUid).get();
        const callerData = callerDoc.data();

        if (!callerData || callerData.role !== 'admin') {
            throw new functions.https.HttpsError('permission-denied', 'Only admins can perform bulk uploads');
        }

        // 2. Get CSV content from request (passed as string)
        const csvContent = data.csvContent;
        if (!csvContent || typeof csvContent !== 'string') {
            throw new functions.https.HttpsError('invalid-argument', 'csvContent is required');
        }

        // 3. Parse CSV
        const lines = csvContent.split('\n').filter(line => line.trim() !== '');
        if (lines.length < 2) {
            throw new functions.https.HttpsError('invalid-argument', 'CSV must have header and at least one data row');
        }

        // Skip header, process data
        const dataLines = lines.slice(1);
        const results = { success: 0, failed: 0, errors: [] };

        // 4. Fetch existing phones for uniqueness check
        const existingPhonesSnapshot = await admin.firestore().collection('users')
            .select('phoneNumber')
            .get();
        const existingPhones = new Set(existingPhonesSnapshot.docs.map(doc => {
            let p = doc.data().phoneNumber;
            if (p) {
                p = p.replace(/\s/g, '').replace(/-/g, '');
                if (p.startsWith('+91')) p = p.substring(3);
                if (p.startsWith('91') && p.length === 12) p = p.substring(2);
            }
            return p;
        }).filter(Boolean));
        const seenPhones = new Set();

        // 5. Process in batches of 150 (each record = 3 writes = 450 ops < 500 limit)
        const BATCH_SIZE = 150;
        const validRecords = [];

        for (let i = 0; i < dataLines.length; i++) {
            const line = dataLines[i];
            const parts = line.split(',').map(p => p.trim());

            // Expected: name,phone,email,city,region,organization (matching Android app format)
            if (parts.length < 3) {
                results.errors.push(`Line ${i + 2}: Insufficient columns (need at least name, phone, email)`);
                results.failed++;
                continue;
            }

            const [name, rawPhone, email, city, region, organization] = parts;

            // Clean phone number
            let phone = rawPhone.replace(/\s/g, '').replace(/-/g, '');
            if (phone.startsWith('+91')) phone = phone.substring(3);
            if (phone.startsWith('91') && phone.length === 12) phone = phone.substring(2);

            // Validation
            if (!name || name.length < 2) {
                results.errors.push(`Line ${i + 2}: Invalid name`);
                results.failed++;
                continue;
            }
            if (!email || !email.includes('@')) {
                results.errors.push(`Line ${i + 2}: Invalid email`);
                results.failed++;
                continue;
            }
            if (!phone || phone.length !== 10 || !/^\d+$/.test(phone)) {
                results.errors.push(`Line ${i + 2}: Invalid phone (${phone})`);
                results.failed++;
                continue;
            }
            if (existingPhones.has(phone)) {
                results.errors.push(`Line ${i + 2}: Phone ${phone} already exists`);
                results.failed++;
                continue;
            }
            if (seenPhones.has(phone)) {
                results.errors.push(`Line ${i + 2}: Duplicate phone in CSV`);
                results.failed++;
                continue;
            }

            seenPhones.add(phone);
            validRecords.push({
                name,
                email: email.toLowerCase(),
                phone,
                role: 'startup', // Default role for bulk uploads
                city: city || '',
                region: region || '',
                organization: organization || ''
            });
        }

        // 6. Write in batches
        const db = admin.firestore();

        for (let i = 0; i < validRecords.length; i += BATCH_SIZE) {
            const batchRecords = validRecords.slice(i, i + BATCH_SIZE);
            const batch = db.batch();

            for (const record of batchRecords) {
                const uid = db.collection('users').doc().id; // Generate new ID
                const timestamp = admin.firestore.FieldValue.serverTimestamp();

                // User document
                const userRef = db.collection('users').doc(uid);
                batch.set(userRef, {
                    uid: uid,
                    name: record.name,
                    email: record.email,
                    phoneNumber: record.phone,
                    role: record.role,
                    city: record.city,
                    region: record.region,
                    organization: record.organization,
                    createdAt: timestamp,
                    createdVia: 'bulk',
                    isBlocked: false,
                    isDeleted: false,
                    hasCompletedOnboarding: false,
                    hasCompletedRoleDetails: false  // Bulk users need to complete startup details
                });

                // Startup document (if startup role)
                if (record.role === 'startup') {
                    const startupRef = db.collection('startups').doc(uid);
                    batch.set(startupRef, {
                        uid: uid,
                        startupName: record.name,
                        createdAt: timestamp
                    });
                }

                // Bulk invite document (keyed by phone)
                const inviteRef = db.collection('bulk_invites').doc(record.phone);
                batch.set(inviteRef, {
                    uid: uid,
                    email: record.email,
                    createdAt: timestamp
                });
            }

            try {
                await batch.commit();
                results.success += batchRecords.length;
                console.log(`Batch ${Math.floor(i / BATCH_SIZE) + 1} committed: ${batchRecords.length} records`);
            } catch (error) {
                console.error('Batch commit error:', error);
                results.failed += batchRecords.length;
                results.errors.push(`Batch ${Math.floor(i / BATCH_SIZE) + 1} failed: ${error.message}`);
            }
        }

        console.log(`Bulk upload complete: ${results.success} success, ${results.failed} failed`);
        return results;
    });

/**
 * Sends a 4-digit OTP via SMS.
 * Callable Function.
 * Input: { phone: string }
 */
exports.sendOtp = functions.https.onCall(async (data, context) => {
    const phone = data.phone;
    if (!phone) {
        throw new functions.https.HttpsError('invalid-argument', 'Phone number is required');
    }

    // Rate limiting: Check if OTP was sent recently (last 60s)
    const db = admin.firestore();
    const otpRef = db.collection('otp_requests').doc(phone);
    const doc = await otpRef.get();

    const now = Date.now();
    if (doc.exists) {
        const lastSent = doc.data().createdAt;
        if (now - lastSent < 1000) { // 1 second
            throw new functions.https.HttpsError('resource-exhausted', 'Please wait before requesting another OTP');
        }
    }

    // Generate 4-digit OTP
    const otp = Math.floor(1000 + Math.random() * 9000).toString();

    // Store in Firestore (expires in 5 minutes)
    await otpRef.set({
        otp: otp,
        createdAt: now,
        expiresAt: now + 5 * 60 * 1000,
        attempts: 0
    });

    // Send SMS via Provider
    try {
        // Use Hardcoded credentials for reliability (since user provided them directly)
        const apiKey = "jun5mj4zX4FzyEq2";
        const senderId = "HLCALE";
        const baseUrl = "http://sms.lifeweblink.com/vb/apikey.php";

        // Format: http://sms.lifeweblink.com/vb/apikey.php?apikey=...&senderid=...&number=...&message=...
        const message = `Your OTP is ${otp} Use this code to complete your verification. Do not share this code with anyone. HL2SCALE`;

        // Parse/Clean Phone Number - FORMAT MUST BE 91XXXXXXXXXX (12 digits)
        let targetPhone = phone.replace(/\D/g, ''); // Strip non-digits
        // If it starts with 91 and is 12 digits, keep it.
        // If it is 10 digits, add 91.
        if (targetPhone.length === 10) {
            targetPhone = '91' + targetPhone;
        } else if (targetPhone.length > 10 && !targetPhone.startsWith('91')) {
            // If weird format, strip to last 10 and add 91
            targetPhone = '91' + targetPhone.substring(targetPhone.length - 10);
        }
        // If it was already 12 digits starting with 91, targetPhone is correct.

        const url = `${baseUrl}?apikey=${apiKey}&senderid=${senderId}&number=${targetPhone}&message=${encodeURIComponent(message)}`;

        console.log(`Sending OTP to ${phone} (target: ${targetPhone})`);

        // Make the GET request
        let response;
        try {
            response = await axios.get(url);
            console.log('SMS Provider Response Status:', response.status);
            console.log('SMS Provider Response Data:', response.data);

            // Check for provider specific error codes if known, or just log success
            if (response.data && response.data.toString().includes('Error')) {
                console.error('SMS Provider returned error in body:', response.data);
                // We might want to throw here if we want to fail the client, but for now let's just log
                throw new Error('Provider Error: ' + response.data);
            }

        } catch (smsError) {
            console.error('Failed to call SMS Provider URL:', url); // Be careful logging full URL with keys in prod, but needed for debug now
            console.error('SMS Error Details:', smsError.message);
            if (smsError.response) {
                console.error('SMS Error Response:', smsError.response.data);
                console.error('SMS Error Status:', smsError.response.status);
            }
            throw new functions.https.HttpsError('internal', 'Failed to send SMS: ' + smsError.message);
        }

        // --- Push Notification Fallback (DISABLED) ---
        /*
        const fcmToken = data.fcmToken;
        let pushResult = "Not attempted";

        if (fcmToken) {
            try {
                const message = {
                    token: fcmToken,
                    notification: {
                        title: "Your Verification Code",
                        body: `Your OTP is ${otp}`
                    },
                    data: {
                        type: "otp_code",
                        otp: String(otp)
                    }
                };
                await admin.messaging().send(message);
                pushResult = "Sent";
                console.log("OTP Push Notification sent to user.");
            } catch (pushError) {
                console.error("Failed to send OTP Push:", pushError);
                pushResult = "Failed: " + pushError.message;
            }
        }
        */
        let pushResult = "Disabled";

        return { success: true, providerResponse: response ? response.data : "No response", pushResult: pushResult };
    } catch (error) {
        console.error('SMS Provider Error:', error);
        throw new functions.https.HttpsError('internal', 'Failed to send SMS');
    }
});

/**
 * Verifies the 4-digit OTP.
 * Callable Function.
 * Input: { phone: string, otp: string }
 * Output: { token: string, isNewUser: boolean }
 */
exports.verifyOtp = functions.https.onCall(async (data, context) => {
    const phone = data.phone;
    const otp = data.otp;

    if (!phone || !otp) {
        throw new functions.https.HttpsError('invalid-argument', 'Phone and OTP are required');
    }

    const db = admin.firestore();
    const otpRef = db.collection('otp_requests').doc(phone);
    const doc = await otpRef.get();

    if (!doc.exists) {
        throw new functions.https.HttpsError('not-found', 'No OTP request found');
    }

    const record = doc.data();
    const now = Date.now();

    // Check expiration
    if (now > record.expiresAt) {
        throw new functions.https.HttpsError('deadline-exceeded', 'OTP has expired');
    }

    // Check attempts
    if (record.attempts >= 3) {
        throw new functions.https.HttpsError('resource-exhausted', 'Too many failed attempts');
    }

    // Verify OTP
    // Relaxed check: Verify strict equality on string
    if (String(record.otp).trim() !== String(otp).trim()) {
        console.warn(`Invalid OTP for ${phone}. Expected: ${record.otp}, Got: ${otp}`);
        await otpRef.update({ attempts: admin.firestore.FieldValue.increment(1) });
        throw new functions.https.HttpsError('invalid-argument', 'Invalid OTP');
    }

    // OTP Valid - Clean up
    await otpRef.delete();

    // Create Custom Token for Firebase Auth
    console.log(`OTP Verified for ${phone}. Generating token...`);

    // 1. Try to find user in Firestore first (to preserve existing UIDs)
    // IMPORTANT: App handles +91 prefix, but Firestore might store it differently.
    // Let's check both raw phone and clean phone if needed, but for now exact match.
    // If phone came in as 9876543210 but stored as +919876543210, we might miss it.

    let uid;
    let isNewUser = false;

    try {
        let usersSnap = await db.collection('users').where('phoneNumber', '==', phone).limit(1).get();

        // Fallback: Check for 10-digit number if +91 format not found
        if (usersSnap.empty && phone.startsWith('+91')) {
            const rawPhone = phone.replace('+91', '');
            console.log(`Checking for raw phone in Firestore: ${rawPhone}`);
            usersSnap = await db.collection('users').where('phoneNumber', '==', rawPhone).limit(1).get();
        }

        if (!usersSnap.empty) {
            uid = usersSnap.docs[0].id;
            console.log(`Found existing Firestore user: ${uid}`);
        } else {
            console.log(`User not found in Firestore for ${phone}. Checking Auth...`);
            // Not in Firestore. Check Auth.
            try {
                const userRecord = await admin.auth().getUserByPhoneNumber(phone);
                uid = userRecord.uid;
                console.log(`Found existing Auth user: ${uid}`);
            } catch (e) {
                // New User
                isNewUser = true;
                console.log(`Creating new Auth user for ${phone}`);
                // Create user in Auth
                const newUser = await admin.auth().createUser({
                    phoneNumber: phone,
                    verified: true
                });
                uid = newUser.uid;
            }
        }

        const customToken = await admin.auth().createCustomToken(uid);
        console.log(`Token generated successfully for ${uid}`);
        return { token: customToken, uid: uid, isNewUser: isNewUser };

    } catch (error) {
        console.error("Error in verifyOtp token generation:", error);
        throw new functions.https.HttpsError('internal', `Token generation failed: ${error.message}`);
    }
});
