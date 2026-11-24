package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.auth.signOut
import com.example.data.model.UiState
import com.example.data.model.formatCents
import com.example.ui.components.TierBadgePill
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.RoseSilk
import com.example.ui.theme.VelvetCard
import com.example.ui.theme.VelvetObsidian
import com.example.ui.viewmodel.MaaryaViewModel

@Composable
fun AccountScreen(
    viewModel: MaaryaViewModel,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val subState by viewModel.mySubscriptionState.collectAsStateWithLifecycle()
    val isPreview by viewModel.isFanPreviewMode.collectAsStateWithLifecycle()

    val mySub = (subState as? UiState.Success)?.data
    val tierName = viewModel.currentFanTierName(mySub)
    val rank = viewModel.currentFanRank(mySub)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VelvetObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Your Account", style = MaterialTheme.typography.displayMedium, color = PearlIvory)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = VelvetCard,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ChampagneGold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.currentUserName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = ChampagneGoldBright
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(viewModel.currentUserName, style = MaterialTheme.typography.titleLarge, color = PearlIvory)
                            Text(viewModel.currentUserEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            TierBadgePill(label = tierName, rank = rank)
                        }
                    }
                }
            }

            if (viewModel.isAdmin) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VelvetCard,
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = ChampagneGoldBright)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Maarya Super Admin", style = MaterialTheme.typography.titleMedium, color = ChampagneGoldBright)
                                Text(
                                    text = if (isPreview) "Currently previewing as Fan" else "Creator Studio controls active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoseSilk
                                )
                            }
                            Switch(
                                checked = isPreview,
                                onCheckedChange = viewModel::setFanPreviewMode
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VelvetCard,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Membership Summary", style = MaterialTheme.typography.titleMedium, color = PearlIvory)
                        Text(
                            text = if (mySub != null) {
                                "Plan: ${mySub.tierName} (${formatCents(mySub.monthlyPriceCents)}/mo) • ${mySub.status}\nProvider: ${mySub.paymentLabel}"
                            } else {
                                "Plan: No Active Tier • Join Tier 1 (\$50) – Tier 4 (\$500) in the Club tab to unlock Maarya's galleries & VIP DMs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VelvetCard,
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Payment & Security Overview", style = MaterialTheme.typography.titleSmall, color = ChampagneGoldBright)
                        Text(
                            text = "• Google Play Billing — Official in-app digital subscriptions for Play Store Android\n" +
                                "• Payoneer Checkout (Pakistan Eligible Business) — International USD receiving -> JazzCash PK\n" +
                                "• JazzCash Merchant & Visa Cards — Disabled until merchant approval & API credentials are verified",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://myaccount.google.com/security")
                                )
                                context.startActivity(intent)
                            },
                            border = BorderStroke(1.dp, RoseSilk.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Google Account Security & Password Recovery", color = RoseSilk)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        signOut(credentialManager = credentialManager, scope = scope, onSignOutComplete = onSignedOut)
                    },
                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
