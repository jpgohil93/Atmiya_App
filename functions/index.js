const functions = require("firebase-functions");
const admin = require("firebase-admin");
const path = require("path");
const os = require("os");
const fs = require("fs");
const csv = require("csv-parser");

admin.initializeApp();
const db = admin.firestore();

exports.processCsvImport = functions.firestore
    .document("imports/{importId}")
    .onCreate(async (snap, context) => {
        const importId = context.params.importId;
        const data = snap.data();

        if (data.status !== "pending") return null;

        const filePath = data.filePath; // "imports/role/filename.csv"
        const role = data.role;
        const bucket = admin.storage().bucket();
        const fileName = path.basename(filePath);
        const tempFilePath = path.join(os.tmpdir(), fileName);

        try {
            // 1. Update status to processing
            await snap.ref.update({ status: "processing" });

            // 2. Download CSV
            await bucket.file(filePath).download({ destination: tempFilePath });

            const results = [];
            let successCount = 0;
            let failureCount = 0;
            const errors = [];

            // 3. Parse CSV
            await new Promise((resolve, reject) => {
                fs.createReadStream(tempFilePath)
                    .pipe(csv())
                    .on("data", (data) => results.push(data))
                    .on("end", resolve)
                    .on("error", reject);
            });

            // 4. Process Rows
            for (let i = 0; i < results.length; i++) {
                const row = results[i];
                const rowNum = i + 1;

                try {
                    // Validate required fields
                    if (!row.email || !row.name) {
                        throw new Error("Missing email or name");
                    }

                    const email = row.email.trim();
                    const name = row.name.trim();
                    const phone = row.phone ? row.phone.trim() : "";

                    // Create Auth User
                    let userRecord;
                    try {
                        userRecord = await admin.auth().getUserByEmail(email);
                    } catch (e) {
                        if (e.code === 'auth/user-not-found') {
                            userRecord = await admin.auth().createUser({
                                email: email,
                                displayName: name,
                                password: "ChangeMe123!", // Temporary password
                                phoneNumber: phone || undefined
                            });
                        } else {
                            throw e;
                        }
                    }

                    // Create Firestore Document
                    const userDoc = {
                        uid: userRecord.uid,
                        name: name,
                        email: email,
                        phoneNumber: phone,
                        role: role,
                        createdAt: admin.firestore.FieldValue.serverTimestamp(),
                        isOnboardingComplete: false,
                        isBlocked: false,
                        isDeleted: false
                    };

                    // Add role specific fields if present in CSV
                    if (role === "startup") {
                        if (row.sector) userDoc.startupCategory = row.sector;
                        if (row.city) userDoc.city = row.city;
                    } else if (role === "investor") {
                        if (row.firmName) userDoc.firmName = row.firmName;
                    }

                    await db.collection("users").doc(userRecord.uid).set(userDoc, { merge: true });

                    // Also create role-specific doc if needed (startups/{uid}, etc)
                    // For now, we keep it simple and just use users collection as primary.

                    successCount++;
                } catch (error) {
                    console.error(`Row ${rowNum} failed:`, error);
                    failureCount++;
                    errors.push({
                        rowNumber: rowNum,
                        errorMessage: error.message,
                        rawData: JSON.stringify(row)
                    });
                }
            }

            // 5. Update Import Record
            await snap.ref.update({
                status: failureCount === 0 ? "completed" : "completed_with_errors",
                totalRows: results.length,
                successCount: successCount,
                failureCount: failureCount,
                completedAt: admin.firestore.FieldValue.serverTimestamp()
            });

            // 6. Write Errors (if any)
            if (errors.length > 0) {
                const batch = db.batch();
                errors.forEach(err => {
                    const errRef = snap.ref.collection("errors").doc();
                    batch.set(errRef, err);
                });
                await batch.commit();
            }

        } catch (error) {
            console.error("Import failed:", error);
            await snap.ref.update({ status: "failed" });
        } finally {
            fs.unlinkSync(tempFilePath);
        }
    });
