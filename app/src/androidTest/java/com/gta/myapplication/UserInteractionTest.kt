package com.gta.myapplication

import android.widget.Toast
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
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
class UserInteractionTest {

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
    fun testStartIconClickHandler() {
        // Test clicking the start icon
        onView(withId(R.id.searchBarPlayground))
            .check(matches(isDisplayed()))

        // Click the start icon area - this should trigger the click listener
        onView(withId(R.id.searchBarPlayground))
            .perform(click())

        // Verify the search bar state changed from IDLE to AWAITING_INPUT
        // We can verify this by checking if the second toggle button is now checked
        onView(withId(R.id.btnSecond))
            .check(matches(isChecked()))
    }

    @Test
    fun testStartIconLongClickHandler() {
        // Test long clicking the start icon
        onView(withId(R.id.searchBarPlayground))
            .check(matches(isDisplayed()))

        // Long click the start icon area
        onView(withId(R.id.searchBarPlayground))
            .perform(longClick())

        // The long click should be handled (returns true from the listener)
        // We can verify the search bar is still displayed and functional
        onView(withId(R.id.searchBarPlayground))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStateToggleButtonsFunctionality() {
        // Test each toggle button changes the search bar state correctly

        // Test IDLE state button
        onView(withId(R.id.btnFirst))
            .perform(click())
            .check(matches(isChecked()))

        // Test AWAITING_INPUT state button
        onView(withId(R.id.btnSecond))
            .perform(click())
            .check(matches(isChecked()))

        // Test RECORDING state button
        onView(withId(R.id.btnThird))
            .perform(click())
            .check(matches(isChecked()))

        // Test PROCESSING state button
        onView(withId(R.id.btnForth))
            .perform(click())
            .check(matches(isChecked()))
    }

    @Test
    fun testStateToggleButtonsSequence() {
        // Test a sequence of state changes
        
        // Start with IDLE (should be default)
        onView(withId(R.id.btnFirst))
            .check(matches(isChecked()))

        // Change to AWAITING_INPUT
        onView(withId(R.id.btnSecond))
            .perform(click())
            .check(matches(isChecked()))

        // Change to RECORDING
        onView(withId(R.id.btnThird))
            .perform(click())
            .check(matches(isChecked()))

        // Change to PROCESSING
        onView(withId(R.id.btnForth))
            .perform(click())
            .check(matches(isChecked()))

        // Back to IDLE
        onView(withId(R.id.btnFirst))
            .perform(click())
            .check(matches(isChecked()))
    }

    @Test
    fun testSearchBarStateConsistencyWithToggleButtons() {
        // Test that the search bar state is consistent with toggle button selection
        
        activityScenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.fragments.firstOrNull() as? com.gta.myapplication.fragment.SearchBarFragment
            assertNotNull("SearchBarFragment should be available", fragment)
            
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertNotNull("SearchBar should be available", searchBar)

            // Test initial state
            assertEquals("Initial state should be IDLE", MotionVoiceSearchBar.MotionState.IDLE, searchBar.getState())
        }

        // Click AWAITING_INPUT button and verify state
        onView(withId(R.id.btnSecond)).perform(click())
        
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("State should be AWAITING_INPUT", MotionVoiceSearchBar.MotionState.AWAITING_INPUT, searchBar.getState())
        }

        // Click RECORDING button and verify state
        onView(withId(R.id.btnThird)).perform(click())
        
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("State should be RECORDING", MotionVoiceSearchBar.MotionState.RECORDING, searchBar.getState())
        }

        // Click PROCESSING button and verify state
        onView(withId(R.id.btnForth)).perform(click())
        
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("State should be PROCESSING", MotionVoiceSearchBar.MotionState.PROCESSING, searchBar.getState())
        }
    }

    @Test
    fun testClickToStateTransition() {
        // Test the specific behavior where clicking start icon transitions from IDLE to AWAITING_INPUT
        
        // Ensure we start in IDLE state
        onView(withId(R.id.btnFirst)).perform(click())
        
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("Should start in IDLE state", MotionVoiceSearchBar.MotionState.IDLE, searchBar.getState())
        }

        // Click the search bar (start icon area)
        onView(withId(R.id.searchBarPlayground)).perform(click())

        // Verify state changed to AWAITING_INPUT
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("Should transition to AWAITING_INPUT", MotionVoiceSearchBar.MotionState.AWAITING_INPUT, searchBar.getState())
        }

        // Verify the toggle button also updated
        onView(withId(R.id.btnSecond)).check(matches(isChecked()))
    }

    @Test
    fun testMultipleClickInteractions() {
        // Test multiple click interactions in sequence
        
        // Start in IDLE
        onView(withId(R.id.btnFirst)).perform(click())
        
        // Click search bar multiple times
        onView(withId(R.id.searchBarPlayground)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())
        onView(withId(R.id.searchBarPlayground)).perform(click())

        // Should still be in AWAITING_INPUT (or whatever the click handler sets)
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            // The click handler only transitions from IDLE to AWAITING_INPUT
            assertEquals("Multiple clicks should maintain AWAITING_INPUT", MotionVoiceSearchBar.MotionState.AWAITING_INPUT, searchBar.getState())
        }
    }

    @Test
    fun testLongClickDoesNotChangeState() {
        // Test that long click doesn't change the state (only shows toast)
        
        // Start in IDLE
        onView(withId(R.id.btnFirst)).perform(click())
        
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("Should start in IDLE", MotionVoiceSearchBar.MotionState.IDLE, searchBar.getState())
        }

        // Long click the search bar
        onView(withId(R.id.searchBarPlayground)).perform(longClick())

        // State should remain IDLE
        activityScenario.onActivity { activity ->
            val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
            assertEquals("Long click should not change state", MotionVoiceSearchBar.MotionState.IDLE, searchBar.getState())
        }
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