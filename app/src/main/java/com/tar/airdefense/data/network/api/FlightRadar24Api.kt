package com.tar.airdefense.data.network.api

import com.tar.airdefense.data.model.FlightRadar24Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * FlightRadar24 API interface for real-time flight data
 * Provides access to comprehensive air traffic information
 */
interface FlightRadar24Api {
    
    /**
     * Get real-time flight data for specific zones
     * Endpoint: /zones/fcgi/js
     */
    @GET("zones/fcgi/js")
    suspend fun getFlightData(
        @Query("bounds") bounds: String? = null,
        @Query("faa") faa: Int = 1,
        @Query("satellite") satellite: Int = 1,
        @Query("mlat") mlat: Int = 1,
        @Query("flarm") flarm: Int = 1,
        @Query("adsb") adsb: Int = 1,
        @Query("gnd") gnd: Int = 1,
        @Query("air") air: Int = 1,
        @Query("vehicles") vehicles: Int = 1,
        @Query("estimated") estimated: Int = 1,
        @Query("maxage") maxAge: Int = 7200,
        @Query("gliders") gliders: Int = 1,
        @Query("stats") stats: Int = 1
    ): FlightRadar24Response
    
    /**
     * Get flight details by ICAO24 code
     */
    @GET("click")
    suspend fun getFlightDetails(
        @Query("icao") icao: String,
        @Query("timestamp") timestamp: Long? = null
    ): FlightRadar24Response
    
    /**
     * Get flight track history
     */
    @GET("click")
    suspend fun getFlightTrack(
        @Query("icao") icao: String,
        @Query("timestamp") timestamp: Long? = null,
        @Query("track") track: Int = 1
    ): FlightRadar24Response
}