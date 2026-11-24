/**
 * MAARYA ONLYFANS — Server-Side Payment Verification & Webhook Reference
 *
 * IMPORTANT:
 * - Zero fake payments: Subscription tiers and protected content unlock ONLY after
 *   cryptographically verified webhook confirmation from:
 *     1. Payoneer Checkout (Eligible Pakistan Business merchant accounts only)
 *     2. JazzCash Merchant Gateway (HMAC-SHA256 pp_SecureHash verification)
 *     3. Google Play Real-Time Developer Notifications (Android Play Store app)
 * - NO PayPal and NO cryptocurrency methods are supported.
 * - Deploy this module to a free-tier HTTPS Node endpoint (e.g., Cloud Functions / Render / Fly)
 *   only AFTER merchant approval & API credentials are obtained and saved in server secrets.
 */

import crypto from 'node:crypto';

export const TIER_CATALOG = {
  tier_1: { id: 'tier_1', name: 'Tier 1 • Pink Silk', rank: 1, priceMonthlyCents: 5000, playProductId: 'maarya_tier_50' },
  tier_2: { id: 'tier_2', name: 'Tier 2 • Velvet Noir', rank: 2, priceMonthlyCents: 10000, playProductId: 'maarya_tier_100' },
  tier_3: { id: 'tier_3', name: 'Tier 3 • Royal Violet', rank: 3, priceMonthlyCents: 20000, playProductId: 'maarya_tier_200' },
  tier_4: { id: 'tier_4', name: 'Tier 4 • Diamond VIP', rank: 4, priceMonthlyCents: 50000, playProductId: 'maarya_tier_500' }
};

/**
 * Verifies JazzCash Merchant HMAC-SHA256 Secure Hash (pp_SecureHash).
 * Fields sorted alphabetically (excluding pp_SecureHash), joined with '&',
 * prefixed with Integrity Salt, hashed with HMAC-SHA256 using Integrity Salt.
 */
export function verifyJazzCashSecureHash(payload, integritySalt) {
  if (!integritySalt || !payload?.pp_SecureHash) return false;
  const sortedKeys = Object.keys(payload)
    .filter((k) => k !== 'pp_SecureHash' && payload[k] !== undefined && payload[k] !== null && payload[k] !== '')
    .sort();
  const hashString = [integritySalt, ...sortedKeys.map((k) => String(payload[k]))].join('&');
  const expected = crypto.createHmac('sha256', integritySalt).update(hashString).digest('hex').toUpperCase();
  const actual = String(payload.pp_SecureHash).toUpperCase();
  if (expected.length !== actual.length) return false;
  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(actual));
}

/**
 * Verifies Payoneer Checkout webhook HMAC-SHA256 signature header.
 * Note: Personal Payoneer accounts in Pakistan are NOT eligible for Checkout API;
 * keep disabled until Payoneer Business merchant approval is confirmed.
 */
export function verifyPayoneerWebhookSignature(rawBody, signatureHeader, webhookSecret) {
  if (!webhookSecret || !signatureHeader) return false;
  const expected = crypto.createHmac('sha256', webhookSecret).update(rawBody).digest('hex');
  const provided = String(signatureHeader).replace(/^sha256=/i, '');
  if (expected.length !== provided.length) return false;
  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(provided));
}

/**
 * Computes exact fee & net creator earnings in cents and atomically unlocks
 * the fan's Firestore subscription + writes the verified ledger transaction.
 * Designed for Firebase Admin SDK `db` + `FieldValue`.
 */
export async function applyVerifiedPaymentUnlock({
  adminDb,
  FieldValue,
  userId,
  fanName,
  fanEmail,
  tierId,
  provider, // 'PAYONEER' | 'JAZZCASH' | 'VISA_CARD' | 'GOOGLE_PLAY'
  externalRef,
  amountCents,
  feeCents
}) {
  const tier = TIER_CATALOG[tierId];
  if (!tier) throw new Error(`Unknown tierId: ${tierId}`);
  if (!['PAYONEER', 'JAZZCASH', 'VISA_CARD', 'GOOGLE_PLAY'].includes(provider)) {
    throw new Error(`Unsupported payment provider: ${provider}`);
  }
  if (amountCents < tier.priceMonthlyCents) {
    throw new Error('Underpaid transaction rejected');
  }

  const netEarningsCents = Math.max(0, amountCents - feeCents);
  const labelMap = {
    PAYONEER: 'Payoneer Checkout • Verified',
    JAZZCASH: 'JazzCash Merchant • Verified',
    VISA_CARD: 'Visa Card • Verified',
    GOOGLE_PLAY: 'Google Play Billing • Verified'
  };

  const batch = adminDb.batch();
  const subRef = adminDb.collection('subscriptions').doc(userId);
  batch.set(
    subRef,
    {
      userId,
      fanName: String(fanName || 'Member').slice(0, 80),
      fanEmail: String(fanEmail || `${userId}@member.maarya`).slice(0, 160),
      tierId: tier.id,
      tierName: tier.name,
      tierRank: tier.rank,
      monthlyPriceCents: tier.priceMonthlyCents,
      status: 'ACTIVE',
      autoRenew: provider === 'GOOGLE_PLAY',
      renewsAtLabel: provider === 'GOOGLE_PLAY' ? 'Renews monthly via Google Play' : 'Verified Paid Period',
      paymentMethod: provider,
      paymentLabel: labelMap[provider],
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    },
    { merge: true }
  );

  const txRef = adminDb.collection('transactions').doc();
  batch.set(txRef, {
    userId,
    fanName: String(fanName || 'Member').slice(0, 80),
    type: 'SUBSCRIPTION',
    tierName: tier.name,
    amountCents,
    feeCents,
    netEarningsCents,
    status: 'SUCCEEDED',
    paymentMethod: provider,
    paymentLabel: labelMap[provider],
    externalRef: String(externalRef || '').slice(0, 128),
    createdAt: FieldValue.serverTimestamp()
  });

  await batch.commit();
  return { unlockedTier: tier.id, netEarningsCents };
}
