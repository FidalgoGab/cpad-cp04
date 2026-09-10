package com.example.cp01

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationScreen()
        }
    }
}

// Classe de dados simples para armazenar o histórico
data class LocationRecord(val latitude: Double, val longitude: Double, val timestamp: String)

@Composable
fun LocationScreen() {
    val context = LocalContext.current

    // CAMADA DE ESTADO (A MEMÓRIA)
    // Variáveis preservadas via remember para sobreviver à re-renderização
    var locationMessage by remember { mutableStateOf("Nenhuma localização capturada ainda.") }
    var currentLatLon by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationHistory by remember { mutableStateOf(listOf<LocationRecord>()) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Função para registrar a hora atual
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // O PLANO B: Tratamento adaptativo da permissão
    val requestPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (isGranted) {
            locationMessage = "Permissão concedida. Buscando GPS..."
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatLon = Pair(location.latitude, location.longitude)
                        locationMessage = "GPS Capturado com sucesso!"

                        // Atualiza o histórico de estado
                        val newRecord = LocationRecord(location.latitude, location.longitude, getCurrentTime())
                        locationHistory = listOf(newRecord) + locationHistory
                    } else {
                        locationMessage = "Sinal de GPS indisponível. Tente abrir o Google Maps para forçar a atualização."
                    }
                }
            } catch (e: SecurityException) {
                locationMessage = "Erro de segurança ao acessar GPS."
            }
        } else {
            // Degradação graciosa: Interface reage sem quebrar
            locationMessage = "Permissão negada. O aplicativo está limitado."
            Toast.makeText(
                context,
                "A permissão de localização é necessária para o app funcionar corretamente.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Função para compartilhar usando Intent Nativa do Android
    fun shareCurrentLocation() {
        val latLon = currentLatLon
        if (latLon != null) {
            val shareText = "Veja minha localização atual: https://maps.google.com/?q=${latLon.first},${latLon.second}"
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Compartilhar via")
            context.startActivity(shareIntent)
        } else {
            Toast.makeText(context, "Capture o GPS primeiro antes de compartilhar.", Toast.LENGTH_SHORT).show()
        }
    }

    // CAMADA DE INTERFACE (A FORMA)
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho
        Text(
            text = "CP01 - Rastreador GPS",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Painel de Status Atual
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Status Atual:", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = locationMessage, color = Color.DarkGray)

            currentLatLon?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Lat: ${it.first}", fontWeight = FontWeight.Bold)
                Text(text = "Lon: ${it.second}", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botões de Ação (Linha para organizar lado a lado)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    locationMessage = "Buscando..."
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            currentLatLon = Pair(location.latitude, location.longitude)
                            locationMessage = "GPS Atualizado!"
                            val newRecord = LocationRecord(location.latitude, location.longitude, getCurrentTime())
                            locationHistory = listOf(newRecord) + locationHistory
                        } else {
                            locationMessage = "Sinal de GPS indisponível."
                        }
                    }
                } else {
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }) {
                Text("Capturar GPS")
            }

            Button(
                onClick = { shareCurrentLocation() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Compartilhar")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = Color.LightGray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Lista de Histórico (Substituindo o ListView tradicional)
        Text(
            text = "Histórico de Buscas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (locationHistory.isEmpty()) {
            Text(
                text = "A lista está vazia.",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(locationHistory) { record ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = "Horário: ${record.timestamp}", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "Lat: ${record.latitude} | Lon: ${record.longitude}", fontSize = 14.sp)
                        Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}