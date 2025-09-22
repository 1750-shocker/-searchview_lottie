package com.gta.myapplication

import com.gta.widget.search.MotionVoiceSearchBar
import org.junit.Assert.*
import org.junit.Test

class SearchBarStateTest {

    @Test
    fun testMotionStateEnum() {
        // Test that all four MotionState values exist (requirement 2.1, 2.2, 2.3, 2.4)
        val states = MotionVoiceSearchBar.MotionState.entries
        assertEquals("Should have 4 states", 4, states.size)
        
        assertTrue("Should contain IDLE", states.contains(MotionVoiceSearchBar.MotionState.IDLE))
        assertTrue("Should contain AWAITING_INPUT", states.contains(MotionVoiceSearchBar.MotionState.AWAITING_INPUT))
        assertTrue("Should contain RECORDING", states.contains(MotionVoiceSearchBar.MotionState.RECORDING))
        assertTrue("Should contain PROCESSING", states.contains(MotionVoiceSearchBar.MotionState.PROCESSING))
    }

    @Test
    fun testStateToSearchIconStateMapping() {
        // Test that each MotionState maps to correct SearchIconState
        assertEquals(
            "IDLE should map to IDLE SearchState",
            com.gta.widget.search.SearchLottieAnimationView.SearchState.IDLE,
            MotionVoiceSearchBar.MotionState.IDLE.toSearchIconState()
        )
        
        assertEquals(
            "AWAITING_INPUT should map to IDLE SearchState",
            com.gta.widget.search.SearchLottieAnimationView.SearchState.IDLE,
            MotionVoiceSearchBar.MotionState.AWAITING_INPUT.toSearchIconState()
        )
        
        assertEquals(
            "RECORDING should map to IDLE_TO_LISTENING SearchState",
            com.gta.widget.search.SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING,
            MotionVoiceSearchBar.MotionState.RECORDING.toSearchIconState()
        )
        
        assertEquals(
            "PROCESSING should map to LISTENING_TO_LOADING SearchState",
            com.gta.widget.search.SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING,
            MotionVoiceSearchBar.MotionState.PROCESSING.toSearchIconState()
        )
    }

    @Test
    fun testSearchLottieAnimationViewStates() {
        // Test that SearchLottieAnimationView.SearchState has correct frame ranges
        val idleState = com.gta.widget.search.SearchLottieAnimationView.SearchState.IDLE
        assertEquals("IDLE start frame should be 0", 0, idleState.startFrame)
        assertEquals("IDLE end frame should be 95", 95, idleState.endFrame)
        
        val listeningState = com.gta.widget.search.SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING
        assertEquals("IDLE_TO_LISTENING start frame should be 96", 96, listeningState.startFrame)
        assertEquals("IDLE_TO_LISTENING end frame should be 198", 198, listeningState.endFrame)
        
        val loadingState = com.gta.widget.search.SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING
        assertEquals("LISTENING_TO_LOADING start frame should be 201", 201, loadingState.startFrame)
        assertEquals("LISTENING_TO_LOADING end frame should be 229", 229, loadingState.endFrame)
    }
}