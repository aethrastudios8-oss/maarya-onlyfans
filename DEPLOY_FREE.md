# MAARYA ONLYFANS — Zero-Budget Free Launch & Deployment Guide

This project is pre-configured for **MAARYA ONLYFANS** (single-creator exclusive club for Maarya `@maarya`) with **zero paid APIs, zero paid hosting, and no domain purchase required initially**.

---

## 1. Free-Tier Architecture Included

1. **Installable Progressive Web App (`/web/`)**:
   - Hosted for **$0/month** on **Firebase Spark Hosting** (`firebase.json` already points `"public": "web"`).
   - Automatic free `.web.app` / `.firebaseapp.com` HTTPS domain (`https://gen-lang-client-0152236103.web.app`).
   - Installable on Android, iPhone (Safari "Add to Home Screen"), and Desktop via `/web/manifest.webmanifest` and `/web/sw.js`.
   - Includes Registration, Email/Password Login, Password Recovery (`sendPasswordResetEmail`), Google Sign-In, Fan Dashboard, and Maarya Admin Studio.

2. **Server-Protected Firestore Database (`firestore.rules`)**:
   - Database ID: `ai-studio-android-maarya-99a25e26-86c9-402a-ab46-491cf1321666`
   - Exclusive post bodies & private media URLs live in `/posts/{postId}/protected/payload`, verified on the server (`status == 'ACTIVE' && tierRank >= requiredTierRank`).
   - Client-side fake payments and tier upgrades are blocked at the security rules level.

3. **Play-Store-Ready Android App (`/app/`)**:
   - Built with Jetpack Compose, Material 3 (Black `#060409`, White `#FFFFFF`, Pink `#FF4D9D`, Purple `#9D4EDD`), Google Credential Manager Sign-In, and official **Google Play Billing Library (`billing-ktx:7.1.1`)** in `PlayBillingManager.kt`.

---

## 2. Deploy Web App & Security Rules Free (No Domain Purchase Needed)

```bash
# 1. Authenticate once with Google account owning project gen-lang-client-0152236103
npx firebase-tools login

# 2. Deploy Firestore rules & the installable PWA on Firebase Spark free tier
npx firebase-tools deploy --only hosting,firestore:rules --project gen-lang-client-0152236103
```

---

## 3. Pakistan Payment & Withdrawal Checklist (No Fake Payments)

All live card/wallet payment gateways are intentionally **DISABLED** until merchant credentials are approved:

- **Payoneer (Pakistan Receiving & Withdrawal -> JazzCash)**:
  - Personal Payoneer accounts in Pakistan receive international USD payouts and withdraw into Pakistani bank / JazzCash accounts, **but do NOT support automated website checkout or recurring subscriptions**.
  - Apply for Payoneer Business / Checkout eligibility before enabling `payoneerCheckoutEnabled`.
- **JazzCash Merchant Gateway (Pakistan)**:
  - Register a JazzCash Business Merchant account for `Merchant ID`, `Password`, and `HMAC Integrity Salt`.
  - Wire `/server-webhooks/payment-webhooks.js` (`verifyJazzCashSecureHash`) on your backend to verify `pp_SecureHash` and unlock subscriptions automatically.
- **Google Play Store (Android App)**:
  - Publish the four subscription SKUs in Google Play Console: `maarya_tier_50` ($50), `maarya_tier_100` ($100), `maarya_tier_200` ($200), `maarya_tier_500` ($500).
