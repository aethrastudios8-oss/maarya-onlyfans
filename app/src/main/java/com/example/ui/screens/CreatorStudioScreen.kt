package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CreatorProfile
import com.example.data.model.MaaryaDefaults
import com.example.data.model.UiState
import com.example.data.model.formatCents
import com.example.ui.components.RevenueBreakdownChart
import com.example.ui.components.VerifiedMaaryaBadge
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DiamondIce
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.RoseSilk
import com.example.ui.theme.VelvetCard
import com.example.ui.theme.VelvetObsidian
import com.example.ui.viewmodel.MaaryaViewModel

private enum class StudioTab(val label: String) {
    EARNINGS("Earnings"),
    PAYOUTS("Payouts (PK)"),
    CONTENT("Content"),
    TIERS("Tiers"),
    FANS("Requests & Fans"),
    MODERATION("Moderation"),
    PROFILE("Profile")
}

@Composable
fun CreatorStudioScreen(
    viewModel: MaaryaViewModel,
    modifier: Modifier = Modifier
) {
    val profileState by viewModel.creatorProfileState.collectAsStateWithLifecycle()
    val tiersState by viewModel.tiersState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val subsState by viewModel.allSubscriptionsState.collectAsStateWithLifecycle()
    val requestsState by viewModel.membershipRequestsState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val payoutState by viewModel.payoutConfigState.collectAsStateWithLifecycle()
    val withdrawalsState by viewModel.withdrawalsState.collectAsStateWithLifecycle()
    val txState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val analytics by viewModel.analyticsState.collectAsStateWithLifecycle()
    val isPreview by viewModel.isFanPreviewMode.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(StudioTab.EARNINGS) }

    val profile = (profileState as? UiState.Success)?.data ?: CreatorProfile()
    val tiers = (tiersState as? UiState.Success)?.data ?: emptyList()
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()
    val subscribers = (subsState as? UiState.Success)?.data ?: emptyList()
    val requests = (requestsState as? UiState.Success)?.data ?: emptyList()
    val reports = (reportsState as? UiState.Success)?.data ?: emptyList()
    val payoutConfig = (payoutState as? UiState.Success)?.data ?: MaaryaDefaults.defaultPayoutConfig
    val withdrawals = (withdrawalsState as? UiState.Success)?.data ?: emptyList()
    val transactions = (txState as? UiState.Success)?.data ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VelvetObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Maarya Creator Studio", style = MaterialTheme.typography.displayMedium, color = PearlIvory)
                        VerifiedMaaryaBadge()
                    }
                    Text(
                        text = "Super Admin Console • Exclusively for @maarya",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoseSilk
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val openReports = reports.count { it.status == "OPEN" }
                        val pendingReqs = requests.count { it.status == "PENDING" }
                        val labelText = when (tab) {
                            StudioTab.MODERATION -> if (openReports > 0) "${tab.label} ($openReports)" else tab.label
                            StudioTab.FANS -> if (pendingReqs > 0) "${tab.label} ($pendingReqs)" else tab.label
                            else -> tab.label
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            label = { Text(labelText) },
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
                            modifier = Modifier.testTag("studio_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            item {
                when (selectedTab) {
                    StudioTab.EARNINGS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudioMetricCard(
                                    title = "Available Balance",
                                    value = formatCents(analytics.availableBalanceCents),
                                    accent = EmeraldActive,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioMetricCard(
                                    title = "Monthly Recurring",
                                    value = formatCents(analytics.monthlyRecurringRevenueCents),
                                    accent = ChampagneGoldBright,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudioMetricCard(
                                    title = "Gross Revenue",
                                    value = formatCents(analytics.totalGrossRevenueCents),
                                    accent = PearlIvory,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioMetricCard(
                                    title = "Provider Fees",
                                    value = formatCents(analytics.totalFeesCents),
                                    accent = CrimsonAlert,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudioMetricCard(
                                    title = "Net Earnings",
                                    value = formatCents(analytics.totalNetEarningsCents),
                                    accent = RoseSilk,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioMetricCard(
                                    title = "Paid Subscribers",
                                    value = "${analytics.activePaidSubscribers}",
                                    accent = DiamondIce,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudioMetricCard(
                                    title = "Payoneer (Intl)",
                                    value = formatCents(analytics.payoneerRevenueCents),
                                    accent = ChampagneGoldBright,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioMetricCard(
                                    title = "JazzCash (PK)",
                                    value = formatCents(analytics.jazzcashRevenueCents),
                                    accent = EmeraldActive,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StudioMetricCard(
                                    title = "Visa Cards",
                                    value = formatCents(analytics.visaRevenueCents),
                                    accent = RoseSilk,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioMetricCard(
                                    title = "Play Billing",
                                    value = formatCents(analytics.playBillingRevenueCents),
                                    accent = DiamondIce,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            RevenueBreakdownChart(series = analytics.revenueSeriesCents)

                            Text(
                                text = "Verified Transaction Ledger",
                                style = MaterialTheme.typography.titleLarge,
                                color = PearlIvory,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            if (transactions.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = VelvetCard,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No verified transactions recorded yet. Webhooks (Payoneer / JazzCash / Visa / Google Play) or verified Admin activations appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    transactions.take(12).forEach { tx ->
                                        ReceiptRow(tx)
                                    }
                                }
                            }
                        }
                    }

                    StudioTab.PAYOUTS -> {
                        StudioPayoutsSection(
                            analytics = analytics,
                            config = payoutConfig,
                            withdrawals = withdrawals,
                            isBusy = isBusy,
                            onSaveConfig = viewModel::savePayoutConfig,
                            onCreateWithdrawal = viewModel::createWithdrawal,
                            onUpdateWithdrawalStatus = viewModel::markWithdrawalSettled
                        )
                    }

                    StudioTab.CONTENT -> {
                        StudioContentSection(
                            posts = posts,
                            tiers = tiers,
                            isBusy = isBusy,
                            onSavePost = viewModel::publishOrUpdatePost,
                            onDeletePost = viewModel::deletePost
                        )
                    }

                    StudioTab.TIERS -> {
                        StudioTiersSection(
                            tiers = tiers,
                            isBusy = isBusy,
                            onSaveTier = viewModel::saveMembershipTier
                        )
                    }

                    StudioTab.FANS -> {
                        StudioRequestsAndFansSection(
                            requests = requests,
                            subscribers = subscribers,
                            tiers = tiers,
                            isBusy = isBusy,
                            onApproveRequest = viewModel::approveMembershipRequest,
                            onDeclineRequest = viewModel::declineMembershipRequest,
                            onUpdateStatus = viewModel::updateSubscriberStatusAsAdmin,
                            onGrantTier = viewModel::grantSubscriberTierAsAdmin
                        )
                    }

                    StudioTab.MODERATION -> {
                        StudioModerationSection(
                            reports = reports,
                            isBusy = isBusy,
                            onResolveReport = viewModel::resolveReport,
                            onDeletePost = viewModel::deletePost
                        )
                    }

                    StudioTab.PROFILE -> {
                        StudioProfileEditor(
                            profile = profile,
                            isFanPreview = isPreview,
                            isBusy = isBusy,
                            onToggleFanPreview = viewModel::setFanPreviewMode,
                            onSaveProfile = { tagline, bio, location ->
                                viewModel.saveCreatorProfile(tagline, bio, location, profile)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioMetricCard(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = VelvetCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = accent)
        }
    }
}

@Composable
private fun StudioProfileEditor(
    profile: CreatorProfile,
    isFanPreview: Boolean,
    isBusy: Boolean,
    onToggleFanPreview: (Boolean) -> Unit,
    onSaveProfile: (String, String, String) -> Unit
) {
    var tagline by remember(profile.tagline) { mutableStateOf(profile.tagline) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
    var location by remember(profile.location) { mutableStateOf(profile.location) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = VelvetCard,
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Preview App as Fan", style = MaterialTheme.typography.titleMedium, color = ChampagneGoldBright)
                    Text(
                        text = "Temporarily view locked posts, club tiers, and fan inbox exactly as subscribers see them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isFanPreview, onCheckedChange = onToggleFanPreview)
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = VelvetCard,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Maarya Public Profile", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { if (it.length <= 160) tagline = it },
                    label = { Text("Headline Tagline") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 1000) bio = it },
                    label = { Text("Creator Bio") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 80) location = it },
                    label = { Text("Studio Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onSaveProfile(tagline, bio, location) },
                    enabled = !isBusy && tagline.isNotBlank() && bio.isNotBlank() && location.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = VelvetObsidian),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Maarya Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
