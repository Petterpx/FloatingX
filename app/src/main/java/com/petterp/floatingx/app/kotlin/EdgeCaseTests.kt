package com.petterp.floatingx.app.kotlin

import android.content.Context
import android.util.Log
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.listener.control.IFxControl

/**
 * Edge case tests for the new getWindowManagerLayoutParams() functionality
 */
object EdgeCaseTests {
    
    /**
     * Test accessing WindowManager.LayoutParams on various control types
     */
    fun testAllControlTypes(context: Context) {
        Log.d("EdgeCaseTests", "Starting comprehensive tests...")
        
        // Test global controls
        FloatingX.allControls().forEach { (tag, control) ->
            testSingleControl(tag, control)
        }
        
        // Test null control
        val nullControl = FloatingX.controlOrNull("non_existent_tag")
        if (nullControl == null) {
            Log.d("EdgeCaseTests", "✅ Non-existent control returns null as expected")
        } else {
            Log.w("EdgeCaseTests", "❌ Non-existent control should return null")
        }
    }
    
    /**
     * Test a single control for WindowManager.LayoutParams availability
     */
    private fun testSingleControl(tag: String, control: IFxControl) {
        try {
            val layoutParams = control.getWindowManagerLayoutParams()
            val managerView = control.getManagerView()
            val isShowing = control.isShow()
            
            Log.d("EdgeCaseTests", "Testing control '$tag':")
            Log.d("EdgeCaseTests", "  - IsShowing: $isShowing")
            Log.d("EdgeCaseTests", "  - ManagerView: ${managerView != null}")
            Log.d("EdgeCaseTests", "  - LayoutParams: ${layoutParams != null}")
            
            if (layoutParams != null) {
                Log.d("EdgeCaseTests", "  - Flags: ${layoutParams.flags}")
                Log.d("EdgeCaseTests", "  - Type: ${layoutParams.type}")
                Log.d("EdgeCaseTests", "  - This appears to be a system floating window")
            } else {
                Log.d("EdgeCaseTests", "  - This appears to be an app-level floating window or uninitialized")
            }
            
        } catch (e: Exception) {
            Log.e("EdgeCaseTests", "Error testing control '$tag': ${e.message}", e)
        }
    }
    
    /**
     * Test safe modification of WindowManager.LayoutParams
     */
    fun testSafeModification(control: IFxControl, context: Context): Boolean {
        return try {
            val originalLayoutParams = control.getWindowManagerLayoutParams()
            if (originalLayoutParams == null) {
                Log.i("EdgeCaseTests", "No WindowManager.LayoutParams to modify (app-level window)")
                return true
            }
            
            // Store original flags
            val originalFlags = originalLayoutParams.flags
            Log.d("EdgeCaseTests", "Original flags: $originalFlags")
            
            // Test modification
            val newFlags = originalFlags or android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            originalLayoutParams.flags = newFlags
            Log.d("EdgeCaseTests", "Modified flags: ${originalLayoutParams.flags}")
            
            // Verify change took effect
            val changeApplied = originalLayoutParams.flags == newFlags
            Log.d("EdgeCaseTests", "Change applied successfully: $changeApplied")
            
            // Restore original flags
            originalLayoutParams.flags = originalFlags
            Log.d("EdgeCaseTests", "Restored flags: ${originalLayoutParams.flags}")
            
            true
        } catch (e: Exception) {
            Log.e("EdgeCaseTests", "Error during safe modification: ${e.message}", e)
            false
        }
    }
    
    /**
     * Test behavior when control is hidden/shown
     */
    fun testShowHideBehavior(control: IFxControl) {
        Log.d("EdgeCaseTests", "Testing show/hide behavior...")
        
        val initialLayoutParams = control.getWindowManagerLayoutParams()
        val wasShowing = control.isShow()
        
        try {
            if (wasShowing) {
                control.hide()
                val hiddenLayoutParams = control.getWindowManagerLayoutParams()
                Log.d("EdgeCaseTests", "Hidden - LayoutParams available: ${hiddenLayoutParams != null}")
                
                control.show()
                val shownLayoutParams = control.getWindowManagerLayoutParams()
                Log.d("EdgeCaseTests", "Shown - LayoutParams available: ${shownLayoutParams != null}")
            } else {
                control.show()
                val shownLayoutParams = control.getWindowManagerLayoutParams()
                Log.d("EdgeCaseTests", "Shown - LayoutParams available: ${shownLayoutParams != null}")
                
                control.hide()
                val hiddenLayoutParams = control.getWindowManagerLayoutParams()
                Log.d("EdgeCaseTests", "Hidden - LayoutParams available: ${hiddenLayoutParams != null}")
            }
        } finally {
            // Restore original state
            if (wasShowing) {
                control.show()
            } else {
                control.hide()
            }
        }
    }
}