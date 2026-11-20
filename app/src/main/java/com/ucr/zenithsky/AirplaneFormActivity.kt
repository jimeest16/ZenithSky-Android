package com.ucr.zenithsky

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ucr.zenithsky.entity.OperacionLog
import com.ucr.zenithsky.data.ZenithDatabase
import com.ucr.zenithsky.model.Airline
import com.ucr.zenithsky.model.AirplaneRequest
import com.ucr.zenithsky.model.AirplaneType
import com.ucr.zenithsky.model.parseApiError
import com.ucr.zenithsky.network.RetrofitClient
import com.ucr.zenithsky.ui.theme.ZenithSkyAppTheme
import kotlinx.coroutines.launch

// CLASE DE LA ACTIVITY DE FORMULARIO
class AirplaneFormActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = ZenithDatabase.getDatabase(this).zenithDao()
        val editId = intent.getIntExtra("AIRPLANE_ID", 0)
        val initialCapacity = intent.getIntExtra("CAPACITY", 0)
        val initialTypeId = intent.getIntExtra("TYPE_ID", 0)
        val initialAirlineId = intent.getIntExtra("AIRLINE_ID", 0)

        setContent {
            ZenithSkyAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var capacityTexto by remember { mutableStateOf(if (initialCapacity > 0) initialCapacity.toString() else "") }
                var tiposAvion by remember { mutableStateOf<List<AirplaneType>>(emptyList()) }
                var aerolineas by remember { mutableStateOf<List<Airline>>(emptyList()) }

                var tipoSeleccionado by remember { mutableStateOf<AirplaneType?>(null) }
                var aerolineaSeleccionada by remember { mutableStateOf<Airline?>(null) }
                var guardando by remember { mutableStateOf(false) }

                // Carga simultánea de catálogos para los dos Dropdowns
                LaunchedEffect(Unit) {
                    try {
                        val resTipos = RetrofitClient.api.getAirplaneTypes()
                        if (resTipos.isSuccessful) {
                            val lista = resTipos.body() ?: emptyList()
                            tiposAvion = lista
                            if (initialTypeId != 0) tipoSeleccionado = lista.find { it.typeId == initialTypeId }
                        }
                        val resAerolineas = RetrofitClient.api.getAirlines()
                        if (resAerolineas.isSuccessful) {
                            val lista = resAerolineas.body() ?: emptyList()
                            aerolineas = lista
                            if (initialAirlineId != 0) aerolineaSeleccionada = lista.find { it.airlineId == initialAirlineId }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al cargar catálogos", Toast.LENGTH_SHORT).show()
                    }
                }

                fun guardar() {
                    val cap = capacityTexto.trim().toIntOrNull() ?: 0
                    val tId = tipoSeleccionado?.typeId ?: 0
                    val aId = aerolineaSeleccionada?.airlineId ?: 0

                    if (cap <= 0) {
                        Toast.makeText(context, "La capacidad debe ser un número mayor a 0", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (tId <= 0) {
                        Toast.makeText(context, "Seleccione un tipo de avión", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (aId <= 0) {
                        Toast.makeText(context, "Seleccione una aerolínea", Toast.LENGTH_SHORT).show()
                        return
                    }

                    guardando = true
                    scope.launch {
                        try {
                            val req = AirplaneRequest(capacity = cap, typeId = tId, airlineId = aId)
                            val detalleLog = "${tipoSeleccionado?.identifier} (${aerolineaSeleccionada?.airlineName}) - $cap pax"

                            if (editId == 0) {
                                val res = RetrofitClient.api.createAirplane(req)
                                if (res.isSuccessful) {
                                    dao.insertarLog(OperacionLog(entidad = "Avión", accion = "Creado", detalle = detalleLog))
                                    Toast.makeText(context, "Avión registrado", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Toast.makeText(context, parseApiError(res), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val res = RetrofitClient.api.updateAirplane(editId, req)
                                if (res.isSuccessful) {
                                    dao.insertarLog(OperacionLog(entidad = "Avión", accion = "Actualizado", detalle = "ID #$editId: $detalleLog"))
                                    Toast.makeText(context, "Avión actualizado", Toast.LENGTH_SHORT).show()
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

                FormularioAvionScreen(
                    esEdicion = editId != 0,
                    airplaneId = editId,
                    capacityTexto = capacityTexto,
                    tiposAvion = tiposAvion,
                    aerolineas = aerolineas,
                    tipoSeleccionado = tipoSeleccionado,
                    aerolineaSeleccionada = aerolineaSeleccionada,
                    guardando = guardando,
                    onCapacityChange = { capacityTexto = it.filter { ch -> ch.isDigit() } },
                    onTipoSelect = { tipoSeleccionado = it },
                    onAerolineaSelect = { aerolineaSeleccionada = it },
                    onGuardarClick = { guardar() },
                    onVolverClick = { finish() }
                )
            }
        }
    }
}

// PANTALLA DE FORMULARIO CON DOS SELECTORES DESPLEGABLES
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioAvionScreen(
    esEdicion: Boolean,
    airplaneId: Int,
    capacityTexto: String,
    tiposAvion: List<AirplaneType>,
    aerolineas: List<Airline>,
    tipoSeleccionado: AirplaneType?,
    aerolineaSeleccionada: Airline?,
    guardando: Boolean,
    onCapacityChange: (String) -> Unit,
    onTipoSelect: (AirplaneType) -> Unit,
    onAerolineaSelect: (Airline) -> Unit,
    onGuardarClick: () -> Unit,
    onVolverClick: () -> Unit
) {
    var expTipo by remember { mutableStateOf(false) }
    var expAerolinea by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (esEdicion) "Editar Avión" else "Nuevo Avión",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (esEdicion) "Aeronave ID #$airplaneId" else "Registro de flota",
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
                                imageVector = Icons.Default.AirplanemodeActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Text(
                            text = "Datos de la aeronave",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = capacityTexto,
                        onValueChange = onCapacityChange,
                        label = { Text("Capacidad de pasajeros") },
                        placeholder = { Text("Ej. 186") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expTipo,
                        onExpandedChange = { expTipo = !expTipo }
                    ) {
                        OutlinedTextField(
                            value = tipoSeleccionado?.let { "${it.identifier} - ${it.description}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Avión") },
                            placeholder = { Text("Selecciona un modelo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expTipo) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                focusedLabelColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expTipo,
                            onDismissRequest = { expTipo = false }
                        ) {
                            tiposAvion.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text("${tipo.identifier} - ${tipo.description}") },
                                    onClick = {
                                        onTipoSelect(tipo)
                                        expTipo = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expAerolinea,
                        onExpandedChange = { expAerolinea = !expAerolinea }
                    ) {
                        OutlinedTextField(
                            value = aerolineaSeleccionada?.let { "[${it.iata}] ${it.airlineName}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Aerolínea Propietaria") },
                            placeholder = { Text("Selecciona una aerolínea") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expAerolinea) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expAerolinea,
                            onDismissRequest = { expAerolinea = false }
                        ) {
                            aerolineas.forEach { aero ->
                                DropdownMenuItem(
                                    text = { Text("[${aero.iata}] ${aero.airlineName}") },
                                    onClick = {
                                        onAerolineaSelect(aero)
                                        expAerolinea = false
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