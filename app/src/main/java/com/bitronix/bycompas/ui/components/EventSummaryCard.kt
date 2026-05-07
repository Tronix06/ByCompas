package com.bitronix.bycompas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.bitronix.bycompas.models.EventData

@Composable
fun EventSummaryCard(
    event: EventData, 
    isPast: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val premiumGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF1565C0)
        )
    )

    val now = System.currentTimeMillis()
    val eventTime = com.bitronix.bycompas.utils.TimeUtils.getMillisFromDate(event.date, event.time)
    val isRecent = now - eventTime < 24 * 60 * 60 * 1000L && eventTime <= now
    val shouldShowBlue = !isPast || isRecent

    // Colores para el estado finalizado (Premium Platinum/Slate)
    val finishedBg = Color(0xFFECEFF1) // Un poco más oscuro para que destaque sobre el fondo
    val finishedTextTitle = Color(0xFF37474F) // Más oscuro para mejor contraste
    val finishedTextSecondary = Color(0xFF78909C)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (shouldShowBlue) Color.White.copy(alpha = 0.1f) else Color(0xFFB0BEC5).copy(alpha = 0.3f)
        ),
        shadowElevation = if (shouldShowBlue) 4.dp else 2.dp // Añadida elevación sutil para destacar
    ) {
        Row(
            modifier = Modifier
                .background(if (shouldShowBlue) premiumGradient else SolidColor(finishedBg))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del Evento
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (shouldShowBlue) Color.White.copy(alpha = 0.2f) else Color(0xFFCFD8DC), 
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    event.sport.contains("Fútbol", ignoreCase = true) || event.sport.contains("⚽") -> "⚽"
                    event.sport.contains("Pádel", ignoreCase = true) || event.sport.contains("🎾") -> "🎾"
                    event.sport.contains("Running", ignoreCase = true) || event.sport.contains("🏃") -> "🏃"
                    event.sport.contains("Baloncesto", ignoreCase = true) || event.sport.contains("🏀") -> "🏀"
                    event.sport.contains("Tenis", ignoreCase = true) || event.sport.contains("🎾") -> "🎾"
                    event.sport.contains("Ciclismo", ignoreCase = true) || event.sport.contains("🚲") -> "🚲"
                    else -> "🏆"
                }
                Text(
                    text = icon, 
                    fontSize = 24.sp, 
                    modifier = Modifier.alpha(if (shouldShowBlue) 1.0f else 0.4f)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title.ifBlank { event.sport },
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = if (shouldShowBlue) Color.White else finishedTextTitle
                )
                Text(
                    text = event.sport,
                    fontSize = 13.sp,
                    color = if (shouldShowBlue) Color.White.copy(alpha = 0.7f) else finishedTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${event.date} • ${event.time}",
                    fontSize = 12.sp,
                    color = if (shouldShowBlue) Color.White else finishedTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onDelete != null) {
                Surface(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (shouldShowBlue) Color.White.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.05f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = if (shouldShowBlue) Color.White else Color.Red.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else if (!isPast) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Confirmado",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (isRecent) Color.White.copy(alpha = 0.2f) else Color(0xFFB0BEC5).copy(alpha = 0.4f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRecent) "EN CURSO" else "FINALIZADO", 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Black, 
                            color = if (isRecent) Color.White else finishedTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
