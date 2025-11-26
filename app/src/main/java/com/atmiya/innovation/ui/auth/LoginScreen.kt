package com.atmiya.innovation.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo Placeholder
        // Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo")
        Text(text = "Atmiya Innovation", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(32.dp))

        if (verificationId == null) {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number (+91...)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        isLoading = true
                        val options = PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber(phoneNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(context as Activity)
                            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    isLoading = false
                                    signInWithPhoneAuthCredential(credential, auth, onLoginSuccess, context)
                                }

                                override fun onVerificationFailed(e: FirebaseException) {
                                    isLoading = false
                                    Toast.makeText(context, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }

                                override fun onCodeSent(
                                    vId: String,
                                    token: PhoneAuthProvider.ForceResendingToken
                                ) {
                                    isLoading = false
                                    verificationId = vId
                                    Toast.makeText(context, "OTP Sent", Toast.LENGTH_SHORT).show()
                                }
                            })
                            .build()
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Sending OTP..." else "Get OTP")
            }
        } else {
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("Enter OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (otp.isNotEmpty()) {
                        isLoading = true
                        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                        signInWithPhoneAuthCredential(credential, auth, onLoginSuccess, context)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Verifying..." else "Verify OTP")
            }
        }
    }
}

private fun signInWithPhoneAuthCredential(
    credential: PhoneAuthCredential,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    context: android.content.Context
) {
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                Toast.makeText(context, "Sign In Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
}
