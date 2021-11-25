package com.udacity.project4.locationreminders

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityRemindersBinding

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private lateinit var binding: ActivityRemindersBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reminders)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(this, navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    override fun onStart() {
        super.onStart()
        requestForegroundAndBackgroundLocationPermissions()
    }

    /*
    *  Determines whether the app has the appropriate permissions across Android 10+ and all other
    *  Android versions.This app need foreground and background location permission to work.
    */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION))

        val backgroundPermissionApproved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                FINE_AND_BACKGROUND_LOCATIONS_REQUEST_CODE
            }
            else -> {
                FINE_LOCATION_REQUEST_CODE
            }
        }
        Log.i(TAG, "Request foreground only location permission")
        //Request permissions passing in the current activity, the permissions array and the result code.
        requestPermissions(permissionsArray, resultCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TURN_DEVICE_LOCATION_ON_REQUEST_CODE) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@RemindersActivity,
                        TURN_DEVICE_LOCATION_ON_REQUEST_CODE)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.navHostFragment,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofence()
            }
        }
    }

    private fun addGeofence() {
        TODO("Not yet implemented")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")
        if (grantResults.isEmpty() || grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == FINE_AND_BACKGROUND_LOCATIONS_REQUEST_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
        ) {
            //This app has very little use when permissions are not granted so present a snackbar explaining
            // that the user needs location permissions in order to play.
            Snackbar.make(binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }
}

private const val FINE_LOCATION_REQUEST_CODE = 33
private const val FINE_AND_BACKGROUND_LOCATIONS_REQUEST_CODE = 34
private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 35
private const val TAG = "RemindersActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1