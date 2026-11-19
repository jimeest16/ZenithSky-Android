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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
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
import com.ucr.zenithsky.model.Airline
import com.ucr.zenithsky.model.parseApiError
import com.ucr.zenithsky.network.RetrofitClient
import com.ucr.zenithsky.ui.theme.ZenithSkyAppTheme
import kotlinx.coroutines.launch

// CLASE DE LA ACTIVITY
class AirlineListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = ZenithDatabase.getDatabase(this).zenithDao()

        setContent {
            ZenithSkyAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var aerolineas by remember { mutableStateOf<List<Airline>>(emptyList()) }
                var cargando by remember { mutableStateOf(true) }
                var mensajeErrorDialogo by remember { mutableStateOf<String?>(null) }
                var recargarTrigger by remember { mutableIntStateOf(0) }

                LaunchedEffect(recargarTrigger) {
                    cargando = true
                    try {
                        val response = RetrofitClient.api.getAirlines()
                        if (response.isSuccessful) {
                            aerolineas = response.body() ?: emptyList()
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

                ListaAerolineasScreen(
                    aerolineas = aerolineas,
                    cargando = cargando,
                    onVolverClick = { finish() },
                    onNuevoClick = {
                        startActivity(Intent(this@AirlineListActivity, AirlineFormActivity::class.java))
                    },
                    onEditarClick = { airline ->
                        val intent = Intent(this@AirlineListActivity, AirlineFormActivity::class.java).apply {
                            putExtra("AIRLINE_ID", airline.airlineId)
                            putExtra("IATA", airline.iata)
                            putExtra("NAME", airline.airlineName)
                            putExtra("BASE_AIRPORT_ID", airline.baseAirport?.airportId ?: 0)
                        }
                        startActivity(intent)
                    },
                    onEliminarClick = { airline ->
                        scope.launch {
                            try {
                                val res = RetrofitClient.api.deleteAirline(airline.airlineId)
                                if (res.isSuccessful) {
                                    dao.insertarLog(
                                        OperacionLog(
                                            entidad = "Aerolínea",
                                            accion = "Eliminada",
                                            detalle = "${airline.iata} - ${airline.airlineName}"
                                        )
                                    )
                                    Toast.makeText(context, "Aerolínea eliminada", Toast.LENGTH_SHORT).show()
                                    recargarTrigger++
                                } else {
                                    // Muestra el 409 Conflict si tiene aviones asociados
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

//  PANTALLA DE LISTADO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaAerolineasScreen(
    aerolineas: List<Airline>,
    cargando: Boolean,
    onVolverClick: () -> Unit,
    onNuevoClick: () -> Unit,
    onEditarClick: (Airline) -> Unit,
    onEliminarClick: (Airline) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Aerolíneas",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${aerolineas.size} registradas",
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
                Icon(Icons.Default.Add, contentDescription = "Nueva Aerolínea")
            }
        }
    ) { innerPadding ->
        if (cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = aerolineas, key = { it.airlineId }) { airline ->
                    AirlineItem(
                        airline = airline,
                        onEditarClick = { onEditarClick(airline) },
                        onEliminarClick = { onEliminarClick(airline) }
                    )
                }
            }
        }
    }
}

// ITEM DE LA LISTA
@Composable
fun AirlineItem(
    airline: Airline,
    onEditarClick: () -> Unit,
    onEliminarClick: () -> Unit
) {
    val aeropuertoBaseTexto = airline.baseAirport?.let { "${it.iata} - ${it.name}" } ?: "Sin aeropuerto base"

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
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = airline.iata,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = airline.airlineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Base: $aeropuertoBaseTexto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
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