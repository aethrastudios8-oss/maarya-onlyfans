const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const MAARYA_UID = "maarya_admin_uid";
const MAARYA_EMAIL = "aethrastudios8@gmail.com";
const ALICE_UID = "fan_alice_123";
const BOB_UID = "fan_bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function maaryaDb() {
  return testEnv
    .authenticatedContext(MAARYA_UID, {
      email: MAARYA_EMAIL,
      email_verified: true,
    })
    .firestore();
}

function spoofedAdminDb() {
  return testEnv
    .authenticatedContext("spoof_uid", {
      email: MAARYA_EMAIL,
      email_verified: false,
    })
    .firestore();
}

function fanDb(uid) {
  return testEnv
    .authenticatedContext(uid, {
      email: `${uid}@example.com`,
      email_verified: true,
    })
    .firestore();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("creator_profile").doc("maarya").get());
  await assertFails(unauth.collection("membership_tiers").where("creatorHandle", "==", "@maarya").get());
  await assertFails(unauth.collection("posts").where("creatorHandle", "==", "@maarya").where("published", "==", true).get());
  await assertFails(unauth.collection("posts").doc("post_1").collection("protected").doc("payload").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("subscriptions").doc(ALICE_UID).get());
  await assertFails(unauth.collection("transactions").get());
  await assertFails(unauth.collection("payout_configs").doc("maarya").get());
  await assertFails(unauth.collection("withdrawals").get());
  await assertFails(unauth.collection("reports").get());
});

test("Spoofed unverified admin email cannot perform Maarya Admin mutations", async () => {
  const spoofDb = spoofedAdminDb();
  const now = new Date(Date.now() - 2000);
  await assertFails(
    spoofDb.collection("creator_profile").doc("maarya").set({
      displayName: "Maarya",
      handle: "@maarya",
      tagline: "Spoofed",
      bio: "Spoofed",
      location: "Lahore, PK",
      verified: true,
      subscriberCount: 100,
      postCount: 1,
      updatedAt: now,
    })
  );
  await assertFails(
    spoofDb.collection("payout_configs").doc("maarya").set({
      payoneerReceivingEmail: "spoof@evil.com",
      payoneerAccountType: "Personal",
      payoneerBusinessApproved: false,
      jazzcashWalletNumber: "03001234567",
      jazzcashAccountTitle: "Spoof",
      settlementSchedule: "Manual",
      updatedAt: now,
    })
  );
});

