package com.tar.airdefense.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Aircraft data model representing flight information from multiple sources
 * Supports data from FlightRadar24, OpenSky Network, and ADS-B sources
 */
@Entity(tableName = "aircraft")
data class Aircraft(
    @PrimaryKey
    val icao24: String,
    
    // Basic identification
    val callsign: String? = null,
    val aircraftType: String? = null,
    val registration: String? = null,
    val operator: String? = null,
    
    // Position data
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val verticalRate: Double? = null,
    
    // Flight information
    val origin: String? = null,
    val destination: String? = null,
    val flightNumber: String? = null,
    val squawk: String? = null,
    
    // Classification
    val isMilitary: Boolean = false,
    val isCommercial: Boolean = false,
    val isPrivate: Boolean = false,
    val isDrone: Boolean = false,
    val isEmergency: Boolean = false,
    
    // Threat assessment
    val threatLevel: ThreatLevel = ThreatLevel.LOW,
    val threatScore: Double = 0.0,
    val threatFactors: List<ThreatFactor> = emptyList(),
    
    // Source information
    val dataSource: DataSource = DataSource.UNKNOWN,
    val lastUpdate: Long = System.currentTimeMillis(),
    val dataQuality: DataQuality = DataQuality.UNKNOWN,
    
    // Additional metadata
    val country: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val yearBuilt: Int? = null,
    
    // C4ISR integration
    val commandCenterId: String? = null,
    val responseStatus: ResponseStatus = ResponseStatus.NONE,
    val interceptCoordinates: InterceptCoordinates? = null
) {
    
    /**
     * Calculate distance from a given point
     */
    fun distanceFrom(lat: Double, lon: Double): Double {
        val earthRadius = 6371000.0 // meters
        
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(lat)
        val deltaLat = Math.toRadians(lat - latitude)
        val deltaLon = Math.toRadians(lon - longitude)
        
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Check if aircraft is in restricted airspace
     */
    fun isInRestrictedAirspace(restrictedZones: List<RestrictedZone>): Boolean {
        return restrictedZones.any { zone ->
            distanceFrom(zone.centerLatitude, zone.centerLongitude) <= zone.radius
        }
    }
    
    /**
     * Get formatted position string
     */
    fun getFormattedPosition(): String {
        return "%.6f, %.6f".format(latitude, longitude)
    }
    
    /**
     * Get formatted altitude string
     */
    fun getFormattedAltitude(): String {
        return altitude?.let { "%.0f ft" } ?: "Unknown"
    }
    
    /**
     * Get formatted speed string
     */
    fun getFormattedSpeed(): String {
        return speed?.let { "%.0f kts" } ?: "Unknown"
    }
    
    /**
     * Check if aircraft requires immediate attention
     */
    fun requiresImmediateAttention(): Boolean {
        return threatLevel == ThreatLevel.CRITICAL ||
                isEmergency ||
                (threatLevel == ThreatLevel.HIGH && isMilitary) ||
                (isDrone && altitude != null && altitude < 1000) // Low flying drone
    }
}

/**
 * Threat level classification
 */
enum class ThreatLevel(val level: Int, val description: String) {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical")
}

/**
 * Data source identification
 */
enum class DataSource(val sourceName: String, val reliability: Double) {
    FLIGHTRADAR24("FlightRadar24", 0.95),
    OPENSKY("OpenSky Network", 0.90),
    ADSB_LOL("ADS-B.lol", 0.85),
    MILITARY("Military Sources", 0.98),
    UNKNOWN("Unknown", 0.50)
}

/**
 * Data quality assessment
 */
enum class DataQuality(val quality: String, val score: Double) {
    EXCELLENT("Excellent", 0.95),
    GOOD("Good", 0.80),
    FAIR("Fair", 0.65),
    POOR("Poor", 0.40),
    UNKNOWN("Unknown", 0.50)
}

/**
 * Threat factors that contribute to threat assessment
 */
enum class ThreatFactor(val factor: String, val weight: Double) {
    UNIDENTIFIED("Unidentified Aircraft", 0.8),
    RESTRICTED_AIRSPACE("Restricted Airspace Violation", 0.9),
    SUSPICIOUS_PATTERN("Suspicious Flight Pattern", 0.7),
    MILITARY_ACTIVITY("Military Activity", 0.6),
    LOW_ALTITUDE("Low Altitude Flight", 0.5),
    HIGH_SPEED("High Speed Maneuvering", 0.6),
    EMERGENCY_SQUAWK("Emergency Squawk", 0.9),
    NO_RESPONSE("No Radio Response", 0.7),
    DRONE_ACTIVITY("Drone Activity", 0.8),
    TERRORIST_THREAT("Terrorist Threat", 1.0)
}

/**
 * Response status for C4ISR integration
 */
enum class ResponseStatus(val status: String) {
    NONE("No Response"),
    MONITORING("Under Monitoring"),
    INTERCEPT_ORDERED("Intercept Ordered"),
    INTERCEPT_IN_PROGRESS("Intercept in Progress"),
    INTERCEPT_COMPLETED("Intercept Completed"),
    THREAT_NEUTRALIZED("Threat Neutralized")
}

/**
 * Intercept coordinates for emergency landing zones
 */
data class InterceptCoordinates(
    val latitude: Double,
    val longitude: Double,
    val zoneName: String,
    val zoneType: ZoneType,
    val safetyLevel: SafetyLevel
)

/**
 * Zone types for emergency landing
 */
enum class ZoneType(val type: String) {
    DESERT("Desert"),
    MOUNTAIN("Mountain"),
    OCEAN("Ocean"),
    RURAL("Rural Area"),
    MILITARY_BASE("Military Base")
}

/**
 * Safety level assessment
 */
enum class SafetyLevel(val level: String, val risk: Double) {
    VERY_SAFE("Very Safe", 0.1),
    SAFE("Safe", 0.3),
    MODERATE("Moderate", 0.5),
    RISKY("Risky", 0.7),
    DANGEROUS("Dangerous", 0.9)
}

/**
 * Restricted airspace zone
 */
data class RestrictedZone(
    val id: String,
    val name: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radius: Double, // meters
    val altitudeMin: Double? = null,
    val altitudeMax: Double? = null,
    val restrictions: List<String> = emptyList()
)