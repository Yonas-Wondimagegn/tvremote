package com.alex.tvremote

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import fi.iki.elonen.NanoHTTPD

class RemoteService : AccessibilityService() {
    private var server: Srv? = null

    override fun onServiceConnected() { server = Srv().also { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) } }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { server?.stop(); super.onDestroy() }

    private fun focused(): AccessibilityNodeInfo? =
        rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

    private fun first(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isFocusable && n.isVisibleToUser) return n
        for (i in 0 until n.childCount) first(n.getChild(i))?.let { return it }
        return null
    }

    private fun move(dir: Int) {
        val cur = focused()
        val next = if (cur == null) first(rootInActiveWindow) else cur.focusSearch(dir)
        next?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    private fun ok() {
        var n = focused()
        while (n != null && !n.isClickable) n = n.parent
        n?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun key(c: String) {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        when (c) {
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "up" -> move(View.FOCUS_UP)
            "down" -> move(View.FOCUS_DOWN)
            "left" -> move(View.FOCUS_LEFT)
            "right" -> move(View.FOCUS_RIGHT)
            "ok" -> ok()
            "volup" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "voldown" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "mute" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
        }
    }

    private fun gesture(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long) {
        val d = resources.displayMetrics
        val p = Path().apply { moveTo(x1 * d.widthPixels, y1 * d.heightPixels); lineTo(x2 * d.widthPixels, y2 * d.heightPixels) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, ms)).build(), null, null)
    }

    private fun type(t: String) {
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, t) }
        focused()?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    inner class Srv : NanoHTTPD(8080) {
        override fun serve(s: IHTTPSession): Response {
            val q = s.parms
            fun f(k: String) = q[k]?.toFloatOrNull() ?: 0f
            when (s.uri) {
                "/" -> return newFixedLengthResponse(Response.Status.OK, "text/html",
                    assets.open("remote.html").bufferedReader().readText())
                "/k" -> key(q["c"] ?: "")
                "/tap" -> gesture(f("x"), f("y"), f("x"), f("y"), 50)
                "/swipe" -> gesture(f("x1"), f("y1"), f("x2"), f("y2"), 250)
                "/text" -> type(q["t"] ?: "")
            }
            return newFixedLengthResponse("ok")
        }
    }
}
