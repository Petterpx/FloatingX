package com.petterp.floatingx.app.test

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.petterp.floatingx.FloatingX
import com.petterp.floatingx.app.addItemView
import com.petterp.floatingx.app.addLinearLayout
import com.petterp.floatingx.app.addNestedScrollView
import com.petterp.floatingx.app.createLinearLayoutToParent
import com.petterp.floatingx.app.R

/**
 * Comprehensive test for edge cases in the lifecycle timing fix
 */
class EdgeCaseTestActivity : AppCompatActivity() {
    
    companion object {
        const val EDGE_TEST_TAG = "edge_test"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createLinearLayoutToParent {
            addNestedScrollView {
                addLinearLayout {
                    addItemView("Test rapid operations") {
                        testRapidOperations()
                    }
                    addItemView("Test duplicate show/hide") {
                        testDuplicateOperations()
                    }
                    addItemView("Test operations during initialization") {
                        testOperationsDuringInit()
                    }
                    addItemView("Test cancel with pending operations") {
                        testCancelWithPending()
                    }
                    addItemView("Test reinstall with operations") {
                        testReinstallWithOperations()
                    }
                    addItemView("Clean up all") {
                        cleanUp()
                    }
                }
            }
        }
    }
    
    private fun testRapidOperations() {
        // Install and immediately call multiple operations
        FloatingX.install {
            setContext(applicationContext)
            setLayout(R.layout.item_floating)
            setTag(EDGE_TEST_TAG)
            setEnableLog(true, "edge_test")
        }
        
        // Rapid fire operations - these should all be queued and executed properly
        FloatingX.control(EDGE_TEST_TAG).move(100f, 100f)
        FloatingX.control(EDGE_TEST_TAG).show()
        FloatingX.control(EDGE_TEST_TAG).move(200f, 200f)
        FloatingX.control(EDGE_TEST_TAG).hide()
        FloatingX.control(EDGE_TEST_TAG).move(300f, 300f)
        FloatingX.control(EDGE_TEST_TAG).show()
        
        Toast.makeText(this, "Rapid operations queued", Toast.LENGTH_SHORT).show()
    }
    
    private fun testDuplicateOperations() {
        if (!FloatingX.isInstalled(EDGE_TEST_TAG)) {
            FloatingX.install {
                setContext(applicationContext)
                setLayout(R.layout.item_floating)
                setTag(EDGE_TEST_TAG)
            }
        }
        
        // Multiple show calls - should not cause issues
        FloatingX.control(EDGE_TEST_TAG).show()
        FloatingX.control(EDGE_TEST_TAG).show()
        FloatingX.control(EDGE_TEST_TAG).show()
        
        // Multiple hide calls - should not cause issues
        handler.postDelayed({
            FloatingX.control(EDGE_TEST_TAG).hide()
            FloatingX.control(EDGE_TEST_TAG).hide()
            FloatingX.control(EDGE_TEST_TAG).hide()
        }, 1000)
        
        Toast.makeText(this, "Duplicate operations test", Toast.LENGTH_SHORT).show()
    }
    
    private fun testOperationsDuringInit() {
        // Reinstall to trigger initialization
        FloatingX.install {
            setContext(applicationContext)
            setLayout(R.layout.item_floating)
            setTag(EDGE_TEST_TAG + "_init")
        }
        
        // Call operations in rapid succession during potential initialization
        for (i in 1..5) {
            handler.postDelayed({
                FloatingX.control(EDGE_TEST_TAG + "_init").move(i * 50f, i * 50f)
                if (i % 2 == 0) {
                    FloatingX.control(EDGE_TEST_TAG + "_init").show()
                } else {
                    FloatingX.control(EDGE_TEST_TAG + "_init").hide()
                }
            }, i * 50L)
        }
        
        Toast.makeText(this, "Operations during init test", Toast.LENGTH_SHORT).show()
    }
    
    private fun testCancelWithPending() {
        // Install and queue operations
        FloatingX.install {
            setContext(applicationContext)
            setLayout(R.layout.item_floating)
            setTag(EDGE_TEST_TAG + "_cancel")
        }
        
        // Queue some operations
        FloatingX.control(EDGE_TEST_TAG + "_cancel").move(400f, 400f)
        FloatingX.control(EDGE_TEST_TAG + "_cancel").show()
        
        // Cancel immediately - pending operations should be cleared
        FloatingX.control(EDGE_TEST_TAG + "_cancel").cancel()
        
        Toast.makeText(this, "Cancel with pending operations", Toast.LENGTH_SHORT).show()
    }
    
    private fun testReinstallWithOperations() {
        val tag = EDGE_TEST_TAG + "_reinstall"
        
        // Install
        FloatingX.install {
            setContext(applicationContext)
            setLayout(R.layout.item_floating)
            setTag(tag)
        }
        
        // Queue operations
        FloatingX.control(tag).move(500f, 500f)
        FloatingX.control(tag).show()
        
        // Reinstall (should cancel previous and start fresh)
        FloatingX.install {
            setContext(applicationContext)
            setLayout(R.layout.item_floating)
            setTag(tag)
        }
        
        // New operations
        FloatingX.control(tag).move(600f, 600f)
        FloatingX.control(tag).show()
        
        Toast.makeText(this, "Reinstall with operations", Toast.LENGTH_SHORT).show()
    }
    
    private fun cleanUp() {
        FloatingX.uninstallAll()
        Toast.makeText(this, "All floating windows uninstalled", Toast.LENGTH_SHORT).show()
    }
}