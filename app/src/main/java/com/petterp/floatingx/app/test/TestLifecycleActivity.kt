package com.petterp.floatingx.test

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.Toast
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.R

/**
 * Test activity to reproduce and verify the fix for the lifecycle timing issue
 * This simulates the problem described in the issue where FloatingX.control() 
 * operations in onCreate() don't execute properly
 */
class TestLifecycleActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Install FloatingX first (simulate this happening in Application or earlier)
        FloatingX.install {
            setContext(applicationContext)
            setLayout(android.R.layout.simple_list_item_1) // Use a simple layout for testing
            setTag("test")
        }
        
        // These operations should now work even when called immediately in onCreate
        // Before the fix, these would be ignored because the floating window wasn't ready
        
        // Line equivalent to 202: move operation
        FloatingX.control("test").move(700f, 700f)
        
        // Line equivalent to 203: hide operation  
        FloatingX.control("test").hide()
        
        // Test showing with move
        showMenu1()
        
        // Test hiding with move
        hideMenu1()
    }
    
    private fun showMenu1() {
        // This should now work properly: move first, then show
        FloatingX.control("test").move(150f, 0f)
        FloatingX.control("test").show()
        
        Toast.makeText(this, "showMenu1 executed", Toast.LENGTH_SHORT).show()
    }
    
    private fun hideMenu1() {
        // This should now show the move animation before hiding
        FloatingX.control("test").move(300f, 300f)
        FloatingX.control("test").hide()
        
        Toast.makeText(this, "hideMenu1 executed", Toast.LENGTH_SHORT).show()
    }
}