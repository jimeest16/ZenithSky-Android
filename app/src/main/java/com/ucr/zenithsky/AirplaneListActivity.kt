package com.ucr.zenithsky

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ucr.zenithsky.entity.AvionDestacado
import com.ucr.zenithsky.entity.OperacionLog
import com.ucr.zenithsky.data.ZenithDatabase
import com.ucr.zenithsky.model.Airline
import com.ucr.zenithsky.model.Airplane
import com.ucr.zenithsky.model.AirplaneType
import com.ucr.zenithsky.model.parseApiError
import com.ucr.zenithsky.network.RetrofitClient
import com.ucr.zenithsky.ui.theme.ZenithSkyAppTheme
import kotlinx.coroutines.launch

// CLASE DE LA ACTIVITY
class AirplaneListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = ZenithDatabase.getDatabase(this).zenithDao()

        setContent {
            ZenithSkyAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val destacados by dao.obtenerDestacados().collectAsState(initial = emptyList())
                val idsDestacados = remember(destacados) { destacados.map { it.airplaneId }.toSet() }

                var aviones by remember { mutableStateOf<List<Airplane>>(emptyList()) }
                var tiposAvion by remember { mutableStateOf<List<AirplaneType>>(emptyList()) }
                var aerolineas by remember { mutableStateOf<List<Airline>>(emptyList()) }

                var filtroTipoId by remember { mutableStateOf<Int?>(null) }
                var filtroAerolineaId by remember { mutableStateOf<Int?>(null) }
                var cargando by remember { mutableStateOf(true) }
                var recargarTrigger by remember { mutableIntStateOf(0) }

                // Carga inicial de catálogos para los filtros
                LaunchedEffect(Unit) {
                    try {
                        val resTipos = RetrofitClient.api.getAirplaneTypes()
                        if (resTipos.isSuccessful) tiposAvion = resTipos.body() ?: emptyList()

                        val resAerolineas = RetrofitClient.api.getAirlines()
                        if (resAerolineas.isSuccessful) aerolineas = resAerolineas.body() ?: emptyList()
                    } catch (_: Exception) { }
                }

                // Consulta GET /api/airplanes con filtros opcionales typeId y airlineId
                LaunchedEffect(filtroTipoId, filtroAerolineaId, recargarTrigger) {
                    cargando = true
                    try {
                        val response = RetrofitClient.api.getAirplanes(
                            typeId = filtroTipoId,
                            airlineId = filtroAerolineaId
                        )
                        if (response.isSuccessful) {
                            aviones = response.body() ?: emptyList()
                        } else {
                            Toast.makeText(context, parseApiError(response), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } finally {
                        cargando = false
                    }
                }

                DisposableEffect(this) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            recargarTrigger++
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                ListaAvionesScreen(
                    aviones = aviones,
                    tiposAvion = tiposAvion,
                    aerolineas = aerolineas,
                    filtroTipoId = filtroTipoId,
                    filtroAerolineaId = filtroAerolineaId,
                    idsDestacados = idsDestacados,
                    cargando = cargando,
                    onFiltroTipoChange = { filtroTipoId = it },
                    onFiltroAerolineaChange = { filtroAerolineaId = it },
                    onVolverClick = { finish() },
                    onNuevoClick = {
                        startActivity(Intent(this@AirplaneListActivity, AirplaneFormActivity::class.java))
                    },
                    onEditarClick = { airplane ->
                        val intent = Intent(this@AirplaneListActivity, AirplaneFormActivity::class.java).apply {
                            putExtra("AIRPLANE_ID", airplane.airplaneId)
                            putExtra("CAPACITY", airplane.capacity)
                            putExtra("TYPE_ID", airplane.airplaneType?.typeId ?: 0)
                            putExtra("AIRLINE_ID", airplane.airline?.airlineId ?: 0)
                        }
                        startActivity(intent)
                    },
                    onEliminarClick = { airplane ->
                        scope.launch {
                            try {
                                val res = RetrofitClient.api.deleteAirplane(airplane.airplaneId)
                                if (res.isSuccessful) {
                                    dao.insertarLog(
                                        OperacionLog(
                                            entidad = "Avión",
                                            accion = "Eliminado",
                                            detalle = "ID #${airplane.airplaneId} (${airplane.airplaneType?.identifier ?: ""})"
                                        )
                                    )
                                    Toast.makeText(context, "Avión eliminado", Toast.LENGTH_SHORT).show()
                                    recargarTrigger++
                                } else {
                                    Toast.makeText(context, parseApiError(res), Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onToggleDestacadoClick = { airplane ->
                        scope.launch {
                            val itemRoom = AvionDestacado(
                                airplaneId = airplane.airplaneId,
                                capacity = airplane.capacity,
                                tipoAvion = airplane.airplaneType?.identifier ?: "Desconocido",
                                aerolinea = airplane.airline?.airlineName ?: "Sin aerolínea"
                            )
                            if (idsDestacados.contains(airplane.airplaneId)) {
                                dao.quitarDestacado(itemRoom)
                                Toast.makeText(context, "Quitado de destacados", Toast.LENGTH_SHORT).show()
                            } else {
                                dao.destacarAvion(itemRoom)
                                Toast.makeText(context, "Guardado en destacados offline", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

//PANTALLA DE LISTADO Y FILTROS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaAvionesScreen(
    aviones: List<Airplane>,
    tiposAvion: List<AirplaneType>,
    aerolineas: List<Airline>,
    filtroTipoId: Int?,
    filtroAerolineaId: Int?,
    idsDestacados: Set<Int>,
    cargando: Boolean,
    onFiltroTipoChange: (Int?) -> Unit,
    onFiltroAerolineaChange: (Int?) -> Unit,
    onVolverClick: () -> Unit,
    onNuevoClick: () -> Unit,
    onEditarClick: (Airplane) -> Unit,
    onEliminarClick: (Airplane) -> Unit,
    onToggleDestacadoClick: (Airplane) -> Unit
) {
    var expTipo by remember { mutableStateOf(false) }
    var expAerolinea by remember { mutableStateOf(false) }

    val textoTipoSeleccionado = tiposAvion.find { it.typeId == filtroTipoId }?.identifier ?: "Todos los tipos"
    val textoAerolineaSeleccionada = aerolineas.find { it.airlineId == filtroAerolineaId }?.airlineName ?: "Todas las aerolíneas"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flota de Aviones (${aviones.size})") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoClick) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Avión")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Fila de filtros por Tipo y Aerolínea
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expTipo,
                    onExpandedChange = { expTipo = !expTipo },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = textoTipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expTipo) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expTipo, onDismissRequest = { expTipo = false }) {
                        DropdownMenuItem(
                            text = { Text("Todos los tipos") },
                            onClick = { onFiltroTipoChange(null); expTipo = false }
                        )
                        tiposAvion.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.identifier) },
                                onClick = { onFiltroTipoChange(tipo.typeId); expTipo = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expAerolinea,
                    onExpandedChange = { expAerolinea = !expAerolinea },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = textoAerolineaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aerolínea") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expAerolinea) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expAerolinea, onDismissRequest = { expAerolinea = false }) {
                        DropdownMenuItem(
                            text = { Text("Todas las aerolíneas") },
                            onClick = { onFiltroAerolineaChange(null); expAerolinea = false }
                        )
                        aerolineas.forEach { aero ->
                            DropdownMenuItem(
                                text = { Text(aero.airlineName) },
                                onClick = { onFiltroAerolineaChange(aero.airlineId); expAerolinea = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = aviones, key = { it.airplaneId }) { airplane ->
                        AirplaneItem(
                            airplane = airplane,
                            esDestacado = idsDestacados.contains(airplane.airplaneId),
                            onToggleDestacadoClick = { onToggleDestacadoClick(airplane) },
                            onEditarClick = { onEditarClick(airplane) },
                            onEliminarClick = { onEliminarClick(airplane) }
                        )
                    }
                }
            }
        }
    }
}

//ITEM DE LA LISTA
@Composable
fun AirplaneItem(
    airplane: Airplane,
    esDestacado: Boolean,
    onToggleDestacadoClick: () -> Unit,
    onEditarClick: () -> Unit,
    onEliminarClick: () -> Unit
) {
    val tipoTexto = airplane.airplaneType?.identifier ?: "Sin tipo"
    val aerolineaTexto = airplane.airline?.airlineName ?: "Sin aerolínea"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Avión #${airplane.airplaneId} • $tipoTexto", fontWeight = FontWeight.Bold)
                Text(text = "Aerolínea: $aerolineaTexto", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Capacidad: ${airplane.capacity} pasajeros", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onToggleDestacadoClick) {
                    Icon(
                        imageVector = if (esDestacado) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Destacar",
                        tint = if (esDestacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditarClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onEliminarClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}