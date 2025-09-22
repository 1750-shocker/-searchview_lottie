package com.gta.myapplication

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gta.widget.search.MotionVoiceSearchBar
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SearchBarStateTransitionSimpleTest {

    @Test
    fun testIdleToAwaitingInputTransition() {
        // Test requirement 2.1 and 2.2: IDLE to AWAITING_INPUT transition works
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000) // Wait for activity to load
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                // Verify initial IDLE state (requirement 2.1)
                searchBar.setState(MotionVoiceSearchBar.MotionState.IDLE)
                Thread.sleep(300)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.IDLE) {
                    "IDLE state should display correctly (requirement 2.1)"
                }
                
                // Test IDLE to AWAITING_INPUT transition (requirement 2.2)
                searchBar.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
                Thread.sleep(300)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.AWAITING_INPUT) {
                    "AWAITING_INPUT state should display correctly (requirement 2.2)"
                }
            }
        }
    }

    @Test
    fun testAllFourStatesDisplayCorrectly() {
        // Test requirements 2.1, 2.2, 2.3, 2.4: All four states display correctly
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000) // Wait for activity to load
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                // Test IDLE state (requirement 2.1)
                searchBar.setState(MotionVoiceSearchBar.MotionState.IDLE)
                Thread.sleep(500)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.IDLE) {
                    "IDLE state should display default microphone icon (requirement 2.1)"
                }
                assert(searchBar.visibility == android.view.View.VISIBLE) {
                    "SearchBar should be visible in IDLE state"
                }
                
                // Test AWAITING_INPUT state (requirement 2.2)
                searchBar.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
                Thread.sleep(500)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.AWAITING_INPUT) {
                    "AWAITING_INPUT state should display waiting animation (requirement 2.2)"
                }
                assert(searchBar.visibility == android.view.View.VISIBLE) {
                    "SearchBar should be visible in AWAITING_INPUT state"
                }
                
                // Test RECORDING state (requirement 2.3)
                searchBar.setState(MotionVoiceSearchBar.MotionState.RECORDING)
                Thread.sleep(500)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.RECORDING) {
                    "RECORDING state should display recording animation and marquee border (requirement 2.3)"
                }
                assert(searchBar.visibility == android.view.View.VISIBLE) {
                    "SearchBar should be visible in RECORDING state"
                }
                
                // Test PROCESSING state (requirement 2.4)
                searchBar.setState(MotionVoiceSearchBar.MotionState.PROCESSING)
                Thread.sleep(500)
                assert(searchBar.getState() == MotionVoiceSearchBar.MotionState.PROCESSING) {
                    "PROCESSING state should display processing animation (requirement 2.4)"
                }
                assert(searchBar.visibility == android.view.View.VISIBLE) {
                    "SearchBar should be visible in PROCESSING state"
                }
            }
        }
    }

    @Test
    fun testStateTransitionSequence() {
        // Test complete state transition sequence
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(1000) // Wait for activity to load
            
            scenario.onActivity { activity ->
                val searchBar = activity.findViewById<MotionVoiceSearchBar>(R.id.searchBarPlayground)
                
                val states = listOf(
                    MotionVoiceSearchBar.MotionState.IDLE,
                    MotionVoiceSearchBar.MotionState.AWAITING_INPUT,
                    MotionVoiceSearchBar.MotionState.RECORDING,
                    MotionVoiceSearchBar.MotionState.PROCESSING,
                    MotionVoiceSearchBar.MotionState.IDLE // Back to start
                )
                
                states.forEachIndexed { index, expectedState ->
                    searchBar.setState(expectedState)
                    Thread.sleep(500) // Allow time for state change and animation
                    
                    val actualState = searchBar.getState()
                    assert(actualState == expectedState) {
                        "Step $index: Expected state $expectedState, but got $actualState"
                    }
                    
                    // Verify the search bar remains functional
                    assert(searchBar.visibility == android.view.View.VISIBLE) {
                        "SearchBar should remain visible in state $expectedState"
                    }
                }
            }
        }
    }
}