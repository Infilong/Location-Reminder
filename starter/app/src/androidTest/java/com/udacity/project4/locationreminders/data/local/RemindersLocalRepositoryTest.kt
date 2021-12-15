package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    //    Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun createRepository() {
        database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java, "locationReminders.db"
        ).build()

        remindersLocalRepository =
            RemindersLocalRepository(database.reminderDao(), Dispatchers.Unconfined)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    // runBlocking is used to wait for all suspend functions to finish before continuing with the execution in the block.
    // Note that we're using runBlocking instead of runBlockingTest because of a bug(https://github.com/Kotlin/kotlinx.coroutines/issues/1204).
    fun getReminder_requestRemindersFromDatabaseSuccess() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        val reminderDTO2 = ReminderDTO("title2", "des2", "loc2", 2.00, 2.00, "2")
        val reminderDTO3 = ReminderDTO("title3", "des3", "loc3", 3.00, 3.00, "3")
        val dataListDTO = listOf(reminderDTO1, reminderDTO2, reminderDTO3)

        remindersLocalRepository.saveReminder(reminderDTO1)
        remindersLocalRepository.saveReminder(reminderDTO2)
        remindersLocalRepository.saveReminder(reminderDTO3)

        val reminder1 = remindersLocalRepository.getReminder("1")
        val reminder2 = remindersLocalRepository.getReminder("2")
        val reminder3 = remindersLocalRepository.getReminder("3")

        assertThat(reminder1 == reminderDTO1, `is`(true))
        assertThat(reminder2 == reminderDTO2, `is`(true))
        assertThat(reminder3 == reminderDTO2, `is`(true))
    }

    @Test
    // runBlocking is used to wait for all suspend functions to finish before continuing with the execution in the block.
    // Note that we're using runBlocking instead of runBlockingTest because of a bug(https://github.com/Kotlin/kotlinx.coroutines/issues/1204).
    fun getReminders_requestRemindersFromDatabaseSuccess() = runBlocking {
        val reminderDTO1 = ReminderDTO("title1", "des1", "loc1", 1.00, 1.00, "1")
        val reminderDTO2 = ReminderDTO("title2", "des2", "loc2", 2.00, 2.00, "2")
        val reminderDTO3 = ReminderDTO("title3", "des3", "loc3", 3.00, 3.00, "3")
        val dataListDTO = listOf(reminderDTO1, reminderDTO2, reminderDTO3)

        remindersLocalRepository.saveReminder(reminderDTO1)
        remindersLocalRepository.saveReminder(reminderDTO2)
        remindersLocalRepository.saveReminder(reminderDTO3)

        val reminder = remindersLocalRepository.getReminders()

        assertThat(reminder == reminderDTO1, `is`(true))

    }
}