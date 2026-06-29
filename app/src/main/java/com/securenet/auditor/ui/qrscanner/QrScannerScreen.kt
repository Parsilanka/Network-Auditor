package com.securenet.auditor.ui.qrscanner

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.securenet.auditor.network.QrWifiParser
import com.securenet.auditor.network.WifiConnectionManager
import com.securenet.auditor.ui.wifi.WifiScannerViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    navController: NavController,
    viewModel: QrScannerViewModel
) {
    val scannedResult by viewModel.scannedResult.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    if (!cameraPermission.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color(0xFF00BFA5), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Camera permission required", style = MaterialTheme.typography.titleMedium)
            Text("Grant camera access to scan Wi-Fi QR codes", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Grant Permission") }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        if (scannedResult == null) {
            CameraPreviewWithQrScanner(onQrScanned = { viewModel.processQrCode(it) })

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0x880D1117)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("QR Wi-Fi Scanner", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier.size(250.dp).align(Alignment.CenterHorizontally).border(
                        width = 2.dp,
                        color = Color(0xFF00BFA5),
                        shape = RoundedCornerShape(16.dp)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Point camera at a Wi-Fi QR code", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().background(Color(0x880D1117)).padding(16.dp))

                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            QrScanResultScreen(
                credentials = scannedResult!!,
                connectionState = connectionState,
                onConnect = { viewModel.connectToScannedNetwork(it) },
                onScanAgain = { viewModel.clearResult() },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun CameraPreviewWithQrScanner(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val barcodeScanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    if (!hasScanned) {
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { value ->
                                        if (!hasScanned) {
                                            hasScanned = true
                                            onQrScanned(value)
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e("QrScanner", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
fun QrScanResultScreen(
    credentials: QrWifiParser.WifiCredentials,
    connectionState: WifiScannerViewModel.ConnectionState,
    onConnect: (QrWifiParser.WifiCredentials) -> Unit,
    onScanAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = Color(0xFF00BFA5), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Wi-Fi QR Code Scanned!", color = Color(0xFFE6EDF3), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Network Details", color = Color(0xFF8B949E), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Wifi, contentDescription = null, tint = Color(0xFF00BFA5), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Network Name (SSID)", color = Color(0xFF8B949E), style = MaterialTheme.typography.labelSmall)
                        Text(credentials.ssid, color = Color(0xFF00BFA5), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF30363D))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Security", color = Color(0xFF8B949E))
                    val (chipColor, chipText) = when (credentials.securityType) {
                        WifiConnectionManager.WifiSecurityType.OPEN -> Color(0xFF4CAF50) to "OPEN"
                        WifiConnectionManager.WifiSecurityType.WPA3 -> Color(0xFF00BFA5) to "WPA3"
                        WifiConnectionManager.WifiSecurityType.WPA2 -> Color(0xFF2196F3) to "WPA2"
                        WifiConnectionManager.WifiSecurityType.WEP -> Color(0xFFF44336) to "WEP"
                        else -> Color(0xFFFFC107) to "WPA"
                    }
                    Surface(color = chipColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(chipText, color = chipColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (!credentials.password.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Password", color = Color(0xFF8B949E))
                        Text("••••••••", color = Color(0xFFE6EDF3), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConnectionStatusBanner(connectionState)

        Spacer(modifier = Modifier.weight(1f))

        if (connectionState !is WifiScannerViewModel.ConnectionState.Connected) {
            Button(
                onClick = { onConnect(credentials) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5), contentColor = Color(0xFF003D36))
            ) {
                Icon(Icons.Outlined.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect to ${credentials.ssid}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Another QR Code")
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDismiss) { Text("Done", color = Color(0xFF8B949E)) }
    }
}

@Composable
fun ConnectionStatusBanner(state: WifiScannerViewModel.ConnectionState) {
    when (state) {
        is WifiScannerViewModel.ConnectionState.Connecting -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF00BFA5), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connecting to ${state.ssid}...", color = Color(0xFFE6EDF3))
            }
        }
        is WifiScannerViewModel.ConnectionState.Connected -> {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2D1A))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✓ Connected to ${state.ssid}", color = Color(0xFF56D364), fontWeight = FontWeight.Bold)
                }
            }
        }
        is WifiScannerViewModel.ConnectionState.Failed -> {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3D0C0C))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Error, contentDescription = null, tint = Color(0xFFF44336))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(state.message, color = Color(0xFFFF7B72))
                }
            }
        }
        else -> {}
    }
}
