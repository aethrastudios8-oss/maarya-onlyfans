package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.PlayBillingManager
import com.example.billing.PlayBillingState
import com.example.data.model.ChatMessage
import com.example.data.model.ContentReport
import com.example.data.model.Conversation
import com.example.data.model.CreatorAnalytics
import com.example.data.model.CreatorPost
import com.example.data.model.CreatorProfile
import com.example.data.model.MaaryaDefaults
import com.example.data.model.MembershipRequest
import com.example.data.model.MembershipTier
import com.example.data.model.PaymentProvider
import com.example.data.model.PaymentSettings
import com.example.data.model.PayoutConfig
import com.example.data.model.PostComment
import com.example.data.model.ProtectedPostPayload
import com.example.data.model.ReportReason
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.data.model.WithdrawalChannel
import com.example.data.model.WithdrawalRecord
import com.example.data.repository.MaaryaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MaaryaViewModel(
    private val repository: MaaryaRepository,
    val currentUserId: String,
    val isAdmin: Boolean,
    val currentUserName: String,
    val currentUserEmail: String,
    private val playBillingManager: PlayBillingManager? = null
) : ViewModel() {

    private val _isFanPreviewMode = MutableStateFlow(false)
    val isFanPreviewMode: StateFlow<Boolean> = _isFanPreviewMode.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _protectedPayloads = MutableStateFlow<Map<String, ProtectedPostPayload>>(emptyMap())
    val protectedPayloads: StateFlow<Map<String, ProtectedPostPayload>> = _protectedPayloads.asStateFlow()

    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeCommentPostId: StateFlow<String?> = _activeCommentPostId.asStateFlow()

    private val _activeChatFanUid = MutableStateFlow<String?>(if (isAdmin) null else currentUserId)
    val activeChatFanUid: StateFlow<String?> = _activeChatFanUid.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val playBillingState: StateFlow<PlayBillingState> =
        playBillingManager?.state ?: MutableStateFlow(PlayBillingState())

    private fun <T> Flow<T>.toUiState(tag: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tag", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Unable to load $tag"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val creatorProfileState: StateFlow<UiState<CreatorProfile>> =
        repository.observeCreatorProfile()
            .map { it ?: MaaryaDefaults.defaultProfile }
            .toUiState("creator profile")

    val tiersState: StateFlow<UiState<List<MembershipTier>>> =
        repository.observeMembershipTiers()
            .map { list -> if (list.isEmpty()) MaaryaDefaults.defaultTiers else list }
            .toUiState("membership tiers")

    val paymentSettingsState: StateFlow<UiState<PaymentSettings>> =
        repository.observePaymentSettings()
            .map { it ?: MaaryaDefaults.defaultPaymentSettings }
            .toUiState("payment settings")

    val postsState: StateFlow<UiState<List<CreatorPost>>> =
        _isFanPreviewMode.flatMapLatest { preview ->
            repository.observePosts(includeDrafts = isAdmin && !preview)
        }.toUiState("posts")

    val mySubscriptionState: StateFlow<UiState<Subscription?>> =
        repository.observeMySubscription(currentUserId)
            .toUiState("subscription")

    val unlockedPostIdsState: StateFlow<UiState<Set<String>>> =
        repository.observeUnlockedPostIds(currentUserId)
            .toUiState("unlocked posts")

    val transactionsState: StateFlow<UiState<List<Transaction>>> =
        _isFanPreviewMode.flatMapLatest { preview ->
            repository.observeTransactions(isAdmin = isAdmin && !preview)
        }.toUiState("transactions")

    val membershipRequestsState: StateFlow<UiState<List<MembershipRequest>>> =
        _isFanPreviewMode.flatMapLatest { preview ->
            repository.observeMembershipRequests(isAdmin = isAdmin && !preview)
        }.toUiState("membership requests")

    val reportsState: StateFlow<UiState<List<ContentReport>>> =
        _isFanPreviewMode.flatMapLatest { preview ->
            repository.observeContentReports(isAdmin = isAdmin && !preview)
        }.toUiState("content reports")

    val allSubscriptionsState: StateFlow<UiState<List<Subscription>>> =
        if (isAdmin) {
            repository.observeAllSubscriptions().toUiState("subscribers")
        } else {
            MutableStateFlow(UiState.Success(emptyList()))
        }

    val payoutConfigState: StateFlow<UiState<PayoutConfig>> =
        if (isAdmin) {
            repository.observePayoutConfig()
                .map { it ?: MaaryaDefaults.defaultPayoutConfig }
                .toUiState("payout config")
        } else {
            MutableStateFlow(UiState.Success(MaaryaDefaults.defaultPayoutConfig))
        }

    val withdrawalsState: StateFlow<UiState<List<WithdrawalRecord>>> =
        if (isAdmin) {
            repository.observeWithdrawals().toUiState("withdrawals")
        } else {
            MutableStateFlow(UiState.Success(emptyList()))
        }

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        _isFanPreviewMode.flatMapLatest { preview ->
            repository.observeConversations(isAdmin = isAdmin && !preview)
        }.toUiState("conversations")

    val activeCommentsState: StateFlow<UiState<List<PostComment>>> =
        _activeCommentPostId.flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList())
            else repository.observePostComments(postId)
        }.toUiState("comments")

    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeChatFanUid.flatMapLatest { fanUid ->
            if (fanUid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeMessages(fanUid)
        }.toUiState("messages")

    val analyticsState: StateFlow<CreatorAnalytics> =
        combine(allSubscriptionsState, transactionsState, withdrawalsState) { subsState, txState, wdState ->
            val subs = (subsState as? UiState.Success)?.data ?: emptyList()
            val txs = (txState as? UiState.Success)?.data ?: emptyList()
            val wds = (wdState as? UiState.Success)?.data ?: emptyList()

            val activeSubs = subs.filter { it.status == SubscriptionStatus.ACTIVE.name }
            val paidActive = activeSubs.filter { it.monthlyPriceCents > 0L }
            val mrr = paidActive.sumOf { it.monthlyPriceCents }

            val succeededTxs = txs.filter { it.status == "SUCCEEDED" }
            val gross = succeededTxs.sumOf { it.amountCents }
            val fees = succeededTxs.sumOf { it.feeCents }
            val net = succeededTxs.sumOf { it.netEarningsCents }
            val withdrawn = wds.sumOf { it.amountCents }
            val available = (net - withdrawn).coerceAtLeast(0L)

            val tipsAndUnlocks = succeededTxs
                .filter { it.type == TransactionType.TIP.name || it.type == TransactionType.UNLOCK.name }
                .sumOf { it.amountCents }

            val visaRev = succeededTxs.filter { it.paymentMethod == PaymentProvider.VISA_CARD.wireValue }.sumOf { it.amountCents }
            val payoneerRev = succeededTxs.filter { it.paymentMethod == PaymentProvider.PAYONEER.wireValue }.sumOf { it.amountCents }
            val jazzcashRev = succeededTxs.filter { it.paymentMethod == PaymentProvider.JAZZCASH.wireValue }.sumOf { it.amountCents }
            val playRev = succeededTxs.filter { it.paymentMethod == PaymentProvider.GOOGLE_PLAY.wireValue }.sumOf { it.amountCents }

            val subOrUpgradeTxs = succeededTxs.filter {
                it.type == TransactionType.SUBSCRIPTION.name || it.type == TransactionType.UPGRADE.name
            }
            val series = listOf("\$50", "\$100", "\$200", "\$500", "Unlocks").map { bucket ->
                val sum = when (bucket) {
                    "\$50" -> subOrUpgradeTxs.filter {
                        it.tierName.contains("Tier 1", true) || it.tierName.contains("\$50", true) || it.tierName.contains("Pink Silk", true)
                    }.sumOf { it.amountCents }
                    "\$100" -> subOrUpgradeTxs.filter {
                        it.tierName.contains("Tier 2", true) || it.tierName.contains("\$100", true) || it.tierName.contains("Velvet Noir", true)
                    }.sumOf { it.amountCents }
                    "\$200" -> subOrUpgradeTxs.filter {
                        it.tierName.contains("Tier 3", true) || it.tierName.contains("\$200", true) || it.tierName.contains("Royal Violet", true)
                    }.sumOf { it.amountCents }
                    "\$500" -> subOrUpgradeTxs.filter {
                        it.tierName.contains("Tier 4", true) || it.tierName.contains("\$500", true) || it.tierName.contains("Diamond", true)
                    }.sumOf { it.amountCents }
                    else -> tipsAndUnlocks
                }
                bucket to sum
            }

            CreatorAnalytics(
                monthlyRecurringRevenueCents = mrr,
                totalGrossRevenueCents = gross,
                totalFeesCents = fees,
                totalNetEarningsCents = net,
                totalWithdrawnCents = withdrawn,
                availableBalanceCents = available,
                activePaidSubscribers = paidActive.size,
                totalSubscribers = subs.size,
                totalTipsAndUnlocksCents = tipsAndUnlocks,
                visaRevenueCents = visaRev,
                payoneerRevenueCents = payoneerRev,
                jazzcashRevenueCents = jazzcashRev,
                playBillingRevenueCents = playRev,
                revenueSeriesCents = series
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CreatorAnalytics()
        )

    init {
        viewModelScope.launch {
            repository.ensureFanUserProfile()
            if (isAdmin) {
                repository.bootstrapMaaryaDefaultsIfAdmin()
            }
        }
        viewModelScope.launch {
            postsState.collect { state ->
                if (state is UiState.Success && state.data.isNotEmpty()) {
                    _likedPostIds.value = repository.fetchLikedPostIds(state.data.map { it.id })
                }
            }
        }
    }

    fun loadProtectedPostIfUnlocked(postId: String) {
        if (_protectedPayloads.value.containsKey(postId)) return
        viewModelScope.launch {
            val payload = repository.fetchProtectedPayload(postId)
            if (payload != null) {
                _protectedPayloads.value = _protectedPayloads.value + (postId to payload)
            }
        }
    }

    fun emitMessage(message: String) {
        viewModelScope.launch {
            _snackbarEvents.emit(message)
        }
    }

    fun setFanPreviewMode(enabled: Boolean) {
        if (!isAdmin) return
        _isFanPreviewMode.value = enabled
        _activeChatFanUid.value = if (enabled) currentUserId else null
        _protectedPayloads.value = emptyMap()
    }

    fun openComments(postId: String?) {
        _activeCommentPostId.value = postId
    }

    fun selectConversation(fanUid: String?) {
        _activeChatFanUid.value = fanUid
        if (!fanUid.isNullOrBlank()) {
            viewModelScope.launch {
                repository.markConversationRead(fanUid, asMaarya = isAdmin && !_isFanPreviewMode.value)
            }
        }
    }

    fun currentFanRank(sub: Subscription?): Long {
        if (isAdmin && !_isFanPreviewMode.value) return 4L
        if (sub == null || sub.status != SubscriptionStatus.ACTIVE.name) return 0L
        return sub.tierRank
    }

    fun currentFanTierName(sub: Subscription?): String {
        if (isAdmin && !_isFanPreviewMode.value) return "Creator (Maarya)"
        if (sub == null || sub.status != SubscriptionStatus.ACTIVE.name) return "No Active Tier"
        return sub.tierName.ifBlank { "No Active Tier" }
    }

    fun submitMembershipRequest(
        tier: MembershipTier,
        preferredProvider: PaymentProvider,
        note: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitMembershipRequest(tier, preferredProvider, note)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Access request for ${tier.name} sent to Maarya Studio.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Request failed")
            }
        }
    }

    fun submitContentReport(
        post: CreatorPost,
        reason: ReportReason,
        details: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitContentReport(post, reason, details)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Report submitted for Maarya moderation review.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Failed to submit report")
            }
        }
    }

    fun updateMySubscriptionStatus(status: SubscriptionStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateSubscriptionStatus(currentUserId, status)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Membership updated to ${status.label}.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Could not update membership")
            }
        }
    }

    fun toggleLike(post: CreatorPost) {
        val currentlyLiked = _likedPostIds.value.contains(post.id)
        _likedPostIds.value = if (currentlyLiked) {
            _likedPostIds.value - post.id
        } else {
            _likedPostIds.value + post.id
        }
        viewModelScope.launch {
            val res = repository.togglePostLike(post, currentlyLiked)
            if (res.isFailure) {
                _likedPostIds.value = if (currentlyLiked) {
                    _likedPostIds.value + post.id
                } else {
                    _likedPostIds.value - post.id
                }
                _snackbarEvents.emit("Could not update like")
            }
        }
    }

    fun addComment(postId: String, tierBadge: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            val authorName = if (isAdmin && !_isFanPreviewMode.value) "Maarya" else currentUserName
            val badge = if (isAdmin && !_isFanPreviewMode.value) "CREATOR" else tierBadge
            val res = repository.addComment(postId, authorName, badge, body)
            res.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Could not post comment")
            }
        }
    }

    fun sendMessage(fanUid: String, fanName: String, fanTierName: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            val asMaarya = isAdmin && !_isFanPreviewMode.value
            val res = repository.sendMessage(
                fanUid = fanUid,
                fanName = fanName,
                fanTierName = fanTierName,
                body = body,
                asMaarya = asMaarya
            )
            res.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Message failed to send")
            }
        }
    }

    // --- Maarya Super Admin Actions ---

    fun approveMembershipRequest(
        request: MembershipRequest,
        tier: MembershipTier,
        provider: PaymentProvider,
        recordTransaction: Boolean,
        externalRef: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.approveMembershipRequestAsAdmin(
                request = request,
                tier = tier,
                provider = provider,
                recordVerifiedTransaction = recordTransaction,
                externalRef = externalRef
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Activated ${request.fanName} on ${tier.name}.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Approval failed")
            }
        }
    }

    fun declineMembershipRequest(requestId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.declineMembershipRequestAsAdmin(requestId)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Request declined.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Decline failed")
            }
        }
    }

    fun resolveReport(reportId: String, newStatus: String, unpublishPostId: String? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.resolveReportAsAdmin(reportId, newStatus, unpublishPostId)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Moderation report updated ($newStatus).")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Report update failed")
            }
        }
    }

    fun savePayoutConfig(config: PayoutConfig) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.savePayoutConfig(config)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Saved Payoneer -> JazzCash settlement settings.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Failed to save payout config")
            }
        }
    }

    fun createWithdrawal(
        channel: WithdrawalChannel,
        amountCents: Long,
        feeCents: Long,
        destinationLabel: String,
        referenceCode: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createWithdrawal(channel, amountCents, feeCents, destinationLabel, referenceCode)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Withdrawal recorded (${channel.label}).")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Withdrawal failed")
            }
        }
    }

    fun markWithdrawalSettled(withdrawalId: String, status: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.markWithdrawalSettled(withdrawalId, status)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Settlement status set to $status.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Update failed")
            }
        }
    }

    fun saveCreatorProfile(tagline: String, bio: String, location: String, current: CreatorProfile) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateCreatorProfile(tagline, bio, location, current)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Maarya's creator profile updated.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Profile update failed")
            }
        }
    }

    fun saveMembershipTier(
        tier: MembershipTier,
        name: String,
        priceMonthlyCents: Long,
        description: String,
        perks: List<String>,
        active: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateMembershipTier(tier, name, priceMonthlyCents, description, perks, active)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Saved tier \"$name\".")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Tier update failed")
            }
        }
    }

    fun publishOrUpdatePost(
        existingPostId: String?,
        title: String,
        caption: String,
        mediaType: String,
        mediaKey: String,
        mediaUri: String?,
        requiredTierRank: Long,
        requiredTierName: String,
        ppvPriceCents: Long,
        published: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.publishOrUpdatePost(
                existingPostId = existingPostId,
                title = title,
                caption = caption,
                mediaType = mediaType,
                mediaKey = mediaKey,
                mediaUri = mediaUri,
                requiredTierRank = requiredTierRank,
                requiredTierName = requiredTierName,
                ppvPriceCents = ppvPriceCents,
                published = published
            )
            _isBusy.value = false
            res.onSuccess {
                _protectedPayloads.value = emptyMap()
                _snackbarEvents.emit(
                    if (existingPostId == null) "Published to Maarya's feed!" else "Post updated!"
                )
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Failed to save post")
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deletePost(postId)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Post removed.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Failed to delete post")
            }
        }
    }

    fun updateSubscriberStatusAsAdmin(targetUid: String, status: SubscriptionStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateSubscriptionStatus(targetUid, status)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Subscriber status updated to ${status.label}.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Update failed")
            }
        }
    }

    fun grantSubscriberTierAsAdmin(subscription: Subscription, newTier: MembershipTier) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.grantSubscriberTierAsAdmin(subscription, newTier)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.emit("Updated ${subscription.fanName} to ${newTier.name}.")
            }.onFailure { err ->
                _snackbarEvents.emit(err.localizedMessage ?: "Update failed")
            }
        }
    }

    private companion object {
        const val TAG = "MaaryaVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
