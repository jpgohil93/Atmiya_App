
/**
 * Temporary function to reset the Test User (1111111111) with the correct UID.
 * Usage: Call this URL via browser/Postman.
 * Steps:
 * 1. Force deletes user with email '1111111111@atmiya.com' (if exists)
 * 2. Force deletes user with phone '+911111111111' (if exists)
 * 3. Force deletes user with UID 'OwICB8c3TNXnC2bNdLC9aGEbEaT2' (if conflicting)
 * 4. Creates fresh user with UID 'OwICB8c3TNXnC2bNdLC9aGEbEaT2', Email '1111111111@atmiya.com', Password 'AIF@123', Phone '+911111111111'
 */
exports.resetTestUser = functions.https.onRequest(async (req, res) => {
    const targetUid = 'OwICB8c3TNXnC2bNdLC9aGEbEaT2';
    const targetEmail = '1111111111@atmiya.com';
    const targetPhone = '+911111111111';
    const targetPassword = 'AIF@123';

    try {
        const errors = [];

        // 1. Clean up by Email
        try {
            const userByEmail = await admin.auth().getUserByEmail(targetEmail);
            await admin.auth().deleteUser(userByEmail.uid);
            console.log(`Deleted existing user by email: ${userByEmail.uid}`);
        } catch (e) {
            if (e.code !== 'auth/user-not-found') errors.push(`Email Cleanup Error: ${e.message}`);
        }

        // 2. Clean up by Phone
        try {
            const userByPhone = await admin.auth().getUserByPhoneNumber(targetPhone);
            // Only delete if it's not the same one we just deleted (async race check)
            try {
                await admin.auth().getUser(userByPhone.uid); // Check if still exists
                await admin.auth().deleteUser(userByPhone.uid);
                console.log(`Deleted existing user by phone: ${userByPhone.uid}`);
            } catch (inner) { }
        } catch (e) {
            if (e.code !== 'auth/user-not-found') errors.push(`Phone Cleanup Error: ${e.message}`);
        }

        // 3. Clean up by UID (Target UID Collision check)
        try {
            await admin.auth().deleteUser(targetUid);
            console.log(`Deleted existing user with target UID: ${targetUid}`);
        } catch (e) {
            if (e.code !== 'auth/user-not-found') errors.push(`UID Cleanup Error: ${e.message}`);
        }

        // 4. Create the CORRECT user
        const newUser = await admin.auth().createUser({
            uid: targetUid,
            email: targetEmail,
            emailVerified: true,
            phoneNumber: targetPhone,
            password: targetPassword,
            displayName: "Test User"
        });

        res.status(200).send({
            success: true,
            message: "User recreated successfully with correct UID.",
            uid: newUser.uid,
            email: newUser.email,
            password: targetPassword,
            phone: newUser.phoneNumber,
            warnings: errors
        });

    } catch (error) {
        console.error("Reset Failed", error);
        res.status(500).send({
            success: false,
            error: error.message
        });
    }
});
