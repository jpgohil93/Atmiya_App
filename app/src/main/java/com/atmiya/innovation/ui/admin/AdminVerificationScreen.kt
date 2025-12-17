package com.atmiya.innovation.ui.admin

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info // Replaced QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.atmiya.innovation.data.Startup
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftButton
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.components.SoftScaffold
import com.atmiya.innovation.ui.components.SoftTextField
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Scan, 1: Phone
    val tabs = listOf("Scan QR", "Phone Search")
    
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    var scannedUid by remember { mutableStateOf<String?>(null) }
    var foundStartup by remember { mutableStateOf<Startup?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Function to fetch startup details
    fun fetchStartup(uid: String) {
        isLoading = true
        errorMessage = null
        successMessage = null
        scope.launch {
            try {
                val startup = repository.getStartup(uid)
                if (startup != null) {
                    foundStartup = startup
                    scannedUid = uid
                } else {
                    errorMessage = "Startup not found for ID: $uid"
                }
            } catch (e: Exception) {
                errorMessage = "Error fetching startup: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Function to verify startup
    fun verifyStartup() {
        val uid = scannedUid ?: return
        isLoading = true
        scope.launch {
            try {
                repository.updateStartupVerification(
                    uid = uid,
                    isVerified = true,
                    adminId = auth.currentUser?.uid ?: "admin"
                )
                successMessage = "Startup Verified Successfully!"
                foundStartup = foundStartup?.copy(isVerified = true)
            } catch (e: Exception) {
                errorMessage = "Verification failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    SoftScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Verify Startup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            selectedTab = index 
                            scannedUid = null
                            foundStartup = null
                            errorMessage = null
                            successMessage = null
                        },
                        text = { Text(title) },
                        icon = { 
                            Icon(
                                if (index == 0) Icons.Default.Info else Icons.Default.Search, 
                                contentDescription = null,
                                tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (foundStartup == null) {
                    when (selectedTab) {
                        0 -> QRScannerView(onCodeScanned = { code -> fetchStartup(code) })
                        1 -> PhoneSearchView(
                            onSearch = { phone ->
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        val user = repository.getUserByPhone(phone)
                                        if (user != null) {
                                            fetchStartup(user.uid)
                                        } else {
                                            errorMessage = "User not found with phone: $phone"
                                            isLoading = false
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error searching user: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // Startup Details View
                    StartupDetailsView(
                        startup = foundStartup!!,
                        onVerify = { verifyStartup() },
                        onCancel = { foundStartup = null; scannedUid = null },
                        isLoading = isLoading
                    )
                }

                if (isLoading && foundStartup == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                
                if (errorMessage != null) {
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        action = { TextButton(onClick = { errorMessage = null }) { Text("Dismiss") } }
                    ) { Text(errorMessage!!) }
                }
                
                if (successMessage != null) {
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer, // Or success container if available
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) { Text(successMessage!!) }
                }
            }
        }
    }
}

@Composable
fun QRScannerView(onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            val scanner = BarcodeScanning.getClient()
                            
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { code ->
                                            onCodeScanned(code)
                                            // Stop analyzing after success to prevent multiple triggers
                                            // In a real app, might want to pause/resume
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("QRScanner", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .size(250.dp)
                .background(Color.Transparent, RoundedCornerShape(16.dp))
                .padding(2.dp)
            ) {
                // Draw corners or frame here if needed
                Text("Align QR Code within frame", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-30).dp))
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required to scan QR codes.")
        }
    }
}

@Composable
fun PhoneSearchView(onSearch: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter Startup Phone Number", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        SoftTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number (e.g., +91...)",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        SoftButton(
            text = "Search",
            onClick = { if (phone.isNotBlank()) onSearch(phone) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StartupDetailsView(
    startup: Startup,
    onVerify: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Startup Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailRow("Name", startup.startupName)
                DetailRow("Sector", startup.sector)
                DetailRow("Stage", startup.stage)
                DetailRow("Track", startup.track)
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.titleMedium)
                    if (startup.isVerified) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(" Verified", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Pending", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!startup.isVerified) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Cancel")
                }

                SoftButton(
                    text = if (isLoading) "Verifying..." else "Verify",
                    onClick = onVerify,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }
        } else {
            Text("This startup is already verified.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Scan Another")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = "$label: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
        Text(text = value)
    }
}
