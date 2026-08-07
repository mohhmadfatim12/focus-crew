package com.focuscrew.app.data.model

/**
 * Subscription Tiers for FocusCrew.
 */
enum class SubscriptionTier(
    val id: String,
    val title: String,
    val priceDisplay: String,
    val periodDisplay: String,
    val dailyPriceDisplay: String,
    val badge: String? = null,
    val isBestValue: Boolean = false
) {
    ANNUAL(
        id = "focuscrew_annual_80",
        title = "1 Year Elite Mastery",
        priceDisplay = "$80",
        periodDisplay = "per year",
        dailyPriceDisplay = "$0.21 / day",
        badge = "👑 BEST VALUE • SAVE 45%",
        isBestValue = true
    ),
    SEMI_ANNUAL(
        id = "focuscrew_6months_40",
        title = "6 Months Focus Pass",
        priceDisplay = "$40",
        periodDisplay = "every 6 months",
        dailyPriceDisplay = "$0.22 / day",
        badge = "⚡ COMMITTED"
    ),
    MONTHLY(
        id = "focuscrew_monthly_6",
        title = "1 Month Access",
        priceDisplay = "$6",
        periodDisplay = "per month",
        dailyPriceDisplay = "$0.20 / day",
        badge = "☕ FLEXIBLE"
    )
}

/**
 * State representation of the user's trial or subscription.
 */
data class SubscriptionState(
    val isProUser: Boolean = false,
    val activeTier: SubscriptionTier? = null,
    val trialStartedAt: Long = 0L,
    val trialEndsAt: Long = 0L,
    val isTrialActive: Boolean = true,
    val trialDaysRemaining: Int = 5,
    val trialHoursRemaining: Int = 120,
    val estimatedHoursSaved: Double = 0.0,
    val distractionsBlockedCount: Int = 0
)
