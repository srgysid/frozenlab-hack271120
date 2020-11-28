package com.frozenlab.hack

import android.content.Context

object Preferences {

    private val sharedPreferences = MainApplication.appContext.getSharedPreferences(
        "${MainApplication.appContext.packageName}.preferences",
        Context.MODE_PRIVATE
    )

    private enum class PreferencesValues(val prefName: String, val defaultValue: Any?) {
        ACCESS_TOKEN( "PREF_ACCESS_TOKEN", ""   ),
        FCM_TOKEN(    "PREF_FCM_TOKEN",    ""   ),
    }

    // Settings
    const val apiURL:              String = "https://api-ra.welive.cloud"
    const val apiCardURL:          String = "https://ai-rosatom-card.welive.cloud"
    const val apiOrderURL:         String = "https://ai-rosatom-ass.welive.cloud"
    const val voiceUrl:            String = "ws://45.137.190.230:27001"
    const val privacyPolicyUrl:    String = "https://"
    const val okHttpSocketTimeOut: Long   = 60 // Seconds (default: 10 seconds)

    const val jsonDateFormat: String = "yyyy-MM-dd HH:mm:ssZ"

    var accessToken: String
        get()      = getStringValue(PreferencesValues.ACCESS_TOKEN)
        set(value) = setStringValue(PreferencesValues.ACCESS_TOKEN, value)

    var fcmToken: String
        get()      = getStringValue(PreferencesValues.FCM_TOKEN)
        set(value) = setStringValue(PreferencesValues.FCM_TOKEN, value)

    // Private area

    private fun getStringValue(pref: PreferencesValues): String {
        return sharedPreferences.getString(pref.prefName, pref.defaultValue as String) ?: ""
    }

    private fun getBooleanValue(pref: PreferencesValues): Boolean {
        return sharedPreferences.getBoolean(pref.prefName, pref.defaultValue as Boolean)
    }

    private fun getLongValue(pref: PreferencesValues): Long {
        return sharedPreferences.getLong(pref.prefName, pref.defaultValue as Long)
    }

    private fun getIntValue(pref: PreferencesValues): Int {
        return sharedPreferences.getInt(pref.prefName, pref.defaultValue as Int)
    }

    private fun setStringValue(pref: PreferencesValues, value: String) {

        sharedPreferences
            .edit()
            .putString(pref.prefName, value)
            .apply()
    }

    private fun setBooleanValue(pref: PreferencesValues, value: Boolean) {

        sharedPreferences
            .edit()
            .putBoolean(pref.prefName, value)
            .apply()
    }

    private fun setLongValue(pref: PreferencesValues, value: Long) {
        sharedPreferences
            .edit()
            .putLong(pref.prefName, value)
            .apply()
    }

    private fun setIntValue(pref: PreferencesValues, value: Int) {
        sharedPreferences
            .edit()
            .putInt(pref.prefName, value)
            .apply()
    }
}
