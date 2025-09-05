package com.petterp.floatingx.app.kotlin

import android.content.Context
import android.util.Log
import android.view.WindowManager
import com.petterp.floatingx.listener.control.IFxControl

/**
 * Test class to demonstrate the new getWindowManagerLayoutParams() functionality
 * 
 * This example shows how to request and lose focus for floating windows,
 * which was the main use case mentioned in the issue.
 */
object TestWindowManagerLayoutParams {
    
    /**
     * Request focus for a floating window by modifying its WindowManager.LayoutParams
     * This matches the user's requested functionality from the issue
     */
    fun requestFocusFloatingView(fxControl: IFxControl, context: Context) {
        try {
            val managerView = fxControl.getManagerView()
            val layoutParams = fxControl.getWindowManagerLayoutParams() // NEW METHOD
            
            if (layoutParams == null) {
                Log.w("FloatingPro", "WindowManager.LayoutParams not available - probably an app-level floating window")
                return
            }

            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                 WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                 WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            managerView?.post {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.updateViewLayout(managerView, layoutParams)
                Log.d("FloatingPro", "Successfully requested focus for floating window")
            }
        } catch (e: Exception) {
            Log.e("FloatingPro", "Error requesting focus: ${e.message}")
        }
    }

    /**
     * Remove focus from a floating window by modifying its WindowManager.LayoutParams
     * This matches the user's requested functionality from the issue
     */
    fun loseFocusFloatingView(fxControl: IFxControl, context: Context) {
        try {
            val managerView = fxControl.getManagerView()
            val layoutParams = fxControl.getWindowManagerLayoutParams() // NEW METHOD
            
            if (layoutParams == null) {
                Log.w("FloatingPro", "WindowManager.LayoutParams not available - probably an app-level floating window")
                return
            }

            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                 WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                 WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            managerView?.post {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.updateViewLayout(managerView, layoutParams)
                Log.d("FloatingPro", "Successfully removed focus from floating window")
            }
        } catch (e: Exception) {
            Log.e("FloatingPro", "Error losing focus: ${e.message}")
        }
    }
    
    /**
     * Example function to demonstrate checking if WindowManager.LayoutParams are available
     */
    fun checkWindowManagerLayoutParamsAvailability(fxControl: IFxControl) {
        val layoutParams = fxControl.getWindowManagerLayoutParams()
        if (layoutParams != null) {
            Log.i("FloatingPro", "System floating window detected - WindowManager.LayoutParams available")
            Log.d("FloatingPro", "Current flags: ${layoutParams.flags}")
            Log.d("FloatingPro", "Current type: ${layoutParams.type}")
        } else {
            Log.i("FloatingPro", "App-level floating window detected - WindowManager.LayoutParams not available")
        }
    }
}