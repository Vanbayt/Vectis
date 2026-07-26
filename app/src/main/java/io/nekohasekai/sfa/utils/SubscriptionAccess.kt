package io.nekohasekai.sfa.utils

/**
 * Manages feature access permissions based on user subscription tiers.
 */
object SubscriptionAccess {

    /**
     * Set of subscription tier names allowed to perform manual protocol and server selection.
     * Currently restricted to "maximum" tier users, but designed for easy expansion in the future.
     */
    private val ALLOWED_MANUAL_SELECTION_TIERS = setOf(
        "maximum"
    )

    /**
     * Checks if the given subscription tier has access to manual protocol/server selection.
     *
     * @param subscriptionTier The subscription tier string (e.g. "maximum", "pro", "free").
     * @return True if the user is allowed to select protocols manually, false otherwise.
     */
    fun canSelectProtocol(subscriptionTier: String?): Boolean {
        if (subscriptionTier.isNullOrBlank()) return false
        val tier = subscriptionTier.lowercase().trim()
        return tier in ALLOWED_MANUAL_SELECTION_TIERS
    }
}
