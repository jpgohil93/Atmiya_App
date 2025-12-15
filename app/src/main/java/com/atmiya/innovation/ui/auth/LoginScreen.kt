package com.atmiya.innovation.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowRight
import compose.icons.tablericons.Eye
import compose.icons.tablericons.EyeOff
import compose.icons.tablericons.Lock
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmiya.innovation.R
import com.atmiya.innovation.ui.components.*
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.atmiya.innovation.ui.theme.SoftBgLight
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

enum class LoginMode { OTP, PASSWORD }
enum class AuthStep { PHONE_INPUT, OTP_VERIFICATION, FORGOT_PASSWORD_OTP, RESET_PASSWORD }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignupClick: () -> Unit
) {
    var loginMode by remember { mutableStateOf(LoginMode.OTP) }
    var authStep by remember { mutableStateOf(AuthStep.PHONE_INPUT) }
    
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var verificationId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Phone Validation
    val isPhoneValid = phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // OTP State
    var otpValue by remember { mutableStateOf("") }
    var isOtpError by remember { mutableStateOf(false) }

    // Timer State
    var ticks by remember { mutableLongStateOf(60L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            val startTime = System.currentTimeMillis()
            while (ticks > 0) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                ticks = 60L - elapsed
                delay(1000)
            }
            isTimerRunning = false
        }
    }

    // State to track if verification is already done (to prevent race conditions)
    var isVerificationCompleted by remember { mutableStateOf(false) }

    // Focus requester for phone input
    val phoneFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // Permission Request Logic
    val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(android.Manifest.permission.CAMERA)
    }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results if needed
    }

    LaunchedEffect(Unit) {
        // Check if any permission is not granted before requesting
        val permissionsToRequest = permissions.filter { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        // Only request if there are permissions that haven't been granted
        if (permissionsToRequest.isNotEmpty()) {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
        
        // Delay to ensure layout is ready, then request focus
        kotlinx.coroutines.delay(300)
        try {
            phoneFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus errors
        }
    }

    fun getSyntheticEmail(phone: String) = "$phone@atmiya.com"

    fun sendOtp(phone: String, isResend: Boolean = false) {
        if (isLoading && !isResend) return // Prevent multiple clicks
        
        val activity = context.findActivity()
        if (activity == null) {
            Toast.makeText(context, "Error: Cannot find Activity", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val start = System.currentTimeMillis()
        val fullNumber = "+91$phone"
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-verification
                    if (isVerificationCompleted) return
                    isVerificationCompleted = true
                    isLoading = false
                    
                    // If we are in Forgot Password flow, we just proceed to Reset Password
                    if (authStep == AuthStep.FORGOT_PASSWORD_OTP) {
                        signInWithPhoneAuthCredential(credential, auth, {
                            authStep = AuthStep.RESET_PASSWORD
                        }, context)
                        return
                    }

                    signInWithPhoneAuthCredential(credential, auth, {
                        onLoginSuccess()
                    }, context)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isLoading = false
                    android.util.Log.e("Auth", "Verification Failed", e)
                    Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    if (isVerificationCompleted) return
                    isLoading = false
                    verificationId = vId
                    
                    if (authStep == AuthStep.PHONE_INPUT) {
                        authStep = AuthStep.OTP_VERIFICATION
                    }
                    // If FORGOT_PASSWORD_OTP, stay there
                    
                    ticks = 60L
                    isTimerRunning = true
                    android.util.Log.d("Perf", "Login: OTP Sent took ${System.currentTimeMillis() - start} ms")
                    Toast.makeText(context, "OTP Sent", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otp: String) {
        val vid = verificationId ?: return
        isLoading = true
        val start = System.currentTimeMillis()
        val credential = PhoneAuthProvider.getCredential(vid, otp)
        
        signInWithPhoneAuthCredential(credential, auth, {
            isLoading = false
            isVerificationCompleted = true
            android.util.Log.d("Perf", "Login: OTP Verification took ${System.currentTimeMillis() - start} ms")
            
            if (authStep == AuthStep.FORGOT_PASSWORD_OTP) {
                authStep = AuthStep.RESET_PASSWORD
            } else {
                onLoginSuccess()
            }
        }, context) {
            isLoading = false
            isOtpError = true
            otpValue = "" // Clear OTP on error
        }
    }

    fun loginWithPassword() {
        if (!isPhoneValid) {
            phoneError = "Invalid phone number"
            return
        }
        if (password.isEmpty()) {
            passwordError = "Enter password"
            return
        }

        isLoading = true
        val email = getSyntheticEmail(phoneNumber)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    passwordError = "Login failed: ${task.exception?.message}"
                }
            }
    }

    fun resetPassword() {
         if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            return
        }
        if (password != confirmPassword) {
            passwordError = "Passwords do not match"
            return
        }
        
        isLoading = true
        val user = auth.currentUser
        if (user != null) {
            // We are signed in via OTP, so we can update password
            val email = getSyntheticEmail(phoneNumber)
            val credential = EmailAuthProvider.getCredential(email, password)
            
            user.updatePassword(password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                         user.linkWithCredential(credential).addOnCompleteListener { 
                            isLoading = false
                            Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    } else {
                         user.linkWithCredential(credential).addOnCompleteListener { linkTask ->
                            isLoading = false
                            if (linkTask.isSuccessful) {
                                Toast.makeText(context, "Password set!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                passwordError = "Failed to update password: ${task.exception?.message}"
                            }
                        }
                    }
                }
        }
    }

    SoftScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .imePadding() // Add padding for IME (keyboard)
                .verticalScroll(rememberScrollState()), // Make scrollable
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Changed from Center to Top
        ) {
            Spacer(modifier = Modifier.height(60.dp)) // Add top spacing for visual balance
            // Logo Card
            // Logo
            // Logo
            Image(
                painter = painterResource(id = R.drawable.netfund_logo),
                contentDescription = "Netfund Logo",
                modifier = Modifier
                    .width(400.dp) // Maximized width
                    .height(220.dp) // Maximized height
                    .padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            SoftCard(modifier = Modifier.fillMaxWidth()) {
                
                // Login Mode Tabs (Only visible in initial input steps)
                if (authStep == AuthStep.PHONE_INPUT) {
                    TabRow(
                        selectedTabIndex = if (loginMode == LoginMode.OTP) 0 else 1,
                        containerColor = Color.Transparent,
                        contentColor = AtmiyaPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[if (loginMode == LoginMode.OTP) 0 else 1]),
                                color = AtmiyaSecondary
                            )
                        }
                    ) {
                        Tab(
                            selected = loginMode == LoginMode.OTP,
                            onClick = { loginMode = LoginMode.OTP },
                            text = { 
                                Text(
                                    "OTP Login", 
                                    fontWeight = if (loginMode == LoginMode.OTP) FontWeight.Bold else FontWeight.Normal,
                                    color = if (loginMode == LoginMode.OTP) AtmiyaPrimary else Color.Gray
                                ) 
                            }
                        )
                        Tab(
                            selected = loginMode == LoginMode.PASSWORD,
                            onClick = { loginMode = LoginMode.PASSWORD },
                            text = { 
                                Text(
                                    "Password Login", 
                                    fontWeight = if (loginMode == LoginMode.PASSWORD) FontWeight.Bold else FontWeight.Normal,
                                    color = if (loginMode == LoginMode.PASSWORD) AtmiyaPrimary else Color.Gray
                                ) 
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                when (authStep) {
                    AuthStep.PHONE_INPUT -> {
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AtmiyaPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (loginMode == LoginMode.OTP) "Enter your mobile number to get OTP" else "Enter your mobile number and password",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        SoftTextField(
                            value = phoneNumber,
                            onValueChange = { 
                                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                    phoneNumber = it
                                    phoneError = null
                                }
                            },
                            label = "Mobile Number",
                            placeholder = "9876543210",
                            modifier = Modifier.focusRequester(phoneFocusRequester),
                            leadingIcon = {
                                Text(
                                    "+91",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                                    color = AtmiyaPrimary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = phoneError != null
                        )

                        if (phoneError != null) {
                            Text(
                                text = phoneError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }

                        if (loginMode == LoginMode.PASSWORD) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { 
                                    password = it
                                    passwordError = null
                                },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = TablerIcons.Lock,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                },
                                trailingIcon = {
                                    val image = if (passwordVisible)
                                        TablerIcons.EyeOff
                                    else
                                        TablerIcons.Eye

                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = passwordError != null,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AtmiyaPrimary,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                )
                            )
                            
                            if (passwordError != null) {
                                Text(
                                    text = passwordError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                TextButton(onClick = { 
                                    if (isPhoneValid) {
                                        sendOtp(phoneNumber)
                                        authStep = AuthStep.FORGOT_PASSWORD_OTP
                                    } else {
                                        phoneError = "Enter valid phone number first"
                                    }
                                }) {
                                    Text("Forgot Password?", color = AtmiyaSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        SoftButton(
                            onClick = {
                                if (loginMode == LoginMode.OTP) {
                                    if (isPhoneValid) {
                                        sendOtp(phoneNumber)
                                    } else {
                                        phoneError = "Please enter a valid 10-digit mobile number."
                                    }
                                } else {
                                    loginWithPassword()
                                }
                            },
                            text = if (loginMode == LoginMode.OTP) "Get OTP" else "Login",
                            icon = TablerIcons.ArrowRight,
                            isLoading = isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))
            
                        TextButton(
                            onClick = onSignupClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("New to Netfund? Sign Up", color = AtmiyaPrimary)
                        }
                    }

                    AuthStep.OTP_VERIFICATION, AuthStep.FORGOT_PASSWORD_OTP -> {
                        Text(
                            text = "Verification Code",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AtmiyaPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Enter the 6-digit code sent to +91-$phoneNumber",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        OtpInput(
                            otpValue = otpValue,
                            onOtpChange = { 
                                otpValue = it
                                isOtpError = false
                                if (it.length == 6) {
                                    verifyOtp(it)
                                }
                            },
                            isError = isOtpError
                        )

                        if (isOtpError) {
                            Text(
                                text = "Invalid OTP. Please try again.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AtmiyaPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (ticks > 0) {
                                Text(
                                    text = "Resend in ${ticks}s",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                TextButton(onClick = { sendOtp(phoneNumber, isResend = true) }) {
                                    Text("Resend OTP", color = AtmiyaSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = { 
                                authStep = AuthStep.PHONE_INPUT 
                                otpValue = ""
                                isOtpError = false
                                isVerificationCompleted = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Phone Number", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    AuthStep.RESET_PASSWORD -> {
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AtmiyaPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Secure your account with a strong password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Password Field
                         OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) TablerIcons.EyeOff else TablerIcons.Eye
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null)
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Confirm Password Field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (confirmPasswordVisible) TablerIcons.EyeOff else TablerIcons.Eye
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(imageVector = image, contentDescription = null)
                                }
                            }
                        )

                        if (passwordError != null) {
                            Text(
                                text = passwordError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        SoftButton(
                            onClick = {
                                resetPassword()
                            },
                            text = "Reset Password",
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OtpInput(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    isError: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

    // Hidden TextField to handle input
    BasicTextField(
        value = otpValue,
        onValueChange = {
            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                onOtpChange(it)
            }
        },
        keyboardOptions = keyboardOptions,
        modifier = Modifier
            .focusRequester(focusRequester)
            .size(1.dp) 
            .alpha(0f)
    )

    // Visible Boxes
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { focusRequester.requestFocus() } // Make the whole row clickable to focus
    ) {
        for (i in 0 until 6) {
            val char = if (i < otpValue.length) otpValue[i].toString() else ""
            val isFocused = i == otpValue.length
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) AtmiyaPrimary else if (isError) MaterialTheme.colorScheme.error else Color.Gray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    
    // Request focus on start safely
    LaunchedEffect(Unit) {
        try {
            delay(300) 
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus errors
        }
    }
}

// Helper to find Activity from Context
fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

private fun signInWithPhoneAuthCredential(
    credential: PhoneAuthCredential,
    auth: FirebaseAuth,
    onSuccess: () -> Unit,
    context: android.content.Context,
    onFailure: (() -> Unit)? = null
) {
    auth.signInWithCredential(credential)
        .addOnCompleteListener(context.findActivity()!!) { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                if (task.exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(context, "Invalid code", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Verification failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
                onFailure?.invoke()
            }
        }
}
