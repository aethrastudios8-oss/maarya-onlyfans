package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.PlayBillingManager
import com.example.data.model.MaaryaDefaults
import com.example.data.model.MembershipRequest
import com.example.data.model.MembershipTier
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.data.model.formatCents
import com.example.ui.components.BillingGateDialog
import com.example.ui.components.LuxuryGradientButton
import com.example.ui.components.TierBadgePill
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.RoseSilk
import com.example.ui.theme.VelvetCard
import com.example.ui.theme.VelvetObsidian
import com.example.ui.viewmodel.MaaryaViewModel

@Composable
fun MembershipTiersScreen(
    viewModel: MaaryaViewModel,
    playBillingManager: PlayBillingManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val tiersState by viewModel.tiersState.collectAsStateWithLifecycle()
    val subState by viewModel.mySubscriptionState.collectAsStateWithLifecycle()
    val txState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val requestsState by viewModel.membershipRequestsState.collectAsStateWithLifecycle()
    val paymentSettingsState by viewModel.paymentSettingsState.collectAsStateWithLifecycle()
    val playState by viewModel.playBillingState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedTierForGate by remember { mutableStateOf<MembershipTier?>(null) }

    val mySub: Subscription? = (subState as? UiState.Success)?.data
    val activeTierRank = viewModel.currentFanRank(mySub)
    val paymentSettings = (paymentSettingsState as? UiState.Success)?.data ?: MaaryaDefaults.defaultPaymentSettings
    val myRequests = (requestsState as? UiState.Success)?.data ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VelvetObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = tiersState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChampagneGold)
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(state.message, color = CrimsonAlert)
                }
            }
            is UiState.Success -> {
                val activeTiers = state.data.filter { it.active }
                val transactions = (txState as? UiState.Success)?.data ?: emptyList()

                LazyColumn(
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "MAARYA ONLYFANS Club",
                                style = MaterialTheme.typography.displayMedium,
                                color = PearlIvory
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Four monthly membership tiers (\$50, \$100, \$200, \$500). Content access unlocks strictly after verified payment or admin approval.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = VelvetCard,
                                border = BorderStroke(1.dp, RoseSilk.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, tint = ChampagneGoldBright, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Payment Gateway Status (No Fake Processing)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ChampagneGoldBright
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = RoseSilk, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Payoneer (Intl -> PK) & JazzCash (PK) & Visa: Disabled until merchant approval & API credentials.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = EmeraldActive, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Google Play Billing: Required for Play Store Android subscriptions (${playState.productsById.size}/4 SKUs live).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (mySub != null) {
                        item {
                            ActiveMembershipBanner(
                                sub = mySub,
                                isBusy = isBusy,
                                onPauseOrResume = {
                                    val next = if (mySub.status == SubscriptionStatus.ACTIVE.name) {
                                        SubscriptionStatus.PAUSED
                                    } else {
                                        SubscriptionStatus.ACTIVE
                                    }
                                    viewModel.updateMySubscriptionStatus(next)
                                },
                                onCancel = {
                                    viewModel.updateMySubscriptionStatus(SubscriptionStatus.CANCELLED)
                                }
                            )
                        }
                    }

                    items(activeTiers, key = { it.id }) { tier ->
                        val isCurrent = mySub?.tierId == tier.id && mySub.status == SubscriptionStatus.ACTIVE.name
                        MembershipTierCard(
                            tier = tier,
                            isCurrent = isCurrent,
                            isUpgrade = tier.rank > activeTierRank,
                            isBusy = isBusy,
                            onSelectTier = { selectedTierForGate = tier }
                        )
                    }

                    if (myRequests.isNotEmpty() && !viewModel.isAdmin) {
                        item {
                            Text(
                                text = "Your Access Requests",
                                style = MaterialTheme.typography.titleLarge,
                                color = PearlIvory,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(myRequests.take(5), key = { it.id }) { req ->
                            RequestRow(req)
                        }
                    }

                    if (transactions.isNotEmpty() && !viewModel.isAdmin) {
                        item {
                            Text(
                                text = "Verified Payment History",
                                style = MaterialTheme.typography.titleLarge,
                                color = PearlIvory,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(transactions.take(8), key = { it.id }) { tx ->
                            ReceiptRow(tx)
                        }
                    }
                }
            }
        }
    }

    selectedTierForGate?.let { tier ->
        BillingGateDialog(
            tier = tier,
            paymentSettings = paymentSettings,
            playBillingState = playState,
            onDismiss = { selectedTierForGate = null },
            onLaunchPlayBilling = { targetTier ->
                if (activity != null && playBillingManager != null) {
                    val errorMsg = playBillingManager.launchSubscriptionFlow(activity, targetTier.playProductId)
                    if (errorMsg != null) {
                        viewModel.emitMessage(errorMsg)
                    }
                } else {
                    viewModel.emitMessage("Install via Google Play Store testing track to launch Play Billing.")
                }
                selectedTierForGate = null
            },
            onSubmitAccessRequest = { targetTier, provider, note ->
                viewModel.submitMembershipRequest(targetTier, provider, note)
                selectedTierForGate = null
            }
        )
    }
}

@Composable
private fun ActiveMembershipBanner(
    sub: Subscription,
    isBusy: Boolean,
    onPauseOrResume: () -> Unit,
    onCancel: () -> Unit
) {
    val status = SubscriptionStatus.safeFrom(sub.status)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = VelvetCard,
        border = BorderStroke(1.dp, EmeraldActive.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("YOUR CURRENT PLAN", style = MaterialTheme.typography.labelSmall, color = EmeraldActive)
                    Spacer(Modifier.height(4.dp))
                    Text(sub.tierName, style = MaterialTheme.typography.headlineMedium, color = PearlIvory)
                }
                TierBadgePill(label = status.label, rank = sub.tierRank)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${formatCents(sub.monthlyPriceCents)}/month • ${sub.renewsAtLabel} • ${sub.paymentLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (sub.monthlyPriceCents > 0L && status != SubscriptionStatus.CANCELLED) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (status == SubscriptionStatus.ACTIVE) {
                        OutlinedButton(
                            onClick = onPauseOrResume,
                            enabled = !isBusy,
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Pause Renewal", color = ChampagneGoldBright)
                        }
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isBusy,
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancel", color = CrimsonAlert)
                    }
                }
            }
        }
    }
}

