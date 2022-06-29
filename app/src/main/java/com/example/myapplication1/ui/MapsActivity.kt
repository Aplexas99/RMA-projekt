package com.example.myapplication1.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.OnSuccessListener
import java.text.SimpleDateFormat
import java.util.*
import com.example.myapplication1.R
import com.example.myapplication1.data.RunRepository
import com.example.myapplication1.di.RunRepositoryFactory
import com.example.myapplication1.di.RunRepositoryFactory.runRepository
import com.example.myapplication1.models.Run
import com.google.android.gms.location.*
import java.text.DecimalFormat

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    val ACTIVITY_RECOGNITION_REQUEST_CODE = 100
    private val SPEED_OFFSET = 0.5f
    private var MAP_LOCATION_UPDATE_INTERVAL = 5
    private var MEASURE_UPDATE_INTERVAL = 5
    private var mMap: GoogleMap? = null
    var speedAlertSpeedUp: MediaPlayer? = null
    var speedAlertSlowDown: MediaPlayer? = null

    //Google's API za lokacijske servise
    var fusedLocationProviderClient: FusedLocationProviderClient? = null

    //LocationRequest je config file za sve postavke vezane za FusedLocationProviderClient
    var locationRequest: LocationRequest? = null
    var locationCallBack: LocationCallback? = null
    var currentLocation: Location? = null
    var locationManager: LocationManager? = null

    //UI elementi
    var sw_gpsOrSave: Switch? = null
    var tvPace: TextView? = null
    var tvPaceValue: TextView? = null
    var tvPaceUnit: TextView? = null
    var tvTime: TextView? = null
    var tvTimeValue: TextView? = null
    var tvDistance: TextView? = null
    var tvDistanceValue: TextView? = null
    var tvRouteDistance: TextView? = null
    var tvDistanceUnit: TextView? = null
    var tvDesiredTime: TextView? = null
    var tvDesiredPace: TextView? = null
    var tvDesiredPaceValue: TextView? = null
    var tvDesiredPaceUnit: TextView? = null
    var myToolbar: androidx.appcompat.widget.Toolbar? = null
    var btnFinish: Button? = null
    var btnStart_Pause: Button? = null

    //Timer
    var isClicked = false
    var isStopClicked = false
    var timerStarted = false
    var isRunning = false
    var firstMeasuring = false
    var isChecked = false
    var hours = 0
    var minutes = 0
    var secs = 0


    // integer to store seconds
    var seconds = 0

    //delay da svakih 10 sekundi mjeri duljinu
    var delay = MEASURE_UPDATE_INTERVAL

    // ukupna otrcana duljina
    var totalDistance = 0f

    // ukupno vrijeme trcanja
    var totalTime = 0.0

    //trenutna brzina
    var currentSpeed = 0f

    //prosla udaljenost potrebna za racunanje brzine preko senzora
    var previousDistance = 0f

    //time limit za trcanje
    var desiredTime = 0.0

    //senzori za korake
    var sManager: SensorManager? = null
    var stepSensor: Sensor? = null
    var steps: Long = 0

    //Potrebno za datum
    private var calendar: Calendar? = null
    private var date: String? = null

    //Proslijeđeni podaci iz fragmenta
    var bundle: Bundle? = null
    var distance: String? = null
    var time: String? = null
    var calculatedPace = 0.0
    var desiredDistance = 0
    var inFeet = false
    var location1: Location? = Location("Previous location")
    var location2: Location? = Location("Current location")

    //rad sa bazom
    var runRepository = RunRepositoryFactory.runRepository
    lateinit var run : Run


    private fun runTimer() {

        // creating handler
        val handlertime = Handler(Looper.getMainLooper())

        handlertime.post(object : Runnable {
            override fun run() {
                hours = seconds / 3600
                minutes = seconds % 3600 / 60
                secs = seconds % 60

                // if running increment the seconds
                if (isRunning) {
                    val time: String = java.lang.String.format(
                        Locale.getDefault(),
                        "%02d-%02d-%02d",
                        hours,
                        minutes,
                        secs
                    )
                    tvTimeValue!!.text = time
                    seconds++
                    delay++
                    totalTime = seconds.toDouble()
                    if (totalTime > desiredTime) {
                        btnStart_Pause?.performClick()
                        onTimeFinished()
                    }

                    // measure distance from two locations
                    if (currentLocation != null && delay >= MEASURE_UPDATE_INTERVAL && !isChecked) {
                        measureDistance()
                        delay = 0
                        currentSpeed = currentLocation!!.speed
                        checkPaceGPS(currentSpeed)
                    } else if (isChecked && delay >= MEASURE_UPDATE_INTERVAL) {
                        updateDistanceRun(getDistanceRun(steps))
                        currentSpeed = calculateSpeedRun(previousDistance)
                        previousDistance = getDistanceRun(steps)
                        checkPaceGPS(currentSpeed)
                    }
                    updateUIValues()
                }
                handlertime.postDelayed(this, 1000)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.onCreate(savedInstanceState)
        mapFragment.getMapAsync(this)
        bundle = intent.extras
        distance = bundle!!.getString("Distance")
        time = bundle!!.getString("Time")
        isChecked = bundle!!.getBoolean("Method")
        inFeet = bundle!!.getBoolean("Unit")
        MAP_LOCATION_UPDATE_INTERVAL = bundle!!.getInt("MapInterval")
        MEASURE_UPDATE_INTERVAL = bundle!!.getInt("MeasureInterval")
        speedAlertSpeedUp = MediaPlayer.create(this, R.raw.speedup)
        speedAlertSlowDown = MediaPlayer.create(this, R.raw.slowdown)
        if (isChecked) {
            setupStepSensor()
        }
        calendar = Calendar.getInstance()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                (MAP_LOCATION_UPDATE_INTERVAL * 1000).toLong(),
                0f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val spawn = LatLng(location.latitude, location.longitude)
                        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(spawn, 18.0f))
                        currentLocation = location
                        currentSpeed = location.speed
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        if (!isChecked) {
                            buildAlertMessageNoGps()
                        }
                    }
                })
        }
        initializeUI()
        setUpAppBar()
        initializeLocationRequest()
        setAllListeners()
        updateGPS()
        runTimer()
        updateUIValues()
    }

    private fun initializeUI() {
        sw_gpsOrSave = findViewById(R.id.swGPSorSavePower)
        tvPace = findViewById(R.id.txtPace)
        tvPaceUnit = findViewById(R.id.txtPaceUnit)
        tvPaceValue = findViewById(R.id.txtPaceValue)
        tvTime = findViewById(R.id.txtTime)
        tvTimeValue = findViewById(R.id.txtTimeValue)
        btnFinish = findViewById(R.id.btnFinish)
        btnStart_Pause = findViewById(R.id.btnStart_Pause)
        tvDistance = findViewById(R.id.txtDistance)
        tvDistanceValue = findViewById(R.id.txtDistanceValue)
        tvRouteDistance = findViewById(R.id.txtRouteDistance)
        tvDistanceUnit = findViewById(R.id.txtDistanceUnit)
        tvDesiredTime = findViewById(R.id.txtDesiredTime)
        tvDesiredPace = findViewById(R.id.txtDesiredPace)
        tvDesiredPaceValue = findViewById(R.id.txtDesiredPaceValue)
        tvDesiredPaceUnit = findViewById(R.id.txtDesiredPaceUnit)
        desiredDistance = distance!!.toInt()
        desiredTime = convertTimeString(time)
    }

    private fun updateUIValues() {
        //gps
        if (!isChecked) {
            if (!inFeet) {
                tvDistanceValue!!.text = totalDistance.toInt().toString()
                tvPaceValue!!.text = displaySpeed(currentSpeed)
            } else {
                //prebacit sve u feets
                tvDesiredPaceUnit?.setText(R.string.feet_unit)
                tvDistanceUnit?.setText(R.string.feet_distance_unit)
                tvPaceUnit?.setText(R.string.feet_unit)
                val speedInFeets = convertToFeets(currentSpeed).toFloat()
                tvPaceValue!!.text = displaySpeed(speedInFeets)
                val distanceInFeet = convertToFeets(totalDistance).toInt()
                tvDistanceValue!!.text = distanceInFeet.toString()
            }
        } else {
            if (!inFeet) {
                tvDistanceValue!!.text = totalDistance.toInt().toString()
                tvPaceValue!!.text = displaySpeed(currentSpeed)
            } else {
                tvDesiredPaceUnit?.setText(R.string.feet_unit)
                tvDistanceUnit?.setText(R.string.feet_distance_unit)
                tvPaceUnit?.setText(R.string.feet_unit)
                var speedInFeets = convertToFeets(currentSpeed).toFloat()
                var distanceInFeets = convertToFeets(totalDistance).toInt()
                tvPaceValue!!.text = displaySpeed(speedInFeets)
                tvDistanceValue!!.text = distanceInFeets.toString()
            }
        }
        tvRouteDistance!!.text = distance
        tvDesiredPaceValue!!.text = displayPace(desiredDistance, time)
        tvDesiredTime!!.text = time
    }

    fun drawRoute(loc1: Location?, loc2: Location?) {
        if (location1 != null && location2 != null) {
            val firstLoc = loc1?.let { LatLng(it.latitude, loc1.longitude) }
            val secondLoc = loc2?.let { LatLng(it.latitude, loc2.longitude) }
            val line = mMap!!.addPolyline(
                PolylineOptions()
                    .add(firstLoc, secondLoc)
                    .width(15f)
                    .color(Color.RED)
            )
        }
    }

    fun setUpAppBar() {
        myToolbar = findViewById<View>(R.id.tbAppbar) as Toolbar
        myToolbar!!.setNavigationIcon(R.drawable.back_icon)
    }

    fun setupStepSensor() {
        if (ContextCompat.checkSelfPermission( this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )

            }
        }

        sManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sManager!!.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    }

    fun checkPaceGPS(speed: Float) {
        if (calculatedPace - SPEED_OFFSET < speed && calculatedPace + SPEED_OFFSET > speed) {
            tvPaceValue!!.setTextColor(resources.getColor(R.color.dark_green))

        } else {
            tvPaceValue!!.setTextColor(resources.getColor(R.color.red))
            if (speed > calculatedPace + SPEED_OFFSET) {
                speedAlertSlowDown!!.start()
            } else if (speed < calculatedPace - SPEED_OFFSET) {
                speedAlertSpeedUp!!.start()
            }
        }
    }

    fun measureDistance() {
        updateGPS()
        //first time measuring location2 will be null
        if (firstMeasuring) {
            currentLocation?.let { location1?.setLatitude(it.latitude) }
            currentLocation?.let { location1?.setLongitude(it.longitude) }
            firstMeasuring = false
        } else {
            location2?.let { location1?.setLatitude(it.latitude) }
            location2?.let { location1?.setLongitude(it.longitude) }
        }
        currentLocation?.let { location2?.setLatitude(it.latitude) }
        currentLocation?.let { location2?.setLongitude(it.longitude) }
        val distance: Float = location1!!.distanceTo(location2)
        totalDistance += distance
        drawRoute(location1, location2)
    }

    fun calculateDesiredSpeed(desiredDistance: Int, duration: String?): Double {
        // variables
        val pace: Double
        val durationTime: Double
        durationTime = convertTimeString(duration)
        pace = desiredDistance / durationTime
        return pace
    }

    fun convertTimeString(duration: String?): Double {
        val durationTime: Double

        //convert string duration into Hour, Minute and seconds as integers
        val durationHour: Int = duration!!.substring(0, 2).toInt()
        val durationMinute: Int = duration.substring(3, 5).toInt()
        val durationSeconds: Double = duration.substring(6, 8).toDouble()

        // calculate pace based on seconds
        durationTime = durationSeconds + durationMinute * 60 + durationHour * 60 * 60
        return durationTime
    }

    fun displayPace(desiredDistance: Int, duration: String?): String {
        calculatedPace = calculateDesiredSpeed(desiredDistance, duration)
        val decimalFormat = DecimalFormat("#0.##")
        return decimalFormat.format(calculatedPace).toString()
    }

    fun displaySpeed(speed: Float): String {
        val decimalFormat = DecimalFormat("#0.##")
        return decimalFormat.format(speed).toString()
    }

    fun calculateSpeedRun(prevDistance: Float): Float {
        val distanceRun = getDistanceRun(steps) - prevDistance
        return distanceRun / MEASURE_UPDATE_INTERVAL
    }

    fun updateDistanceRun(dist: Float) {
        totalDistance = dist
    }

    private fun initializeLocationRequest() {
        locationRequest = LocationRequest.create().apply{
            interval = (1000 * DEFAULT_UPDATE_INTERVAL).toLong()
            fastestInterval = (1000 * FAST_UPDATE_INTERVAL).toLong()
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                //save the location
                val location: Location = locationResult.lastLocation
            }
        }
        setAllListeners()
    }

    fun setAllListeners() {
        sw_gpsOrSave!!.setOnClickListener {
            if (sw_gpsOrSave!!.isChecked) {
                //GPS
                locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            } else {
                locationRequest?.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }
        }


        //Back button
        myToolbar?.setNavigationOnClickListener(View.OnClickListener {
            println("closing")
            this.finish();
        })

        //Start/Pause timer button
        btnStart_Pause?.setOnClickListener(View.OnClickListener {
            if (!isClicked) {
                isClicked = true
                if (timerStarted == false) {
                    isRunning = true
                    firstMeasuring = true
                    updateGPS()
                } else {
                    isRunning = false
                }
                startLocationUpdates()
                btnStart_Pause?.setText(R.string.pause_btn)
            } else {
                isClicked = false
                stopLocationUpdates()
                isRunning = false
                btnStart_Pause?.setText(R.string.start_btn)
            }
        })
        btnFinish?.setOnClickListener(View.OnClickListener {
            if (!isStopClicked) {
                isStopClicked = true
                btnFinish?.alpha = 0.5f
                btnStart_Pause?.isClickable = false
                isClicked = true
                stopLocationUpdates()

                try {
                    date = SimpleDateFormat("dd/MM/yyyy").format(calendar?.time)
                    var defaultTitle = date


                    val pace: Float
                    pace = if (totalDistance > 0) {
                        (totalDistance / totalTime).toFloat()
                    } else 0f

                    val time: String = java.lang.String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d",
                        hours,
                        minutes,
                        secs
                    )

                    val distance: String = totalDistance.toInt().toString()

                    run = Run(0,
                        defaultTitle?:"Title",time,displaySpeed(pace), distance, date?:"Date")

                    runRepository.save(run)

                } catch (e: Exception) {
                    Toast.makeText(applicationContext, R.string.database_error, Toast.LENGTH_LONG)
                        .show()
                }
            }
        })
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient?.requestLocationUpdates(locationRequest, locationCallBack, null)
        updateGPS()
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallBack)
    }

    private fun updateGPS() {
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@MapsActivity)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient?.lastLocation?.addOnSuccessListener(this,
                OnSuccessListener<Any> { location -> //we got permission
                    currentLocation = location as Location?
                    if (currentLocation != null) {
                        currentSpeed = location.speed
                    }
                })
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_FINE_LOCATION
                )
            }
        }
    }

    fun onTimeFinished() {

        // inflate the layout of the popup window
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_window, null)

        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(findViewById(R.id.mapsActivity), Gravity.CENTER, 0, 0)

        // dismiss the popup window when touched
        popupView.setOnTouchListener { _, event ->
            popupWindow.dismiss()
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_FINE_LOCATION -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateGPS()
            } else {
                Toast.makeText(this, R.string.permissionNotice, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun convertToFeets(speed: Float): Double {
        //for feet/sec
        return ONE_FEET * speed
    }

    override fun onMapReady(googleMap: GoogleMap) {
        updateGPS()
        mMap = googleMap
        val spawn = LatLng(0.0, 0.0)
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(spawn, 10.0f))
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, com.example.myapplication1.R.string.permission_not_granted, Toast.LENGTH_LONG).show()
        } else {
            mMap!!.isMyLocationEnabled = true
        }
    }

    fun getDistanceRun(steps: Long): Float {
        return (steps * 78).toFloat() / 100f
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensor: Sensor = event.sensor
        val stepsTaken = event!!.values[0]
       if (sensor.type === Sensor.TYPE_STEP_DETECTOR && isRunning) {
            steps+= stepsTaken.toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, i: Int) {}
    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (locationManager == null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    (MAP_LOCATION_UPDATE_INTERVAL * 1000).toLong(),
                    0f,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            val spawn = LatLng(location.latitude, location.longitude)
                            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(spawn, 18.0f))
                            currentLocation = location
                            currentSpeed = location.speed
                        }

                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {
                            if (!isChecked) {
                                buildAlertMessageNoGps()
                            }
                        }
                    })
            }
        }
        if (isChecked) {
            sManager!!.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }

        if(isRunning){
            checkPaceGPS(currentSpeed)
        }
    }

    override fun onStop() {
        super.onStop()
        speedAlertSlowDown!!.stop()
        speedAlertSpeedUp!!.stop()
        if (isChecked) {
            sManager!!.unregisterListener(this, stepSensor)
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes",
                DialogInterface.OnClickListener { _, id -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialog, id ->
                finish()
                dialog.cancel()
            })
        val alert: android.app.AlertDialog? = builder.create()
        alert?.show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    companion object {
        const val DEFAULT_UPDATE_INTERVAL = 5
        const val FAST_UPDATE_INTERVAL = 2
        private const val PERMISSION_FINE_LOCATION = 99
        private const val ONE_FEET = 3.2808399
    }
}
