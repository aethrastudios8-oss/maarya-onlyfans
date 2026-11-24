package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MediaType(val label: String) {
    PHOTO("Photo Set"),
    VIDEO("Video Diary"),
    NOTE("Studio Update");

    companion object {
        fun safeFrom(raw: String?): MediaType =
            entries.firstOrNull { it.name == raw } ?: PHOTO
    }
}

enum class SubscriptionStatus(val label: String) {
    ACTIVE("Active"),
    CANCELLED("Cancelled"),
    PAUSED("Paused");

    companion object {
        fun safeFrom(raw: String?): SubscriptionStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class TransactionType(val label: String) {
    SUBSCRIPTION("Membership"),
    UPGRADE("Tier Upgrade"),
    TIP("Creator Tip"),
    UNLOCK("Post Unlock");

    companion object {
        fun safeFrom(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: SUBSCRIPTION
    }
}

enum class PaymentProvider(
    val wireValue: String,
    val displayName: String,
    val subtitle: String,
    val supportsWebRecurring: Boolean
) {
    VISA_CARD(
        wireValue = "VISA_CARD",
        displayName = "Visa Debit / Credit",
        subtitle = "Disabled until merchant gateway approval & API keys",
        supportsWebRecurring = false
    ),
    PAYONEER(
        wireValue = "PAYONEER",
        displayName = "Payoneer Checkout",
        subtitle = "Eligible business accounts only (personal Payoneer cannot run site checkout)",
        supportsWebRecurring = false
    ),
    JAZZCASH(
        wireValue = "JAZZCASH",
        displayName = "JazzCash Merchant (PK)",
        subtitle = "Pakistani wallet & card gateway (disabled until Merchant ID & Salt)",
        supportsWebRecurring = false
    ),
    GOOGLE_PLAY(
        wireValue = "GOOGLE_PLAY",
        displayName = "Google Play Billing",
        subtitle = "Required for Play Store Android digital subscriptions",
        supportsWebRecurring = true
    ),
    ADMIN_GRANT(
        wireValue = "ADMIN_GRANT",
        displayName = "Verified Admin Grant",
        subtitle = "Recorded directly by Maarya Studio",
        supportsWebRecurring = false
    );

    companion object {
        fun safeFrom(raw: String?): PaymentProvider =
            entries.firstOrNull { it.wireValue == raw } ?: PAYONEER

        val fanSelectableProviders = listOf(GOOGLE_PLAY, VISA_CARD, PAYONEER, JAZZCASH)
    }
}

enum class ReportReason(val label: String) {
    POLICY_VIOLATION("Platform Policy Violation"),
    COPYRIGHT("Copyright / Unauthorized Reupload"),
    HARASSMENT("Harassment or Abuse"),
    SPAM("Spam or Scams"),
    OTHER("Other Concern");

    companion object {
        fun safeFrom(raw: String?): ReportReason =
            entries.firstOrNull { it.name == raw } ?: POLICY_VIOLATION
    }
}

enum class WithdrawalChannel(val wireValue: String, val label: String) {
    PAYONEER_TO_JAZZCASH("PAYONEER_TO_JAZZCASH", "Payoneer USD -> Pakistan JazzCash"),
    JAZZCASH_DIRECT("JAZZCASH_DIRECT", "JazzCash Merchant -> Wallet Settlement");

    companion object {
        fun safeFrom(raw: String?): WithdrawalChannel =
            entries.firstOrNull { it.wireValue == raw } ?: PAYONEER_TO_JAZZCASH
    }
}

@Keep
@IgnoreExtraProperties
data class CreatorProfile(
    @DocumentId val id: String = "maarya",
    val displayName: String = "Maarya",
    val handle: String = "@maarya",
    val tagline: String = "MAARYA ONLYFANS • Private Black, Pink & Purple Studio",
    val bio: String = "Welcome to my private, single-creator club. Subscribe to unlock my exclusive galleries, intimate studio updates, and direct 1-on-1 fan messaging.",
    val location: String = "Lahore, Pakistan • Private Studio",
    val verified: Boolean = true,
    val subscriberCount: Long = 0L,
    val postCount: Long = 4L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MembershipTier(
    @DocumentId val id: String = "",
    val creatorHandle: String = "@maarya",
    val name: String = "",
    val rank: Long = 1L,
    val priceMonthlyCents: Long = 5000L,
    val badgeLabel: String = "",
    val description: String = "",
    val perks: List<String> = emptyList(),
    val active: Boolean = true,
    val playProductId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreatorPost(
    @DocumentId val id: String = "",
    val creatorHandle: String = "@maarya",
    val authorId: String = "",
    val title: String = "",
    val caption: String = "",
    val mediaType: String = MediaType.PHOTO.name,
    val mediaKey: String = "maarya_hero",
    val mediaUri: String? = null,
    val requiredTierRank: Long = 1L,
    val requiredTierName: String = "TIER 1 • $50",
    val ppvPriceCents: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProtectedPostPayload(
    @DocumentId val id: String = "payload",
    val postId: String = "",
    val requiredTierRank: Long = 1L,
    val fullCaption: String = "",
    val privateMediaUrl: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorTierBadge: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UnlockedPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val amountCents: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fanName: String = "",
    val fanEmail: String = "",
    val tierId: String = "",
    val tierName: String = "",
    val tierRank: Long = 1L,
    val monthlyPriceCents: Long = 5000L,
    val status: String = SubscriptionStatus.ACTIVE.name,
    val autoRenew: Boolean = true,
    val renewsAtLabel: String = "",
    val paymentMethod: String = PaymentProvider.ADMIN_GRANT.wireValue,
    val paymentLabel: String = "Verified Access",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fanName: String = "",
    val type: String = TransactionType.SUBSCRIPTION.name,
    val tierName: String = "",
    val amountCents: Long = 0L,
    val feeCents: Long = 0L,
    val netEarningsCents: Long = 0L,
    val status: String = "SUCCEEDED",
    val paymentMethod: String = PaymentProvider.PAYONEER.wireValue,
    val paymentLabel: String = "Payoneer",
    val externalRef: String? = null,
    val postId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MembershipRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fanName: String = "",
    val fanEmail: String = "",
    val requestedTierId: String = "",
    val requestedTierName: String = "",
    val preferredProvider: String = PaymentProvider.JAZZCASH.wireValue,
    val note: String? = null,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postTitle: String = "",
    val reporterUid: String = "",
    val reporterName: String = "",
    val reason: String = ReportReason.POLICY_VIOLATION.name,
    val details: String? = null,
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentSettings(
    @DocumentId val id: String = "maarya",
    val visaCheckoutEnabled: Boolean = false,
    val payoneerCheckoutEnabled: Boolean = false,
    val jazzcashGatewayEnabled: Boolean = false,
    val googlePlayBillingEnabled: Boolean = false,
    val payoneerEligibilityNote: String = "Disabled: Payoneer Checkout / recurring requires approved Business Merchant eligibility (Pakistan).",
    val jazzcashEligibilityNote: String = "Disabled: Requires JazzCash Merchant ID, Password, and Integrity Salt.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutConfig(
    @DocumentId val id: String = "maarya",
    val payoneerReceivingEmail: String = "aethrastudios8@gmail.com",
    val payoneerAccountType: String = "Personal Account (Checkout Disabled Until Business Approval)",
    val payoneerBusinessApproved: Boolean = false,
    val jazzcashWalletNumber: String = "",
    val jazzcashAccountTitle: String = "Maarya",
    val jazzcashMerchantId: String = "",
    val settlementSchedule: String = "Manual Payoneer -> JazzCash Transfer",
    val notes: String = "Receive international USD payouts in Payoneer, then withdraw eligible funds to Pakistani JazzCash account.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRecord(
    @DocumentId val id: String = "",
    val channel: String = WithdrawalChannel.PAYONEER_TO_JAZZCASH.wireValue,
    val amountCents: Long = 0L,
    val feeCents: Long = 0L,
    val netPayoutCents: Long = 0L,
    val destinationLabel: String = "",
    val referenceCode: String? = null,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val fanUid: String = "",
    val fanName: String = "",
    val fanTierName: String = "No Active Tier",
    val lastMessage: String = "",
    val unreadByCreator: Long = 0L,
    val unreadByFan: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val fanUid: String = "",
    val senderId: String = "",
    val senderRole: String = "FAN",
    val senderName: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CreatorAnalytics(
    val monthlyRecurringRevenueCents: Long = 0L,
    val totalGrossRevenueCents: Long = 0L,
    val totalFeesCents: Long = 0L,
    val totalNetEarningsCents: Long = 0L,
    val totalWithdrawnCents: Long = 0L,
    val availableBalanceCents: Long = 0L,
    val activePaidSubscribers: Int = 0,
    val totalSubscribers: Int = 0,
    val totalTipsAndUnlocksCents: Long = 0L,
    val visaRevenueCents: Long = 0L,
    val payoneerRevenueCents: Long = 0L,
    val jazzcashRevenueCents: Long = 0L,
    val playBillingRevenueCents: Long = 0L,
    val revenueSeriesCents: List<Pair<String, Long>> = emptyList()
)

object MaaryaDefaults {
    val defaultProfile = CreatorProfile(
        id = "maarya",
        displayName = "Maarya",
        handle = "@maarya",
        tagline = "MAARYA ONLYFANS • Private Black, Pink & Purple Studio",
        bio = "Welcome to my official private community. Subscribe to unlock curated photo sets, studio films, behind-the-scenes updates, and 1-on-1 VIP DMs.",
        location = "Lahore, Pakistan • Private Studio",
        verified = true,
        subscriberCount = 0L,
        postCount = 4L
    )

    val defaultPaymentSettings = PaymentSettings(
        id = "maarya",
        visaCheckoutEnabled = false,
        payoneerCheckoutEnabled = false,
        jazzcashGatewayEnabled = false,
        googlePlayBillingEnabled = false,
        payoneerEligibilityNote = "Disabled until Payoneer Business merchant approval & API keys are obtained (Pakistan accounts need commercial eligibility).",
        jazzcashEligibilityNote = "Disabled until JazzCash Merchant ID, Password, and HMAC Integrity Salt are configured."
    )

    val defaultPayoutConfig = PayoutConfig(
        id = "maarya",
        payoneerReceivingEmail = "aethrastudios8@gmail.com",
        payoneerAccountType = "Personal Account (Website Checkout Disabled Until Business Eligibility)",
        payoneerBusinessApproved = false,
        jazzcashWalletNumber = "",
        jazzcashAccountTitle = "Maarya",
        jazzcashMerchantId = "",
        settlementSchedule = "Manual (Payoneer USD -> JazzCash PK)",
        notes = "International earnings land in Payoneer USD; transfer eligible cleared funds to Pakistani JazzCash account."
    )

    val defaultTiers = listOf(
        MembershipTier(
            id = "tier_1",
            creatorHandle = "@maarya",
            name = "Tier 1 • Pink Silk",
            rank = 1L,
            priceMonthlyCents = 5000L,
            badgeLabel = "TIER 1 • $50",
            description = "Entry private community membership with curated studio photos and weekly updates.",
            perks = listOf(
                "Curated Tier 1 photo galleries",
                "Weekly private community updates",
                "Pink Silk member badge in comments"
            ),
            active = true,
            playProductId = "maarya_tier_50"
        ),
        MembershipTier(
            id = "tier_2",
            creatorHandle = "@maarya",
            name = "Tier 2 • Velvet Noir",
            rank = 2L,
            priceMonthlyCents = 10000L,
            badgeLabel = "TIER 2 • $100",
            description = "Full access to HD behind-the-scenes studio films, fashion lookbooks, and priority DMs.",
            perks = listOf(
                "Everything in Tier 1 ($50)",
                "HD behind-the-scenes video diaries",
                "Priority 1-on-1 direct messaging"
            ),
            active = true,
            playProductId = "maarya_tier_100"
        ),
        MembershipTier(
            id = "tier_3",
            creatorHandle = "@maarya",
            name = "Tier 3 • Royal Violet",
            rank = 3L,
            priceMonthlyCents = 20000L,
            badgeLabel = "TIER 3 • $200",
            description = "Unedited private editorial archives, early access drops, and custom Q&A requests.",
            perks = listOf(
                "Everything in Tier 2 ($100)",
                "Full unedited editorial archive",
                "Theme voting & private studio Q&A"
            ),
            active = true,
            playProductId = "maarya_tier_200"
        ),
        MembershipTier(
            id = "tier_4",
            creatorHandle = "@maarya",
            name = "Tier 4 • Diamond VIP",
            rank = 4L,
            priceMonthlyCents = 50000L,
            badgeLabel = "TIER 4 • $500",
            description = "Maarya's ultimate inner circle: complete Polaroid vault, personal shoutouts & VIP line.",
            perks = listOf(
                "Everything in Tier 3 ($200)",
                "Complete private Polaroid vault",
                "Top-priority direct chat with Maarya",
                "Monthly personal appreciation note"
            ),
            active = true,
            playProductId = "maarya_tier_500"
        )
    )

    fun starterPosts(adminUid: String): List<Triple<String, CreatorPost, String>> = listOf(
        Triple(
            "post_welcome_portrait",
            CreatorPost(
                id = "post_welcome_portrait",
                creatorHandle = "@maarya",
                authorId = adminUid,
                title = "Warm Peach Silk — Studio Morning",
                caption = "Soft morning portraits in natural light. Join Tier 1 ($50/mo) to read the full studio note and unlock the gallery.",
                mediaType = MediaType.PHOTO.name,
                mediaKey = "maarya_hero",
                requiredTierRank = 1L,
                requiredTierName = "TIER 1 • $50",
                ppvPriceCents = 1500L,
                likesCount = 0L,
                commentsCount = 0L,
                published = true
            ),
            "Full Tier 1 ($50) Member Note: Shot at 7:30 AM before styling began. Thank you to everyone joining the private MAARYA ONLYFANS community — new portrait drops arrive every Friday."
        ),
        Triple(
            "post_sunlit_floral",
            CreatorPost(
                id = "post_sunlit_floral",
                creatorHandle = "@maarya",
                authorId = adminUid,
                title = "Golden Floral Series — Full Lookbook",
                caption = "Shot at golden hour in my private apartment studio. Unlock Tier 2 ($100/mo) for the full series.",
                mediaType = MediaType.PHOTO.name,
                mediaKey = "maarya_portrait",
                requiredTierRank = 2L,
                requiredTierName = "TIER 2 • $100",
                ppvPriceCents = 2500L,
                likesCount = 0L,
                commentsCount = 0L,
                published = true
            ),
            "Full Tier 2 ($100) Member Note: 18 curated frames from the Golden Floral afternoon session, plus lighting notes and my wardrobe selection for next week's set."
        ),
        Triple(
            "post_velvet_bts",
            CreatorPost(
                id = "post_velvet_bts",
                creatorHandle = "@maarya",
                authorId = adminUid,
                title = "After Hours Studio Vlog — Uncut 14m Film",
                caption = "A quiet late-night Q&A answering fan DMs and sharing behind-the-scenes styling clips. Tier 3 ($200/mo) & above.",
                mediaType = MediaType.VIDEO.name,
                mediaKey = "maarya_hero",
                requiredTierRank = 3L,
                requiredTierName = "TIER 3 • $200",
                ppvPriceCents = 5000L,
                likesCount = 0L,
                commentsCount = 0L,
                published = true
            ),
            "Full Tier 3 ($200) Studio Diary: Uncut 14-minute late-night Q&A, outfit lookbook walkthrough, and sneak peek at next month's private shoot themes."
        ),
        Triple(
            "post_diamond_vault",
            CreatorPost(
                id = "post_diamond_vault",
                creatorHandle = "@maarya",
                authorId = adminUid,
                title = "Diamond Inner Circle: Private Polaroid Vault",
                caption = "My private unedited Polaroid archive + handwritten notes exclusively for Tier 4 ($500/mo) Diamond VIPs.",
                mediaType = MediaType.NOTE.name,
                mediaKey = "maarya_portrait",
                requiredTierRank = 4L,
                requiredTierName = "TIER 4 • $500",
                ppvPriceCents = 10000L,
                likesCount = 0L,
                commentsCount = 0L,
                published = true
            ),
            "Full Tier 4 ($500) Diamond VIP Vault: Private handwritten studio journal, Polaroid scans from this month's session, and direct VIP input for next month's shoot."
        )
    )
}

fun formatCents(cents: Long): String {
    if (cents <= 0L) return "$0"
    val dollars = cents / 100
    val rem = cents % 100
    return if (rem == 0L) "$$dollars" else "$${dollars}.${rem.toString().padStart(2, '0')}"
}
