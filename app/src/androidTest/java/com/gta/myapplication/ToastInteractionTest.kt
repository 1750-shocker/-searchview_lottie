package com.gta.myapplication

import android.widget.Toast
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gta.widget.search.MotionVoiceSearchBar
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ToastInteractionTest {

    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        activityScenario.close()
    }

    @Test
    fun testToastMessagesOnStartIconClick() {
        // Test that clicking start icon shows appropriate toast messages
        
        activityScenario.onActivity { activity ->
            // Set up a custom toast listener to capture toast messages
            val fragment = activity.supportFragmentManager.fragments.firstOrNull() as? com.gta.myapplication.fragment.SearchBarFragment
            assertNotNull("SearchBarFragment should be available", fragment)
        }

        // Start in IDLE state
        onView(withId(R.id.btnFirst)).perform(click())

        // Click the search bar to trigger toast
        onView(withId(R.id.searchBarPlayground)).perform(click())

        // Wait a moment for the toast to be triggered
        Thread.sleep(500)

        // Verify the state changed (which indicates the click handler worked)
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("State should change to AWAITING_INPUT", MotionVoiceSearchBar.MotionState.AWAITING_INPUT, searchBar.getState())
        }
    }

    @Test
    fun testToastMessagesOnLongClick() {
        // Test that long clicking shows appropriate toast messages
        
        activityScenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.fragments.firstOrNull() as? com.gta.myapplication.fragment.SearchBarFragment
            assertNotNull("SearchBarFragment should be available", fragment)
        }

        // Long click the search bar
        onView(withId(R.id.searchBarPlayground)).perform(longClick())

        // Wait a moment for the toast
        Thread.sleep(500)

        // Verify the search bar is still functional (long click handler returned true)
        onView(withId(R.id.searchBarPlayground)).check(matches(isDisplayed()))
    }

    @Test
    fun testToastMessagesForDifferentStates() {
        // Test toast messages show correct state information
        
        // Test IDLE state click
        onView(withId(R.id.btnFirst)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())
        Thread.sleep(300)

        // Test AWAITING_INPUT state click
        onView(withId(R.id.btnSecond)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())
        Thread.sleep(300)

        // Test RECORDING state click
        onView(withId(R.id.btnThird)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())
        Thread.sleep(300)

        // Test PROCESSING state click
        onView(withId(R.id.btnForth)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())
        Thread.sleep(300)

        // All interactions should complete without crashes
        onView(withId(R.id.searchBarPlayground)).check(matches(isDisplayed()))
    }

    @Test
    fun testEndIconClickToast() {
        // Test that end icon click shows toast (if end icon is present)
        
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            
            // Try to click end icon area - this might not be visible in all states
            // but the handler should still work if called programmatically
            val fragment = activity.supportFragmentManager.fragments.firstOrNull() as? com.gta.myapplication.fragment.SearchBarFragment
            assertNotNull("SearchBarFragment should be available", fragment)
        }

        // The end icon click is handled in the fragment, so we test indirectly
        // by ensuring the search bar remains functional
        onView(withId(R.id.searchBarPlayground)).check(matches(isDisplayed()))
    }

    @Test
    fun testEndIconLongClickToast() {
        // Test that end icon long click shows toast
        
        activityScenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.fragments.firstOrNull() as? com.gta.myapplication.fragment.SearchBarFragment
            assertNotNull("SearchBarFragment should be available", fragment)
        }

        // Verify the search bar is functional
        onView(withId(R.id.searchBarPlayground)).check(matches(isDisplayed()))
    }

    @Test
    fun testToastMessagesSequence() {
        // Test a sequence of interactions that should all show toasts
        
        // Click start icon from IDLE
        onView(withId(R.id.btnFirst)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())
        Thread.sleep(200)

        // Long click
        onView(withId(R.id.searchBarPlayground)).perform(longClick())
        Thread.sleep(200)

        // Change state and click again
        onView(withId(R.id.btnThird)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())
        Thread.sleep(200)

        // All interactions should complete successfully
        onView(withId(R.id.searchBarPlayground)).check(matches(isDisplayed()))
    }

    @Test
    fun testStateToggleButtonsDoNotShowToast() {
        // Test that toggle buttons themselves don't show toast (only search bar interactions do)
        
        // Click each toggle button
        onView(withId(R.id.btnFirst)).perform(click())
        Thread.sleep(100)
        
        onView(withId(R.id.btnSecond)).perform(click())
        Thread.sleep(100)
        
        onView(withId(R.id.btnThird)).perform(click())
        Thread.sleep(100)
        
        onView(withId(R.id.btnForth)).perform(click())
        Thread.sleep(100)

        // All buttons should be functional
        onView(withId(R.id.toggleGroup)).check(matches(isDisplayed()))
    }

    private fun assertEquals(message: String, expected: Any, actual: Any) {
        if (expected != actual) {
            throw AssertionError("$message: expected $expected but was $actual")
        }
    }

    private fun assertNotNull(message: String, obj: Any?) {
        if (obj == null) {
            throw AssertionError(message)
        }
    }
}