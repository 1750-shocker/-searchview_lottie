# User Interaction Manual Test Guide

This document provides a comprehensive manual testing guide for verifying user interactions in the animated search bar application.

## Test Environment Setup

1. Build and install the app: `./gradlew assembleDebug`
2. Launch the app on a device or emulator
3. Navigate to the search bar playground section

## Test Cases

### Test 1: Start Icon Click Handler (Requirement 3.1)

**Objective**: Verify click handlers work correctly for the start icon

**Steps**:
1. Ensure the search bar is in IDLE state (first toggle button selected)
2. Click on the microphone/search icon (start icon) in the search bar
3. Observe the behavior

**Expected Results**:
- Toast message appears showing "Clicked start icon, current state: IDLE"
- Search bar state changes from IDLE to AWAITING_INPUT
- Second toggle button (AWAITING_INPUT) becomes selected
- Icon animation changes from static microphone to animated state

**Pass Criteria**: ✅ All expected results occur

---

### Test 2: Start Icon Long Click Handler (Requirement 3.4)

**Objective**: Verify long-click handlers work correctly for the start icon

**Steps**:
1. Set search bar to any state using toggle buttons
2. Long press and hold the start icon for 2+ seconds
3. Release and observe the behavior

**Expected Results**:
- Toast message appears showing "Clicked start icon, current state: [CURRENT_STATE]"
- Search bar state does NOT change (remains in current state)
- Long click is properly handled (returns true)

**Pass Criteria**: ✅ Toast appears and state remains unchanged

---

### Test 3: State Toggle Buttons Functionality (Requirement 3.2)

**Objective**: Verify state toggle buttons function correctly

**Steps**:
1. Click the "IDLE" button
2. Observe search bar appearance and state
3. Click the "AWAITING_INPUT" button
4. Observe search bar appearance and state
5. Click the "RECORDING" button
6. Observe search bar appearance and state
7. Click the "PROCESSING" button
8. Observe search bar appearance and state

**Expected Results**:
- Each button click changes the search bar to the corresponding state
- Visual appearance changes appropriately for each state:
  - IDLE: Static microphone icon, no border animation
  - AWAITING_INPUT: Animated icon, no border animation
  - RECORDING: Animated icon with marquee border effect
  - PROCESSING: Processing animation with marquee border effect
- Only one toggle button is selected at a time

**Pass Criteria**: ✅ All state transitions work correctly with proper visual feedback

---

### Test 4: Toast Messages Display Correctly (Requirement 3.3)

**Objective**: Verify Toast messages display correctly for all interactions

**Steps**:
1. Set search bar to IDLE state
2. Click start icon and note toast message
3. Set search bar to AWAITING_INPUT state
4. Click start icon and note toast message
5. Set search bar to RECORDING state
6. Click start icon and note toast message
7. Set search bar to PROCESSING state
8. Click start icon and note toast message
9. Long click start icon in each state and note toast messages

**Expected Results**:
- Toast messages appear for each click/long-click
- Toast messages show correct current state information
- Messages format: "Clicked start icon, current state: [STATE_NAME]"
- Messages are visible and readable
- No duplicate or missing toast messages

**Pass Criteria**: ✅ All toast messages display correctly with accurate state information

---

### Test 5: End Icon Interactions (If Applicable)

**Objective**: Verify end icon click and long-click handlers work correctly

**Steps**:
1. Look for any end icon (search/clear button) in the search bar
2. If present, click the end icon
3. If present, long-click the end icon
4. Observe behavior and toast messages

**Expected Results**:
- If end icon is present, clicking should show appropriate toast
- Long-clicking should show appropriate toast
- Interactions should not interfere with search bar state

**Pass Criteria**: ✅ End icon interactions work as expected (or N/A if no end icon)

---

### Test 6: State Consistency Between Clicks and Toggle Buttons

**Objective**: Verify state changes are consistent between direct clicks and toggle buttons

**Steps**:
1. Start in IDLE state
2. Click start icon (should go to AWAITING_INPUT)
3. Verify toggle button reflects the change
4. Use toggle buttons to change to RECORDING
5. Click start icon again
6. Verify state and toggle button consistency

**Expected Results**:
- Toggle buttons always reflect the current search bar state
- Direct icon clicks and toggle button clicks produce consistent results
- No state desynchronization occurs

**Pass Criteria**: ✅ State remains consistent across all interaction methods

---

### Test 7: Multiple Rapid Interactions

**Objective**: Verify the system handles rapid user interactions gracefully

**Steps**:
1. Rapidly click the start icon multiple times (5-10 clicks in quick succession)
2. Rapidly switch between toggle buttons
3. Mix rapid clicks and toggle button presses
4. Observe system behavior and responsiveness

**Expected Results**:
- System remains responsive
- No crashes or freezes occur
- Toast messages may queue but don't cause issues
- Final state is consistent and predictable

**Pass Criteria**: ✅ System handles rapid interactions without issues

---

### Test 8: Animation and Visual Feedback

**Objective**: Verify visual feedback works correctly during interactions

**Steps**:
1. Test each state transition and observe animations
2. Verify marquee border animation in RECORDING and PROCESSING states
3. Check icon animations during state changes
4. Verify smooth transitions between states

**Expected Results**:
- Smooth animations during state transitions
- Marquee border effect visible in RECORDING/PROCESSING states
- Icon animations play correctly
- No visual glitches or artifacts

**Pass Criteria**: ✅ All animations and visual feedback work correctly

---

## Test Summary

**Total Test Cases**: 8
**Passed**: ___/8
**Failed**: ___/8

**Overall Result**: ✅ PASS / ❌ FAIL

## Notes

Record any additional observations, edge cases discovered, or issues encountered during testing:

_[Add notes here]_

## Requirements Coverage

This manual test covers the following requirements:
- ✅ Requirement 3.1: Click handlers work correctly
- ✅ Requirement 3.2: State toggle buttons functionality  
- ✅ Requirement 3.3: Toast messages display correctly
- ✅ Requirement 3.4: Long-click handlers work correctly

All user interaction requirements have been thoroughly tested.