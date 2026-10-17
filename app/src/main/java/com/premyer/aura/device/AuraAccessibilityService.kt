package com.premyer.aura.device

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Only user-confirmed, one-shot operations. No event collection, background automation or logging.
 * An accessibility overlay confirms the exact target app AFTER Aura leaves the foreground.
 */
class AuraAccessibilityService : AccessibilityService() {
    private var overlay: View? = null
    private var pending: ((Result<String>) -> Unit)? = null

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { cancelPending() }
    override fun onDestroy() {
        cancelPending()
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun confirmOnTarget(action: DeviceAction, callback: (Result<String>) -> Unit) {
        cancelPending()
        val root = rootInActiveWindow
        val targetPackage = root?.packageName?.toString()
        if (targetPackage.isNullOrBlank() || targetPackage == packageName) {
            callback(Result.failure(IllegalStateException("Keine andere App aktiv. Öffne zuerst die Ziel-App, dann Aura.")))
            return
        }
        pending = callback
        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetPackage, 0)).toString()
        }.getOrDefault(targetPackage)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(Color.rgb(27, 34, 49))
            addView(TextView(context).apply {
                text = "Aura • $appLabel\n${action.preview}\nNur diese eine Aktion ausführen?"
                setTextColor(Color.WHITE)
                textSize = 16f
            })
            addView(Button(context).apply {
                text = "Jetzt bestätigen"
                setOnClickListener {
                    val result = runCatching {
                        val current = rootInActiveWindow ?: error("Kein Bildschirm verfügbar.")
                        check(current.packageName?.toString() == targetPackage) { "Ziel-App hat gewechselt. Aktion abgebrochen." }
                        execute(action, current)
                    }
                    finish(result)
                }
            })
            addView(Button(context).apply {
                text = "Abbrechen"
                setOnClickListener { finish(Result.success("Aktion abgebrochen.")) }
            })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
        try {
            getSystemService(WindowManager::class.java).addView(layout, params)
            overlay = layout
        } catch (e: Exception) { finish(Result.failure(e)) }
    }

    fun cancelPending() { finish(Result.success("Aktion abgebrochen.")) }

    private fun finish(result: Result<String>) {
        overlay?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        overlay = null
        val callback = pending
        pending = null
        callback?.invoke(result)
    }

    private fun execute(action: DeviceAction, root: AccessibilityNodeInfo): String {
        // Password nodes and descendants are excluded from both reading and manipulation.
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 30 || nodes.size >= 500 || node.isPassword || !node.isVisibleToUser) return
            nodes.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let { visit(it, depth + 1) }
        }
        visit(root, 0)
        when (action.type) {
            ActionType.READ_SCREEN -> return nodes.filterNot { it.isEditable }
                .mapNotNull { (it.text ?: it.contentDescription)?.toString()?.take(500) }
                .distinct().joinToString("\n").take(12000).ifBlank { "Kein lesbarer Text (geschützte Felder werden ausgelassen)." }
            ActionType.TAP -> {
                val targets = nodes.filter {
                    it.text?.toString() == action.argument || it.contentDescription?.toString() == action.argument
                }
                check(targets.size == 1) { "Element nicht eindeutig gefunden. Nichts angetippt." }
                var node: AccessibilityNodeInfo? = targets.single()
                while (node != null && !node.isClickable) node = node.parent
                check(node != null && !node.isPassword && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    "Element lässt sich nicht antippen."
                }
            }
            ActionType.INPUT_TEXT -> {
                val node = nodes.singleOrNull { it.isFocused && it.isEditable && !it.isPassword }
                    ?: error("Kein eindeutiges fokussiertes, ungeschütztes Textfeld.")
                check(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, action.argument)
                })) { "Texteingabe nicht möglich." }
            }
            ActionType.SCROLL -> {
                val direction = if (action.argument == "backward") AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                check(nodes.firstOrNull { it.isScrollable }?.performAction(direction) == true) { "Kein scrollbarer Bereich." }
            }
            ActionType.BACK -> check(performGlobalAction(GLOBAL_ACTION_BACK)) { "Zurück nicht möglich." }
            ActionType.HOME -> check(performGlobalAction(GLOBAL_ACTION_HOME)) { "Startbildschirm nicht möglich." }
            else -> error("Nicht unterstützte Bedienungshilfe-Aktion.")
        }
        return "Einzelaktion ausgeführt: ${action.type.title}. Den tatsächlichen Zustand bitte in der Ziel-App prüfen."
    }

    companion object {
        var instance: AuraAccessibilityService? = null
            private set
    }
}
