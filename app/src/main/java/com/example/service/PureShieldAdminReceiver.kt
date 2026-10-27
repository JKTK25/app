package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class PureShieldAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("PureShieldAdmin", "Parental Control Device Administration Enabled.")
        Toast.makeText(context, "PureShield Anti-Tamper & Anti-Uninstall Lock Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val prefs = context.getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        val pin = prefs.getString("security_pin", "") ?: ""
        return if (pin.isNotBlank()) {
            "⚠️ CRITICAL: PureShield Anti-Tamper Protection is locked with a Master Password. Disabling this administrator requires entering your Master Password inside the PureShield application."
        } else {
            "PureShield active parental protection and anti-uninstall security will be deactivated."
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("PureShieldAdmin", "Parental Control Device Administration Disabled.")
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, PureShieldAdminReceiver::class.java)
        }

        fun isDeviceAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
            return dpm.isAdminActive(getComponentName(context))
        }

        fun createDeviceAdminIntent(context: Context): Intent {
            val component = getComponentName(context)
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Activate PureShield Device Administrator to physically block unauthorized uninstallation, prevent long-press app removal, and secure parental controls."
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}
