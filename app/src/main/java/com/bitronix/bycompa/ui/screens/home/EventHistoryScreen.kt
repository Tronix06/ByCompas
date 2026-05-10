package com.bitronix.bycompa.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.navigation.NavController
import com.bitronix.bycompa.models.EventData
import com.bitronix.bycompa.ui.components.EventSummaryCard
import com.bitronix.bycompa.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventHistoryScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var historyEvents by remember { mutableStateOf<List<EventData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val premiumGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        // Obtenemos todos los eventos donde el usuario ha participado o creado
        db.collection("events")
            .whereArrayContains("participants", uid)
            .get()
            .addOnSuccessListener { snap ->
                val all = snap.documents.mapNotNull { 
                    it.toObject(EventData::class.java)?.copy(id = it.id)
                }
                historyEvents = all.sortedByDescending { 
                    TimeUtils.getMillisFromDate(it.date, it.time) 
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val now = System.currentTimeMillis()
    val dayInMillis = 24 * 60 * 60 * 1000L

    val startedEvents = historyEvents.filter { 
        val eventTime = TimeUtils.getMillisFromDate(it.date, it.time)
        eventTime <= now && eventTime > (now - dayInMillis)
    }
    
    val finishedEvents = historyEvents.filter { 
        val eventTime = TimeUtils.getMillisFromDate(it.date, it.time)
        eventTime <= (now - dayInMillis)
    }

    var selectedEventForDetail by remember { mutableStateOf<EventData?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

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
                        .padding(top = 40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            "Historial", 
                            color = Color.White, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 22.sp
                        )
                    }

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
                                "EMPEZADOS", 
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
                                "FINALIZADOS", 
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (historyEvents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Default.History, 
                                null, 
                                modifier = Modifier.padding(20.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Historial vacío", 
                            fontWeight = FontWeight.Black, 
                            fontSize = 22.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            "¡Ve al Radar para crear tu propia historia!", 
                            color = Color.Gray, 
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val currentList = if (pageIndex == 0) startedEvents else finishedEvents
                    
                    if (currentList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (pageIndex == 0) "No hay eventos recientes" else "No hay eventos antiguos",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(currentList) { event ->
                                EventSummaryCard(
                                    event = event, 
                                    isPast = true,
                                    onClick = { 
                                        if (pageIndex == 0) {
                                            selectedEventForDetail = event
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hoja de detalles para eventos recientes
        if (selectedEventForDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedEventForDetail = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.Transparent,
                dragHandle = {},
                scrimColor = Color.Black.copy(alpha = 0.5f)
            ) {
                com.bitronix.bycompa.ui.components.EventDetailSheet(
                    event = selectedEventForDetail!!,
                    myLocation = null,
                    navController = navController,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
