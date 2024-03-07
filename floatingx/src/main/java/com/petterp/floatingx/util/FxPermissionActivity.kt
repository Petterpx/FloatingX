@file:JvmName("_FxPermissionActivity")

package com.petterp.floatingx.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

private const val FX_RESULT_CODE = 5001
private const val FX_FRAGMENT_TAG = "FxPermissionFragment"

typealias FxPermissionResultAction = (agree: Boolean) -> Unit

interface IFxPermissionControl {
    fun requestPermission(key: String, agreeAction: FxPermissionResultAction?)

    fun clear()
}

internal val Activity.permissionControl: IFxPermissionControl?
    @RequiresApi(Build.VERSION_CODES.M)
    get() {
        if (this is FragmentActivity) return permissionFragment
        return permissionSupportFragment
    }

internal fun Activity.safeRemovePermissionFragment(fxLog: FxLog) {
    kotlin.runCatching {
        if (this is FragmentActivity) {
            val fxFragment = supportFragmentManager.findFragmentByTag(FX_FRAGMENT_TAG)
            if (fxFragment != null) {
                supportFragmentManager.beginTransaction().remove(fxFragment)
                    .commitAllowingStateLoss()
                fxLog.d("fxSystem-> permission, remove FxPermissionFragment success")
            }
        } else {
            val fxFragment = fragmentManager.findFragmentByTag(FX_FRAGMENT_TAG)
            if (fxFragment != null) {
                fragmentManager.beginTransaction().remove(fxFragment).commitAllowingStateLoss()
                fxLog.d("fxSystem-> permission, remove FxPermissionFragment success")
            }
        }
    }
}

internal val FragmentActivity.permissionFragment: IFxPermissionControl?
    @RequiresApi(Build.VERSION_CODES.M)
    get() {
        var fragment = supportFragmentManager.findFragmentByTag(FX_FRAGMENT_TAG)
        if (fragment == null) {
            fragment = FxPermissionFragment()
            supportFragmentManager.beginTransaction()
                .add(fragment, FX_FRAGMENT_TAG)
                .commitAllowingStateLoss()
            supportFragmentManager.executePendingTransactions()
        }
        return fragment as? IFxPermissionControl
    }

internal val Activity.permissionSupportFragment: IFxPermissionControl?
    @RequiresApi(Build.VERSION_CODES.M)
    get() {
        var fragment = fragmentManager.findFragmentByTag(FX_FRAGMENT_TAG)
        if (fragment == null) {
            fragment = FxPermissionSupportFragment()
            fragmentManager.beginTransaction()
                .add(fragment, FX_FRAGMENT_TAG)
                .commitAllowingStateLoss()
            fragmentManager.executePendingTransactions()
        }
        return fragment as? IFxPermissionControl
    }

/**
 *
 * @author petterp
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class FxPermissionFragment : Fragment(0), IFxPermissionControl {
    private var actions = mutableMapOf<String, FxPermissionResultAction?>()
    private var isRequestLoading = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FX_RESULT_CODE) return
        kotlin.runCatching {
            isRequestLoading = false
            val isAgree = Settings.canDrawOverlays(activity)
            actions.forEach {
                it.value?.invoke(isAgree)
                actions[it.key] = null
            }
        }
    }

    override fun requestPermission(key: String, agreeAction: FxPermissionResultAction?) {
        if (agreeAction == null) return
        actions[key] = agreeAction
        if (isRequestLoading) return
        kotlin.runCatching {
            val uri = Uri.parse("package:${context?.packageName ?: return}")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            startActivityForResult(intent, FX_RESULT_CODE)
            isRequestLoading = true
        }
    }

    override fun clear() {
        actions.clear()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal class FxPermissionSupportFragment : android.app.Fragment(), IFxPermissionControl {
    private var actions = mutableMapOf<String, FxPermissionResultAction?>()
    private var isRequestLoading = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FX_RESULT_CODE) return
        kotlin.runCatching {
            isRequestLoading = false
            val isAgree = Settings.canDrawOverlays(activity)
            actions.forEach {
                it.value?.invoke(isAgree)
                actions[it.key] = null
            }
        }
    }

    override fun requestPermission(key: String, agreeAction: FxPermissionResultAction?) {
        if (agreeAction == null) return
        actions[key] = agreeAction
        if (isRequestLoading) return
        kotlin.runCatching {
            val uri = Uri.parse("package:${context?.packageName ?: return}")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            startActivityForResult(intent, FX_RESULT_CODE)
            isRequestLoading = true
        }
    }

    override fun clear() {
        actions.clear()
    }
}
