package com.petterp.floatingx.app.test

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent
import com.petterp.floatingx.app.R

/**
 * Test activity to reproduce and verify the fix for the lifecycle timing issue
 * This simulates the problem described in the issue where FloatingX.control() 
 * operations in onCreate() don't execute properly
 */
class TestLifecycleActivity : AppCompatActivity() {
    
    companion object {
        const val TEST_TAG = "lifecycle_test"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("Install FloatingX") {
                        installFloatingX()
                    }
                    addItemView("Test onCreate() operations (BEFORE fix: ignored)") {
                        testOnCreateOperations()
                    }
                    addItemView("Show Menu 1 (move then show)") {
                        showMenu1()
                    }
                    addItemView("Hide Menu 1 (move then hide)") {
                        hideMenu1()
                    }
                    addItemView("Test immediate operations") {
                        testImmediateOperations()
                    }
                    addItemView("Cancel FloatingX") {
                        FloatingX.controlOrNull(TEST_TAG)?.cancel()
                    }
                }
            }
        }
    }
    
    private fun installFloatingX() {
        FloatingX.install {
            setContext(applicationContext)
            setLayout(R.layout.item_floating)
            setTag(TEST_TAG)
            setEnableLog(true, "lifecycle_test")
        }
        Toast.makeText(this, "FloatingX installed", Toast.LENGTH_SHORT).show()
    }
    
    private fun testOnCreateOperations() {
        // Simulate operations being called immediately in onCreate
        // Before fix: these would be ignored if internal view isn't ready
        // After fix: these get queued and executed when ready
        
        if (!FloatingX.isInstalled(TEST_TAG)) {
            installFloatingX()
        }
        
        // Line equivalent to 202: move operation
        FloatingX.control(TEST_TAG).move(700f, 700f)
        
        // Line equivalent to 203: hide operation  
        FloatingX.control(TEST_TAG).hide()
        
        Toast.makeText(this, "onCreate operations executed", Toast.LENGTH_SHORT).show()
    }
    
    private fun showMenu1() {
        if (!FloatingX.isInstalled(TEST_TAG)) {
            installFloatingX()
        }
        
        // This should now work properly: move first, then show
        // Before fix: move would be ignored on first call
        FloatingX.control(TEST_TAG).move(150f, 100f)
        FloatingX.control(TEST_TAG).show()
        
        Toast.makeText(this, "showMenu1 executed (move + show)", Toast.LENGTH_SHORT).show()
    }
    
    private fun hideMenu1() {
        if (!FloatingX.isInstalled(TEST_TAG)) {
            installFloatingX()
        }
        
        // This should now show the move animation before hiding
        // Before fix: would hide immediately without move animation
        FloatingX.control(TEST_TAG).move(300f, 300f)
        FloatingX.control(TEST_TAG).hide()
        
        Toast.makeText(this, "hideMenu1 executed (move + hide)", Toast.LENGTH_SHORT).show()
    }
    
    private fun testImmediateOperations() {
        // Test calling operations immediately after install
        FloatingX.install {
            setContext(applicationContext)
            setLayout(R.layout.item_floating)
            setTag(TEST_TAG + "_immediate")
            setEnableLog(true, "immediate_test")
        }
        
        // These should work with queuing
        FloatingX.control(TEST_TAG + "_immediate").move(400f, 200f)
        FloatingX.control(TEST_TAG + "_immediate").show()
        
        Toast.makeText(this, "Immediate operations executed", Toast.LENGTH_SHORT).show()
    }
}