@Composable
private fun MembershipTierCard(
    tier: MembershipTier,
    isCurrent: Boolean,
    isUpgrade: Boolean,
    isBusy: Boolean,
    onSelectTier: () -> Unit
) {
    val featured = tier.rank == 2L
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = VelvetCard,
        border = BorderStroke(
            width = if (featured || isCurrent) 1.5.dp else 1.dp,
            color = when {
                isCurrent -> EmeraldActive
                featured -> ChampagneGold
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier.fillMaxWidth().testTag("tier_card_${tier.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TierBadgePill(label = tier.badgeLabel, rank = tier.rank)
                    if (featured) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ChampagneGold.copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = ChampagneGoldBright, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("MOST POPULAR", style = MaterialTheme.typography.labelSmall, color = ChampagneGoldBright)
                            }
                        }
                    }
                }
                Text(
                    text = "${formatCents(tier.priceMonthlyCents)}/mo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ChampagneGoldBright
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(text = tier.name, style = MaterialTheme.typography.titleLarge, color = PearlIvory)
            Spacer(Modifier.height(4.dp))
            Text(text = tier.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tier.perks.forEach { perk ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (tier.rank >= 2L) ChampagneGold else RoseSilk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = perk, style = MaterialTheme.typography.bodySmall, color = PearlIvory)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (isCurrent) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmeraldActive.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldActive.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Active Membership", color = EmeraldActive, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else if (featured) {
                LuxuryGradientButton(
                    text = if (isUpgrade) "Upgrade • ${formatCents(tier.priceMonthlyCents)}/mo" else "Select Tier • ${formatCents(tier.priceMonthlyCents)}/mo",
                    onClick = onSelectTier,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("subscribe_tier_${tier.id}")
                )
            } else {
                Button(
                    onClick = onSelectTier,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChampagneGold.copy(alpha = 0.18f),
                        contentColor = ChampagneGoldBright
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("subscribe_tier_${tier.id}")
                ) {
                    Text(
                        text = if (isUpgrade) "Upgrade • ${formatCents(tier.priceMonthlyCents)}/mo" else "Select Tier • ${formatCents(tier.priceMonthlyCents)}/mo",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestRow(req: MembershipRequest) {
    val badgeColor = when (req.status) {
        "APPROVED" -> EmeraldActive
        "DECLINED" -> CrimsonAlert
        else -> ChampagneGoldBright
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = VelvetCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(req.requestedTierName, style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                Text(
                    text = "Preferred: ${req.preferredProvider} • ${req.note ?: "Awaiting Maarya approval"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(req.status, style = MaterialTheme.typography.labelMedium, color = badgeColor)
        }
    }
}

@Composable
fun ReceiptRow(tx: Transaction) {
    val type = TransactionType.safeFrom(tx.type)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = VelvetCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${type.label} • ${tx.tierName}", style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                Text(
                    text = "${tx.fanName} • ${tx.paymentLabel} • ${tx.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Fee: ${formatCents(tx.feeCents)} • Net: ${formatCents(tx.netEarningsCents)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoseSilk
                )
            }
            Text(
                text = formatCents(tx.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = ChampagneGoldBright
            )
        }
    }
}
