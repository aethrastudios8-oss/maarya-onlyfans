package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.ContentReport
import com.example.data.model.Conversation
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
import com.example.data.model.UnlockedPost
import com.example.data.model.WithdrawalChannel
import com.example.data.model.WithdrawalRecord
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MaaryaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    private fun handleStreamError(error: Throwable, uid: String, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    fun observeCreatorProfile(): Flow<CreatorProfile?> = flow {
        val uid = requireUserId()
        val path = "creator_profile/maarya"
        emitAll(
            db.collection("creator_profile").document("maarya")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(CreatorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeMembershipTiers(): Flow<List<MembershipTier>> = flow {
        val uid = requireUserId()
        val path = "membership_tiers"
        emitAll(
            db.collection("membership_tiers")
                .whereEqualTo("creatorHandle", "@maarya")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MembershipTier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.rank }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePosts(includeDrafts: Boolean): Flow<List<CreatorPost>> = flow {
        val uid = requireUserId()
        val path = "posts"
        val baseQuery = db.collection("posts").whereEqualTo("creatorHandle", "@maarya")
        val query = if (includeDrafts) baseQuery else baseQuery.whereEqualTo("published", true)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CreatorPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMySubscription(userId: String): Flow<Subscription?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user subscription observation." }
        val path = "subscriptions/$userId"
        emitAll(
            db.collection("subscriptions").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(Subscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeAllSubscriptions(): Flow<List<Subscription>> = flow {
        val uid = requireUserId()
        val path = "subscriptions"
        emitAll(
            db.collection("subscriptions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Subscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeTransactions(isAdmin: Boolean): Flow<List<Transaction>> = flow {
        val uid = requireUserId()
        val path = "transactions"
        val query = if (isAdmin) {
            db.collection("transactions")
        } else {
            db.collection("transactions").whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Transaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUnlockedPostIds(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user unlocked posts observation." }
        val path = "users/$userId/unlocked_posts"
        emitAll(
            db.collection("users").document(userId).collection("unlocked_posts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UnlockedPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.postId }
                        .toSet()
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePaymentSettings(): Flow<PaymentSettings?> = flow {
        val uid = requireUserId()
        val path = "payment_settings/maarya"
        emitAll(
            db.collection("payment_settings").document("maarya")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(PaymentSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeMembershipRequests(isAdmin: Boolean): Flow<List<MembershipRequest>> = flow {
        val uid = requireUserId()
        val path = "membership_requests"
        val query = if (isAdmin) {
            db.collection("membership_requests")
        } else {
            db.collection("membership_requests").whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MembershipRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeContentReports(isAdmin: Boolean): Flow<List<ContentReport>> = flow {
        val uid = requireUserId()
        val path = "reports"
        val query = if (isAdmin) {
            db.collection("reports")
        } else {
            db.collection("reports").whereEqualTo("reporterUid", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ContentReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePayoutConfig(): Flow<PayoutConfig?> = flow {
        val uid = requireUserId()
        val path = "payout_configs/maarya"
        emitAll(
            db.collection("payout_configs").document("maarya")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(PayoutConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeWithdrawals(): Flow<List<WithdrawalRecord>> = flow {
        val uid = requireUserId()
        val path = "withdrawals"
        emitAll(
            db.collection("withdrawals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WithdrawalRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePostComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeConversations(isAdmin: Boolean): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "conversations"
        val query = if (isAdmin) {
            db.collection("conversations")
        } else {
            db.collection("conversations").whereEqualTo("fanUid", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMessages(fanUid: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "conversations/$fanUid/messages"
        emitAll(
            db.collection("conversations").document(fanUid).collection("messages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun fetchProtectedPayload(postId: String): ProtectedPostPayload? {
        auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("posts").document(postId)
                .collection("protected").document("payload")
                .get().await()
            if (snap.exists()) {
                snap.toObject(ProtectedPostPayload::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Denied cleanly by Firestore rules if subscriber lacks required tier
            null
        }
    }

    suspend fun fetchLikedPostIds(postIds: List<String>): Set<String> {
        val uid = auth.currentUser?.uid ?: return emptySet()
        val liked = mutableSetOf<String>()
        for (postId in postIds) {
            try {
                val doc = db.collection("posts").document(postId)
                    .collection("likes").document(uid).get().await()
                if (doc.exists()) liked.add(postId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Ignore individual check error
            }
        }
        return liked
    }

    suspend fun ensureFanUserProfile(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/${user.uid}"
        return try {
            val docRef = db.collection("users").document(user.uid)
            val snap = docRef.get().await()
            val displayName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Maarya Fan").take(80)
            val email = (user.email?.takeIf { it.isNotBlank() } ?: "${user.uid}@member.maarya").take(160)
            val photoUrl = user.photoUrl?.toString()?.take(1000)

            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to user.uid,
                    "displayName" to displayName,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) payload["photoUrl"] = photoUrl
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) updates["photoUrl"] = photoUrl
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun bootstrapMaaryaDefaultsIfAdmin(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val profileRef = db.collection("creator_profile").document("maarya")
            if (!profileRef.get().await().exists()) {
                val def = MaaryaDefaults.defaultProfile
                profileRef.set(
                    mapOf(
                        "displayName" to def.displayName,
                        "handle" to def.handle,
                        "tagline" to def.tagline,
                        "bio" to def.bio,
                        "location" to def.location,
                        "verified" to def.verified,
                        "subscriberCount" to def.subscriberCount,
                        "postCount" to def.postCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val settingsRef = db.collection("payment_settings").document("maarya")
            if (!settingsRef.get().await().exists()) {
                val def = MaaryaDefaults.defaultPaymentSettings
                settingsRef.set(
                    mapOf(
                        "visaCheckoutEnabled" to def.visaCheckoutEnabled,
                        "payoneerCheckoutEnabled" to def.payoneerCheckoutEnabled,
                        "jazzcashGatewayEnabled" to def.jazzcashGatewayEnabled,
                        "googlePlayBillingEnabled" to def.googlePlayBillingEnabled,
                        "payoneerEligibilityNote" to def.payoneerEligibilityNote,
                        "jazzcashEligibilityNote" to def.jazzcashEligibilityNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val payoutRef = db.collection("payout_configs").document("maarya")
            if (!payoutRef.get().await().exists()) {
                val def = MaaryaDefaults.defaultPayoutConfig
                payoutRef.set(
                    mapOf(
                        "payoneerReceivingEmail" to def.payoneerReceivingEmail,
                        "payoneerAccountType" to def.payoneerAccountType,
                        "payoneerBusinessApproved" to def.payoneerBusinessApproved,
                        "jazzcashWalletNumber" to def.jazzcashWalletNumber,
                        "jazzcashAccountTitle" to def.jazzcashAccountTitle,
                        "jazzcashMerchantId" to def.jazzcashMerchantId,
                        "settlementSchedule" to def.settlementSchedule,
                        "notes" to def.notes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Ensure $50 / $100 / $200 / $500 tiers
            val existingTiers = db.collection("membership_tiers")
                .whereEqualTo("creatorHandle", "@maarya")
                .get().await()
            val tier1Doc = existingTiers.documents.firstOrNull { it.id == "tier_1" }
            val needsTierMigration = existingTiers.isEmpty ||
                tier1Doc == null ||
                tier1Doc.getString("playProductId") == "maarya_tier_10" ||
                (tier1Doc.getLong("priceMonthlyCents") != 5000L && tier1Doc.getString("playProductId") != "maarya_tier_50")

            if (needsTierMigration) {
                for (legacyId in listOf("free", "silk", "velvet", "diamond")) {
                    if (existingTiers.documents.any { it.id == legacyId }) {
                        db.collection("membership_tiers").document(legacyId).delete().await()
                    }
                }
                for (tier in MaaryaDefaults.defaultTiers) {
                    db.collection("membership_tiers").document(tier.id).set(
                        mapOf(
                            "creatorHandle" to "@maarya",
                            "name" to tier.name,
                            "rank" to tier.rank,
                            "priceMonthlyCents" to tier.priceMonthlyCents,
                            "badgeLabel" to tier.badgeLabel,
                            "description" to tier.description,
                            "perks" to tier.perks,
                            "active" to tier.active,
                            "playProductId" to tier.playProductId,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // Ensure starter posts & protected payloads
            val existingPosts = db.collection("posts")
                .whereEqualTo("creatorHandle", "@maarya")
                .get().await()
            if (existingPosts.isEmpty) {
                for ((docId, post, fullBody) in MaaryaDefaults.starterPosts(uid)) {
                    db.collection("posts").document(docId).set(
                        mapOf(
                            "creatorHandle" to "@maarya",
                            "authorId" to uid,
                            "title" to post.title,
                            "caption" to post.caption,
                            "mediaType" to post.mediaType,
                            "mediaKey" to post.mediaKey,
                            "requiredTierRank" to post.requiredTierRank,
                            "requiredTierName" to post.requiredTierName,
                            "ppvPriceCents" to post.ppvPriceCents,
                            "likesCount" to 0L,
                            "commentsCount" to 0L,
                            "published" to post.published,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    db.collection("posts").document(docId)
                        .collection("protected").document("payload")
                        .set(
                            mapOf(
                                "postId" to docId,
                                "requiredTierRank" to post.requiredTierRank,
                                "fullCaption" to fullBody,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                }
            } else if (needsTierMigration) {
                for ((docId, post, fullBody) in MaaryaDefaults.starterPosts(uid)) {
                    if (existingPosts.documents.any { it.id == docId }) {
                        db.collection("posts").document(docId).update(
                            mapOf(
                                "requiredTierRank" to post.requiredTierRank,
                                "requiredTierName" to post.requiredTierName,
                                "ppvPriceCents" to post.ppvPriceCents,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        db.collection("posts").document(docId)
                            .collection("protected").document("payload")
                            .set(
                                mapOf(
                                    "postId" to docId,
                                    "requiredTierRank" to post.requiredTierRank,
                                    "fullCaption" to fullBody,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "bootstrap_maarya")
            Result.failure(e)
        }
    }

    suspend fun submitMembershipRequest(
        tier: MembershipTier,
        preferredProvider: PaymentProvider,
        note: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        val path = "membership_requests"
        return try {
            val fanName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Maarya Fan").take(80)
            val fanEmail = (user.email?.takeIf { it.isNotBlank() } ?: "$uid@member.maarya").take(160)
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "fanName" to fanName,
                "fanEmail" to fanEmail,
                "requestedTierId" to tier.id,
                "requestedTierName" to tier.name.take(40),
                "preferredProvider" to preferredProvider.wireValue,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )
            val cleanNote = note.trim().take(300)
            if (cleanNote.isNotEmpty()) payload["note"] = cleanNote
            db.collection("membership_requests").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitContentReport(
        post: CreatorPost,
        reason: ReportReason,
        details: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        val path = "reports"
        return try {
            val reporterName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Member").take(80)
            val payload = mutableMapOf<String, Any>(
                "postId" to post.id,
                "postTitle" to post.title.take(120),
                "reporterUid" to uid,
                "reporterName" to reporterName,
                "reason" to reason.name,
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanDetails = details.trim().take(500)
            if (cleanDetails.isNotEmpty()) payload["details"] = cleanDetails
            db.collection("reports").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSubscriptionStatus(
        targetUid: String,
        newStatus: SubscriptionStatus
    ): Result<Unit> {
        val path = "subscriptions/$targetUid"
        return try {
            requireUserId()
            db.collection("subscriptions").document(targetUid).update(
                mapOf(
                    "status" to newStatus.name,
                    "autoRenew" to (newStatus == SubscriptionStatus.ACTIVE),
                    "renewsAtLabel" to when (newStatus) {
                        SubscriptionStatus.ACTIVE -> "Renews monthly"
                        SubscriptionStatus.PAUSED -> "Paused"
                        SubscriptionStatus.CANCELLED -> "Ends at period close"
                    },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePostLike(post: CreatorPost, currentlyLiked: Boolean): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val postRef = db.collection("posts").document(post.id)
            val likeRef = postRef.collection("likes").document(uid)
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                if (post.likesCount > 0L) {
                    batch.update(
                        postRef,
                        mapOf(
                            "likesCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        postId: String,
        authorName: String,
        authorTierBadge: String,
        body: String
    ): Result<Unit> {
        val path = "posts/$postId/comments"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim()
            require(cleanBody.isNotEmpty() && cleanBody.length <= 500) {
                "Comment must be 1 to 500 characters."
            }
            val postRef = db.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document()
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to postId,
                    "userId" to uid,
                    "authorName" to authorName.take(60).ifBlank { "Member" },
                    "authorTierBadge" to authorTierBadge.take(40).ifBlank { "MEMBER" },
                    "body" to cleanBody,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        fanUid: String,
        fanName: String,
        fanTierName: String,
        body: String,
        asMaarya: Boolean
    ): Result<Unit> {
        val convPath = "conversations/$fanUid"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim()
            require(cleanBody.isNotEmpty() && cleanBody.length <= 1000) {
                "Message must be 1 to 1,000 characters."
            }
            val convRef = db.collection("conversations").document(fanUid)
            val existing = convRef.get().await()
            val lastPreview = cleanBody.take(500)
            val safeFanName = fanName.take(80).ifBlank { "Member" }
            val safeTierName = fanTierName.take(40).ifBlank { "No Active Tier" }

            if (!existing.exists()) {
                convRef.set(
                    mapOf(
                        "fanUid" to fanUid,
                        "fanName" to safeFanName,
                        "fanTierName" to safeTierName,
                        "lastMessage" to lastPreview,
                        "unreadByCreator" to if (asMaarya) 0L else 1L,
                        "unreadByFan" to if (asMaarya) 1L else 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                convRef.update(
                    mapOf(
                        "fanName" to safeFanName,
                        "fanTierName" to safeTierName,
                        "lastMessage" to lastPreview,
                        "unreadByCreator" to if (asMaarya) 0L else FieldValue.increment(1L),
                        "unreadByFan" to if (asMaarya) FieldValue.increment(1L) else 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val msgRef = convRef.collection("messages").document()
            msgRef.set(
                mapOf(
                    "fanUid" to fanUid,
                    "senderId" to uid,
                    "senderRole" to if (asMaarya) "MAARYA" else "FAN",
                    "senderName" to if (asMaarya) "Maarya" else safeFanName,
                    "body" to cleanBody,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convPath)
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(fanUid: String, asMaarya: Boolean): Result<Unit> {
        val convPath = "conversations/$fanUid"
        return try {
            requireUserId()
            val convRef = db.collection("conversations").document(fanUid)
            if (convRef.get().await().exists()) {
                convRef.update(
                    mapOf(
                        if (asMaarya) "unreadByCreator" to 0L else "unreadByFan" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convPath)
            Result.failure(e)
        }
    }

    // --- Maarya Super Admin Mutations ---

    suspend fun approveMembershipRequestAsAdmin(
        request: MembershipRequest,
        tier: MembershipTier,
        provider: PaymentProvider,
        recordVerifiedTransaction: Boolean,
        externalRef: String?
    ): Result<Unit> {
        val path = "subscriptions/${request.userId}"
        return try {
            requireUserId()
            val subRef = db.collection("subscriptions").document(request.userId)
            val existing = subRef.get().await()
            val label = when (provider) {
                PaymentProvider.PAYONEER -> "Payoneer • Verified"
                PaymentProvider.JAZZCASH -> "JazzCash • Verified"
                PaymentProvider.VISA_CARD -> "Visa Card • Verified"
                PaymentProvider.GOOGLE_PLAY -> "Google Play Billing"
                PaymentProvider.ADMIN_GRANT -> "Verified Studio Grant"
            }

            if (!existing.exists()) {
                subRef.set(
                    mapOf(
                        "userId" to request.userId,
                        "fanName" to request.fanName.take(80),
                        "fanEmail" to request.fanEmail.take(160),
                        "tierId" to tier.id,
                        "tierName" to tier.name,
                        "tierRank" to tier.rank,
                        "monthlyPriceCents" to tier.priceMonthlyCents,
                        "status" to SubscriptionStatus.ACTIVE.name,
                        "autoRenew" to (provider == PaymentProvider.GOOGLE_PLAY),
                        "renewsAtLabel" to if (recordVerifiedTransaction) "Verified Paid Period" else "Granted by Maarya",
                        "paymentMethod" to provider.wireValue,
                        "paymentLabel" to label,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                subRef.update(
                    mapOf(
                        "tierId" to tier.id,
                        "tierName" to tier.name,
                        "tierRank" to tier.rank,
                        "monthlyPriceCents" to tier.priceMonthlyCents,
                        "status" to SubscriptionStatus.ACTIVE.name,
                        "autoRenew" to (provider == PaymentProvider.GOOGLE_PLAY),
                        "renewsAtLabel" to if (recordVerifiedTransaction) "Verified Paid Period" else "Updated by Maarya",
                        "paymentMethod" to provider.wireValue,
                        "paymentLabel" to label,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (recordVerifiedTransaction) {
                val feeRatePct = when (provider) {
                    PaymentProvider.PAYONEER -> 3L
                    PaymentProvider.JAZZCASH -> 2L
                    PaymentProvider.VISA_CARD -> 3L
                    PaymentProvider.GOOGLE_PLAY -> 15L
                    PaymentProvider.ADMIN_GRANT -> 0L
                }
                val feeCents = (tier.priceMonthlyCents * feeRatePct) / 100L
                val netCents = (tier.priceMonthlyCents - feeCents).coerceAtLeast(0L)
                val txPayload = mutableMapOf<String, Any>(
                    "userId" to request.userId,
                    "fanName" to request.fanName.take(80),
                    "type" to TransactionType.SUBSCRIPTION.name,
                    "tierName" to tier.name.take(40),
                    "amountCents" to tier.priceMonthlyCents,
                    "feeCents" to feeCents,
                    "netEarningsCents" to netCents,
                    "status" to "SUCCEEDED",
                    "paymentMethod" to provider.wireValue,
                    "paymentLabel" to label,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                val cleanRef = externalRef?.trim()?.takeIf { it.isNotEmpty() }?.take(128)
                if (cleanRef != null) txPayload["externalRef"] = cleanRef
                db.collection("transactions").document().set(txPayload).await()
            }

            db.collection("membership_requests").document(request.id)
                .update(mapOf("status" to "APPROVED")).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun declineMembershipRequestAsAdmin(requestId: String): Result<Unit> {
        val path = "membership_requests/$requestId"
        return try {
            requireUserId()
            db.collection("membership_requests").document(requestId)
                .update(mapOf("status" to "DECLINED")).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun grantSubscriberTierAsAdmin(
        subscription: Subscription,
        newTier: MembershipTier
    ): Result<Unit> {
        val path = "subscriptions/${subscription.userId}"
        return try {
            requireUserId()
            val safeMethod = PaymentProvider.safeFrom(subscription.paymentMethod).wireValue
            val safeLabel = subscription.paymentLabel.ifBlank { "Verified Studio Grant" }
            db.collection("subscriptions").document(subscription.userId).update(
                mapOf(
                    "tierId" to newTier.id,
                    "tierName" to newTier.name,
                    "tierRank" to newTier.rank,
                    "monthlyPriceCents" to newTier.priceMonthlyCents,
                    "status" to SubscriptionStatus.ACTIVE.name,
                    "autoRenew" to false,
                    "renewsAtLabel" to "Updated by Maarya",
                    "paymentMethod" to safeMethod,
                    "paymentLabel" to safeLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveReportAsAdmin(
        reportId: String,
        newStatus: String,
        unpublishPostId: String? = null
    ): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireUserId()
            if (!unpublishPostId.isNullOrBlank()) {
                db.collection("posts").document(unpublishPostId).update(
                    mapOf(
                        "published" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun savePayoutConfig(config: PayoutConfig): Result<Unit> {
        val path = "payout_configs/maarya"
        return try {
            requireUserId()
            val payload = mutableMapOf<String, Any>(
                "payoneerReceivingEmail" to config.payoneerReceivingEmail.trim().take(160),
                "payoneerAccountType" to config.payoneerAccountType.trim().take(80).ifBlank { "Personal Account" },
                "payoneerBusinessApproved" to config.payoneerBusinessApproved,
                "jazzcashWalletNumber" to config.jazzcashWalletNumber.trim().take(40),
                "jazzcashAccountTitle" to config.jazzcashAccountTitle.trim().take(80).ifBlank { "Maarya" },
                "settlementSchedule" to config.settlementSchedule.trim().take(60).ifBlank { "Manual" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val merchantId = config.jazzcashMerchantId.trim().take(60)
            if (merchantId.isNotEmpty()) payload["jazzcashMerchantId"] = merchantId
            val notes = config.notes.trim().take(400)
            if (notes.isNotEmpty()) payload["notes"] = notes
            db.collection("payout_configs").document("maarya").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createWithdrawal(
        channel: WithdrawalChannel,
        amountCents: Long,
        feeCents: Long,
        destinationLabel: String,
        referenceCode: String
    ): Result<Unit> {
        val path = "withdrawals"
        return try {
            requireUserId()
            require(amountCents >= 100L) { "Minimum withdrawal amount is $1." }
            require(feeCents in 0L..amountCents) { "Fee cannot exceed withdrawal amount." }
            val net = (amountCents - feeCents).coerceAtLeast(0L)
            val payload = mutableMapOf<String, Any>(
                "channel" to channel.wireValue,
                "amountCents" to amountCents,
                "feeCents" to feeCents,
                "netPayoutCents" to net,
                "destinationLabel" to destinationLabel.trim().take(120).ifBlank { "JazzCash Pakistan" },
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanRef = referenceCode.trim().take(80)
            if (cleanRef.isNotEmpty()) payload["referenceCode"] = cleanRef
            db.collection("withdrawals").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markWithdrawalSettled(withdrawalId: String, status: String): Result<Unit> {
        val path = "withdrawals/$withdrawalId"
        return try {
            requireUserId()
            db.collection("withdrawals").document(withdrawalId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCreatorProfile(
        tagline: String,
        bio: String,
        location: String,
        currentProfile: CreatorProfile
    ): Result<Unit> {
        val path = "creator_profile/maarya"
        return try {
            requireUserId()
            val cleanTagline = tagline.trim().take(160)
            val cleanBio = bio.trim().take(1000)
            val cleanLocation = location.trim().take(80)
            require(cleanTagline.isNotEmpty() && cleanBio.isNotEmpty() && cleanLocation.isNotEmpty()) {
                "Tagline, bio, and location cannot be empty."
            }
            db.collection("creator_profile").document("maarya").set(
                mapOf(
                    "displayName" to "Maarya",
                    "handle" to "@maarya",
                    "tagline" to cleanTagline,
                    "bio" to cleanBio,
                    "location" to cleanLocation,
                    "verified" to true,
                    "subscriberCount" to currentProfile.subscriberCount,
                    "postCount" to currentProfile.postCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMembershipTier(
        tier: MembershipTier,
        name: String,
        priceMonthlyCents: Long,
        description: String,
        perks: List<String>,
        active: Boolean
    ): Result<Unit> {
        val path = "membership_tiers/${tier.id}"
        return try {
            requireUserId()
            val cleanName = name.trim().take(40)
            val cleanDesc = description.trim().take(300)
            val cleanPerks = perks.map { it.trim() }.filter { it.isNotEmpty() }.take(8)
            require(cleanName.length >= 2 && cleanDesc.isNotEmpty()) {
                "Tier name and description are required."
            }
            require(priceMonthlyCents in 100L..200000L) {
                "Monthly price must be between $1 and $2,000."
            }
            val docRef = db.collection("membership_tiers").document(tier.id)
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "rank" to tier.rank,
                        "priceMonthlyCents" to priceMonthlyCents,
                        "badgeLabel" to tier.badgeLabel,
                        "description" to cleanDesc,
                        "perks" to cleanPerks,
                        "active" to active,
                        "playProductId" to tier.playProductId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "creatorHandle" to "@maarya",
                        "name" to cleanName,
                        "rank" to tier.rank,
                        "priceMonthlyCents" to priceMonthlyCents,
                        "badgeLabel" to tier.badgeLabel,
                        "description" to cleanDesc,
                        "perks" to cleanPerks,
                        "active" to active,
                        "playProductId" to tier.playProductId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishOrUpdatePost(
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
    ): Result<Unit> {
        val isCreate = existingPostId.isNullOrBlank()
        val docRef = if (isCreate) {
            db.collection("posts").document()
        } else {
            db.collection("posts").document(existingPostId)
        }
        val path = docRef.path
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanCaption = caption.trim().take(2000)
            require(cleanTitle.isNotEmpty() && cleanCaption.isNotEmpty()) {
                "Title and caption are required."
            }
            require(ppvPriceCents in 0L..100000L) {
                "PPV unlock price must be between $0 and $1,000."
            }
            val cleanUri = mediaUri?.trim()?.takeIf { it.isNotEmpty() }?.take(1000)
            val previewTeaser = if (cleanCaption.length > 110) cleanCaption.take(110).trimEnd() + "..." else cleanCaption

            if (isCreate) {
                val payload = mutableMapOf<String, Any>(
                    "creatorHandle" to "@maarya",
                    "authorId" to uid,
                    "title" to cleanTitle,
                    "caption" to previewTeaser,
                    "mediaType" to mediaType,
                    "mediaKey" to mediaKey.take(64),
                    "requiredTierRank" to requiredTierRank,
                    "requiredTierName" to requiredTierName.take(40),
                    "ppvPriceCents" to ppvPriceCents,
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "published" to published,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "caption" to previewTeaser,
                    "mediaType" to mediaType,
                    "mediaKey" to mediaKey.take(64),
                    "mediaUri" to FieldValue.delete(),
                    "requiredTierRank" to requiredTierRank,
                    "requiredTierName" to requiredTierName.take(40),
                    "ppvPriceCents" to ppvPriceCents,
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }

            // Write full content & private media URL strictly inside subscriber-protected subcollection
            val protectedMap = mutableMapOf<String, Any>(
                "postId" to docRef.id,
                "requiredTierRank" to requiredTierRank,
                "fullCaption" to cleanCaption,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanUri != null) protectedMap["privateMediaUrl"] = cleanUri
            docRef.collection("protected").document("payload").set(protectedMap).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
