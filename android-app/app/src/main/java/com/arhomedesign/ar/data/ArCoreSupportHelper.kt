package com.arhomedesign.ar.data

import android.app.Activity
import android.content.Context
import com.google.ar.core.ArCoreApk

/**
 * Maps ARCore availability to UI-friendly copy. Uses [ArCoreApk.Availability.name] so we stay
 * compatible across ARCore SDK versions where Kotlin may not resolve every enum constant.
 */
data class ArCoreDeviceStatus(
    val availabilityName: String,
    val headline: String,
    val description: String,
    val canOpenSession: Boolean,
    val showInstallButton: Boolean
) {
    companion object {
        fun checking(): ArCoreDeviceStatus = ArCoreDeviceStatus(
            availabilityName = "UNKNOWN_CHECKING",
            headline = "Checking AR support",
            description = "Verifying whether this device can run ARCore…",
            canOpenSession = false,
            showInstallButton = false
        )
    }
}

object ArCoreSupportHelper {

    fun checkStatus(context: Context): ArCoreDeviceStatus {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        return fromAvailability(availability)
    }

    fun fromAvailability(availability: ArCoreApk.Availability): ArCoreDeviceStatus =
        mapByName(availability.name)

    private fun mapByName(name: String): ArCoreDeviceStatus = when (name) {
        // Older SDKs used SUPPORTED; newer ARCore (e.g. 1.40+) reports SUPPORTED_INSTALLED when the
        // Play Services for AR package is present — both mean live AR is allowed.
        "SUPPORTED",
        "SUPPORTED_INSTALLED" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "ARCore ready",
            description = "Google Play Services for AR is installed. You can start live camera AR, or enter room sizes manually above.",
            canOpenSession = true,
            showInstallButton = false
        )
        "SUPPORTED_NOT_INSTALLED" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "AR services not installed",
            description = "Install “Google Play Services for AR” from the Play Store. " +
                "Most emulators need a Google Play system image, then install ARCore from the store.",
            canOpenSession = false,
            showInstallButton = true
        )
        "SUPPORTED_APK_TOO_OLD" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "AR services need an update",
            description = "Open the Play Store and update “Google Play Services for AR”, then try again.",
            canOpenSession = false,
            showInstallButton = true
        )
        "UNSUPPORTED_DEVICE_NOT_CAPABLE" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "AR not supported on this device",
            description = "This hardware is not ARCore-capable. You can still enter room sizes manually and use all design tools.",
            canOpenSession = false,
            showInstallButton = false
        )
        "UNKNOWN_ERROR" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "Could not check AR support",
            description = "Try again in a moment. On emulators, use a Google Play image with ARCore installed.",
            canOpenSession = false,
            showInstallButton = false
        )
        "UNKNOWN_CHECKING" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "Still checking…",
            description = "Wait a second and use “Refresh AR status”, or leave and reopen this screen.",
            canOpenSession = false,
            showInstallButton = false
        )
        "UNKNOWN_TIMED_OUT" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "Couldn’t verify AR (timeout)",
            description = "Check your connection, open the Play Store once, then tap “Refresh AR status”. You can still use manual room sizes.",
            canOpenSession = false,
            showInstallButton = false
        )
        "TRANSIENT_ERROR" -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "Temporary AR error",
            description = "Google Play Services may be updating. Retry shortly.",
            canOpenSession = false,
            showInstallButton = false
        )
        else -> ArCoreDeviceStatus(
            availabilityName = name,
            headline = "Unexpected AR status",
            description = "ARCore reported “$name”. Try “Refresh AR status” or restart the app. Manual room sizes still work.",
            canOpenSession = false,
            showInstallButton = false
        )
    }

    fun ensureCanOpenSession(context: Context) {
        val status = checkStatus(context)
        if (!status.canOpenSession) {
            throw IllegalStateException(status.description)
        }
    }

    fun requestInstall(activity: Activity) {
        ArCoreApk.getInstance().requestInstall(activity, true)
    }
}
