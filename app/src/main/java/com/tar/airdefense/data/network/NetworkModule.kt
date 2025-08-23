package com.tar.airdefense.data.network

import android.content.Context
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.logging.HttpLoggingInterceptor
import com.squareup.retrofit2.Retrofit
import com.squareup.retrofit2.converter.gson.GsonConverterFactory
import com.squareup.retrofit2.converter.scalars.ScalarsConverterFactory
import com.tar.airdefense.BuildConfig
import com.tar.airdefense.data.network.api.FlightRadar24Api
import com.tar.airdefense.data.network.api.OpenSkyApi
import com.tar.airdefense.data.network.api.AdsbLolApi
import com.tar.airdefense.data.network.api.MilitaryApi
import com.tar.airdefense.data.network.interceptor.AuthInterceptor
import com.tar.airdefense.data.network.interceptor.CacheInterceptor
import com.tar.airdefense.data.network.interceptor.ThreatAnalysisInterceptor
import com.tar.airdefense.utils.NetworkUtils
import okhttp3.Cache
import okhttp3.Protocol
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Network Module for TAR Air Defense System
 * Provides HTTP/3 support and integration with multiple flight data sources
 */
object NetworkModule {
    
    private const val CACHE_SIZE = 50 * 1024 * 1024L // 50 MB
    private const val CONNECT_TIMEOUT = 10L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    
    private lateinit var flightRadar24Api: FlightRadar24Api
    private lateinit var openSkyApi: OpenSkyApi
    private lateinit var adsbLolApi: AdsbLolApi
    private lateinit var militaryApi: MilitaryApi
    
    private lateinit var httpClient: OkHttpClient
    private lateinit var http3Client: OkHttpClient
    
    fun initialize(context: Context) {
        Timber.i("Initializing Network Module")
        
        setupHttpClients(context)
        setupApis()
        
        Timber.i("Network Module initialized successfully")
    }
    
    private fun setupHttpClients(context: Context) {
        // Setup cache
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE)
        
        // HTTP/1.1 Client
        httpClient = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .addInterceptor(CacheInterceptor())
            .addInterceptor(ThreatAnalysisInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.BASIC
                }
            })
            .build()
        
        // HTTP/3 Client with QUIC support
        http3Client = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.QUIC, Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor(AuthInterceptor())
            .addInterceptor(CacheInterceptor())
            .addInterceptor(ThreatAnalysisInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.BASIC
                }
            })
            .build()
        
        Timber.i("HTTP clients configured with HTTP/3 support")
    }
    
    private fun setupApis() {
        // FlightRadar24 API
        flightRadar24Api = createRetrofit(
            "https://data-cloud.flightradar24.com/",
            http3Client // Use HTTP/3 for FlightRadar24
        ).create(FlightRadar24Api::class.java)
        
        // OpenSky Network API
        openSkyApi = createRetrofit(
            "https://opensky-network.org/",
            httpClient
        ).create(OpenSkyApi::class.java)
        
        // ADS-B.lol API
        adsbLolApi = createRetrofit(
            "https://api.adsb.lol/",
            http3Client // Use HTTP/3 for ADS-B.lol
        ).create(AdsbLolApi::class.java)
        
        // Military API
        militaryApi = createRetrofit(
            "https://api.military.defense.gov/", // Example military endpoint
            httpClient
        ).create(MilitaryApi::class.java)
        
        Timber.i("All APIs configured successfully")
    }
    
    private fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    fun getFlightRadar24Api(): FlightRadar24Api = flightRadar24Api
    fun getOpenSkyApi(): OpenSkyApi = openSkyApi
    fun getAdsbLolApi(): AdsbLolApi = adsbLolApi
    fun getMilitaryApi(): MilitaryApi = militaryApi
    
    fun getHttpClient(): OkHttpClient = httpClient
    fun getHttp3Client(): OkHttpClient = http3Client
    
    /**
     * Check network connectivity and quality
     */
    fun checkNetworkStatus(context: Context): NetworkStatus {
        return try {
            val isConnected = NetworkUtils.isNetworkAvailable(context)
            val connectionType = NetworkUtils.getConnectionType(context)
            val isHttp3Supported = NetworkUtils.isHttp3Supported()
            
            NetworkStatus(
                isConnected = isConnected,
                connectionType = connectionType,
                isHttp3Supported = isHttp3Supported,
                latency = NetworkUtils.measureLatency(),
                bandwidth = NetworkUtils.measureBandwidth()
            )
        } catch (e: Exception) {
            Timber.e(e, "Error checking network status")
            NetworkStatus(
                isConnected = false,
                connectionType = "Unknown",
                isHttp3Supported = false,
                latency = -1L,
                bandwidth = -1L
            )
        }
    }
    
    /**
     * Get optimal client based on network conditions
     */
    fun getOptimalClient(): OkHttpClient {
        return if (NetworkUtils.isHttp3Supported()) {
            http3Client
        } else {
            httpClient
        }
    }
}

/**
 * Network status information
 */
data class NetworkStatus(
    val isConnected: Boolean,
    val connectionType: String,
    val isHttp3Supported: Boolean,
    val latency: Long, // milliseconds
    val bandwidth: Long // bits per second
)