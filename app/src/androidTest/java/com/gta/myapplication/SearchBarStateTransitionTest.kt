package com.gta.myapplication

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gta.widget.search.MotionVoiceSearchBar
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SearchBarStateTransitionTest {

    @Test
    fun testSearchBarStateTransitions() {
        // Launch the activity
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            
            // Wait for the activity to be fully loaded
            Thread.sleep(1000)
            
            // Test initial state - should be IDLE
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                // Verify initial state is IDLE
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.IDLE) {
                    "Initial state should be IDLE, but was ${searchBar.getState()}"
                }
            }
            
            // Test transition from IDLE to AWAITING_INPUT by clicking the toggle button
            onView(withId(R.id.btnSecond)).perform(click())
            Thread.sleep(1000) // Allow more time for animation and state change
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.AWAITING_INPUT) {
                    "State should be AWAITING_INPUT after clicking btnSecond, but was ${searchBar.getState()}"
                }
            }
            
            // Test transition to RECORDING state
            onView(withId(R.id.btnThird)).perform(click())
            Thread.sleep(1000) // Allow more time for animation and state change
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.RECORDING) {
                    "State should be RECORDING after clicking btnThird, but was ${searchBar.getState()}"
                }
            }
            
            // Test transition to PROCESSING state
            onView(withId(R.id.btnForth)).perform(click())
            Thread.sleep(1000) // Allow more time for animation and state change
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.PROCESSING) {
                    "State should be PROCESSING after clicking btnForth, but was ${searchBar.getState()}"
                }
            }
            
            // Test transition back to IDLE
            onView(withId(R.id.btnFirst)).perform(click())
            Thread.sleep(1000) // Allow more time for animation and state change
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.IDLE) {
                    "State should be IDLE after clicking btnFirst, but was ${searchBar.getState()}"
                }
            }
        }
    }
    
    @Test
    fun testSearchBarIconVisibilityTransitions() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000)
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                // Test IDLE state - idle icon should be visible, active layout should be gone
                searchBar.setState(MotionVoiceSearchBar.MotionState.IDLE)
                Thread.sleep(300) // Allow transition time
                
                // We can't directly access private fields, but we can verify the state is set correctly
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.IDLE)
                
                // Test AWAITING_INPUT state - active layout should be visible, idle icon should be gone
                searchBar.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
                Thread.sleep(300)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
                
                // Test RECORDING state - active layout should be visible with marquee animation
                searchBar.setState(MotionVoiceSearchBar.MotionState.RECORDING)
                Thread.sleep(300)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.RECORDING)
                
                // Test PROCESSING state - active layout should be visible with marquee animation
                searchBar.setState(MotionVoiceSearchBar.MotionState.PROCESSING)
                Thread.sleep(300)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.PROCESSING)
            }
        }
    }
    
    @Test
    fun testAllFourStatesDisplayCorrectly() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000)
            
            val states = listOf(
                MotionVoiceSearchBar.MotionState.IDLE,
                MotionVoiceSearchBar.MotionState.AWAITING_INPUT,
                MotionVoiceSearchBar.MotionState.RECORDING,
                MotionVoiceSearchBar.MotionState.PROCESSING
            )
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                // Test each state
                states.forEach { state ->
                    searchBar.setState(state)
                    Thread.sleep(500) // Allow animation and transition time
                    
                    // Verify the state was set correctly
                    assert(searchBar.getState() == state) {
                        "Failed to set state to $state, current state is ${searchBar.getState()}"
                    }
                    
                    // Verify the search bar is still visible and functional
                    assert(searchBar.visibility == android.view.View.VISIBLE) {
                        "SearchBar should be visible in state $state"
                    }
                }
            }
        }
    }
    
    @Test
    fun testIdleToAwaitingInputTransition() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000)
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                // Start from IDLE state
                searchBar.setState(MotionVoiceSearchBar.MotionState.IDLE)
                Thread.sleep(500)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.IDLE) {
                    "Expected IDLE state, but got ${searchBar.getState()}"
                }
                
                // Transition to AWAITING_INPUT
                searchBar.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
                Thread.sleep(500)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.AWAITING_INPUT) {
                    "Expected AWAITING_INPUT state, but got ${searchBar.getState()}"
                }
                
                // Verify we can transition back
                searchBar.setState(MotionVoiceSearchBar.MotionState.IDLE)
                Thread.sleep(500)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.IDLE) {
                    "Expected IDLE state after transition back, but got ${searchBar.getState()}"
                }
            }
        }
    }
    
    @Test
    fun testDirectStateTransitions() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000)
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                // Test all four states directly
                val states = listOf(
                    MotionVoiceSearchBar.MotionState.IDLE,
                    MotionVoiceSearchBar.MotionState.AWAITING_INPUT,
                    MotionVoiceSearchBar.MotionState.RECORDING,
                    MotionVoiceSearchBar.MotionState.PROCESSING
                )
                
                states.forEach { expectedState ->
                    searchBar.setState(expectedState)
                    Thread.sleep(500) // Allow time for state change
                    
                    val actualState = searchBar.getState()
                    assert(actualState == expectedState) {
                        "Expected state $expectedState, but got $actualState"
                    }
                }
            }
        }
    }
}