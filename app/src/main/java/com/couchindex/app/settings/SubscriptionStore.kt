package com.couchindex.app.settings

import android.content.Context
import com.couchindex.core.Subscription

class SubscriptionStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(defaults: List<Subscription>): List<Subscription> =
        defaults.map { subscription ->
            subscription.copy(
                enabled = preferences.getBoolean(subscription.key, subscription.enabled),
            )
        }

    fun setEnabled(providerId: String, enabled: Boolean) {
        preferences.edit()
            .putBoolean(key(providerId), enabled)
            .apply()
    }

    private val Subscription.key: String
        get() = key(providerId)

    private fun key(providerId: String): String =
        "provider.$providerId.enabled"

    companion object {
        private const val PREFERENCES_NAME = "couchindex-subscriptions"
    }
}
