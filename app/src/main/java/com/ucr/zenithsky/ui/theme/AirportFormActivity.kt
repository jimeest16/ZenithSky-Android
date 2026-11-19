package com.ucr.zenithsky

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ucr.zenithsky.data.OperacionLog
import com.ucr.zenithsky.data.ZenithDatabase
import com.ucr.zenithsky.model.AirportRequest
import com.ucr.zenithsky.model.parseApiError
import com.ucr.zenithsky.network.RetrofitClient
import com.ucr.zenithsky.ui.theme.ZenithSkyAppTheme
import kotlinx.coroutines.launch

// CLASE DE LA ACTIVITY DE FORMULARIO
class AirportFormActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = ZenithDatabase.getDatabase(this).zenithDao()
        val editId = intent.getIntExtra("AIRPORT_ID", 0)
        val initialIata = intent.getStringExtra("IATA") ?: ""
        val initialIcao = intent.getStringExtra("ICAO") ?: ""
        val initialName = intent.getStringExtra("NAME") ?: ""

        setContent {
            ZenithSkyAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var iata by remember { mutableStateOf(initialIata) }
                var icao by remember { mutableStateOf(initialIcao) }
                var name by remember { mutableStateOf(initialName) }
                var guardando by remember { mutableStateOf(false) }

                fun guardar() {
                    val iataClean = iata.trim().uppercase()
                    val icaoClean = icao.trim().uppercase()
                    val nameClean = name.trim()

                    if (iataClean.length != 3 || !iataClean.all { it.isLetter() }) {
                        Toast.makeText(context, "El IATA debe tener exactamente 3 letras", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (icaoClean.length != 4 || !icaoClean.all { it.isLetter() }) {
                        Toast.makeText(context, "El ICAO debe tener exactamente 4 letras", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (nameClean.isBlank()) {
                        Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                        return
                    }

                    guardando = true
                    scope.launch {
                        try {
                            val req =
                                AirportRequest(iata = iataClean, icao = icaoClean, name = nameClean)
                            if (editId == 0) {
                                val res = RetrofitClient.api.createAirport(req)
                                if (res.isSuccessful) {
                                    dao.insertarLog(
                                        OperacionLog(
                                            entidad = "Aeropuerto",
                                            accion = "Creado",
                                            detalle = "$iataClean - $nameClean"
                                        )
                                    )
                                    Toast.makeText(context, "Aeropuerto creado", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Toast.makeText(context, parseApiError(res), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val res = RetrofitClient.api.updateAirport(editId, req)
                                if (res.isSuccessful) {
                                    dao.insertarLog(
                                        OperacionLog(
                                            entidad = "Aeropuerto",
                                            accion = "Actualizado",
                                            detalle = "$iataClean - $nameClean"
                                        )
                                    )
                                    Toast.makeText(context, "Aeropuerto actualizado", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Toast.makeText(context, parseApiError(res), Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            guardando = false
                        }
                    }
                }

                FormularioAeropuertoScreen(
                    esEdicion = editId != 0,
                    airportId = editId,
                    iata = iata,
                    icao = icao,
                    name = name,
                    guardando = guardando,
                    onIataChange = { if (it.length <= 3) iata = it.uppercase() },
                    onIcaoChange = { if (it.length <= 4) icao = it.uppercase() },
                    onNameChange = { name = it },
                    onGuardarClick = { guardar() },
                    onVolverClick = { finish() }
                )
            }
        }
    }
}

// PANTALLA DE FORMULARIO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAeropuertoScreen(
    esEdicion: Boolean,
    airportId: Int,
    iata: String,
    icao: String,
    name: String,
    guardando: Boolean,
    onIataChange: (String) -> Unit,
    onIcaoChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onGuardarClick: () -> Unit,
    onVolverClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (esEdicion) "Editar Aeropuerto" else "Nuevo Aeropuerto",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (esEdicion) "Aeropuerto ID #$airportId" else "Registro de terminal aérea",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Text(
                            text = "Datos del aeropuerto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (iata.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = iata,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (icao.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = icao,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = iata,
                        onValueChange = onIataChange,
                        label = { Text("Código IATA (3 letras, ej. SJO)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = icao,
                        onValueChange = onIcaoChange,
                        label = { Text("Código ICAO (4 letras, ej. MROC)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Nombre del Aeropuerto") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = onGuardarClick,
                enabled = !guardando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUARDANDO...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}