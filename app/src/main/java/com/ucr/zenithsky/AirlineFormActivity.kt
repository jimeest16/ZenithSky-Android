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
import androidx.compose.material.icons.filled.Business
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
import com.ucr.zenithsky.model.AirlineRequest
import com.ucr.zenithsky.model.Airport
import com.ucr.zenithsky.model.parseApiError
import com.ucr.zenithsky.network.RetrofitClient
import com.ucr.zenithsky.ui.theme.ZenithSkyAppTheme
import kotlinx.coroutines.launch

// CLASE DE LA ACTIVITY DE FORMULARIO
class AirlineFormActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = ZenithDatabase.getDatabase(this).zenithDao()
        val editId = intent.getIntExtra("AIRLINE_ID", 0)
        val initialIata = intent.getStringExtra("IATA") ?: ""
        val initialName = intent.getStringExtra("NAME") ?: ""
        val initialBaseAirportId = intent.getIntExtra("BASE_AIRPORT_ID", 0)

        setContent {
            ZenithSkyAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var iata by remember { mutableStateOf(initialIata) }
                var name by remember { mutableStateOf(initialName) }
                var aeropuertos by remember { mutableStateOf<List<Airport>>(emptyList()) }
                var aeropuertoSeleccionado by remember { mutableStateOf<Airport?>(null) }
                var guardando by remember { mutableStateOf(false) }

                // Carga catálogo de aeropuertos para el Dropdown
                LaunchedEffect(Unit) {
                    try {
                        val res = RetrofitClient.api.getAirports()
                        if (res.isSuccessful) {
                            val lista = res.body() ?: emptyList()
                            aeropuertos = lista
                            if (initialBaseAirportId != 0) {
                                aeropuertoSeleccionado = lista.find { it.airportId == initialBaseAirportId }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al cargar aeropuertos", Toast.LENGTH_SHORT).show()
                    }
                }

                fun guardar() {
                    val iataClean = iata.trim().uppercase()
                    val nameClean = name.trim()
                    val baseId = aeropuertoSeleccionado?.airportId ?: 0

                    if (iataClean.length != 2) {
                        Toast.makeText(context, "El código IATA debe tener exactamente 2 caracteres", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (nameClean.isBlank()) {
                        Toast.makeText(context, "El nombre de la aerolínea es obligatorio", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (baseId <= 0) {
                        Toast.makeText(context, "Seleccione un aeropuerto base", Toast.LENGTH_SHORT).show()
                        return
                    }

                    guardando = true
                    scope.launch {
                        try {
                            val req = AirlineRequest(
                                iata = iataClean,
                                airlinename = nameClean,
                                baseAirport = baseId
                            )
                            if (editId == 0) {
                                val res = RetrofitClient.api.createAirline(req)
                                if (res.isSuccessful) {
                                    dao.insertarLog(
                                        OperacionLog(
                                            entidad = "Aerolínea",
                                            accion = "Creada",
                                            detalle = "$iataClean - $nameClean"
                                        )
                                    )
                                    Toast.makeText(context, "Aerolínea creada", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Toast.makeText(context, parseApiError(res), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val res = RetrofitClient.api.updateAirline(editId, req)
                                if (res.isSuccessful) {
                                    dao.insertarLog(
                                        OperacionLog(
                                            entidad = "Aerolínea",
                                            accion = "Actualizada",
                                            detalle = "$iataClean - $nameClean"
                                        )
                                    )
                                    Toast.makeText(context, "Aerolínea actualizada", Toast.LENGTH_SHORT).show()
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

                FormularioAerolineaScreen(
                    esEdicion = editId != 0,
                    airlineId = editId,
                    iata = iata,
                    name = name,
                    aeropuertos = aeropuertos,
                    aeropuertoSeleccionado = aeropuertoSeleccionado,
                    guardando = guardando,
                    onIataChange = { if (it.length <= 2) iata = it.uppercase() },
                    onNameChange = { name = it },
                    onAeropuertoSelect = { aeropuertoSeleccionado = it },
                    onGuardarClick = { guardar() },
                    onVolverClick = { finish() }
                )
            }
        }
    }
}

// PANTALLA DE FORMULARIO CON SELECTOR DESPLEGABLE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAerolineaScreen(
    esEdicion: Boolean,
    airlineId: Int,
    iata: String,
    name: String,
    aeropuertos: List<Airport>,
    aeropuertoSeleccionado: Airport?,
    guardando: Boolean,
    onIataChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onAeropuertoSelect: (Airport) -> Unit,
    onGuardarClick: () -> Unit,
    onVolverClick: () -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (esEdicion) "Editar Aerolínea" else "Nueva Aerolínea",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (esEdicion) "Aerolínea ID #$airlineId" else "Registro de operador aéreo",
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
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Text(
                            text = "Datos de la aerolínea",
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
                    }

                    OutlinedTextField(
                        value = iata,
                        onValueChange = onIataChange,
                        label = { Text("Código IATA (2 letras, ej. ZS)") },
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
                        label = { Text("Nombre de la Aerolínea") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandido,
                        onExpandedChange = { expandido = !expandido }
                    ) {
                        OutlinedTextField(
                            value = aeropuertoSeleccionado?.let { "${it.iata} - ${it.name}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Aeropuerto Base") },
                            placeholder = { Text("Selecciona un aeropuerto base") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandido,
                            onDismissRequest = { expandido = false }
                        ) {
                            aeropuertos.forEach { airport ->
                                DropdownMenuItem(
                                    text = { Text("${airport.iata} - ${airport.name}") },
                                    onClick = {
                                        onAeropuertoSelect(airport)
                                        expandido = false
                                    }
                                )
                            }
                        }
                    }
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