package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        //        call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        // handle the geofencing transition events and
        // send a notification to the user when he enters the geofence area
        // call @sendNotification
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceTransJobIntent", "${geofencingEvent.errorCode}")
            return
        }

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            sendNotification(geofencingEvent.triggeringGeofences)
        }
    }

    //get the request id of the current geofence
    private fun sendNotification(triggeringGeofences: List<Geofence>) {
//        Since the user might want to add multiple reminders at the same location,
//        using a for loop to loop through all triggering Geofences
        for (i in 1..triggeringGeofences.size) {
            val requestId = triggeringGeofences[i].requestId

            //Get the local repository instance
            //koin inject from Myapp.kt single { RemindersLocalRepository(get()) as ReminderDataSource }
            val remindersLocalRepository: ReminderDataSource by inject()
            // Interaction to the repository has to be through a coroutine scope
//        A failure or cancellation of a child does not cause the supervisor job to fail and does not affect its other children,
//        so a supervisor can implement a custom policy for handling failures of its children
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                //get the reminder with the request id
                val result = remindersLocalRepository.getReminder(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }
    }
}