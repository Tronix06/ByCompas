package com.bitronix.bycompas.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

object LevelManager {
    /**
     * Calcula el nivel basado en la XP acumulada
     * Fórmula: Nivel = (sqrt(XP / 100) + 1)
     */
    fun calculateLevel(xp: Int): Int {
        if (xp <= 0) return 1
        val level = (sqrt(xp.toDouble() / 100.0) + 1).toInt()
        return level.coerceIn(1, 100)
    }

    /**
     * Calcula el progreso (0.0 a 1.0) dentro del nivel actual
     */
    fun getLevelProgress(xp: Int): Float {
        val currentLevel = calculateLevel(xp)
        val xpForCurrent = 100 * (currentLevel - 1) * (currentLevel - 1)
        val xpForNext = 100 * (currentLevel) * (currentLevel)
        
        val progress = (xp - xpForCurrent).toFloat() / (xpForNext - xpForCurrent).toFloat()
        return progress.coerceIn(0f, 1f)
    }

    /**
     * XP total requerida para el siguiente nivel
     */
    fun getXPForNextLevel(currentLevel: Int): Int {
        return 100 * (currentLevel) * (currentLevel)
    }
}

object TimeUtils {
    private val dateTimeFormat = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())

    fun getMillisFromDate(date: String, time: String): Long {
        return try {
            dateTimeFormat.parse("$date $time")?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun formatCountdown(targetMillis: Long): String {
        val diff = targetMillis - System.currentTimeMillis()
        if (diff <= 0) return "¡Empezando!"
        
        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60
        
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
