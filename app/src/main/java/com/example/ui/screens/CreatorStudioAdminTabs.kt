package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ContentReport
import com.example.data.model.CreatorAnalytics
import com.example.data.model.MembershipRequest
import com.example.data.model.MembershipTier
import com.example.data.model.PaymentProvider
import com.example.data.model.PayoutConfig
import com.example.data.model.ReportReason
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionStatus
import com.example.data.model.WithdrawalChannel
import com.example.data.model.WithdrawalRecord
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
fun StudioPayoutsSection(
    analytics: CreatorAnalytics,
    config: PayoutConfig,
    withdrawals: List<WithdrawalRecord>,
    isBusy: Boolean,
    onSaveConfig: (PayoutConfig) -> Unit,
    onCreateWithdrawal: (WithdrawalChannel, Long, Long, String, String) -> Unit,
    onUpdateWithdrawalStatus: (String, String) -> Unit
) {
    var payoneerEmail by remember(config.payoneerReceivingEmail) { mutableStateOf(config.payoneerReceivingEmail) }
    var payoneerType by remember(config.payoneerAccountType) { mutableStateOf(config.payoneerAccountType) }
    var payoneerApproved by remember(config.payoneerBusinessApproved) { mutableStateOf(config.payoneerBusinessApproved) }
    var jazzWallet by remember(config.jazzcashWalletNumber) { mutableStateOf(config.jazzcashWalletNumber) }
    var jazzTitle by remember(config.jazzcashAccountTitle) { mutableStateOf(config.jazzcashAccountTitle) }
    var jazzMerchantId by remember(config.jazzcashMerchantId) { mutableStateOf(config.jazzcashMerchantId) }
    var schedule by remember(config.settlementSchedule) { mutableStateOf(config.settlementSchedule) }
    var notes by remember(config.notes) { mutableStateOf(config.notes) }

    var wdChannel by remember { mutableStateOf(WithdrawalChannel.PAYONEER_TO_JAZZCASH) }
    var wdDollars by remember { mutableStateOf("") }
    var wdFeeDollars by remember { mutableStateOf("0") }
    var wdRef by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Pakistan Settlement Guidance Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = VelvetCard,
            border = BorderStroke(1.dp, RoseSilk.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pakistan Receiving & Settlement Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChampagneGoldBright
                )
                Text(
                    text = "• International USD payouts land in Payoneer, then settle into your Pakistan JazzCash wallet.\n" +
                        "• Important: Personal Payoneer accounts in Pakistan cannot run automated website checkout or recurring subscriptions. Keep Payoneer Checkout disabled until commercial business merchant eligibility is approved.\n" +
                        "• JazzCash Merchant Gateway requires verified Merchant ID, Password & Integrity Salt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PearlIvory.copy(alpha = 0.85f)
                )
            }
        }

        // Payout Configuration Form
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = VelvetCard,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Payoneer -> JazzCash Settings", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
                OutlinedTextField(
                    value = payoneerEmail,
                    onValueChange = { if (it.length <= 160) payoneerEmail = it },
                    label = { Text("Payoneer Receiving Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = payoneerType,
                    onValueChange = { if (it.length <= 80) payoneerType = it },
                    label = { Text("Payoneer Account Classification") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payoneer Business Checkout Approved", style = MaterialTheme.typography.bodySmall, color = PearlIvory)
                    Switch(checked = payoneerApproved, onCheckedChange = { payoneerApproved = it })
                }
                OutlinedTextField(
                    value = jazzWallet,
                    onValueChange = { if (it.length <= 40) jazzWallet = it },
                    label = { Text("Pakistan JazzCash Mobile Wallet (e.g. 0300...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = jazzTitle,
                    onValueChange = { if (it.length <= 80) jazzTitle = it },
                    label = { Text("JazzCash Account Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = jazzMerchantId,
                    onValueChange = { if (it.length <= 60) jazzMerchantId = it },
                    label = { Text("JazzCash Merchant ID (Optional until approved)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { if (it.length <= 60) schedule = it },
                    label = { Text("Settlement Schedule") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 400) notes = it },
                    label = { Text("Settlement Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onSaveConfig(
                            config.copy(
                                payoneerReceivingEmail = payoneerEmail,
                                payoneerAccountType = payoneerType,
                                payoneerBusinessApproved = payoneerApproved,
                                jazzcashWalletNumber = jazzWallet,
                                jazzcashAccountTitle = jazzTitle,
                                jazzcashMerchantId = jazzMerchantId,
                                settlementSchedule = schedule,
                                notes = notes
                            )
                        )
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = VelvetObsidian),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save Settlement Config", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Withdrawal Ledger Creator
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = VelvetCard,
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Record Settlement Transfer", style = MaterialTheme.typography.titleLarge, color = ChampagneGoldBright)
                Text(
                    text = "Available Verified Balance: ${formatCents(analytics.availableBalanceCents)} (Withdrawn: ${formatCents(analytics.totalWithdrawnCents)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldActive
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WithdrawalChannel.entries.forEach { ch ->
                        val active = wdChannel == ch
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ChampagneGold else VelvetCardElevated,
                            modifier = Modifier.weight(1f).height(44.dp).clickable { wdChannel = ch }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                Text(
                                    text = if (ch == WithdrawalChannel.PAYONEER_TO_JAZZCASH) "Payoneer -> JazzCash" else "JazzCash Direct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) VelvetObsidian else PearlIvory
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wdDollars,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) wdDollars = it },
                        label = { Text("Amount ($ USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wdFeeDollars,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 5) wdFeeDollars = it },
                        label = { Text("FX/Fee ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = wdRef,
                    onValueChange = { if (it.length <= 80) wdRef = it },
                    label = { Text("Payoneer / JazzCash Transfer Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val parsedCents = (wdDollars.toLongOrNull() ?: 0L) * 100L
                val feeCents = (wdFeeDollars.toLongOrNull() ?: 0L) * 100L
                Button(
                    onClick = {
                        val dest = if (jazzWallet.isNotBlank()) "JazzCash $jazzWallet ($jazzTitle)" else "Payoneer -> JazzCash PK"
                        onCreateWithdrawal(wdChannel, parsedCents, feeCents.coerceAtMost(parsedCents), dest, wdRef)
                        wdDollars = ""
                        wdRef = ""
                    },
                    enabled = !isBusy && parsedCents >= 100L,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseSilk, contentColor = VelvetObsidian),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Log Withdrawal Batch", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Withdrawal History", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
        if (withdrawals.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VelvetCard,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No withdrawal batches logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            withdrawals.forEach { wd ->
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
                            Text(WithdrawalChannel.safeFrom(wd.channel).label, style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                            Text("${wd.destinationLabel} • ${wd.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!wd.referenceCode.isNullOrBlank()) {
                                Text("Ref: ${wd.referenceCode}", style = MaterialTheme.typography.labelSmall, color = RoseSilk)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatCents(wd.netPayoutCents), style = MaterialTheme.typography.titleMedium, color = ChampagneGoldBright)
                            if (wd.status != "SETTLED") {
                                TextButton(onClick = { onUpdateWithdrawalStatus(wd.id, "SETTLED") }) {
                                    Text("Mark Settled", color = EmeraldActive)
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
fun StudioRequestsAndFansSection(
    requests: List<MembershipRequest>,
    subscribers: List<Subscription>,
    tiers: List<MembershipTier>,
    isBusy: Boolean,
    onApproveRequest: (MembershipRequest, MembershipTier, PaymentProvider, Boolean, String?) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onUpdateStatus: (String, SubscriptionStatus) -> Unit,
    onGrantTier: (Subscription, MembershipTier) -> Unit
) {
    var approvingReq by remember { mutableStateOf<MembershipRequest?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pending Access Requests (${requests.count { it.status == "PENDING" }})", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
        if (requests.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VelvetCard,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No membership requests queued yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            requests.forEach { req ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VelvetCard,
                    border = BorderStroke(1.dp, if (req.status == "PENDING") ChampagneGold.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(req.fanName, style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                            Text(
                                text = req.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (req.status) {
                                    "APPROVED" -> EmeraldActive
                                    "DECLINED" -> CrimsonAlert
                                    else -> ChampagneGoldBright
                                }
                            )
                        }
                        Text("${req.fanEmail} • ${req.requestedTierName} via ${req.preferredProvider}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!req.note.isNullOrBlank()) {
                            Text("\"${req.note}\"", style = MaterialTheme.typography.bodySmall, color = RoseSilk)
                        }
                        if (req.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { approvingReq = req },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = VelvetObsidian),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Approve & Activate", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onDeclineRequest(req.id) },
                                    enabled = !isBusy,
                                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Decline", color = CrimsonAlert)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("All Subscribers (${subscribers.size})", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
        StudioSubscribersSection(
            subscribers = subscribers,
            tiers = tiers,
            isBusy = isBusy,
            onUpdateStatus = onUpdateStatus,
            onGrantTier = onGrantTier
        )
    }

    approvingReq?.let { req ->
        val defaultTier = tiers.firstOrNull { it.id == req.requestedTierId } ?: tiers.firstOrNull()
        var chosenTier by remember { mutableStateOf(defaultTier) }
        var provider by remember { mutableStateOf(PaymentProvider.safeFrom(req.preferredProvider)) }
        var recordTx by remember { mutableStateOf(false) }
        var extRef by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { approvingReq = null },
            containerColor = VelvetCardElevated,
            title = { Text("Activate ${req.fanName}", style = MaterialTheme.typography.headlineMedium, color = PearlIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tier to Activate", style = MaterialTheme.typography.labelMedium, color = ChampagneGoldBright)
                    tiers.sortedBy { it.rank }.forEach { t ->
                        val active = chosenTier?.id == t.id
                        OutlinedButton(
                            onClick = { chosenTier = t },
                            border = BorderStroke(1.dp, if (active) ChampagneGold else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${t.name} (${formatCents(t.priceMonthlyCents)}/mo)", color = if (active) ChampagneGoldBright else PearlIvory)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Record Verified Revenue Ledger Entry", style = MaterialTheme.typography.bodySmall, color = PearlIvory)
                        Switch(checked = recordTx, onCheckedChange = { recordTx = it })
                    }
                    if (recordTx) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(PaymentProvider.PAYONEER, PaymentProvider.JAZZCASH, PaymentProvider.VISA_CARD).forEach { p ->
                                val active = provider == p
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) ChampagneGold else VelvetCard,
                                    modifier = Modifier.weight(1f).height(38.dp).clickable { provider = p }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(p.wireValue, style = MaterialTheme.typography.labelSmall, color = if (active) VelvetObsidian else PearlIvory)
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = extRef,
                            onValueChange = { if (it.length <= 120) extRef = it },
                            label = { Text("Receipt / Webhook Ref ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        chosenTier?.let { t ->
                            val finalProvider = if (recordTx) provider else PaymentProvider.ADMIN_GRANT
                            onApproveRequest(req, t, finalProvider, recordTx, extRef)
                        }
                        approvingReq = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive, contentColor = VelvetObsidian)
                ) {
                    Text("Activate Tier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { approvingReq = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun StudioModerationSection(
    reports: List<ContentReport>,
    isBusy: Boolean,
    onResolveReport: (String, String, String?) -> Unit,
    onDeletePost: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Content Moderation Queue", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
        if (reports.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VelvetCard,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No moderation reports submitted. Fan reports appear here for creator compliance review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            reports.forEach { report ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VelvetCard,
                    border = BorderStroke(1.dp, if (report.status == "OPEN") CrimsonAlert.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(report.postTitle, style = MaterialTheme.typography.titleSmall, color = PearlIvory)
                            TierBadgePill(label = report.status, rank = if (report.status == "OPEN") 1L else 4L)
                        }
                        Text(
                            text = "Reason: ${ReportReason.safeFrom(report.reason).label} • Reported by ${report.reporterName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseSilk
                        )
                        if (!report.details.isNullOrBlank()) {
                            Text(report.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (report.status == "OPEN") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onResolveReport(report.id, "RESOLVED", report.postId) },
                                    enabled = !isBusy,
                                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Unpublish Post", color = ChampagneGoldBright)
                                }
                                OutlinedButton(
                                    onClick = { onResolveReport(report.id, "DISMISSED", null) },
                                    enabled = !isBusy,
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Dismiss", color = PearlIvory)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onDeletePost(report.postId)
                                        onResolveReport(report.id, "RESOLVED", null)
                                    },
                                    enabled = !isBusy,
                                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Delete", color = CrimsonAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