test("Maarya Admin sets $50/$100/$200/$500 tiers & enforces protected post gating", async () => {
  const admin = maaryaDb();
  const alice = fanDb(ALICE_UID);
  const bob = fanDb(BOB_UID);
  const now = new Date(Date.now() - 3000);

  // Valid $50 and $500 tiers succeed
  await assertSucceeds(
    admin.collection("membership_tiers").doc("tier_1").set({
      creatorHandle: "@maarya",
      name: "Tier 1 • Pink Silk",
      rank: 1,
      priceMonthlyCents: 5000,
      badgeLabel: "TIER 1 • $50",
      description: "Entry private gallery access",
      perks: ["Photo sets"],
      active: true,
      playProductId: "maarya_tier_50",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    admin.collection("membership_tiers").doc("tier_4").set({
      creatorHandle: "@maarya",
      name: "Tier 4 • Diamond VIP",
      rank: 4,
      priceMonthlyCents: 50000,
      badgeLabel: "TIER 4 • $500",
      description: "Inner circle vault",
      perks: ["All VIP drops"],
      active: true,
      playProductId: "maarya_tier_500",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Publish Tier 2 ($100) post + protected payload
  await assertSucceeds(
    admin.collection("posts").doc("post_t2").set({
      creatorHandle: "@maarya",
      authorId: MAARYA_UID,
      title: "Midnight Orchid Set",
      caption: "Preview teaser for subscribers...",
      mediaType: "PHOTO",
      mediaKey: "maarya_hero",
      requiredTierRank: 2,
      requiredTierName: "TIER 2 • $100",
      ppvPriceCents: 1500,
      likesCount: 0,
      commentsCount: 0,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    admin.collection("posts").doc("post_t2").collection("protected").doc("payload").set({
      postId: "post_t2",
      requiredTierRank: 2,
      fullCaption: "Full private behind-the-scenes story & full gallery link.",
      privateMediaUrl: "https://example.com/private-set",
      updatedAt: now,
    })
  );

  // Non-subscriber Bob cannot read protected payload
  await assertFails(bob.collection("posts").doc("post_t2").collection("protected").doc("payload").get());

  // Grant Alice Tier 1 ($50) - still denied Tier 2 protected payload!
  await assertSucceeds(
    admin.collection("subscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fanName: "Alice",
      fanEmail: "alice@example.com",
      tierId: "tier_1",
      tierName: "Tier 1 • Pink Silk",
      tierRank: 1,
      monthlyPriceCents: 5000,
      status: "ACTIVE",
      autoRenew: true,
      renewsAtLabel: "Renews monthly",
      paymentMethod: "JAZZCASH",
      paymentLabel: "JazzCash • 0300****",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(alice.collection("posts").doc("post_t2").collection("protected").doc("payload").get());

  // Upgrade Alice to Tier 2 ($100) -> protected payload SUCCEEDS!
  await assertSucceeds(
    admin.collection("subscriptions").doc(ALICE_UID).update({
      tierId: "tier_2",
      tierName: "Tier 2 • Velvet Noir",
      tierRank: 2,
      monthlyPriceCents: 10000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(alice.collection("posts").doc("post_t2").collection("protected").doc("payload").get());

  // Single post unlock grants Bob protected payload access
  await assertSucceeds(
    admin.collection("users").doc(BOB_UID).collection("unlocked_posts").doc("post_t2").set({
      userId: BOB_UID,
      postId: "post_t2",
      amountCents: 1500,
      createdAt: now,
    })
  );
  await assertSucceeds(bob.collection("posts").doc("post_t2").collection("protected").doc("payload").get());
});

test("Fans cannot forge payments or subscriptions; PayPal & Crypto rejected; Payoneer/JazzCash/Visa/Play allowed", async () => {
  const admin = maaryaDb();
  const alice = fanDb(ALICE_UID);
  const now = new Date(Date.now() - 4000);

  // Fan cannot self-create a paid subscription or fake SUCCEEDED transaction
  await assertFails(
    alice.collection("subscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fanName: "Alice",
      fanEmail: "alice@example.com",
      tierId: "tier_4",
      tierName: "Tier 4 • Diamond VIP",
      tierRank: 4,
      monthlyPriceCents: 50000,
      status: "ACTIVE",
      autoRenew: true,
      renewsAtLabel: "Fake",
      paymentMethod: "VISA_CARD",
      paymentLabel: "Fake",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    alice.collection("transactions").doc("tx_fake").set({
      userId: ALICE_UID,
      fanName: "Alice",
      type: "SUBSCRIPTION",
      tierName: "Tier 4",
      amountCents: 50000,
      feeCents: 2500,
      netEarningsCents: 47500,
      status: "SUCCEEDED",
      paymentMethod: "PAYONEER",
      paymentLabel: "Fake Payoneer",
      createdAt: now,
    })
  );

  // PayPal and Crypto explicitly rejected even for admin
  await assertFails(
    admin.collection("transactions").doc("tx_paypal").set({
      userId: ALICE_UID,
      fanName: "Alice",
      type: "SUBSCRIPTION",
      tierName: "Tier 1",
      amountCents: 1000,
      feeCents: 50,
      netEarningsCents: 950,
      status: "SUCCEEDED",
      paymentMethod: "PAYPAL",
      paymentLabel: "PayPal",
      createdAt: now,
    })
  );
  await assertFails(
    admin.collection("transactions").doc("tx_crypto").set({
      userId: ALICE_UID,
      fanName: "Alice",
      type: "SUBSCRIPTION",
      tierName: "Tier 1",
      amountCents: 1000,
      feeCents: 0,
      netEarningsCents: 1000,
      status: "SUCCEEDED",
      paymentMethod: "CRYPTO",
      paymentLabel: "USDT",
      createdAt: now,
    })
  );

  // Valid verified Payoneer & JazzCash transactions succeed for Admin / webhook backend
  await assertSucceeds(
    admin.collection("transactions").doc("tx_payoneer").set({
      userId: ALICE_UID,
      fanName: "Alice",
      type: "SUBSCRIPTION",
      tierName: "Tier 2 • Velvet Noir",
      amountCents: 5000,
      feeCents: 250,
      netEarningsCents: 4750,
      status: "SUCCEEDED",
      paymentMethod: "PAYONEER",
      paymentLabel: "Payoneer Checkout",
      externalRef: "payo_99182",
      createdAt: now,
    })
  );
  await assertSucceeds(
    admin.collection("transactions").doc("tx_jazzcash").set({
      userId: BOB_UID,
      fanName: "Bob",
      type: "UNLOCK",
      tierName: "Studio Drop",
      amountCents: 1000,
      feeCents: 35,
      netEarningsCents: 965,
      status: "SUCCEEDED",
      paymentMethod: "JAZZCASH",
      paymentLabel: "JazzCash • 0301****",
      createdAt: now,
    })
  );

  // Fan CAN submit a non-payment membership access request
  await assertSucceeds(
    alice.collection("membership_requests").doc("req_1").set({
      userId: ALICE_UID,
      fanName: "Alice",
      fanEmail: "alice@example.com",
      requestedTierId: "tier_1",
      requestedTierName: "Tier 1 • Pink Silk",
      preferredProvider: "JAZZCASH",
      note: "Interested when JazzCash checkout goes live.",
      status: "PENDING",
      createdAt: now,
    })
  );
});

test("Content reporting, Pakistan Payoneer/JazzCash payout config & withdrawals security", async () => {
  const admin = maaryaDb();
  const alice = fanDb(ALICE_UID);
  const bob = fanDb(BOB_UID);
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    admin.collection("posts").doc("post_1").set({
      creatorHandle: "@maarya",
      authorId: MAARYA_UID,
      title: "Studio Check-in",
      caption: "Weekly update",
      mediaType: "NOTE",
      mediaKey: "maarya_portrait",
      requiredTierRank: 1,
      requiredTierName: "TIER 1 • $50",
      ppvPriceCents: 0,
      likesCount: 0,
      commentsCount: 0,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice files a content report; Bob cannot read Alice's report; Maarya Admin can resolve it
  await assertSucceeds(
    alice.collection("reports").doc("rep_1").set({
      postId: "post_1",
      postTitle: "Studio Check-in",
      reporterUid: ALICE_UID,
      reporterName: "Alice",
      reason: "OTHER",
      details: "Test report check",
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bob.collection("reports").doc("rep_1").get());
  await assertFails(alice.collection("reports").doc("rep_1").update({ status: "RESOLVED", updatedAt: new Date(Date.now() - 500) }));
  await assertSucceeds(admin.collection("reports").doc("rep_1").update({ status: "RESOLVED", updatedAt: new Date(Date.now() - 500) }));

  // Payout config & withdrawals are strictly Admin-only
  await assertSucceeds(
    admin.collection("payout_configs").doc("maarya").set({
      payoneerReceivingEmail: "aethrastudios8@gmail.com",
      payoneerAccountType: "Personal (Awaiting Business Merchant Eligibility)",
      payoneerBusinessApproved: false,
      jazzcashWalletNumber: "03001234567",
      jazzcashAccountTitle: "Maarya Studio",
      jazzcashMerchantId: "",
      settlementSchedule: "Manual (Payoneer -> JazzCash)",
      notes: "Receive international funds in Payoneer USD, settle to Pakistan JazzCash.",
      updatedAt: now,
    })
  );
  await assertFails(alice.collection("payout_configs").doc("maarya").get());

  await assertSucceeds(
    admin.collection("withdrawals").doc("wd_1").set({
      channel: "PAYONEER_TO_JAZZCASH",
      amountCents: 5000,
      feeCents: 150,
      netPayoutCents: 4850,
      destinationLabel: "JazzCash • 03001234567",
      referenceCode: "PAYO-JC-001",
      status: "SETTLED",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(alice.collection("withdrawals").doc("wd_1").get());
});
