package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billing.PlayBillingState
import com.example.data.model.CreatorPost
import com.example.data.model.MembershipTier
import com.example.data.model.PaymentProvider
import com.example.data.model.PaymentSettings
import com.example.data.model.ReportReason
import com.example.data.model.formatCents
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
fun BillingGateDialog(
    tier: MembershipTier,
    paymentSettings: PaymentSettings,
    playBillingState: PlayBillingState,
    onDismiss: () -> Unit,
    onLaunchPlayBilling: (MembershipTier) -> Unit,
    onSubmitAccessRequest: (MembershipTier, PaymentProvider, String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(PaymentProvider.PAYONEER) }
    var note by remember { mutableStateOf("") }
    val hasPlaySku = playBillingState.productsById.containsKey(tier.playProductId)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetCardElevated,
        title = {
            Column {
                Text(
                    text = tier.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PearlIvory
                )
                Text(
                    text = "${formatCents(tier.priceMonthlyCents)} / month • Official Single-Creator Club",
                    style = MaterialTheme.typography.titleSmall,
                    color = ChampagneGoldBright
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoseSilk.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, RoseSilk.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = RoseSilk,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "No fake payments are processed. Web gateways remain disabled until merchant approval & API credentials are configured. Paid Play Store purchases use official Google Play Billing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PearlIvory
                        )
                    }
                }

                // Official Google Play Billing section
                ProviderStatusCard(
                    icon = Icons.Filled.Shop,
                    title = "Google Play Billing (Android App)",
                    statusBadge = if (hasPlaySku) "READY" else "PLAY CONSOLE SETUP NEEDED",
                    isEnabled = hasPlaySku && paymentSettings.googlePlayBillingEnabled,
                    description = playBillingState.statusMessage,
                    selected = selectedProvider == PaymentProvider.GOOGLE_PLAY,
                    onClick = { selectedProvider = PaymentProvider.GOOGLE_PLAY }
                )

                if (hasPlaySku) {
                    Button(
                        onClick = { onLaunchPlayBilling(tier) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldActive,
                            contentColor = VelvetObsidian
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_play_billing_button")
                    ) {
                        Text("Subscribe via Google Play", fontWeight = FontWeight.Bold)
                    }
                }

                // Payoneer Pakistan Receiving / Checkout
                ProviderStatusCard(
                    icon = Icons.Filled.AccountBalance,
                    title = "Payoneer Checkout (International -> PK)",
                    statusBadge = if (paymentSettings.payoneerCheckoutEnabled) "ENABLED" else "DISABLED • APPROVAL REQUIRED",
                    isEnabled = paymentSettings.payoneerCheckoutEnabled,
                    description = "Eligible Pakistan business accounts only. Personal Payoneer accounts cannot process website checkout or recurring subscriptions.",
                    selected = selectedProvider == PaymentProvider.PAYONEER,
                    onClick = { selectedProvider = PaymentProvider.PAYONEER }
                )

                // JazzCash Pakistan Merchant Gateway
                ProviderStatusCard(
                    icon = Icons.Filled.PhoneAndroid,
                    title = "JazzCash Merchant (Pakistan)",
                    statusBadge = if (paymentSettings.jazzcashGatewayEnabled) "ENABLED" else "DISABLED • CREDENTIALS NEEDED",
                    isEnabled = paymentSettings.jazzcashGatewayEnabled,
                    description = "Requires verified JazzCash Merchant ID, Password, and HMAC Integrity Salt before live fan payments.",
                    selected = selectedProvider == PaymentProvider.JAZZCASH,
                    onClick = { selectedProvider = PaymentProvider.JAZZCASH }
                )

                // Visa Debit / Credit Card
                ProviderStatusCard(
                    icon = Icons.Filled.CreditCard,
                    title = "Visa Debit / Credit Card",
                    statusBadge = if (paymentSettings.visaCheckoutEnabled) "ENABLED" else "DISABLED • MERCHANT NEEDED",
                    isEnabled = paymentSettings.visaCheckoutEnabled,
                    description = "Processed via eligible merchant checkout once live webhook verification is active.",
                    selected = selectedProvider == PaymentProvider.VISA_CARD,
                    onClick = { selectedProvider = PaymentProvider.VISA_CARD }
                )

                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Request Membership Access / Launch Notification",
                    style = MaterialTheme.typography.titleSmall,
                    color = ChampagneGoldBright
                )
                Text(
                    text = "Submit your preferred payment method (${selectedProvider.displayName}). Maarya reviews requests inside her Studio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 280) note = it },
                    label = { Text("Optional note for Maarya") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("membership_request_note")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitAccessRequest(tier, selectedProvider, note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampagneGold,
                    contentColor = VelvetObsidian
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("submit_membership_request_button")
            ) {
                Text("Request ${formatCents(tier.priceMonthlyCents)} Access", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ProviderStatusCard(
    icon: ImageVector,
    title: String,
    statusBadge: String,
    isEnabled: Boolean,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = VelvetCard,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) ChampagneGold else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isEnabled) EmeraldActive else RoseSilk,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = PearlIvory
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isEnabled) EmeraldActive.copy(alpha = 0.16f) else CrimsonAlert.copy(alpha = 0.16f)
            ) {
                Text(
                    text = statusBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEnabled) EmeraldActive else CrimsonAlert,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReportContentDialog(
    post: CreatorPost,
    onDismiss: () -> Unit,
    onSubmitReport: (CreatorPost, ReportReason, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportReason.POLICY_VIOLATION) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetCardElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Flag, contentDescription = null, tint = CrimsonAlert)
                Text("Report Content", style = MaterialTheme.typography.headlineMedium, color = PearlIvory)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reporting \"${post.title}\" for moderation review by Maarya Studio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReportReason.entries.forEach { reason ->
                    val active = selectedReason == reason
                    OutlinedButton(
                        onClick = { selectedReason = reason },
                        border = BorderStroke(
                            1.dp,
                            if (active) ChampagneGold else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reason.label,
                            color = if (active) ChampagneGoldBright else PearlIvory
                        )
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 450) details = it },
                    label = { Text("Additional details (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReport(post, selectedReason, details) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert, contentColor = PearlIvory),
                modifier = Modifier.height(48.dp).testTag("submit_report_button")
            ) {
                Text("Submit Report", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
