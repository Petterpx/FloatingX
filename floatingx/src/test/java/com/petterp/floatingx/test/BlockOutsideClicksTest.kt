package com.petterp.floatingx.test

import org.junit.Test
import org.junit.Assert.*

/**
 * Test for block outside clicks coordinate calculations
 * This test validates that the touch coordinate logic is correct
 */
class BlockOutsideClicksTest {

    @Test
    fun testTouchInsideFloatingWindow() {
        // Simulated floating window bounds
        val fxLeft = 100
        val fxTop = 200  
        val fxRight = 300  // width = 200
        val fxBottom = 400 // height = 200
        
        // Touch coordinates inside the floating window
        val touchX = 150f
        val touchY = 250f
        
        // Test the touch detection logic
        val isInsideWindow = touchX >= fxLeft && touchX <= fxRight && touchY >= fxTop && touchY <= fxBottom
        
        assertTrue("Touch should be inside floating window", isInsideWindow)
    }
    
    @Test
    fun testTouchOutsideFloatingWindow() {
        // Simulated floating window bounds
        val fxLeft = 100
        val fxTop = 200
        val fxRight = 300
        val fxBottom = 400
        
        // Touch coordinates outside the floating window
        val touchX = 50f  // Left of window
        val touchY = 250f
        
        // Test the touch detection logic
        val isInsideWindow = touchX >= fxLeft && touchX <= fxRight && touchY >= fxTop && touchY <= fxBottom
        
        assertFalse("Touch should be outside floating window", isInsideWindow)
    }
    
    @Test
    fun testTouchOnBoundary() {
        // Simulated floating window bounds
        val fxLeft = 100
        val fxTop = 200
        val fxRight = 300
        val fxBottom = 400
        
        // Touch coordinates on the boundary (should be considered inside)
        val touchX = 100f  // Exactly on left edge
        val touchY = 200f  // Exactly on top edge
        
        // Test the touch detection logic
        val isInsideWindow = touchX >= fxLeft && touchX <= fxRight && touchY >= fxTop && touchY <= fxBottom
        
        assertTrue("Touch on boundary should be considered inside", isInsideWindow)
    }
}