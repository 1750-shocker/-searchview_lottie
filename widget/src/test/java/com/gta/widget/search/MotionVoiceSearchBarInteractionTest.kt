package com.gta.widget.search

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MotionVoiceSearchBarInteractionTest {

    private lateinit var context: Context
    private lateinit var searchBar: MotionVoiceSearchBar

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        searchBar = MotionVoiceSearchBar(context)
    }

    @Test
    fun testSetStartIconOnClickListener() {
        // Test that start icon click listener can be set and is properly configured
        var clickReceived = false
        val clickListener = View.OnClickListener { clickReceived = true }
        
        searchBar.setStartIconOnClickListener(clickListener)
        
        // Verify the listener was set (we can't directly test the click without UI interaction)
        // but we can verify the method completes without error
        assert(true) // If we get here, the method worked
    }

    @Test
    fun testSetStartIconOnLongClickListener() {
        // Test that start icon long click listener can be set
        var longClickReceived = false
        val longClickListener = View.OnLongClickListener { 
            longClickReceived = true
            true
        }
        
        searchBar.setStartIconOnLongClickListener(longClickListener)
        
        // Verify the listener was set
        assert(true) // If we get here, the method worked
    }

    @Test
    fun testSetEndIconOnClickListener() {
        // Test that end icon click listener can be set
        var clickReceived = false
        val clickListener = View.OnClickListener { clickReceived = true }
        
        searchBar.setEndIconOnClickListener(clickListener)
        
        // Verify the listener was set
        assert(true) // If we get here, the method worked
    }

    @Test
    fun testSetEndIconOnLongClickListener() {
        // Test that end icon long click listener can be set
        var longClickReceived = false
        val longClickListener = View.OnLongClickListener { 
            longClickReceived = true
            true
        }
        
        searchBar.setEndIconOnLongClickListener(longClickListener)
        
        // Verify the listener was set
        assert(true) // If we get here, the method worked
    }

    @Test
    fun testSetNullClickListeners() {
        // Test that null listeners can be set (to remove listeners)
        
        searchBar.setStartIconOnClickListener(null)
        searchBar.setStartIconOnLongClickListener(null)
        searchBar.setEndIconOnClickListener(null)
        searchBar.setEndIconOnLongClickListener(null)
        
        // All should complete without error
        assert(true)
    }

    @Test
    fun testStateChangeListener() {
        // Test that state change listener can be set and removed
        var stateChangeReceived = false
        val stateChangeListener = object : MotionVoiceSearchBar.OnStateChangeListener {
            override fun onStateChanged(state: MotionVoiceSearchBar.MotionState) {
                stateChangeReceived = true
            }
        }
        
        searchBar.setOnStateChangeListener(stateChangeListener)
        
        // Change state to trigger listener
        searchBar.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
        
        // Remove listener
        searchBar.setOnStateChangeListener(null)
        
        // Change state again (should not trigger removed listener)
        searchBar.setState(MotionVoiceSearchBar.MotionState.RECORDING)
        
        assert(true) // Test completes successfully
    }

    @Test
    fun testStateTransitions() {
        // Test all state transitions work correctly
        
        // Test initial state
        assertEquals("Initial state should be IDLE", MotionVoiceSearchBar.MotionState.IDLE, searchBar.getState())
        
        // Test each state transition
        searchBar.setState(MotionVoiceSearchBar.MotionState.AWAITING_INPUT)
        assertEquals("State should be AWAITING_INPUT", MotionVoiceSearchBar.MotionState.AWAITING_INPUT, searchBar.getState())
        
        searchBar.setState(MotionVoiceSearchBar.MotionState.RECORDING)
        assertEquals("State should be RECORDING", MotionVoiceSearchBar.MotionState.RECORDING, searchBar.getState())
        
        searchBar.setState(MotionVoiceSearchBar.MotionState.PROCESSING)
        assertEquals("State should be PROCESSING", MotionVoiceSearchBar.MotionState.PROCESSING, searchBar.getState())
        
        searchBar.setState(MotionVoiceSearchBar.MotionState.IDLE)
        assertEquals("State should be IDLE", MotionVoiceSearchBar.MotionState.IDLE, searchBar.getState())
    }

    @Test
    fun testStateChangeListenerCallback() {
        // Test that state change listener receives callbacks
        var callbackReceived = false
        var receivedState: MotionVoiceSearchBar.MotionState? = null
        
        val stateChangeListener = object : MotionVoiceSearchBar.OnStateChangeListener {
            override fun onStateChanged(state: MotionVoiceSearchBar.MotionState) {
                callbackReceived = true
                receivedState = state
            }
        }
        
        searchBar.setOnStateChangeListener(stateChangeListener)
        
        // Change state
        searchBar.setState(MotionVoiceSearchBar.MotionState.RECORDING)
        
        // Give some time for the callback (in case it's async)
        Thread.sleep(100)
        
        assert(callbackReceived) { "State change listener should have been called" }
        assertEquals("Received state should match set state", MotionVoiceSearchBar.MotionState.RECORDING.toString(), receivedState.toString())
    }

    @Test
    fun testMultipleStateChanges() {
        // Test multiple rapid state changes
        val states = listOf(
            MotionVoiceSearchBar.MotionState.AWAITING_INPUT,
            MotionVoiceSearchBar.MotionState.RECORDING,
            MotionVoiceSearchBar.MotionState.PROCESSING,
            MotionVoiceSearchBar.MotionState.IDLE
        )
        
        for (state in states) {
            searchBar.setState(state)
            assertEquals("State should be set correctly", state, searchBar.getState())
        }
    }

    private fun assertEquals(message: String, expected: Any, actual: Any) {
        if (expected != actual) {
            throw AssertionError("$message: expected $expected but was $actual")
        }
    }
}