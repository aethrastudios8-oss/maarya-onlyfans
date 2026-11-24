package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.formatCents
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldBright
import com.example.ui.theme.DiamondIce
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.PearlIvory
import com.example.ui.theme.RoseSilk
import com.example.ui.theme.VelvetCard
import com.example.ui.theme.VelvetObsidian

@Composable
fun VerifiedMaaryaBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified Official Creator",
        tint = ChampagneGold,
        modifier = modifier.size(18.dp)
    )
}

@Composable
fun TierBadgePill(
    label: String,
    rank: Long,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, borderColor) = when {
        rank >= 4L -> Triple(Color(0xFF28133D), DiamondIce, DiamondIce.copy(alpha = 0.65f))
        rank == 3L -> Triple(Color(0xFF2B124C), RoseSilk, RoseSilk.copy(alpha = 0.7f))
        rank == 2L -> Triple(Color(0xFF3E102C), ChampagneGoldBright, ChampagneGold.copy(alpha = 0.7f))
        rank == 1L -> Triple(Color(0xFF2A0D20), ChampagneGold, ChampagneGold.copy(alpha = 0.55f))
        else -> Triple(VelvetCard, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LuxuryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = VelvetObsidian,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) {
                        Brush.horizontalGradient(listOf(ChampagneGold, ChampagneGoldBright, RoseSilk))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun MaaryaMediaFrame(
    mediaKey: String,
    mediaUri: String?,
    mediaType: String,
    locked: Boolean,
    requiredTierName: String,
    ppvPriceCents: Long,
    onUnlockTierClick: () -> Unit,
    onUnlockPpvClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(VelvetObsidian)
    ) {
        val imageModifier = if (locked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.fillMaxSize().blur(22.dp)
        } else {
            Modifier.fillMaxSize()
        }

        if (!mediaUri.isNullOrBlank()) {
            AsyncImage(
                model = mediaUri,
                contentDescription = "Maarya exclusive media",
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        } else {
            val resId = when (mediaKey) {
                "maarya_portrait" -> R.drawable.maarya_portrait
                else -> R.drawable.maarya_hero
            }
            Image(
                painter = painterResource(resId),
                contentDescription = "Maarya exclusive studio media",
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = if (locked) 0.80f else 0.34f)
                        )
                    )
                )
        )

        if (!locked && mediaType == "VIDEO") {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(62.dp),
                shape = CircleShape,
                color = VelvetObsidian.copy(alpha = 0.78f),
                border = BorderStroke(1.5.dp, ChampagneGold)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Video diary preview",
                        tint = ChampagneGoldBright,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        if (locked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VelvetObsidian.copy(alpha = 0.68f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = ChampagneGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Exclusive locked content",
                            tint = ChampagneGoldBright,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Exclusive to $requiredTierName",
                    style = MaterialTheme.typography.titleLarge,
                    color = PearlIvory
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Active club membership required — protected on server",
                    style = MaterialTheme.typography.bodySmall,
                    color = PearlIvory.copy(alpha = 0.80f)
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onUnlockTierClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampagneGold,
                            contentColor = VelvetObsidian
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Subscribe via Club Tab", fontWeight = FontWeight.Bold)
                    }
                    if (ppvPriceCents > 0L) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VelvetCard.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, RoseSilk.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .height(48.dp)
                                .clickable(onClick = onUnlockPpvClick)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "Single ${formatCents(ppvPriceCents)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = RoseSilk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueBreakdownChart(
    series: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    val maxVal = (series.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(100L)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(VelvetCard)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Revenue by Stream",
            style = MaterialTheme.typography.titleMedium,
            color = PearlIvory
        )
        Spacer(Modifier.height(14.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = series.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.44f).coerceAtMost(56f)
            val colors = listOf(RoseSilk, ChampagneGold, ChampagneGoldBright, DiamondIce, EmeraldActive)

            series.forEachIndexed { idx, (_, value) ->
                val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f)
                val barHeight = size.height * ratio
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    color = colors[idx % colors.size].copy(alpha = 0.22f),
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = colors[idx % colors.size],
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            series.forEach { (label, cents) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCents(cents),
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampagneGoldBright
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
