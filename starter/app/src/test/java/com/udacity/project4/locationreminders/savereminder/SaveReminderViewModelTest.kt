package com.udacity.project4.locationreminders.savereminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    //provide testing to the SaveReminderView and its live data objects
    private lateinit var fakeDataSource: FakeDataSource

    @Test
    fun saveReminder() {
        val saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

}