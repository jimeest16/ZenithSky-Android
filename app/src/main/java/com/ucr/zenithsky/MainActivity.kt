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
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.ucr.zenithsky.data.ZenithDatabase
import com.ucr.zenithsky.entity.AvionDestacado
import com.ucr.zenithsky.entity.OperacionLog
import com.ucr.zenithsky.ui.theme.ZenithSkyAppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// bloque de CLASE DE LA ACTIVITY PRINCIPAL
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = ZenithDatabase.getDatabase(this).zenithDao()

        setContent {
            ZenithSkyAppTheme {
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                // Flujos reactivos desde SQLite (Room)
                val listaLogs by dao.obtenerLogs().collectAsState(initial = emptyList())
                val listaDestacados by dao.obtenerDestacados().collectAsState(initial = emptyList())

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        BarraSuperiorZenith(
                            onAbrirAeropuertos = {
                                startActivity(Intent(this@MainActivity, AirportListActivity::class.java))
                            },
                            onAbrirAerolineas = {
                                startActivity(Intent(this@MainActivity, AirlineListActivity::class.java))
                            },
                            onAbrirAviones = {
                                startActivity(Intent(this@MainActivity, AirplaneListActivity::class.java))
                            }
                        )
                    }
                ) { innerPadding ->
                    PanelPrincipalScreen(
                        logs = listaLogs,
                        destacados = listaDestacados,
                        onAbrirAeropuertos = {
                            startActivity(Intent(this@MainActivity, AirportListActivity::class.java))
                        },
                        onAbrirAerolineas = {
                            startActivity(Intent(this@MainActivity, AirlineListActivity::class.java))
                        },
                        onAbrirAviones = {
                            startActivity(Intent(this@MainActivity, AirplaneListActivity::class.java))
                        },
                        onQuitarDestacado = { avion ->
                            scope.launch {
                                dao.quitarDestacado(avion)
                                Toast.makeText(
                                    context,
                                    "Avión #${avion.airplaneId} quitado de destacados",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

//  BARRA DE ARRIBA Y PANTALLA PRINCIPAL
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperiorZenith(
    onAbrirAeropuertos: () -> Unit,
    onAbrirAerolineas: () -> Unit,
    onAbrirAviones: () -> Unit
) {
    var menuExpandido by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "ZenithSky",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Panel Operativo · Gestión Aérea",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        },
        actions = {
            IconButton(onClick = { menuExpandido = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Módulos")
            }
            DropdownMenu(
                expanded = menuExpandido,
                onDismissRequest = { menuExpandido = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Aeropuertos") },
                    leadingIcon = { Icon(Icons.Default.FlightTakeoff, contentDescription = null) },
                    onClick = { menuExpandido = false; onAbrirAeropuertos() }
                )
                DropdownMenuItem(
                    text = { Text("Aerolíneas") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    onClick = { menuExpandido = false; onAbrirAerolineas() }
                )
                DropdownMenuItem(
                    text = { Text("Flota de Aviones") },
                    leadingIcon = { Icon(Icons.Default.AirplanemodeActive, contentDescription = null) },
                    onClick = { menuExpandido = false; onAbrirAviones() }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun PanelPrincipalScreen(
    logs: List<OperacionLog>,
    destacados: List<AvionDestacado>,
    onAbrirAeropuertos: () -> Unit,
    onAbrirAerolineas: () -> Unit,
    onAbrirAviones: () -> Unit,
    onQuitarDestacado: (AvionDestacado) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Accesos rápidos con color por módulo
        item {
            Text(
                text = "Módulos Operativos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccesoModuloCard(
                    titulo = "Aeropuertos",
                    icon = Icons.Default.FlightTakeoff,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onAbrirAeropuertos,
                    modifier = Modifier.weight(1f)
                )
                AccesoModuloCard(
                    titulo = "Aerolíneas",
                    icon = Icons.Default.Business,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onAbrirAerolineas,
                    modifier = Modifier.weight(1f)
                )
                AccesoModuloCard(
                    titulo = "Flota",
                    icon = Icons.Default.AirplanemodeActive,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onAbrirAviones,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aviones Destacados Offline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${destacados.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (destacados.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "Aún no hay aviones marcados como favoritos desde la flota.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(items = destacados, key = { "dest_${it.airplaneId}" }) { avion ->
                AvionDestacadoItem(avion = avion, onQuitarClick = { onQuitarDestacado(avion) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Bitácora de Operaciones Locales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${logs.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "Sin operaciones registradas todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(items = logs, key = { "log_${it.id}" }) { log ->
                OperacionLogItem(log = log)
            }
        }
    }
}

@Composable
fun AccesoModuloCard(
    titulo: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = titulo)
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

//  TARJETAS DE ITEMS PARA LA LISTA
@Composable
fun AvionDestacadoItem(avion: AvionDestacado, onQuitarClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            text = "#${avion.airplaneId}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = avion.tipoAvion,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Aerolínea: ${avion.aerolinea} • ${avion.capacity} pax",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onQuitarClick) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Quitar destacado",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun OperacionLogItem(log: OperacionLog) {
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val fechaLegible = formatoFecha.format(Date(log.creadoEn))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        text = log.entidad.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = log.accion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = log.detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Registrado: $fechaLegible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}