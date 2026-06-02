package com.boardingbuddy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Senior-friendly palette: high contrast, calm greens.
private val Ink = Color(0xFF1D2B1F)
private val Paper = Color(0xFFF6F4EC)
private val Green = Color(0xFF2F7D4A)
private val GreenDark = Color(0xFF205536)
private val Blue = Color(0xFF2F6F8F)
private val Sky = Color(0xFFEAF2EC)
private val Amber = Color(0xFFD98A2B)

sealed class Screen {
    object Scan : Screen()
    object Manual : Screen()
    data class ConfirmGate(val pass: BoardingPass?) : Screen()
    data class Route(val pass: BoardingPass?, val route: GateRoute) : Screen()
}

class MainActivity : ComponentActivity() {
    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Blue)) {
                App()
            }
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
fun App() {
    var screen by remember { mutableStateOf<Screen>(Screen.Scan) }

    Surface(Modifier.fillMaxSize(), color = Paper) {
        when (val s = screen) {
            is Screen.Scan -> ScannerScreen(
                onScanned = { raw ->
                    val pass = Bcbp.parse(raw)
                    screen = Screen.ConfirmGate(pass)
                },
                onManualEntry = { screen = Screen.Manual }
            )

            is Screen.Manual -> ManualEntryScreen(
                onDone = { gate -> screen = Screen.Route(null, T3Router.route(gate)) },
                onBack = { screen = Screen.Scan }
            )

            is Screen.ConfirmGate -> ConfirmGateScreen(
                pass = s.pass,
                onConfirm = { gate -> screen = Screen.Route(s.pass, T3Router.route(gate)) },
                onRescan = { screen = Screen.Scan }
            )

            is Screen.Route -> RouteScreen(
                pass = s.pass,
                route = s.route,
                onRestart = { screen = Screen.Scan }
            )
        }
    }
}

@Composable
fun ConfirmGateScreen(pass: BoardingPass?, onConfirm: (String) -> Unit, onRescan: () -> Unit) {
    var gate by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        if (pass != null) {
            Text("Boarding pass scanned ✓", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = GreenDark)
            Spacer(Modifier.height(16.dp))
            InfoCard(pass)
        } else {
            Text("Couldn't read that barcode", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Amber)
            Spacer(Modifier.height(8.dp))
            Text("No problem — just enter your gate below.", fontSize = 20.sp, color = Ink)
        }
        Spacer(Modifier.height(28.dp))
        Text("Which gate are you flying from?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text("It's printed on your boarding pass, e.g. 27A", fontSize = 17.sp, color = Color(0xFF5F6B60))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = gate,
            onValueChange = { gate = it.uppercase() },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        BigButton("Show me the way →", Green, enabled = gate.isNotBlank()) { onConfirm(gate.trim()) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRescan) { Text("Scan again", fontSize = 18.sp, color = GreenDark) }
    }
}

@Composable
fun ManualEntryScreen(onDone: (String) -> Unit, onBack: () -> Unit) {
    var gate by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter your gate", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = GreenDark)
        Spacer(Modifier.height(8.dp))
        Text("From your boarding pass, e.g. 27A or 52", fontSize = 18.sp, color = Color(0xFF5F6B60))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = gate, onValueChange = { gate = it.uppercase() },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 30.sp),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        BigButton("Show me the way →", Green, enabled = gate.isNotBlank()) { onDone(gate.trim()) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("← Back to camera", fontSize = 18.sp, color = GreenDark) }
    }
}

@Composable
fun RouteScreen(pass: BoardingPass?, route: GateRoute, onRestart: () -> Unit) {
    var legIdx by remember { mutableStateOf(0) }
    val leg = route.legs[legIdx]
    val metersLeft = route.legs.drop(legIdx).sumOf { it.meters }
    val minsLeft = maxOf(1, Math.round(metersLeft / 60.0).toInt())
    val arrived = legIdx == route.legs.size - 1

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text("DELHI T3 · ${route.sectionLabel}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5F6B60))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(if (metersLeft == 0) "Arrived" else "$metersLeft m", "To gate")
            Stat(if (metersLeft == 0) "—" else "$minsLeft min", "Walk")
            Stat("${legIdx + 1}/${route.legs.size}", "Step")
        }
        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(leg.icon, fontSize = 48.sp)
                Spacer(Modifier.height(6.dp))
                Text(leg.say, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                Spacer(Modifier.height(8.dp))
                Text(leg.instruction, fontSize = 21.sp, color = Ink, lineHeight = 30.sp)
                Spacer(Modifier.height(14.dp))
                Text("👀  Look for: ${leg.landmark}", fontSize = 18.sp, color = Color(0xFF5F6B60))
                if (leg.hasBench) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().background(Sky, RoundedCornerShape(12.dp)).padding(14.dp)) {
                        Text("🪑  A bench is right here if you'd like to rest.", fontSize = 17.sp, color = Ink)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (arrived) {
            BigButton("I'm at the gate ✓", Green) { onRestart() }
        } else {
            BigButton("I've reached here — next", Blue) { legIdx++ }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            if (legIdx > 0) TextButton(onClick = { legIdx-- }) { Text("Previous", fontSize = 17.sp, color = GreenDark) }
            TextButton(onClick = onRestart) { Text("Start over", fontSize = 17.sp, color = GreenDark) }
        }
    }
}

@Composable
private fun InfoCard(pass: BoardingPass) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GreenDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("${pass.airline} ${pass.flightNumber}", fontSize = 16.sp, color = Color(0xCCFFFFFF))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pass.fromAirport, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("   ✈   ", fontSize = 20.sp, color = Color.White)
                Text(pass.toAirport, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text("${pass.passengerName}  ·  Seat ${pass.seat}", fontSize = 18.sp, color = Color(0xEEFFFFFF))
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label, fontSize = 13.sp, color = Color(0xFF5F6B60))
    }
}

@Composable
private fun BigButton(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().height(68.dp)
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
