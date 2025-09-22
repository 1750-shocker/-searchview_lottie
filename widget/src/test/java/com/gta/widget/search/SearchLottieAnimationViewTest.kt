package com.gta.widget.search

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit test class for SearchLottieAnimationView functionality
 * Tests enum values and frame ranges without resource loading
 */
class SearchLottieAnimationViewTest {



    @Test
    fun testSearchStateEnumFrameRanges() {
        // Test all SearchState frame ranges are valid
        val states = SearchLottieAnimationView.SearchState.values()
        
        for (state in states) {
            assertTrue(state.startFrame >= 0, "Start frame should be non-negative for $state")
            assertTrue(state.endFrame > state.startFrame, "End frame should be greater than start frame for $state")
            assertTrue(state.endFrame <= 270, "End frame should not exceed total animation frames for $state")
        }
        
        // Test specific frame ranges match the design specification
        assertEquals(0, SearchLottieAnimationView.SearchState.IDLE.startFrame)
        assertEquals(95, SearchLottieAnimationView.SearchState.IDLE.endFrame)
        
        assertEquals(96, SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING.startFrame)
        assertEquals(198, SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING.endFrame)
        
        assertEquals(138, SearchLottieAnimationView.SearchState.LISTENING.startFrame)
        assertEquals(198, SearchLottieAnimationView.SearchState.LISTENING.endFrame)
        
        assertEquals(201, SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING.startFrame)
        assertEquals(229, SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING.endFrame)
        
        assertEquals(230, SearchLottieAnimationView.SearchState.LOADING.startFrame)
        assertEquals(269, SearchLottieAnimationView.SearchState.LOADING.endFrame)
    }







    @Test
    fun testFrameRangeValidation() {
        // Verify that all frame ranges are within expected animation bounds (270 frames)
        val totalFrames = 270
        
        SearchLottieAnimationView.SearchState.values().forEach { state ->
            assertTrue(
                state.startFrame >= 0, 
                "Start frame for $state should be >= 0"
            )
            assertTrue(
                state.startFrame < totalFrames, 
                "Start frame for $state should be < total frames ($totalFrames)"
            )
            assertTrue(
                state.endFrame >= state.startFrame, 
                "End frame for $state should be >= start frame"
            )
            assertTrue(
                state.endFrame < totalFrames, 
                "End frame for $state should be < total frames ($totalFrames)"
            )
        }
    }

    @Test
    fun testAnimationFrameRangeConsistency() {
        // Test that frame ranges are logically consistent for smooth transitions
        
        // Verify IDLE state starts at frame 0
        assertEquals(0, SearchLottieAnimationView.SearchState.IDLE.startFrame, 
            "IDLE state should start at frame 0")
        
        // Verify transition states connect properly
        val idleToListening = SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING
        val listening = SearchLottieAnimationView.SearchState.LISTENING
        val listeningToLoading = SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING
        val loading = SearchLottieAnimationView.SearchState.LOADING
        
        // IDLE_TO_LISTENING should start after IDLE ends
        assertTrue(idleToListening.startFrame > SearchLottieAnimationView.SearchState.IDLE.endFrame,
            "IDLE_TO_LISTENING should start after IDLE ends")
        
        // LISTENING should overlap with or connect to IDLE_TO_LISTENING
        assertTrue(listening.startFrame <= idleToListening.endFrame,
            "LISTENING should connect with IDLE_TO_LISTENING")
        
        // LISTENING_TO_LOADING should start after LISTENING range
        assertTrue(listeningToLoading.startFrame > listening.endFrame,
            "LISTENING_TO_LOADING should start after LISTENING range")
        
        // LOADING should start after LISTENING_TO_LOADING
        assertTrue(loading.startFrame > listeningToLoading.endFrame,
            "LOADING should start after LISTENING_TO_LOADING")
    }

    @Test
    fun testAnimationFrameRangesCoverExpectedSequence() {
        // Test that the frame ranges cover the expected animation sequence
        // Based on the Lottie JSON, we expect:
        // - IDLE: 0-95 (static microphone)
        // - IDLE_TO_LISTENING: 96-198 (transition animation)
        // - LISTENING: 138-198 (listening animation, overlaps with transition)
        // - LISTENING_TO_LOADING: 201-229 (transition to loading)
        // - LOADING: 230-269 (loading animation)
        
        assertEquals(0, SearchLottieAnimationView.SearchState.IDLE.startFrame)
        assertEquals(95, SearchLottieAnimationView.SearchState.IDLE.endFrame)
        
        assertEquals(96, SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING.startFrame)
        assertEquals(198, SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING.endFrame)
        
        assertEquals(138, SearchLottieAnimationView.SearchState.LISTENING.startFrame)
        assertEquals(198, SearchLottieAnimationView.SearchState.LISTENING.endFrame)
        
        assertEquals(201, SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING.startFrame)
        assertEquals(229, SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING.endFrame)
        
        assertEquals(230, SearchLottieAnimationView.SearchState.LOADING.startFrame)
        assertEquals(269, SearchLottieAnimationView.SearchState.LOADING.endFrame)
    }

    @Test
    fun testAnimationFrameRangesMatchLottieFile() {
        // Verify that frame ranges are compatible with the actual Lottie animation
        // The search_status.json file has 270 frames (0-269)
        val maxExpectedFrame = 269
        
        SearchLottieAnimationView.SearchState.values().forEach { state ->
            assertTrue(
                state.endFrame <= maxExpectedFrame,
                "State $state end frame ${state.endFrame} should not exceed max frame $maxExpectedFrame"
            )
        }
        
        // Verify that the frame ranges make sense for animation purposes
        // Each state should have at least a few frames for meaningful animation
        SearchLottieAnimationView.SearchState.values().forEach { state ->
            val frameCount = state.endFrame - state.startFrame + 1
            assertTrue(
                frameCount >= 1,
                "State $state should have at least 1 frame (has $frameCount)"
            )
        }
    }
}