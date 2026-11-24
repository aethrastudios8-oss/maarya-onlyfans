package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.ui.components.TierBadgePill
import com.example.ui.components.VerifiedMaaryaBadge
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.RoseSilk
import com.example.ui.theme.VelvetCard
import com.example.ui.theme.VelvetCardElevated
import com.example.ui.theme.VelvetObsidian
import com.example.ui.viewmodel.MaaryaViewModel

@Composable
fun MessagesScreen(
    viewModel: MaaryaViewModel,
    onNavigateToClub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview by viewModel.isFanPreviewMode.collectAsStateWithLifecycle()
    val isEffectiveAdmin = viewModel.isAdmin && !isPreview
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeChatFanUid by viewModel.activeChatFanUid.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val mySubState by viewModel.mySubscriptionState.collectAsStateWithLifecycle()

    val mySub = (mySubState as? UiState.Success)?.data
    val fanTierName = viewModel.currentFanTierName(mySub)
    val fanRank = viewModel.currentFanRank(mySub)

    if (isEffectiveAdmin && activeChatFanUid != null) {
        BackHandler { viewModel.selectConversation(null) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VelvetObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            if (isEffectiveAdmin && activeChatFanUid == null) {
                CreatorInboxList(
                    conversationsState = conversationsState,
                    onSelectConversation = { conv -> viewModel.selectConversation(conv.fanUid) }
                )
            } else {
                val targetFanUid = activeChatFanUid ?: viewModel.currentUserId
                val activeConv = (conversationsState as? UiState.Success)?.data
                    ?.firstOrNull { it.fanUid == targetFanUid }

                ChatThreadPane(
                    isCreatorView = isEffectiveAdmin,
                    partnerName = if (isEffectiveAdmin) activeConv?.fanName ?: "Fan" else "Maarya",
                    partnerBadge = if (isEffectiveAdmin) activeConv?.fanTierName ?: "Member" else fanTierName,
                    partnerRank = if (isEffectiveAdmin) 2L else fanRank,
                    showBackButton = isEffectiveAdmin,
                    onBack = { viewModel.selectConversation(null) },
                    onUpgradePrompt = onNavigateToClub,
                    messagesState = messagesState,
                    onSendMessage = { text ->
                        viewModel.sendMessage(
                            fanUid = targetFanUid,
                            fanName = activeConv?.fanName ?: viewModel.currentUserName,
                            fanTierName = activeConv?.fanTierName ?: fanTierName,
                            body = text
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CreatorInboxList(
    conversationsState: UiState<List<Conversation>>,
    onSelectConversation: (Conversation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Maarya's VIP Inbox", style = MaterialTheme.typography.displayMedium, color = PearlIvory)
        Text(
            text = "Reply directly to fans and VIP subscribers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        when (conversationsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ChampagneGold)
            }
            is UiState.Error -> Text(conversationsState.message, color = CrimsonAlert)
            is UiState.Success -> {
                if (conversationsState.data.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VelvetCard,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChatBubbleOutline,
                                contentDescription = null,
                                tint = ChampagneGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("No fan messages yet", style = MaterialTheme.typography.titleMedium, color = PearlIvory)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "When subscribers send you a DM, their threads will appear here with their membership badge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(conversationsState.data, key = { it.fanUid }) { conv ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = VelvetCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (conv.unreadByCreator > 0L) ChampagneGold else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectConversation(conv) }
                                    .testTag("conversation_${conv.fanUid}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(ChampagneGold.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = conv.fanName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = ChampagneGoldBright
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(conv.fanName, style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                                            TierBadgePill(label = conv.fanTierName, rank = 1L)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = conv.lastMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (conv.unreadByCreator > 0L) {
                                        Surface(shape = CircleShape, color = ChampagneGold) {
                                            Text(
                                                text = "${conv.unreadByCreator}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VelvetObsidian,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadPane(
    isCreatorView: Boolean,
    partnerName: String,
    partnerBadge: String,
    partnerRank: Long,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onUpgradePrompt: () -> Unit,
    messagesState: UiState<List<ChatMessage>>,
    onSendMessage: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header bar
        Surface(
            color = VelvetCard,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Inbox",
                            tint = PearlIvory
                        )
                    }
                }
                if (!isCreatorView) {
                    Image(
                        painter = painterResource(R.drawable.maarya_portrait),
                        contentDescription = "Maarya avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, ChampagneGold, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(partnerName, style = MaterialTheme.typography.titleLarge, color = PearlIvory)
                        if (!isCreatorView) {
                            Spacer(Modifier.width(6.dp))
                            VerifiedMaaryaBadge(modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = if (isCreatorView) "Fan Tier: $partnerBadge" else "Direct VIP Line • Your tier: $partnerBadge",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseSilk
                    )
                }
                if (!isCreatorView && partnerRank < 2L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ChampagneGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable(onClick = onUpgradePrompt)
                    ) {
                        Text(
                            text = "Priority Upgrade",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGoldBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Messages stream
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (messagesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChampagneGold)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(messagesState.message, color = CrimsonAlert)
                }
                is UiState.Success -> {
                    if (messagesState.data.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isCreatorView) "Start chatting with $partnerName" else "Message Maarya Directly",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PearlIvory
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isCreatorView) {
                                    "Send a personal note or welcome message."
                                } else {
                                    "Tier 2+ Velvet Noir, Royal Violet & Diamond VIP members receive priority replies straight from Maarya."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messagesState.data, key = { it.id }) { msg ->
                                val isFromMe = if (isCreatorView) msg.senderRole == "MAARYA" else msg.senderRole == "FAN"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = if (isFromMe) 18.dp else 4.dp,
                                            bottomEnd = if (isFromMe) 4.dp else 18.dp
                                        ),
                                        color = if (isFromMe) ChampagneGold else VelvetCardElevated,
                                        modifier = Modifier.widthIn(max = 290.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                            Text(
                                                text = msg.senderName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isFromMe) VelvetObsidian.copy(alpha = 0.72f) else RoseSilk
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = msg.body,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isFromMe) VelvetObsidian else PearlIvory
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Composer
        Surface(
            color = VelvetCard,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 1000) draft = it },
                    placeholder = {
                        Text(if (isCreatorView) "Reply as Maarya..." else "Send a message to Maarya...")
                    },
                    maxLines = 3,
                    modifier = Modifier.weight(1f).testTag("dm_input")
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendMessage(trimmed)
                            draft = ""
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(ChampagneGold)
                        .testTag("send_dm_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = VelvetObsidian
                    )
                }
            }
        }
    }
}
