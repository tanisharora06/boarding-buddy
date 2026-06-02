package com.boardingbuddy.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Full-screen camera that watches for a PDF417 boarding-pass barcode.
 * On the first successful decode it calls onScanned(rawBcbpString) once.
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
fun ScannerScreen(
    onScanned: (String) -> Unit,
    onManualEntry: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize()) {
        if (hasPermission) {
            CameraPreview(onScanned = onScanned, lifecycleOwner = lifecycleOwner)
            // Guidance overlay
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Point the camera at the barcode on your boarding pass",
                    fontSize = 22.sp,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
            Button(
                onClick = onManualEntry,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).height(64.dp).fillMaxWidth(0.9f)
            ) {
                Text("Type it in instead", fontSize = 20.sp)
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("We need camera access to scan your boarding pass.", fontSize = 22.sp)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.height(64.dp)) {
                    Text("Allow camera", fontSize = 20.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onManualEntry, modifier = Modifier.height(64.dp)) {
                    Text("Type it in instead", fontSize = 20.sp)
                }
            }
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
private fun CameraPreview(
    onScanned: (String) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val handled = remember { mutableStateOf(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_PDF417, Barcode.FORMAT_AZTEC, Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                processFrame(scanner, imageProxy) { value ->
                    if (!handled.value) {
                        handled.value = true
                        onScanned(value)
                    }
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(ctx))
        previewView
    }, modifier = Modifier.fillMaxSize())
}

@androidx.camera.core.ExperimentalGetImage
private fun processFrame(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onValue: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) { imageProxy.close(); return }
    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { onValue(it) }
        }
        .addOnCompleteListener { imageProxy.close() }
}
