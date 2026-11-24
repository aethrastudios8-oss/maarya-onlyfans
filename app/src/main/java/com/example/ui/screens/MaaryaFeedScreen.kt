package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CreatorPost
import com.example.data.model.CreatorProfile
import com.example.data.model.MediaType
import com.example.data.model.PostComment
import com.example.data.model.ProtectedPostPayload
import com.example.data.model.Subscription
import com.example.data.model.UiState
import com.example.ui.components.MaaryaMediaFrame
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.TierBadgePill
import com.example.ui.components.VerifiedMaaryaBadge
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.RoseSilk
import com.example.ui.theme.VelvetCard
import com.example.ui.theme.VelvetCardElevated
import com.example.ui.theme.VelvetObsidian
import com.example.ui.viewmodel.MaaryaViewModel

private enum class FeedFilter(val label: String) {
    ALL("All Drops"),
    PHOTO("Photos"),
    VIDEO("Videos"),
    NOTE("Studio Notes"),
    UNLOCKED("Unlocked")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaaryaFeedScreen(
    viewModel: MaaryaViewModel,
    onNavigateToClub: () -> Unit,
    onNavigateToMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profileState by viewModel.creatorProfileState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val subState by viewModel.mySubscriptionState.collectAsStateWithLifecycle()
    val unlockedState by viewModel.unlockedPostIdsState.collectAsStateWithLifecycle()
    val protectedPayloads by viewModel.protectedPayloads.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val activeCommentPostId by viewModel.activeCommentPostId.collectAsStateWithLifecycle()
    val commentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(FeedFilter.ALL) }
    var reportTargetPost by remember { mutableStateOf<CreatorPost?>(null) }

