package com.petterp.floatingx.listener

/**
 * fx监听事件，用于监听浮窗上的一些手势事件，已废弃，建议直接使用[IFxTouchListener]
 * */
@Deprecated("建议直接使用IFxTouchListener,更正命名", replaceWith = ReplaceWith("IFxTouchListener"))
interface IFxScrollListener : IFxTouchListener
