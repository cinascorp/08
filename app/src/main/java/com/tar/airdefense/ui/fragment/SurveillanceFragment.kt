package com.tar.airdefense.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.tar.airdefense.R
import com.tar.airdefense.data.model.Aircraft
import com.tar.airdefense.data.model.ThreatLevel
import com.tar.airdefense.databinding.FragmentSurveillanceBinding
import com.tar.airdefense.ui.viewmodel.SurveillanceViewModel
import com.tar.airdefense.utils.MapUtils
import com.tar.airdefense.utils.ThreatAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Surveillance Fragment - Core component for real-time air traffic monitoring
 * Integrates with multiple flight data sources and provides threat analysis
 */
class SurveillanceFragment : Fragment(), OnMapReadyCallback {
    
    private var _binding: FragmentSurveillanceBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SurveillanceViewModel by viewModels()
    
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val aircraftMarkers = ConcurrentHashMap<String, Marker>()
    private val threatMarkers = ConcurrentHashMap<String, Marker>()
    
    private var currentLocation: Location? = null
    private var isMapReady = false
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableMyLocation()
                startLocationUpdates()
            }
            else -> {
                Toast.makeText(context, R.string.location_permission_required, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSurveillanceBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMap()
        setupUI()
        setupObservers()
        checkLocationPermission()
        
        // Start real-time surveillance
        startSurveillance()
    }
    
    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    
    private fun setupUI() {
        // Setup refresh button
        binding.refreshButton.setOnClickListener {
            viewModel.refreshFlightData()
        }
        
        // Setup filter buttons
        binding.filterMilitary.setOnClickListener {
            viewModel.toggleMilitaryFilter()
        }
        
        binding.filterCommercial.setOnClickListener {
            viewModel.toggleCommercialFilter()
        }
        
        binding.filterPrivate.setOnClickListener {
            viewModel.togglePrivateFilter()
        }
        
        binding.filterDrones.setOnClickListener {
            viewModel.toggleDroneFilter()
        }
        
        // Setup threat level indicator
        binding.threatLevelCard.setOnClickListener {
            showThreatDetails()
        }
        
        // Setup aircraft list
        binding.aircraftListButton.setOnClickListener {
            showAircraftList()
        }
    }
    
    private fun setupObservers() {
        viewModel.aircraftData.observe(viewLifecycleOwner) { aircraftList ->
            updateAircraftOnMap(aircraftList)
        }
        
        viewModel.threatLevel.observe(viewLifecycleOwner) { level ->
            updateThreatLevelDisplay(level)
        }
        
        viewModel.systemStatus.observe(viewLifecycleOwner) { status ->
            updateSystemStatus(status)
        }
        
        viewModel.activeFilters.observe(viewLifecycleOwner) { filters ->
            updateFilterButtons(filters)
        }
    }
    
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
                startLocationUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionRationale()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    private fun enableMyLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        } catch (e: SecurityException) {
            Timber.e(e, "Location permission not granted")
        }
    }
    
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    centerMapOnLocation(it)
                }
            }
        }
    }
    
    private fun centerMapOnLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 10f)
        )
    }
    
    private fun startSurveillance() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                try {
                    viewModel.updateFlightData()
                    delay(1000) // Update every second for real-time monitoring
                } catch (e: Exception) {
                    Timber.e(e, "Error updating flight data")
                    delay(5000) // Wait longer on error
                }
            }
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true
        
        // Configure map settings
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
            isMyLocationButtonEnabled = true
        }
        
        // Set map style for military/defense theme
        MapUtils.setMapStyle(map, requireContext())
        
        // Enable location if permission granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        }
        
        // Center map on Middle East region
        val middleEastCenter = LatLng(32.4279, 53.6880) // Iran center
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(middleEastCenter, 5f))
        
        Timber.i("Map ready for surveillance")
    }
    
    private fun updateAircraftOnMap(aircraftList: List<Aircraft>) {
        if (!isMapReady) return
        
        // Clear old markers
        aircraftMarkers.values.forEach { it.remove() }
        aircraftMarkers.clear()
        
        aircraftList.forEach { aircraft ->
            val position = LatLng(aircraft.latitude, aircraft.longitude)
            val markerOptions = MarkerOptions()
                .position(position)
                .title(aircraft.callsign ?: aircraft.icao24)
                .snippet(getAircraftInfo(aircraft))
            
            // Set marker icon based on aircraft type and threat level
            val icon = getAircraftIcon(aircraft)
            markerOptions.icon(icon)
            
            // Add marker to map
            val marker = googleMap?.addMarker(markerOptions)
            marker?.let {
                aircraftMarkers[aircraft.icao24] = it
                
                // Add info window for detailed aircraft information
                it.tag = aircraft
            }
        }
        
        // Update threat markers
        updateThreatMarkers(aircraftList)
        
        // Update aircraft count
        binding.aircraftCount.text = aircraftList.size.toString()
    }
    
    private fun getAircraftIcon(aircraft: Aircraft): BitmapDescriptor {
        return when {
            aircraft.isMilitary -> {
                when (aircraft.threatLevel) {
                    ThreatLevel.HIGH -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    ThreatLevel.MEDIUM -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                }
            }
            aircraft.isDrone -> {
                when (aircraft.threatLevel) {
                    ThreatLevel.HIGH -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
                    ThreatLevel.MEDIUM -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)
                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                }
            }
            aircraft.isCommercial -> {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            }
            else -> {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            }
        }
    }
    
    private fun getAircraftInfo(aircraft: Aircraft): String {
        return buildString {
            append("Type: ${aircraft.aircraftType ?: "Unknown"}\n")
            append("Altitude: ${aircraft.altitude ?: "Unknown"} ft\n")
            append("Speed: ${aircraft.speed ?: "Unknown"} kts\n")
            append("Heading: ${aircraft.heading ?: "Unknown"}°\n")
            if (aircraft.isMilitary) append("MILITARY AIRCRAFT\n")
            if (aircraft.isDrone) append("DRONE DETECTED\n")
            if (aircraft.threatLevel != ThreatLevel.LOW) {
                append("THREAT LEVEL: ${aircraft.threatLevel.name}\n")
            }
        }
    }
    
    private fun updateThreatMarkers(aircraftList: List<Aircraft>) {
        // Clear old threat markers
        threatMarkers.values.forEach { it.remove() }
        threatMarkers.clear()
        
        // Add threat indicators for high-threat aircraft
        aircraftList.filter { it.threatLevel == ThreatLevel.HIGH }.forEach { aircraft ->
            val position = LatLng(aircraft.latitude, aircraft.longitude)
            
            // Add threat circle
            val circleOptions = CircleOptions()
                .center(position)
                .radius(5000.0) // 5km radius
                .fillColor(Color.argb(50, 255, 0, 0))
                .strokeColor(Color.RED)
                .strokeWidth(3f)
            
            googleMap?.addCircle(circleOptions)
            
            // Add threat marker
            val markerOptions = MarkerOptions()
                .position(position)
                .title("HIGH THREAT: ${aircraft.callsign ?: aircraft.icao24}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            
            val marker = googleMap?.addMarker(markerOptions)
            marker?.let {
                threatMarkers[aircraft.icao24] = it
            }
        }
    }
    
    private fun updateThreatLevelDisplay(level: ThreatLevel) {
        binding.threatLevelIndicator.text = level.name
        binding.threatLevelIndicator.setTextColor(
            when (level) {
                ThreatLevel.LOW -> Color.GREEN
                ThreatLevel.MEDIUM -> Color.YELLOW
                ThreatLevel.HIGH -> Color.RED
                ThreatLevel.CRITICAL -> Color.MAGENTA
            }
        )
        
        // Update threat level card background
        binding.threatLevelCard.setCardBackgroundColor(
            when (level) {
                ThreatLevel.LOW -> Color.argb(50, 0, 255, 0)
                ThreatLevel.MEDIUM -> Color.argb(50, 255, 255, 0)
                ThreatLevel.HIGH -> Color.argb(50, 255, 0, 0)
                ThreatLevel.CRITICAL -> Color.argb(50, 255, 0, 255)
            }
        )
    }
    
    private fun updateSystemStatus(status: String) {
        binding.systemStatus.text = status
        binding.systemStatus.setTextColor(
            when {
                status.contains("ONLINE", ignoreCase = true) -> Color.GREEN
                status.contains("WARNING", ignoreCase = true) -> Color.YELLOW
                status.contains("ERROR", ignoreCase = true) -> Color.RED
                else -> Color.GRAY
            }
        )
    }
    
    private fun updateFilterButtons(filters: Set<String>) {
        binding.filterMilitary.isSelected = filters.contains("military")
        binding.filterCommercial.isSelected = filters.contains("commercial")
        binding.filterPrivate.isSelected = filters.contains("private")
        binding.filterDrones.isSelected = filters.contains("drones")
    }
    
    private fun showThreatDetails() {
        // Show detailed threat analysis
        ThreatDetailsDialog().show(childFragmentManager, "threat_details")
    }
    
    private fun showAircraftList() {
        // Show aircraft list dialog
        AircraftListDialog().show(childFragmentManager, "aircraft_list")
    }
    
    private fun showLocationPermissionRationale() {
        // Show dialog explaining why location permission is needed
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.location_permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}