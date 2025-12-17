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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging

enum class LoginMode { OTP, PASSWORD }
enum class AuthStep { PHONE_INPUT, OTP_VERIFICATION, FORGOT_PASSWORD_OTP, RESET_PASSWORD }


const val USE_DYNAMIC_OTP = true

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
    val scope = rememberCoroutineScope()
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

    // Dynamic OTP Helper Functions
    // Dynamic OTP Helper Functions
    // Cached FCM Token
    var cachedFcmToken by remember { mutableStateOf<String?>(null) }

    // Eagerly fetch FCM token
    LaunchedEffect(Unit) {
        try {
            cachedFcmToken = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            android.util.Log.d("Auth", "FCM Token cached: $cachedFcmToken")
        } catch (e: Exception) {
            android.util.Log.e("Auth", "Failed to cache FCM token", e)
        }
    }

    fun sendDynamicOtp(phone: String, isResend: Boolean = false) { // Accepted param
        scope.launch {
            isLoading = true
            try {
                // Use cached token if available, otherwise try fetch with timeout
                val fcmToken = cachedFcmToken ?: try {
                    kotlinx.coroutines.withTimeoutOrNull(3000) {
                        FirebaseMessaging.getInstance().token.await()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Auth", "Failed to get FCM token for fallback", e)
                    null
                }
                
                if (fcmToken == null) {
                     android.util.Log.w("Auth", "Proceeding without FCM token")
                }

                // Call Cloud Function
                val functions = FirebaseFunctions.getInstance()
                val data = hashMapOf(
                    "phone" to "+91$phone",
                    "fcmToken" to fcmToken
                )
                
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    functions.getHttpsCallable("sendOtp")
                        .call(data)
                        .await()
                }

                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    // isOtpSent = true // <-- REMOVED, not in LoginScreen state
                    
                    if (authStep == AuthStep.PHONE_INPUT) {
                         authStep = AuthStep.OTP_VERIFICATION
                    }
                    
                    ticks = 60L
                    isTimerRunning = true
                    Toast.makeText(context, "OTP Sent", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    val errorMsg = if (e is com.google.firebase.functions.FirebaseFunctionsException) {
                        if (e.code == com.google.firebase.functions.FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED) {
                            "Please wait a moment before requesting another OTP."
                        } else {
                            // Only show safe messages or generic error
                            e.message ?: "An error occurred. Please try again."
                        }
                    } else {
                        e.message ?: "Unknown Error"
                    }
                    android.util.Log.e("Auth", "Dynamic OTP Send Failed: $errorMsg", e)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun verifyDynamicOtp(otp: String) {
        isLoading = true
        val start = System.currentTimeMillis()
        
        scope.launch {
             try {
                val functions = FirebaseFunctions.getInstance()
                val data = hashMapOf(
                    "phone" to "+91$phoneNumber",
                    "otp" to otp
                )
                
                val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                     functions.getHttpsCallable("verifyOtp").call(data).await()
                }
                
                val resultData = result.data as? Map<String, Any>
                val customToken = resultData?.get("token") as? String
                
                if (customToken != null) {
                    // Sign in with the custom token
                    auth.signInWithCustomToken(customToken).await()
                    
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isLoading = false
                        isVerificationCompleted = true
                        android.util.Log.d("Perf", "Dynamic OTP Verification took ${System.currentTimeMillis() - start} ms")
                        
                        if (authStep == AuthStep.FORGOT_PASSWORD_OTP) {
                            authStep = AuthStep.RESET_PASSWORD
                        } else {
                            onLoginSuccess()
                        }
                    }
                } else {
                    throw Exception("Invalid response from server")
                }
             } catch (e: Exception) {
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    isOtpError = true
                    otpValue = "" // Clear OTP on error
                    
                    val errorMsg = if (e is com.google.firebase.functions.FirebaseFunctionsException) {
                        "Code: ${e.code}, Msg: ${e.message}"
                    } else {
                        e.message ?: "Unknown Verify Error"
                    }
                    
                    android.util.Log.e("Auth", "Dynamic OTP Verification Failed: $errorMsg", e)
                    Toast.makeText(context, "Verify Failed: $errorMsg", Toast.LENGTH_LONG).show()
                 }
             }
        }
    }

    fun sendOtp(phone: String, isResend: Boolean = false) {
        if (isLoading && !isResend) return // Prevent multiple clicks
        
        // Dynamic OTP Logic (Skip for Admin 9999999999 which uses static flow)
        if (USE_DYNAMIC_OTP && phone != "9999999999") {
            sendDynamicOtp(phone, isResend)
            return
        }
        
        val activity = context.findActivity()
        if (activity == null) {
            Toast.makeText(context, "Error: Cannot find Activity", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Static/Firebase Flow (Existing)
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
        // Dynamic OTP Check (Skip for Admin 9999999999 or Admin OTP 771993)
        // Admin phone logic is inside static flow, so we just check phone number here.
        val cleanPhone = phoneNumber.replace("\\s".toRegex(), "").replace("-", "")
        
        if (USE_DYNAMIC_OTP && cleanPhone != "9999999999") {
             verifyDynamicOtp(otp)
             return
        }

        val vid = verificationId ?: return
        isLoading = true
        val start = System.currentTimeMillis()
        
        
        // HARDCODED ADMIN CHECK: Admin phone is 9999999999, OTP is 1993
        // For admin, we map OTP 1993 → use Firebase's actual verification with an internal OTP
        val isAdminPhone = cleanPhone == "9999999999"
        
        if (isAdminPhone) {
            if (otp == "1993") {
                android.util.Log.d("LoginScreen", "Admin OTP 1993 accepted for phone 9999999999")
                // Admin uses 1993, but we need to actually sign in via Firebase
                // Use Firebase test phone number OTP (123456) internally since admin phone is registered as test number
                val actualFirebaseOtp = "123456" // Firebase test number OTP for 9999999999
                val credential = PhoneAuthProvider.getCredential(vid, actualFirebaseOtp)
                
                signInWithPhoneAuthCredential(credential, auth, {
                    isLoading = false
                    isVerificationCompleted = true
                    android.util.Log.d("LoginScreen", "Admin login successful")
                    
                    if (authStep == AuthStep.FORGOT_PASSWORD_OTP) {
                        authStep = AuthStep.RESET_PASSWORD
                    } else {
                        onLoginSuccess()
                    }
                }, context) {
                    isLoading = false
                    isOtpError = true
                    otpValue = ""
                    android.util.Log.e("LoginScreen", "Admin Firebase auth failed")
                }
                return
            } else {
                // Admin phone with wrong OTP - REJECT
                android.util.Log.d("LoginScreen", "Admin phone but wrong OTP: $otp (expected 1993)")
                scope.launch {
                    isLoading = false
                    isOtpError = true
                    otpValue = ""
                    android.widget.Toast.makeText(context, "Invalid OTP", android.widget.Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        
        // Non-admin: Normal OTP verification via Firebase (for users with OTP 123456 via test numbers)
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
        if (!isPhoneValid && !phoneNumber.contains("@")) { // Allow email input directly too? For now stick to phone logic or email
             // The UI asks for "Mobile Number" but label implies it.
             // If input is email, treat as email. If phone, synthetic.
        }
        
        // Handling both Phone (synthetic) and Email input
        val emailInput = if (phoneNumber.contains("@")) phoneNumber else getSyntheticEmail(phoneNumber)
        
        if (password.isEmpty()) {
            passwordError = "Enter password"
            return
        }

        // Logic Switch: For Bulk Users, we rely on Phone Number lookup.
        // We will pass the input (assumed to be phone if pure digits, else email)
        // Ideally user enters Phone Number.
        val isPhone = phoneNumber.all { it.isDigit() } && phoneNumber.length == 10
        val loginIdentifier = if (isPhone) getSyntheticEmail(phoneNumber) else phoneNumber  // Firebase auth expects email

        isLoading = true
        // Try standard auth first
        auth.signInWithEmailAndPassword(loginIdentifier, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isLoading = false
                    onLoginSuccess()
                } else {
                    // Check if failure is due to User Not Found AND Password is "AIF@2025"
                    val exception = task.exception
                    // We can't check password validity if user doesn't exist.
                    // BUT, if the user enters the magic password, we check Firestore.
                    
                    if (password == "AIF@2025" || password == "AIF@123") {
                         // Attempt Lazy Claim
                         val firestoreRepo = com.atmiya.innovation.repository.FirestoreRepository()
                                                  scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                              try {
                                  // Look up by Phone Number (Original Input) - PUBLIC READ, no auth needed
                                  val existingUid = if (isPhone) firestoreRepo.getBulkInviteUid(phoneNumber) else null
                                  
                                  if (existingUid != null) {
                                     // Found a pending bulk user!
                                     // Create Auth Account FIRST (before reading protected user data)
                                     try {
                                         val authResult = auth.createUserWithEmailAndPassword(loginIdentifier, password).await()
                                         val newUid = authResult.user?.uid
                                         
                                         if (newUid != null) {
                                             // Migrate Firestore Data (now authenticated)
                                             firestoreRepo.linkBulkUserToAuth(existingUid, newUid)
                                             
                                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                 isLoading = false
                                                 Toast.makeText(context, "Account Activated!", Toast.LENGTH_SHORT).show()
                                                 onLoginSuccess()
                                             }
                                         }
                                     } catch (e: Exception) {
                                          android.util.Log.e("LoginScreen", "Activation failed during linkBulkUserToAuth", e)
                                          val detailedError = "[DEBUG] Link Failed:\n${e::class.simpleName}: ${e.message}\nCause: ${e.cause?.message ?: "None"}"
                                          withContext(kotlinx.coroutines.Dispatchers.Main) {
                                             isLoading = false
                                             passwordError = detailedError
                                         }
                                     }
                                 } else {
                                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                                         isLoading = false
                                         passwordError = "Login failed: User not found" // Or standard error
                                     }
                                 }
                             } catch (e: Exception) {
                                 android.util.Log.e("LoginScreen", "Error during bulk user lookup or claim", e)
                                 val detailedError = "[DEBUG] Lookup/Claim:\n${e::class.simpleName}: ${e.message}\nCause: ${e.cause?.message ?: "None"}"
                                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                                     isLoading = false
                                     passwordError = detailedError
                                 }
                             }
                         }
                    } else {
                        isLoading = false
                        passwordError = "Login failed: ${task.exception?.message}"
                    }
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
            val logoRes = if (androidx.compose.foundation.isSystemInDarkTheme()) R.drawable.netfund_logo_dark else R.drawable.netfund_logo
            Image(
                painter = painterResource(id = logoRes),
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
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[if (loginMode == LoginMode.OTP) 0 else 1]),
                                color = MaterialTheme.colorScheme.secondary
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
                                    color = if (loginMode == LoginMode.OTP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                                    color = if (loginMode == LoginMode.PASSWORD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = MaterialTheme.colorScheme.primary,
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
                                    color = MaterialTheme.colorScheme.primary
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
                                    password = it.replace("\n", "") // Prevent newlines
                                    passwordError = null
                                },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = TablerIcons.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = { loginWithPassword() }
                                ),
                                isError = passwordError != null,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                                    Text("Forgot Password?", color = MaterialTheme.colorScheme.secondary)
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
                        Text(
                            text = "Enable push notifications to receive OTP if SMS fails.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))
            
                        TextButton(
                            onClick = onSignupClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("New to Netfund? Sign Up", color = MaterialTheme.colorScheme.primary)
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
                        
                        val otpLength = if (USE_DYNAMIC_OTP) 4 else 6
                        Text(
                            text = "Enter the $otpLength-digit code sent to +91-$phoneNumber",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        OtpInput(
                            otpValue = otpValue,
                            otpLength = otpLength,
                            onOtpChange = { 
                                otpValue = it
                                isOtpError = false
                                if (it.length == otpLength) {
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
    otpLength: Int = 6,
    onOtpChange: (String) -> Unit,
    isError: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

    // Hidden TextField to handle input
    BasicTextField(
        value = otpValue,
        onValueChange = {
            if (it.length <= otpLength && it.all { char -> char.isDigit() }) {
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
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { focusRequester.requestFocus() } // Make the whole row clickable to focus
    ) {
        for (i in 0 until otpLength) {
            val char = if (i < otpValue.length) otpValue[i].toString() else ""
            val isFocused = i == otpValue.length
            
            Box(
                modifier = Modifier
                    .size(50.dp) // Fixed size
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
