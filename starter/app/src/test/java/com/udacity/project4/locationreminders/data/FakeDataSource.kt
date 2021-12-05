package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    //    Create a fake data source to act as a double to the real data source
    var setError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (setError) {
            Result.Error("Set result error", 410)
        } else {
            Result.Success(reminders)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (setError) {
            Result.Error("Set result error", 410)
        } else {
            val reminder: ReminderDTO? = reminders.find { it.id == id }
            if (reminder != null) {
                Result.Success(reminder)
            } else {
                Result.Error("Not Found!", 404)
            }
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}