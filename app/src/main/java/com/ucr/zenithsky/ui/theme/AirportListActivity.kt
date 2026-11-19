package com.ucr.zenithsky

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ucr.zenithsky.data.OperacionLog
import com.ucr.zenithsky.data.ZenithDatabase
import com.ucr.zenithsky.model.Airport
import com.ucr.zenithsky.model.parseApiError
import com.ucr.zenithsky.network.RetrofitClient
import com.ucr.zenithsky.ui.theme.ZenithSkyAppTheme
import kotlinx.coroutines.launch

//  CLASE DE LA ACTIVITY
class AirportListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = ZenithDatabase.getDatabase(this).zenithDao()

        setContent {
            ZenithSkyAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var aeropuertos by remember { mutableStateOf<List<Airport>>(emptyList()) }
                var busqueda by remember { mutableStateOf("") }
                var cargando by remember { mutableStateOf(true) }
                var mensajeErrorDialogo by remember { mutableStateOf<String?>(null) }
                var recargarTrigger by remember { mutableIntStateOf(0) }

                // Se ejecuta al entrar y cada vez que cambia búsqueda o recargarTrigger
                LaunchedEffect(busqueda, recargarTrigger) {
                    cargando = true
                    try {
                        val response = RetrofitClient.api.getAirports(busqueda.ifBlank { null })
                        if (response.isSuccessful) {
                            aeropuertos = response.body() ?: emptyList()
                        } else {
                            Toast.makeText(context, parseApiError(response), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } finally {
                        cargando = false
                    }
                }

                // Recarga al volver desde AirportFormActivity
                DisposableEffect(this) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            recargarTrigger++
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                if (mensajeErrorDialogo != null) {
                    AlertDialog(
                        onDismissRequest = { mensajeErrorDialogo = null },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = {
                            Text(
                                text = "No se puede eliminar",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                text = mensajeErrorDialogo ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { mensajeErrorDialogo = null }) {
                                Text(
                                    text = "Entendido",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }

                ListaAeropuertosScreen(
                    aeropuertos = aeropuertos,
                    busqueda = busqueda,
                    cargando = cargando,
                    onBusquedaChange = { busqueda = it },
                    onVolverClick = { finish() },
                    onNuevoClick = {
                        startActivity(Intent(this@AirportListActivity, AirportFormActivity::class.java))
                    },
                    onEditarClick = { airport ->
                        val intent = Intent(this@AirportListActivity, AirportFormActivity::class.java).apply {
                            putExtra("AIRPORT_ID", airport.airportId)
                            putExtra("IATA", airport.iata)
                            putExtra("ICAO", airport.icao)
                            putExtra("NAME", airport.name)
                        }
                        startActivity(intent)
                    },
                    onEliminarClick = { airport ->
                        scope.launch {
                            try {
                                val res = RetrofitClient.api.deleteAirport(airport.airportId)
                                if (res.isSuccessful) {
                                    dao.insertarLog(
                                        OperacionLog(
                                            entidad = "Aeropuerto",
                                            accion = "Eliminado",
                                            detalle = "${airport.iata} - ${airport.name}"
                                        )
                                    )
                                    Toast.makeText(context, "Aeropuerto eliminado", Toast.LENGTH_SHORT).show()
                                    recargarTrigger++
                                } else {
                                    // Muestra el 409 Conflict del backend en AlertDialog
                                    mensajeErrorDialogo = parseApiError(res)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

//PANTALLA DE LISTADO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaAeropuertosScreen(
    aeropuertos: List<Airport>,
    busqueda: String,
    cargando: Boolean,
    onBusquedaChange: (String) -> Unit,
    onVolverClick: () -> Unit,
    onNuevoClick: () -> Unit,
    onEditarClick: (Airport) -> Unit,
    onEliminarClick: (Airport) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Aeropuertos",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${aeropuertos.size} mostrados",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevoClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Aeropuerto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                shadowElevation = 2.dp
            ) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = onBusquedaChange,
                    label = { Text("Buscar por nombre, IATA o ICAO") },
                    placeholder = { Text("Ej. SJO, MROC, Juan Santamaría") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { onBusquedaChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = aeropuertos, key = { it.airportId }) { airport ->
                        AirportItem(
                            airport = airport,
                            onEditarClick = { onEditarClick(airport) },
                            onEliminarClick = { onEliminarClick(airport) }
                        )
                    }
                }
            }
        }
    }
}

// ITEM DE LA LISTA
@Composable
fun AirportItem(
    airport: Airport,
    onEditarClick: () -> Unit,
    onEliminarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "IATA: ${airport.iata}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ICAO: ${airport.icao}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = airport.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditarClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onEliminarClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}