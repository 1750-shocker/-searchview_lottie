package com.gta.widget.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airbnb.lottie.LottieDrawable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Integration test for SearchLottieAnimationView functionality
 * Tests animation loading, state transitions, and frame ranges with actual Android resources
 */
@RunWith(AndroidJUnit4::class)
class SearchLottieAnimationViewIntegrationTest {

    private lateinit var context: Context
    private lateinit var animationView: SearchLottieAnimationView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        animationView = SearchLottieAnimationView(context)
        
        // Wait a bit for the animation to load
        Thread.sleep(100)
    }

    @Test
    fun testAnimationResourceLoading() {
        // Verify that search_status.json loads correctly
        assertNotNull(animationView.composition, "Animation composition should not be null")
        
        // Verify animation properties
        assertEquals(1f, animationView.speed, "Animation speed should be 1f")
        assertEquals(LottieDrawable.RESTART, animationView.repeatMode, "Repeat mode should be RESTART")
        
        // Verify the animation file is loaded from the correct resource
        val composition = animationView.composition
        assertNotNull(composition, "Composition should be loaded")
        
        // Check that the animation has the expected frame count (270 frames as per JSON)
        assertTrue(composition!!.endFrame > 0, "Animation should have frames")
        assertEquals(270f, composition.endFrame, "Animation should have 270 frames")
    }

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
    fun testStateTransitions() {
        // Wait for animation to be ready
        Thread.sleep(200)
        
        // Test setting different states
        animationView.setState(SearchLottieAnimationView.SearchState.IDLE)
        assertEquals(0, animationView.minFrame?.toInt())
        assertEquals(95, animationView.maxFrame?.toInt())
        assertEquals(LottieDrawable.INFINITE, animationView.repeatCount)

        animationView.setState(SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING)
        assertEquals(96, animationView.minFrame?.toInt())
        assertEquals(198, animationView.maxFrame?.toInt())
        assertEquals(0, animationView.repeatCount) // Transition states don't repeat

        animationView.setState(SearchLottieAnimationView.SearchState.LISTENING)
        assertEquals(138, animationView.minFrame?.toInt())
        assertEquals(198, animationView.maxFrame?.toInt())
        assertEquals(LottieDrawable.INFINITE, animationView.repeatCount)

        animationView.setState(SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING)
        assertEquals(201, animationView.minFrame?.toInt())
        assertEquals(229, animationView.maxFrame?.toInt())
        assertEquals(0, animationView.repeatCount) // Transition states don't repeat

        animationView.setState(SearchLottieAnimationView.SearchState.LOADING)
        assertEquals(230, animationView.minFrame?.toInt())
        assertEquals(269, animationView.maxFrame?.toInt())
        assertEquals(LottieDrawable.INFINITE, animationView.repeatCount)
    }

    @Test
    fun testAnimationRepeatBehavior() {
        // Wait for animation to be ready
        Thread.sleep(200)
        
        // Test that looping states have infinite repeat
        val loopingStates = listOf(
            SearchLottieAnimationView.SearchState.IDLE,
            SearchLottieAnimationView.SearchState.LISTENING,
            SearchLottieAnimationView.SearchState.LOADING
        )
        
        for (state in loopingStates) {
            animationView.setState(state)
            assertEquals(LottieDrawable.INFINITE, animationView.repeatCount, 
                "State $state should have infinite repeat count")
        }
        
        // Test that transition states don't repeat
        val transitionStates = listOf(
            SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING,
            SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING
        )
        
        for (state in transitionStates) {
            animationView.setState(state)
            assertEquals(0, animationView.repeatCount, 
                "Transition state $state should not repeat")
        }
    }

    @Test
    fun testFrameRangeValidation() {
        // Wait for animation to be ready
        Thread.sleep(200)
        
        // Verify that all frame ranges are within the animation bounds
        val composition = animationView.composition
        assertNotNull(composition, "Composition should be loaded for frame validation")
        
        val totalFrames = composition!!.endFrame.toInt()
        
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
}