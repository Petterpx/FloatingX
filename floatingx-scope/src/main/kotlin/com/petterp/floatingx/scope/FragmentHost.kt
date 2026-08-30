package com.petterp.floatingx.scope

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.petterp.floatingx.core.container.FxContainer
import com.petterp.floatingx.core.container.FxLayerContainer
import com.petterp.floatingx.core.host.FxHost
import com.petterp.floatingx.core.host.FxHostSession
import com.petterp.floatingx.core.layout.FxBounds
import com.petterp.floatingx.core.layout.FxRect

/**
 * Fragment 承载（spec §5，修 #244）：跟随 viewLifecycleOwner——
 * view 创建后 ready、view 销毁即 lost；fragment 自己销毁时由 Fragment.fxScope 负责 cancel。
 * 使用方需自行依赖 androidx.fragment（本模块只 compileOnly）。
 */
public class FragmentHost(private val fragment: Fragment) : FxHost {

    override val context: Context
        get() = checkNotNull(fragment.context) { "Fragment 尚未 attach，请在 onCreate/onViewCreated 之后调用 fxScope" }

    private var session: FxHostSession? = null
    private var root: ViewGroup? = null

    private val viewOwnerObserver = Observer<LifecycleOwner?> { owner ->
        if (owner == null) onViewLost() else onViewCreated(owner)
    }

    private val viewLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            onViewLost()
        }
    }

    override fun bind(session: FxHostSession) {
        this.session = session
        // 以 fragment 为 owner：fragment DESTROYED 时自动移除，不会泄漏 host
        fragment.viewLifecycleOwnerLiveData.observe(fragment, viewOwnerObserver)
    }

    private fun onViewCreated(owner: LifecycleOwner) {
        if (root != null) return
        val view = fragment.view
        root = checkNotNull(view as? ViewGroup) { "Fragment 根 view 必须是 ViewGroup 才能承载浮窗，当前为 ${view?.javaClass?.name}" }
        owner.lifecycle.addObserver(viewLifecycleObserver)
        session?.onHostReady()
    }

    private fun onViewLost() {
        if (root == null) return
        root = null
        session?.onHostLost()
    }

    override fun createContainer(): FxContainer = FxLayerContainer(context)

    override fun attach(container: FxContainer) {
        val r = checkNotNull(root) { "FragmentHost 尚未 ready 就被 attach" }
        r.addView(container.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun detach(container: FxContainer) {
        (container.view.parent as? ViewGroup)?.removeView(container.view)
    }

    override fun bounds(): FxBounds {
        val r = root ?: return FxBounds(FxRect(0f, 0f, 0f, 0f))
        return FxBounds(FxRect(0f, 0f, r.width.toFloat(), r.height.toFloat()))
    }

    override fun release() {
        fragment.viewLifecycleOwnerLiveData.removeObserver(viewOwnerObserver)
        root = null
        session = null
    }
}
