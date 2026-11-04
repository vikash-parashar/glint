package app.glint.data

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Glint Pro: one-time unlock for extras (Dock mode now; more later). The launcher and
 * all widgets stay free. Play Billing gets wired in before the Play Store release; until
 * then test builds can unlock Pro by hand.
 */
class ProStore(private val context: Context) {
    private val p = context.getSharedPreferences("glint_pro", Context.MODE_PRIVATE)

    val isPro: Boolean get() = p.getBoolean("pro", false)

    /** Called by billing after a verified purchase, or by the test unlock in debug builds. */
    fun setPro(value: Boolean) = p.edit().putBoolean("pro", value).apply()

    /** Debug/test builds show an "Unlock for testing" button. */
    val canTestUnlock: Boolean
        get() = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    companion object {
        const val PRICE_LABEL = "₹149 once"
    }
}
