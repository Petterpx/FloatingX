package com.petterp.floatingx.compose

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class FxComposeLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore
        get() = store


    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    /**
     * Compose uses the Window's decor view to locate the
     * Lifecycle/ViewModel/SavedStateRegistry owners.
     * Therefore, we need to set this class as the "owner" for the decor view.
     */
    fun attachToDecorView(decorView: View?) {
        decorView?.let {
            it.setViewTreeViewModelStoreOwner(this)
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        } ?: return
    }

    fun detachFromDecorView(decorView: View?) {
        decorView?.let {
            it.setViewTreeViewModelStoreOwner(null)
            it.setViewTreeLifecycleOwner(null)
            it.setViewTreeSavedStateRegistryOwner(null)
        } ?: return
    }
}