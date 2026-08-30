package com.petterp.floatingx.system.permission

/** 权限申请的三种策略（spec §4） */
public sealed class FxPermissionStrategy {

    /** 默认：无权限时自动弹透明页申请，拒绝后走 fallback（若有） */
    public object Auto : FxPermissionStrategy()

    /** 把决定权交给拦截器：由业务方决定何时 proceed / deny / useFallback */
    public class Manual(public val interceptor: FxPermissionInterceptor) : FxPermissionStrategy()

    /** 不检查权限直接挂窗口（业务方已自行申请，或 type 不需要权限） */
    public object Skip : FxPermissionStrategy()

    public companion object {
        /** Java 侧入口：等价于 [Auto] */
        @JvmStatic
        public fun auto(): FxPermissionStrategy = Auto

        /** Java 侧入口：等价于 [Manual] */
        @JvmStatic
        public fun manual(interceptor: FxPermissionInterceptor): FxPermissionStrategy = Manual(interceptor)

        /** Java 侧入口：等价于 [Skip] */
        @JvmStatic
        public fun skip(): FxPermissionStrategy = Skip
    }
}

/** Manual 策略下的拦截器：拿到 [FxPermissionRequest] 句柄后自行决定走向 */
public fun interface FxPermissionInterceptor {
    public fun onRequest(request: FxPermissionRequest)
}

/** Manual 策略下交给拦截器的句柄；三个方法只应调用一个 */
public interface FxPermissionRequest {
    /** 弹系统设置页申请 */
    public fun proceed()

    /** 放弃：状态停在 INSTALLED，之后可 SystemHost.retryPermission() */
    public fun deny()

    /** 直接降级到 Builder.fallback 指定的 host（未配置则等同 deny） */
    public fun useFallback()
}

/** 权限申请结果回调 */
public fun interface FxPermissionCallback {
    public fun onResult(granted: Boolean)
}
