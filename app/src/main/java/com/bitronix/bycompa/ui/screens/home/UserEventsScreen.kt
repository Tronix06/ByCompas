package com.bitronix.bycompa.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.EventNote
import androidx.navigation.NavController
import com.bitronix.bycompa.models.EventData
import com.bitronix.bycompa.ui.components.EventDetailSheet
import com.bitronix.bycompa.ui.components.EventSummaryCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bitronix.bycompa.utils.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEventsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    
    val premiumGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    // Repositorio para borrado
    val repository = remember {
        val eventDao = com.bitronix.bycompa.database.AppDatabase.getDatabase(context).eventDao()
        com.bitronix.bycompa.repository.HomeRepository(eventDao, db)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var events by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Estados para borrado seguro
    var eventToDelete by remember { mutableStateOf<EventData?>(null) }
    var selectedEventForDetail by remember { mutableStateOf<EventData?>(null) }
    var confirmationInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
        
        isLoading = true
        val query = if (selectedTab == 0) {
            db.collection("events").whereArrayContains("participants", uid)
        } else {
            db.collection("events").whereEqualTo("creatorId", uid)
        }

        query.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(EventData::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            events = list.sortedBy { event ->
                TimeUtils.getMillisFromDate(event.date, event.time)
            }
            isLoading = false
        }
    }

    val filteredEvents = events.filter { 
        it.sport.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(premiumGradient)
                        .padding(top = 40.dp, bottom = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            "Mis Eventos", 
                            color = Color.White, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 22.sp
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 4.dp,
                                color = Color.White
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0, 
                            onClick = { selectedTab = 0 }
                        ) {
                            Text(
                                "INSCRITO", 
                                modifier = Modifier.padding(16.dp), 
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Tab(
                            selected = selectedTab == 1, 
                            onClick = { selectedTab = 1 }
                        ) {
                            Text(
                                "MIS EVENTOS",
                                modifier = Modifier.padding(16.dp), 
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                placeholder = { Text("Buscar en mis eventos...", color = Color.Gray) },
                leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (filteredEvents.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.EventNote, 
                                null, 
                                modifier = Modifier.size(80.dp),
                                tint = Color.Gray.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                if (pageIndex == 0) "Sin inscripciones" else "Sin eventos creados",
                                fontWeight = FontWeight.Black, 
                                fontSize = 20.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                "No tienes eventos en esta sección actualmente.",
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredEvents) { event ->
                            EventSummaryCard(
                                event = event,
                                onDelete = if (pageIndex == 1) { 
                                    { eventToDelete = event; confirmationInput = "" } 
                                } else null,
                                onClick = { selectedEventForDetail = event }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO DE BORRADO SEGURO ---
    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            icon = { 
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.Red.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(32.dp))
                    }
                }
            },
            title = { 
                Text(
                    "¿Eliminar evento?", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Esta acción borrará el evento para todos los participantes de forma permanente.",
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Escribe 'ELIMINAR' para confirmar:", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 11.sp, 
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmationInput,
                        onValueChange = { confirmationInput = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ELIMINAR", color = Color.LightGray) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Red.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.3f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = eventToDelete?.id ?: ""
                        scope.launch {
                            try {
                                repository.deleteEvent(id)
                                snackbarHostState.showSnackbar("Evento eliminado correctamente")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error al eliminar")
                            }
                            eventToDelete = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = Color.Red.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = confirmationInput == "ELIMINAR"
                ) {
                    Text("BORRAR EVENTO", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { eventToDelete = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar y volver", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (selectedEventForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedEventForDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            dragHandle = {},
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            EventDetailSheet(
                event = selectedEventForDetail!!,
                myLocation = null,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
