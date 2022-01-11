package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    //data held by SaveReminderViewModel is synced across SaveReminderFragment and SelectLocationFragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(contxt, GeofenceBroadcastReceiver::class.java)
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(contxt, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private lateinit var geofencingClient: GeofencingClient

    private lateinit var newReminder: ReminderDataItem

    private lateinit var contxt: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        //setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        //A GeofencingClient is the most basic way to interact with the Geofencing APIs.
        geofencingClient = LocationServices.getGeofencingClient(contxt)

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contxt = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

//            use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            newReminder = ReminderDataItem(title, description, location, latitude, longitude)

            if (newReminder.latitude != null && newReminder.longitude != null && _viewModel.validateEnteredData(
                    newReminder)
            ) {
                _viewModel.saveReminder(newReminder)
            } else {
                _viewModel.validateEnteredData(newReminder)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TURN_DEVICE_LOCATION_ON_REQUEST_CODE) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    /*
    *  Uses the Location Client to check the current state of location settings, and gives the user
    *  the opportunity to turn on location services within our app.
    */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(contxt)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            //Unlike what's taught in this lesson ("Check Device Location"),
            // where Exception.startResolutionForResult() is called in an activity to show the location settings dialog,
            // here we are actually in a fragment. In fragments, we should instead call the fragment equivalent
//            private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
//                val locationRequest = LocationRequest.create().apply {
//                    priority = LocationRequest.PRIORITY_LOW_POWER
//                }
//                val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
//                val settingsClient = LocationServices.getSettingsClient(this)
//                val locationSettingsResponseTask =
//                    settingsClient.checkLocationSettings(builder.build())
//                locationSettingsResponseTask.addOnFailureListener { exception ->
//                    if (exception is ResolvableApiException && resolve){
//                        try {
//                            exception.startResolutionForResult(this@HuntMainActivity,
//                                REQUEST_TURN_DEVICE_LOCATION_ON)
//                        } catch (sendEx: IntentSender.SendIntentException) {
//                            Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
//                        }
//                    } else {
//                        Snackbar.make(
//                            binding.activityMapsMain,
//                            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
//                        ).setAction(android.R.string.ok) {
//                            checkDeviceLocationSettingsAndStartGeofence()
//                        }.show()
//                    }
//                }
//                locationSettingsResponseTask.addOnCompleteListener {
//                    if ( it.isSuccessful ) {
//                        addGeofenceForClue()
//                    }
//                }
//            }

            // In fragments, we should instead call the fragment equivalent
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(exception.resolution.intentSender,
                        TURN_DEVICE_LOCATION_ON_REQUEST_CODE,null,0,0,0,null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForReminder()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder() {
        if (_viewModel.validateEnteredData(newReminder)) {
            val geofence = Geofence.Builder()
                .setRequestId(newReminder.id)
                .setCircularRegion(newReminder.latitude!!,
                    newReminder.longitude!!,
                    GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
                // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
                // is already inside that geofence.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Toast.makeText(contxt, R.string.geofence_added, Toast.LENGTH_LONG)
                        .show()
                    Log.e("Add Geofence", geofence.requestId)
                }
                addOnFailureListener {
                    Toast.makeText(contxt,
                        R.string.geofences_not_added,
                        Toast.LENGTH_SHORT)
                        .show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message)
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.ACTION_GEOFENCE_EVENT"
    }

}

private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 35
private const val FINE_LOCATION_REQUEST_CODE = 33
private const val FINE_AND_BACKGROUND_LOCATIONS_REQUEST_CODE = 34
const val TAG = "RemindersActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)
private const val GEOFENCE_RADIUS_IN_METERS = 100f