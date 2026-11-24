package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CreatorPost
import com.example.data.model.MediaType
import com.example.data.model.MembershipTier
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionStatus
import com.example.data.model.formatCents
import com.example.ui.components.TierBadgePill
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.RoseSilk
import com.example.ui.theme.VelvetCard
import com.example.ui.theme.VelvetCardElevated
import com.example.ui.theme.VelvetObsidian

@Composable
fun StudioContentSection(
    posts: List<CreatorPost>,
    tiers: List<MembershipTier>,
    isBusy: Boolean,
    onSavePost: (
        existingId: String?,
        title: String,
        caption: String,
        mediaType: String,
        mediaKey: String,
        mediaUri: String?,
        requiredTierRank: Long,
        requiredTierName: String,
        ppvPriceCents: Long,
        published: Boolean
    ) -> Unit,
    onDeletePost: (String) -> Unit
) {
    var editingPost by remember { mutableStateOf<CreatorPost?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { showCreateDialog = true },
            enabled = !isBusy,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = VelvetObsidian),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("studio_new_post_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Publish New Exclusive Drop", fontWeight = FontWeight.Bold)
        }

        posts.forEach { post ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VelvetCard,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TierBadgePill(label = post.requiredTierName, rank = post.requiredTierRank)
                            Text(
                                text = if (post.published) "LIVE" else "DRAFT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (post.published) EmeraldActive else CrimsonAlert
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(post.title, style = MaterialTheme.typography.titleMedium, color = PearlIvory)
                        Text(
                            text = "${MediaType.safeFrom(post.mediaType).label} • ${post.likesCount} likes • ${post.commentsCount} comments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { editingPost = post },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit post", tint = ChampagneGoldBright)
                    }
                    IconButton(
                        onClick = { onDeletePost(post.id) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete post", tint = CrimsonAlert)
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingPost != null) {
        PostEditorDialog(
            initial = editingPost,
            tiers = tiers,
            onDismiss = {
                showCreateDialog = false
                editingPost = null
            },
            onSave = { title, caption, mediaType, mediaKey, mediaUri, rank, tierName, ppvCents, published ->
                onSavePost(
                    editingPost?.id,
                    title,
                    caption,
                    mediaType,
                    mediaKey,
                    mediaUri,
                    rank,
                    tierName,
                    ppvCents,
                    published
                )
                showCreateDialog = false
                editingPost = null
            }
        )
    }
}

@Composable
private fun PostEditorDialog(
    initial: CreatorPost?,
    tiers: List<MembershipTier>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String?, Long, String, Long, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var caption by remember { mutableStateOf(initial?.caption ?: "") }
    var mediaType by remember { mutableStateOf(initial?.mediaType ?: MediaType.PHOTO.name) }
    var mediaKey by remember { mutableStateOf(initial?.mediaKey ?: "maarya_hero") }
    var mediaUri by remember { mutableStateOf(initial?.mediaUri) }
    var selectedRank by remember { mutableLongStateOf(initial?.requiredTierRank ?: 1L) }
    var ppvDollars by remember {
        mutableStateOf(if ((initial?.ppvPriceCents ?: 0L) > 0L) "${initial!!.ppvPriceCents / 100}" else "")
    }
    var published by remember { mutableStateOf(initial?.published ?: true) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                mediaUri = uri.toString()
                mediaKey = "custom_uri"
            }
        }
    )

    val selectedTierName = tiers.firstOrNull { it.rank == selectedRank }?.badgeLabel ?: "TIER 1 • \$50"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetCardElevated,
        title = {
            Text(
                text = if (initial == null) "Publish Exclusive Drop" else "Edit Exclusive Drop",
                style = MaterialTheme.typography.headlineMedium,
                color = PearlIvory
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Drop Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("post_title_input")
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 2000) caption = it },
                    label = { Text("Editorial Caption / Note") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("post_caption_input")
                )

                Text("Media Format", style = MaterialTheme.typography.labelMedium, color = ChampagneGoldBright)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaType.entries.forEach { mt ->
                        val active = mediaType == mt.name
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ChampagneGold else VelvetCard,
                            modifier = Modifier.weight(1f).height(40.dp).clickable { mediaType = mt.name }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = mt.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) VelvetObsidian else PearlIvory
                                )
                            }
                        }
                    }
                }

                Text("Visual Media", style = MaterialTheme.typography.labelMedium, color = ChampagneGoldBright)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("maarya_hero" to "Peach Silk", "maarya_portrait" to "Sunlit Floral").forEach { (key, label) ->
                        val active = mediaKey == key && mediaUri.isNullOrBlank()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) RoseSilk else VelvetCard,
                            modifier = Modifier.weight(1f).height(40.dp).clickable {
                                mediaKey = key
                                mediaUri = null
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) VelvetObsidian else PearlIvory
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = ChampagneGoldBright, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (mediaUri.isNullOrBlank()) "Pick Photo from Device" else "Device Photo Selected ✓",
                        color = ChampagneGoldBright
                    )
                }

                Text("Minimum Tier Required", style = MaterialTheme.typography.labelMedium, color = ChampagneGoldBright)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tiers.sortedBy { it.rank }.forEach { tier ->
                        val active = selectedRank == tier.rank
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ChampagneGold else VelvetCard,
                            modifier = Modifier.weight(1f).height(40.dp).clickable { selectedRank = tier.rank }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "T${tier.rank} ${formatCents(tier.priceMonthlyCents)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) VelvetObsidian else PearlIvory
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = ppvDollars,
                    onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 4) ppvDollars = it },
                    label = { Text("Optional Single-Post Unlock ($ USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Published Live to Fans", style = MaterialTheme.typography.bodyMedium, color = PearlIvory)
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ppvCents = (ppvDollars.toLongOrNull() ?: 0L) * 100L
                    onSave(
                        title,
                        caption,
                        mediaType,
                        mediaKey,
                        mediaUri,
                        selectedRank,
                        selectedTierName,
                        ppvCents,
                        published
                    )
                },
                enabled = title.isNotBlank() && caption.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = VelvetObsidian),
                modifier = Modifier.testTag("save_post_button")
            ) {
                Text("Save Drop", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun StudioTiersSection(
    tiers: List<MembershipTier>,
    isBusy: Boolean,
    onSaveTier: (MembershipTier, String, Long, String, List<String>, Boolean) -> Unit
) {
    var editingTier by remember { mutableStateOf<MembershipTier?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiers.sortedBy { it.rank }.forEach { tier ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VelvetCard,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TierBadgePill(label = tier.badgeLabel, rank = tier.rank)
                            Text(
                                text = if (tier.active) "ACTIVE" else "HIDDEN",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tier.active) EmeraldActive else CrimsonAlert
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${tier.name} — ${formatCents(tier.priceMonthlyCents)}/mo", style = MaterialTheme.typography.titleMedium, color = PearlIvory)
                        Text(tier.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = { editingTier = tier },
                        enabled = !isBusy,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_tier_${tier.id}")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit tier pricing", tint = ChampagneGoldBright)
                    }
                }
            }
        }
    }

    editingTier?.let { tier ->
        var name by remember { mutableStateOf(tier.name) }
        var priceDollars by remember { mutableStateOf("${tier.priceMonthlyCents / 100}") }
        var description by remember { mutableStateOf(tier.description) }
        var perksText by remember { mutableStateOf(tier.perks.joinToString("\n")) }
        var active by remember { mutableStateOf(tier.active) }

        AlertDialog(
            onDismissRequest = { editingTier = null },
            containerColor = VelvetCardElevated,
            title = { Text("Edit ${tier.badgeLabel} Tier", style = MaterialTheme.typography.headlineMedium, color = PearlIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 40) name = it },
                        label = { Text("Tier Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = priceDollars,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) priceDollars = it },
                        label = { Text("Monthly Price ($ USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 300) description = it },
                        label = { Text("Description") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = perksText,
                        onValueChange = { perksText = it },
                        label = { Text("Perks (one per line)") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Visible to Fans", color = PearlIvory)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                val parsedDollars = priceDollars.toLongOrNull() ?: 0L
                val isValidTier = name.trim().length >= 2 && description.isNotBlank() && parsedDollars in 1L..2000L
                Button(
                    onClick = {
                        val cents = parsedDollars * 100L
                        val perksList = perksText.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(8)
                        onSaveTier(tier, name, cents, description, perksList, active)
                        editingTier = null
                    },
                    enabled = isValidTier,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = VelvetObsidian)
                ) {
                    Text("Save Tier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTier = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun StudioSubscribersSection(
    subscribers: List<Subscription>,
    tiers: List<MembershipTier>,
    isBusy: Boolean,
    onUpdateStatus: (String, SubscriptionStatus) -> Unit,
    onGrantTier: (Subscription, MembershipTier) -> Unit
) {
    var manageTarget by remember { mutableStateOf<Subscription?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (subscribers.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VelvetCard,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No subscribers yet. As fans join Maarya's club tiers, you can manage their status and tier access here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            subscribers.forEach { sub ->
                val status = SubscriptionStatus.safeFrom(sub.status)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VelvetCard,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().clickable { manageTarget = sub }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.fanName, style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                            Text("${sub.fanEmail} • ${sub.paymentLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                TierBadgePill(label = sub.tierName, rank = sub.tierRank)
                                Text(
                                    text = status.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (status == SubscriptionStatus.ACTIVE) EmeraldActive else CrimsonAlert
                                )
                            }
                        }
                        Text(
                            text = "${formatCents(sub.monthlyPriceCents)}/mo",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGoldBright
                        )
                    }
                }
            }
        }
    }

    manageTarget?.let { sub ->
        AlertDialog(
            onDismissRequest = { manageTarget = null },
            containerColor = VelvetCardElevated,
            title = { Text("Manage ${sub.fanName}", style = MaterialTheme.typography.headlineMedium, color = PearlIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Assign Tier", style = MaterialTheme.typography.labelMedium, color = ChampagneGoldBright)
                    tiers.sortedBy { it.rank }.forEach { tier ->
                        OutlinedButton(
                            onClick = {
                                onGrantTier(sub, tier)
                                manageTarget = null
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("${tier.name} (${formatCents(tier.priceMonthlyCents)}/mo)", color = PearlIvory)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Subscription Status", style = MaterialTheme.typography.labelMedium, color = ChampagneGoldBright)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubscriptionStatus.entries.forEach { st ->
                            OutlinedButton(
                                onClick = {
                                    onUpdateStatus(sub.userId, st)
                                    manageTarget = null
                                },
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(st.label, style = MaterialTheme.typography.labelSmall, color = PearlIvory)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { manageTarget = null }) {
                    Text("Close", color = ChampagneGoldBright)
                }
            }
        )
    }
}
