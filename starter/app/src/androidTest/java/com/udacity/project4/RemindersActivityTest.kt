package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = ServiceLocator.provideRemindersLocalRepository(context = appContext)

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun reset() {
        ServiceLocator.resetRepository()
    }

    //    add End to End testing to the app
    @Test
    fun addNewReminder_verifyNewReminderAdded() = runBlocking {
        // Start up Tasks screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        // Espresso code will go here.
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("des"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(longClick())

        onView(withId(R.id.select_location_save_button)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        assertThat((repository.getReminders() as Result.Success).data.size, `is`(1))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun snackbar_verifyTitleMissingSnackbar() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(ViewAssertions.matches(withText(R.string.err_enter_title)))

        activityScenario.close()
    }

    @Test
    fun snackbar_verifyDescriptionMissingSnackbar() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("title"))
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(ViewAssertions.matches(withText(R.string.err_enter_description)))

        activityScenario.close()
    }

    @Test
    fun snackbar_verifyLocationMissingSnackbar() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("des"))
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(ViewAssertions.matches(withText(R.string.err_select_location)))

        activityScenario.close()
    }

    @Test
    fun toast_verifyTwoToasts() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("des"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withText(R.string.choose_a_place)).inRoot(withDecorView(not(`is`(getActivity(
            activityScenario)?.window?.decorView))))
            .check(matches(isDisplayed()))

        // Delay
        runBlocking {
            delay(2000)
        }

        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.select_location_save_button)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(R.string.reminder_location_selected)).inRoot(withDecorView(not(`is`(getActivity(
            activityScenario)?.window?.decorView))))
            .check(matches(isDisplayed()))

        activityScenario.close()
    }

    // get activity context
    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }


    @Test
    fun selectLocationScreen_clickBackButton_backToAddReminderScreen() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        // Confirm that if we click back once, we end up back at the save reminder page
        Espresso.pressBack()
        onView(withId(R.id.fragment_save_reminder)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun selectLocationScreen_clickUpButton_backToAddReminderScreen() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        // Confirm that if we click up once, we end up back at the save reminder page
        onView(withContentDescription(activityScenario.getToolbarNavigationContentDescription())
        ).perform(click())
        onView(withId(R.id.fragment_save_reminder)).check(matches(isDisplayed()))

        activityScenario.close()
    }

}

fun <T : Activity> ActivityScenario<T>.getToolbarNavigationContentDescription()
        : String {
    var description = ""
    onActivity {
        description =
            it.findViewById<Toolbar>(R.id.action_bar).navigationContentDescription as String
    }
    return description
}