    val profile = (profileState as? UiState.Success)?.data ?: CreatorProfile()
    val mySub: Subscription? = (subState as? UiState.Success)?.data
    val unlockedIds = (unlockedState as? UiState.Success)?.data ?: emptySet()
    val fanRank = viewModel.currentFanRank(mySub)
    val fanTierName = viewModel.currentFanTierName(mySub)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VelvetObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = postsState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChampagneGold)
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(state.message, color = CrimsonAlert, style = MaterialTheme.typography.bodyLarge)
                }
            }
            is UiState.Success -> {
                val filteredPosts = remember(state.data, selectedFilter, fanRank, unlockedIds) {
                    state.data.filter { post ->
                        val isUnlocked = fanRank >= post.requiredTierRank || unlockedIds.contains(post.id)
                        when (selectedFilter) {
                            FeedFilter.ALL -> true
                            FeedFilter.PHOTO -> post.mediaType == MediaType.PHOTO.name
                            FeedFilter.VIDEO -> post.mediaType == MediaType.VIDEO.name
                            FeedFilter.NOTE -> post.mediaType == MediaType.NOTE.name
                            FeedFilter.UNLOCKED -> isUnlocked
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MaaryaHeroHeader(
                            profile = profile,
                            fanTierName = fanTierName,
                            fanRank = fanRank,
                            totalPublished = state.data.count { it.published },
                            onJoinClub = onNavigateToClub,
                            onMessageMaarya = onNavigateToMessages
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeedFilter.entries.forEach { filter ->
                                val selected = selectedFilter == filter
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampagneGold,
                                        selectedLabelColor = VelvetObsidian,
                                        containerColor = VelvetCard,
                                        labelColor = PearlIvory
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.testTag("feed_filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filteredPosts.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = VelvetCard,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No exclusive drops in this filter yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PearlIvory
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Maarya publishes new galleries, films, and notes directly from her Studio.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            val isUnlocked = fanRank >= post.requiredTierRank || unlockedIds.contains(post.id)
                            LaunchedEffect(post.id, isUnlocked) {
                                if (isUnlocked) {
                                    viewModel.loadProtectedPostIfUnlocked(post.id)
                                }
                            }
                            val payload = protectedPayloads[post.id]
                            CreatorPostCard(
                                post = post,
                                protectedPayload = payload,
                                isUnlocked = isUnlocked,
                                isLiked = likedIds.contains(post.id),
                                onLikeClick = { viewModel.toggleLike(post) },
                                onCommentsClick = { viewModel.openComments(post.id) },
                                onReportClick = { reportTargetPost = post },
                                onUnlockTierClick = onNavigateToClub,
                                onUnlockPpvClick = {
                                    viewModel.emitMessage("Single-drop web checkout is disabled until merchant keys are active. Request access in the Club tab.")
                                    onNavigateToClub()
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeCommentPostId != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.openComments(null) },
            containerColor = VelvetCardElevated
        ) {
            CommentsSheetContent(
                commentsState = commentsState,
                onPostComment = { text ->
                    activeCommentPostId?.let { postId ->
                        viewModel.addComment(postId, fanTierName, text)
                    }
                }
            )
        }
    }

    reportTargetPost?.let { target ->
        ReportContentDialog(
            post = target,
            onDismiss = { reportTargetPost = null },
            onSubmitReport = { post, reason, details ->
                viewModel.submitContentReport(post, reason, details)
                reportTargetPost = null
            }
        )
    }
}

@Composable
private fun MaaryaHeroHeader(
    profile: CreatorProfile,
    fanTierName: String,
    fanRank: Long,
    totalPublished: Int,
    onJoinClub: () -> Unit,
    onMessageMaarya: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
            Image(
                painter = painterResource(R.drawable.maarya_hero),
                contentDescription = "Maarya cover banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                VelvetObsidian.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp)
                    .offset(y = 32.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Image(
                    painter = painterResource(R.drawable.maarya_portrait),
                    contentDescription = "Maarya avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, ChampagneGold, CircleShape)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = PearlIvory
                        )
                        if (profile.verified) {
                            Spacer(Modifier.width(6.dp))
                            VerifiedMaaryaBadge()
                        }
                    }
                    Text(
                        text = "MAARYA ONLYFANS • ${profile.handle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseSilk
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = profile.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TierBadgePill(label = fanTierName, rank = fanRank)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = profile.tagline,
                style = MaterialTheme.typography.titleMedium,
                color = ChampagneGoldBright
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = PearlIvory.copy(alpha = 0.86f)
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(value = "${profile.subscriberCount}", label = "Club Members", modifier = Modifier.weight(1f))
                StatChip(value = "$totalPublished", label = "Exclusive Drops", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onJoinClub,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold,
                        contentColor = VelvetObsidian
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_membership_button")
                ) {
                    Text(
                        text = if (fanRank == 0L) "Join Club (\$50+)" else "Manage Tier",
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VelvetCard,
                    border = BorderStroke(1.dp, RoseSilk.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(onClick = onMessageMaarya)
                        .testTag("hero_dm_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Message Maarya",
                            tint = RoseSilk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("DM Maarya", color = RoseSilk, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = VelvetCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = PearlIvory)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreatorPostCard(
    post: CreatorPost,
    protectedPayload: ProtectedPostPayload?,
    isUnlocked: Boolean,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onReportClick: () -> Unit,
    onUnlockTierClick: () -> Unit,
    onUnlockPpvClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaType = MediaType.safeFrom(post.mediaType)
    val effectiveCaption = if (isUnlocked) {
        protectedPayload?.fullCaption?.takeIf { it.isNotBlank() } ?: post.caption
    } else {
        post.caption
    }
    val effectiveMediaUri = if (isUnlocked) {
        protectedPayload?.privateMediaUrl?.takeIf { it.isNotBlank() } ?: post.mediaUri
    } else {
        null
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(22.dp),
        color = VelvetCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnlocked) MaterialTheme.colorScheme.outlineVariant else ChampagneGold.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.maarya_portrait),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, ChampagneGold, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Maarya", style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                            Spacer(Modifier.width(4.dp))
                            VerifiedMaaryaBadge(modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = mediaType.label + if (!post.published) " • DRAFT" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!post.published) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isUnlocked) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Unlocked",
                            tint = EmeraldActive,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    TierBadgePill(label = post.requiredTierName, rank = post.requiredTierRank)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(text = post.title, style = MaterialTheme.typography.titleLarge, color = PearlIvory)
            Spacer(Modifier.height(6.dp))
            Text(
                text = effectiveCaption,
                style = MaterialTheme.typography.bodyMedium,
                color = PearlIvory.copy(alpha = 0.88f)
            )

            if (post.mediaKey.isNotBlank() || !effectiveMediaUri.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                MaaryaMediaFrame(
                    mediaKey = post.mediaKey,
                    mediaUri = effectiveMediaUri,
                    mediaType = post.mediaType,
                    locked = !isUnlocked,
                    requiredTierName = post.requiredTierName,
                    ppvPriceCents = post.ppvPriceCents,
                    onUnlockTierClick = onUnlockTierClick,
                    onUnlockPpvClick = onUnlockPpvClick
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("like_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like post",
                            tint = if (isLiked) RoseSilk else PearlIvory
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = PearlIvory
                    )
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = onCommentsClick,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("comment_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = PearlIvory
                        )
                    }
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = PearlIvory
                    )
                }

                TextButton(
                    onClick = onReportClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("report_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report content",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Report", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CommentsSheetContent(
    commentsState: UiState<List<PostComment>>,
    onPostComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("Member Comments", style = MaterialTheme.typography.headlineMedium, color = PearlIvory)
        Spacer(Modifier.height(12.dp))

        when (commentsState) {
            is UiState.Loading -> CircularProgressIndicator(color = ChampagneGold)
            is UiState.Error -> Text(commentsState.message, color = CrimsonAlert)
            is UiState.Success -> {
                if (commentsState.data.isEmpty()) {
                    Text(
                        text = "Be the first member to comment on Maarya's drop.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(commentsState.data, key = { it.id }) { comment ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = VelvetCard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = comment.authorName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ChampagneGoldBright
                                        )
                                        TierBadgePill(label = comment.authorTierBadge, rank = 1L)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = comment.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PearlIvory
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 500) draft = it },
                placeholder = { Text("Write a note to Maarya...") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("comment_input")
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty()) {
                        onPostComment(trimmed)
                        draft = ""
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(ChampagneGold)
                    .testTag("send_comment_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment", tint = VelvetObsidian)
            }
        }
    }
}
