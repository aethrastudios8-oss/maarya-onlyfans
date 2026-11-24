import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import {
  getAuth,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  sendEmailVerification,
  updateProfile,
  GoogleAuthProvider,
  signInWithPopup,
  signOut
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';
import {
  initializeFirestore,
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  onSnapshot,
  serverTimestamp,
  increment,
  deleteField
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js';

const firebaseConfig = {
  apiKey: 'AIzaSyCM15Go5DQ-SluYJs5OAvuHMJuBfHorHkE',
  authDomain: 'gen-lang-client-0152236103.firebaseapp.com',
  projectId: 'gen-lang-client-0152236103',
  storageBucket: 'gen-lang-client-0152236103.firebasestorage.app',
  messagingSenderId: '908936917523',
  appId: '1:908936917523:android:47c263a151a8742a6885e6'
};

const DATABASE_ID = 'ai-studio-android-maarya-99a25e26-86c9-402a-ab46-491cf1321666';
const MAARYA_ADMIN_EMAIL = 'aethrastudios8@gmail.com';

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = initializeFirestore(app, {}, DATABASE_ID);

const DEFAULT_TIERS = [
  { id: 'tier_1', name: 'Tier 1 • Pink Silk', rank: 1, priceMonthlyCents: 5000, badgeLabel: 'TIER 1 • $50', description: 'Entry private community membership with curated studio photos and weekly updates.', perks: ['Curated Tier 1 photo galleries', 'Weekly private community updates', 'Pink Silk member badge'], active: true, playProductId: 'maarya_tier_50' },
  { id: 'tier_2', name: 'Tier 2 • Velvet Noir', rank: 2, priceMonthlyCents: 10000, badgeLabel: 'TIER 2 • $100', description: 'Full access to HD behind-the-scenes studio films, fashion lookbooks, and priority DMs.', perks: ['Everything in Tier 1 ($50)', 'HD behind-the-scenes video diaries', 'Priority 1-on-1 direct messaging'], active: true, playProductId: 'maarya_tier_100' },
  { id: 'tier_3', name: 'Tier 3 • Royal Violet', rank: 3, priceMonthlyCents: 20000, badgeLabel: 'TIER 3 • $200', description: 'Unedited private editorial archives, early access drops, and custom Q&A requests.', perks: ['Everything in Tier 2 ($100)', 'Full unedited editorial archive', 'Theme voting & private studio Q&A'], active: true, playProductId: 'maarya_tier_200' },
  { id: 'tier_4', name: 'Tier 4 • Diamond VIP', rank: 4, priceMonthlyCents: 50000, badgeLabel: 'TIER 4 • $500', description: "Maarya's ultimate inner circle: complete Polaroid vault, personal shoutouts & VIP line.", perks: ['Everything in Tier 3 ($200)', 'Complete private Polaroid vault', 'Top-priority direct chat with Maarya'], active: true, playProductId: 'maarya_tier_500' }
];

const state = {
  user: null,
  isAdmin: false,
  fanPreview: false,
  activeTab: 'feed',
  feedFilter: 'ALL',
  studioTab: 'earnings',
  profile: {
    displayName: 'Maarya',
    handle: '@maarya',
    tagline: 'MAARYA ONLYFANS • Private Black, Pink & Purple Studio',
    bio: 'Welcome to my official private community. Subscribe to unlock curated photo sets, studio films, behind-the-scenes updates, and 1-on-1 VIP DMs.',
    location: 'Lahore, Pakistan • Private Studio',
    subscriberCount: 0,
    postCount: 4
  },
  tiers: DEFAULT_TIERS,
  posts: [],
  protectedPayloads: {},
  likedIds: new Set(),
  unlockedPostIds: new Set(),
  mySubscription: null,
  transactions: [],
  requests: [],
  reports: [],
  allSubscriptions: [],
  paymentSettings: {
    visaCheckoutEnabled: false,
    payoneerCheckoutEnabled: false,
    jazzcashGatewayEnabled: false,
    googlePlayBillingEnabled: false
  },
  payoutConfig: {
    payoneerReceivingEmail: MAARYA_ADMIN_EMAIL,
    payoneerAccountType: 'Personal Account (Checkout Disabled Until Business Eligibility)',
    payoneerBusinessApproved: false,
    jazzcashWalletNumber: '',
    jazzcashAccountTitle: 'Maarya',
    jazzcashMerchantId: '',
    settlementSchedule: 'Manual (Payoneer USD -> JazzCash PK)',
    notes: 'Receive international USD earnings in Payoneer, then transfer eligible funds to Pakistani JazzCash account.'
  },
  withdrawals: [],
  conversations: [],
  activeChatFanUid: null,
  messages: [],
  unsubs: [],
  msgUnsub: null
};

const $ = (id) => document.getElementById(id);

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatCents(cents) {
  const n = Number(cents || 0);
  if (n <= 0) return '$0';
  const d = Math.floor(n / 100);
  const r = n % 100;
  return r === 0 ? `$${d}` : `$${d}.${String(r).padStart(2, '0')}`;
}

function showToast(msg) {
  const el = $('toast');
  el.textContent = msg;
  el.classList.remove('hidden');
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => el.classList.add('hidden'), 3600);
}

function mediaSrcFor(post, protectedPayload) {
  if (protectedPayload && protectedPayload.privateMediaUrl) return protectedPayload.privateMediaUrl;
  return post.mediaKey === 'maarya_portrait' ? '/assets/maarya_portrait.png' : '/assets/maarya_hero.jpg';
}

function currentFanRank() {
  if (state.isAdmin && !state.fanPreview) return 4;
  if (!state.mySubscription || state.mySubscription.status !== 'ACTIVE') return 0;
  return Number(state.mySubscription.tierRank || 0);
}

function currentFanTierName() {
  if (state.isAdmin && !state.fanPreview) return 'Creator (Maarya)';
  if (!state.mySubscription || state.mySubscription.status !== 'ACTIVE') return 'No Active Tier';
  return state.mySubscription.tierName || 'No Active Tier';
}

// --- PWA Service Worker & Install Prompt ---
let deferredInstallPrompt = null;
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').catch(() => {});
}
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;
  $('pwa-install-banner')?.classList.remove('hidden');
});
$('pwa-install-btn')?.addEventListener('click', async () => {
  if (!deferredInstallPrompt) return;
  deferredInstallPrompt.prompt();
  await deferredInstallPrompt.userChoice;
  deferredInstallPrompt = null;
  $('pwa-install-banner')?.classList.add('hidden');
});

// --- Auth UI ---
function switchAuthMode(mode) {
  ['login', 'register', 'reset'].forEach((m) => {
    $(`auth-tab-${m}`).classList.toggle('active', m === mode);
    $(`${m}-form`).classList.toggle('hidden', m !== mode);
  });
  $('auth-feedback').textContent = '';
}
$('auth-tab-login').addEventListener('click', () => switchAuthMode('login'));
$('auth-tab-register').addEventListener('click', () => switchAuthMode('register'));
$('auth-tab-reset').addEventListener('click', () => switchAuthMode('reset'));

$('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('auth-feedback').textContent = 'Signing in...';
  try {
    await signInWithEmailAndPassword(auth, $('login-email').value.trim(), $('login-password').value);
  } catch (err) {
    $('auth-feedback').textContent = err.message;
  }
});

$('register-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('auth-feedback').textContent = 'Creating private member account...';
  try {
    const cred = await createUserWithEmailAndPassword(
      auth,
      $('reg-email').value.trim(),
      $('reg-password').value
    );
    const name = $('reg-name').value.trim() || 'Maarya Fan';
    await updateProfile(cred.user, { displayName: name });
    await sendEmailVerification(cred.user).catch(() => {});
    showToast('Account created! Verification email sent.');
  } catch (err) {
    $('auth-feedback').textContent = err.message;
  }
});

$('reset-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await sendPasswordResetEmail(auth, $('reset-email').value.trim());
    $('auth-feedback').textContent = 'Password recovery link sent to your email.';
  } catch (err) {
    $('auth-feedback').textContent = err.message;
  }
});

$('google-signin-btn').addEventListener('click', async () => {
  $('auth-feedback').textContent = 'Opening Google Sign-In...';
  try {
    await signInWithPopup(auth, new GoogleAuthProvider());
  } catch (err) {
    $('auth-feedback').textContent = err.message;
  }
});

$('rail-signout-btn').addEventListener('click', () => signOut(auth));

// --- Navigation ---
function switchTab(tab) {
  if (tab === 'studio' && !state.isAdmin) return;
  state.activeTab = tab;
  document.querySelectorAll('[data-tab]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tab === tab);
  });
  ['feed', 'club', 'messages', 'studio', 'account'].forEach((t) => {
    $(`tab-${t}`).classList.toggle('hidden', t !== tab);
  });
  renderCurrentTab();
}
document.querySelectorAll('[data-tab]').forEach((btn) => {
  btn.addEventListener('click', () => switchTab(btn.dataset.tab));
});

// --- Firestore Streams & Bootstrap ---
function clearListeners() {
  state.unsubs.forEach((u) => u && u());
  state.unsubs = [];
  if (state.msgUnsub) {
    state.msgUnsub();
    state.msgUnsub = null;
  }
}

async function ensureFanProfile(user) {
  const ref = doc(db, 'users', user.uid);
  const snap = await getDoc(ref);
  const displayName = (user.displayName || 'Maarya Fan').slice(0, 80);
  const email = (user.email || `${user.uid}@member.maarya`).slice(0, 160);
  if (!snap.exists()) {
    await setDoc(ref, {
      userId: user.uid,
      displayName,
      email,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  }
}

async function loadProtectedPayloadIfNeeded(postId) {
  if (state.protectedPayloads[postId]) return;
  try {
    const snap = await getDoc(doc(db, 'posts', postId, 'protected', 'payload'));
    if (snap.exists()) {
      state.protectedPayloads[postId] = snap.data();
      if (state.activeTab === 'feed') renderFeed();
    }
  } catch (_) {
    // Denied cleanly by Firestore rules when user lacks active required tier
  }
}

function attachStreams(user) {
  clearListeners();
  const uid = user.uid;
  const effectiveAdmin = state.isAdmin && !state.fanPreview;

  state.unsubs.push(
    onSnapshot(doc(db, 'creator_profile', 'maarya'), (snap) => {
      if (snap.exists()) state.profile = { ...state.profile, ...snap.data() };
      renderCurrentTab();
    }, () => {})
  );

  state.unsubs.push(
    onSnapshot(query(collection(db, 'membership_tiers'), where('creatorHandle', '==', '@maarya')), (snap) => {
      const rows = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => a.rank - b.rank);
      if (rows.length) state.tiers = rows;
      renderCurrentTab();
    }, () => {})
  );

  state.unsubs.push(
    onSnapshot(doc(db, 'payment_settings', 'maarya'), (snap) => {
      if (snap.exists()) state.paymentSettings = { ...state.paymentSettings, ...snap.data() };
      renderCurrentTab();
    }, () => {})
  );

  const postsQuery = effectiveAdmin
    ? query(collection(db, 'posts'), where('creatorHandle', '==', '@maarya'))
    : query(collection(db, 'posts'), where('creatorHandle', '==', '@maarya'), where('published', '==', true));

  state.unsubs.push(
    onSnapshot(postsQuery, async (snap) => {
      state.posts = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
        .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      const rank = currentFanRank();
      for (const p of state.posts) {
        if (rank >= p.requiredTierRank || state.unlockedPostIds.has(p.id)) {
          loadProtectedPayloadIfNeeded(p.id);
        }
        try {
          const likeSnap = await getDoc(doc(db, 'posts', p.id, 'likes', uid));
          if (likeSnap.exists()) state.likedIds.add(p.id); else state.likedIds.delete(p.id);
        } catch (_) {}
      }
      renderCurrentTab();
    }, () => {})
  );

  state.unsubs.push(
    onSnapshot(doc(db, 'subscriptions', uid), (snap) => {
      state.mySubscription = snap.exists() ? { id: snap.id, ...snap.data() } : null;
      const rank = currentFanRank();
      state.posts.forEach((p) => {
        if (rank >= p.requiredTierRank) loadProtectedPayloadIfNeeded(p.id);
      });
      renderCurrentTab();
    }, () => {})
  );

  state.unsubs.push(
    onSnapshot(collection(db, 'users', uid, 'unlocked_posts'), (snap) => {
      state.unlockedPostIds = new Set(snap.docs.map((d) => d.id));
      renderCurrentTab();
    }, () => {})
  );

  const txQ = effectiveAdmin
    ? collection(db, 'transactions')
    : query(collection(db, 'transactions'), where('userId', '==', uid));
  state.unsubs.push(
    onSnapshot(txQ, (snap) => {
      state.transactions = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
        .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      renderCurrentTab();
    }, () => {})
  );

  const reqQ = effectiveAdmin
    ? collection(db, 'membership_requests')
    : query(collection(db, 'membership_requests'), where('userId', '==', uid));
  state.unsubs.push(
    onSnapshot(reqQ, (snap) => {
      state.requests = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
        .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      renderCurrentTab();
    }, () => {})
  );

  const repQ = effectiveAdmin
    ? collection(db, 'reports')
    : query(collection(db, 'reports'), where('reporterUid', '==', uid));
  state.unsubs.push(
    onSnapshot(repQ, (snap) => {
      state.reports = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
        .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      renderCurrentTab();
    }, () => {})
  );

  const convQ = effectiveAdmin
    ? collection(db, 'conversations')
    : query(collection(db, 'conversations'), where('fanUid', '==', uid));
  state.unsubs.push(
    onSnapshot(convQ, (snap) => {
      state.conversations = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
        .sort((a, b) => (b.updatedAt?.seconds || 0) - (a.updatedAt?.seconds || 0));
      renderCurrentTab();
    }, () => {})
  );

  if (!effectiveAdmin) {
    state.activeChatFanUid = uid;
    listenMessages(uid);
  }

  if (state.isAdmin) {
    state.unsubs.push(
      onSnapshot(collection(db, 'subscriptions'), (snap) => {
        state.allSubscriptions = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderCurrentTab();
      }, () => {})
    );
    state.unsubs.push(
      onSnapshot(doc(db, 'payout_configs', 'maarya'), (snap) => {
        if (snap.exists()) state.payoutConfig = { ...state.payoutConfig, ...snap.data() };
        renderCurrentTab();
      }, () => {})
    );
    state.unsubs.push(
      onSnapshot(collection(db, 'withdrawals'), (snap) => {
        state.withdrawals = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
          .sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
        renderCurrentTab();
      }, () => {})
    );
  }
}

function listenMessages(fanUid) {
  if (state.msgUnsub) state.msgUnsub();
  if (!fanUid) {
    state.messages = [];
    return;
  }
  state.msgUnsub = onSnapshot(collection(db, 'conversations', fanUid, 'messages'), (snap) => {
    state.messages = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
    if (state.activeTab === 'messages') renderMessages();
  }, () => {});
}

onAuthStateChanged(auth, async (user) => {
  state.user = user;
  if (!user) {
    clearListeners();
    $('auth-view').classList.remove('hidden');
    $('app-view').classList.add('hidden');
    return;
  }
  const emailMatch = user.email === MAARYA_ADMIN_EMAIL && user.emailVerified;
  let adminDocExists = false;
  try {
    adminDocExists = (await getDoc(doc(db, 'admins', user.uid))).exists();
  } catch (_) {}
  state.isAdmin = Boolean(emailMatch || adminDocExists);

  $('rail-studio-btn').classList.toggle('hidden', !state.isAdmin);
  $('bnav-studio-btn').classList.toggle('hidden', !state.isAdmin);
  $('auth-view').classList.add('hidden');
  $('app-view').classList.remove('hidden');

  await ensureFanProfile(user).catch(() => {});
  attachStreams(user);
  renderCurrentTab();
});

// --- Rendering ---
function renderCurrentTab() {
  if (!state.user) return;
  if (state.activeTab === 'feed') renderFeed();
  else if (state.activeTab === 'club') renderClub();
  else if (state.activeTab === 'messages') renderMessages();
  else if (state.activeTab === 'studio') renderStudio();
  else if (state.activeTab === 'account') renderAccount();
}

function renderFeed() {
  const root = $('tab-feed');
  const rank = currentFanRank();
  const tierLabel = currentFanTierName();
  const publishedCount = state.posts.filter((p) => p.published).length;

  const filtered = state.posts.filter((p) => {
    const unlocked = rank >= p.requiredTierRank || state.unlockedPostIds.has(p.id);
    if (state.feedFilter === 'UNLOCKED') return unlocked;
    if (state.feedFilter === 'ALL') return true;
    return p.mediaType === state.feedFilter;
  });

  root.innerHTML = `
    <div class="creator-banner">
      <div class="creator-cover"><img src="/assets/maarya_hero.jpg" alt="Maarya Banner" /></div>
      <div class="creator-meta">
        <div class="space-between">
          <div class="row-gap">
            <img src="/assets/maarya_portrait.png" alt="Maarya Portrait" class="creator-avatar-lg" />
            <div>
              <h2>${escapeHtml(state.profile.displayName)} <span class="pill">Verified</span></h2>
              <div style="color:var(--pink-bright);font-weight:600;">MAARYA ONLYFANS • ${escapeHtml(state.profile.handle)}</div>
            </div>
          </div>
          <span class="pill ${rank >= 3 ? 'pill-purple' : ''}">${escapeHtml(tierLabel)}</span>
        </div>
        <p style="margin-top:12px; color:var(--pink-bright); font-weight:600;">${escapeHtml(state.profile.tagline)}</p>
        <p class="muted" style="margin-top:4px;">${escapeHtml(state.profile.bio)}</p>
        <div class="grid-2" style="margin-top:14px;">
          <div class="card"><span class="muted">Location</span><div style="font-weight:700;">${escapeHtml(state.profile.location)}</div></div>
          <div class="card"><span class="muted">Published Drops</span><div style="font-weight:700;">${publishedCount} Exclusive Posts</div></div>
        </div>
        <div class="row-gap" style="margin-top:14px;">
          <button class="btn-primary" style="flex:1;" id="feed-join-club">${rank === 0 ? 'Join Club ($50+)' : 'Manage Membership'}</button>
          <button class="btn-outline" id="feed-open-dms">DM Maarya</button>
        </div>
      </div>
    </div>

    <div class="filter-row">
      ${[
        ['ALL', 'All Drops'],
        ['PHOTO', 'Photos'],
        ['VIDEO', 'Videos'],
        ['NOTE', 'Studio Notes'],
        ['UNLOCKED', 'Unlocked']
      ].map(([k, l]) => `<button class="filter-chip ${state.feedFilter === k ? 'active' : ''}" data-filter="${k}">${l}</button>`).join('')}
    </div>

    <div class="stack">
      ${filtered.length === 0 ? `<div class="card muted">No drops in this filter yet.</div>` : filtered.map((post) => {
        const unlocked = rank >= post.requiredTierRank || state.unlockedPostIds.has(post.id);
        const payload = state.protectedPayloads[post.id];
        const caption = unlocked && payload?.fullCaption ? payload.fullCaption : post.caption;
        const imgUrl = mediaSrcFor(post, unlocked ? payload : null);
        const liked = state.likedIds.has(post.id);
        return `
          <article class="card ${unlocked ? '' : 'card-accent'}">
            <div class="space-between">
              <div class="row-gap">
                <img src="/assets/maarya_portrait.png" alt="Maarya" class="brand-avatar" style="width:38px;height:38px;" />
                <div>
                  <strong>Maarya</strong>
                  <div class="muted" style="font-size:0.78rem;">${escapeHtml(post.mediaType)}${post.published ? '' : ' • DRAFT'}</div>
                </div>
              </div>
              <div class="row-gap">
                ${unlocked ? `<span class="pill pill-green">Unlocked</span>` : `<span class="pill pill-purple">Locked</span>`}
                <span class="pill">${escapeHtml(post.requiredTierName)}</span>
              </div>
            </div>
            <h3 style="margin-top:12px;">${escapeHtml(post.title)}</h3>
            <p class="muted" style="margin-top:4px; color:#f3eaf8;">${escapeHtml(caption)}</p>
            <div class="media-box ${unlocked ? '' : 'locked'}">
              <img src="${escapeHtml(imgUrl)}" alt="${escapeHtml(post.title)}" />
              ${unlocked ? '' : `
                <div class="lock-overlay">
                  <span class="pill pill-purple">Exclusive to ${escapeHtml(post.requiredTierName)}</span>
                  <h3 style="margin-top:10px;">Subscribe to Unlock Full Media</h3>
                  <p class="muted" style="margin:6px 0 14px;">Full resolution content is verified server-side per membership tier.</p>
                  <button class="btn-primary" style="max-width:220px;" data-goto-club="1">View Club Tiers ($50–$500)</button>
                </div>
              `}
            </div>
            <div class="post-actions">
              <div class="row-gap">
                <button class="btn-outline" data-like="${post.id}">${liked ? '♥ Liked' : '♡ Like'} (${post.likesCount || 0})</button>
                <button class="btn-outline" data-comments="${post.id}">Comments (${post.commentsCount || 0})</button>
              </div>
              <button class="btn-danger" data-report="${post.id}">Report</button>
            </div>
          </article>
        `;
      }).join('')}
    </div>
  `;

  $('feed-join-club')?.addEventListener('click', () => switchTab('club'));
  $('feed-open-dms')?.addEventListener('click', () => switchTab('messages'));
  root.querySelectorAll('[data-filter]').forEach((b) => b.addEventListener('click', () => {
    state.feedFilter = b.dataset.filter;
    renderFeed();
  }));
  root.querySelectorAll('[data-goto-club]').forEach((b) => b.addEventListener('click', () => switchTab('club')));
  root.querySelectorAll('[data-like]').forEach((b) => b.addEventListener('click', () => toggleLike(b.dataset.like)));
  root.querySelectorAll('[data-comments]').forEach((b) => b.addEventListener('click', () => openCommentsModal(b.dataset.comments)));
  root.querySelectorAll('[data-report]').forEach((b) => b.addEventListener('click', () => openReportModal(b.dataset.report)));
}

async function toggleLike(postId) {
  const post = state.posts.find((p) => p.id === postId);
  if (!post || !state.user) return;
  const uid = state.user.uid;
  const likeRef = doc(db, 'posts', postId, 'likes', uid);
  const postRef = doc(db, 'posts', postId);
  try {
    if (state.likedIds.has(postId)) {
      await deleteDoc(likeRef);
      if ((post.likesCount || 0) > 0) await updateDoc(postRef, { likesCount: increment(-1), updatedAt: serverTimestamp() });
    } else {
      await setDoc(likeRef, { userId: uid, createdAt: serverTimestamp() });
      await updateDoc(postRef, { likesCount: increment(1), updatedAt: serverTimestamp() });
    }
  } catch (err) {
    showToast(err.message);
  }
}

function renderClub() {
  const root = $('tab-club');
  const rank = currentFanRank();
  const sub = state.mySubscription;

  root.innerHTML = `
    <div class="stack">
      <div>
        <h2>MAARYA ONLYFANS Membership Tiers</h2>
        <p class="muted">Four monthly tiers ($50, $100, $200, $500). Payment processing remains safely disabled until Payoneer Business & JazzCash Merchant credentials are approved.</p>
      </div>

      <div class="card card-accent">
        <div class="row-gap" style="margin-bottom:8px;">
          <span class="pill pill-red">Gateways Disabled Until Merchant Keys</span>
          <span class="pill pill-purple">No Fake Payments</span>
        </div>
        <p class="muted">
          • <strong>Payoneer Checkout (Pakistan):</strong> Eligible business merchant accounts only — personal Payoneer accounts cannot run automated website checkout or recurring subscriptions.<br/>
          • <strong>JazzCash Merchant (PK) & Visa Cards:</strong> Disabled until Merchant ID, Password, and HMAC Integrity Salt are configured.<br/>
          • <strong>Android Play Store:</strong> Uses official Google Play Billing API.
        </p>
      </div>

      ${sub ? `
        <div class="card" style="border-color: var(--green);">
          <div class="space-between">
            <div>
              <span class="pill pill-green">${escapeHtml(sub.status)}</span>
              <h3 style="margin-top:6px;">${escapeHtml(sub.tierName)}</h3>
              <div class="muted">${formatCents(sub.monthlyPriceCents)}/mo • ${escapeHtml(sub.paymentLabel)} • ${escapeHtml(sub.renewsAtLabel)}</div>
            </div>
            <div class="row-gap">
              ${sub.status === 'ACTIVE' ? `<button class="btn-outline" id="sub-pause-btn">Pause</button>` : ''}
              ${sub.status !== 'CANCELLED' ? `<button class="btn-danger" id="sub-cancel-btn">Cancel</button>` : ''}
            </div>
          </div>
        </div>
      ` : ''}

      <div class="grid-2">
        ${state.tiers.filter((t) => t.active).map((t) => {
          const isCurrent = sub && sub.tierId === t.id && sub.status === 'ACTIVE';
          return `
            <div class="card ${t.rank === 2 ? 'card-accent' : ''}" style="display:flex;flex-direction:column;justify-content:space-between;gap:12px;">
              <div>
                <div class="space-between">
                  <span class="pill ${t.rank >= 3 ? 'pill-purple' : ''}">${escapeHtml(t.badgeLabel)}</span>
                  <strong style="font-size:1.3rem;color:var(--pink-bright);">${formatCents(t.priceMonthlyCents)}/mo</strong>
                </div>
                <h3 style="margin-top:8px;">${escapeHtml(t.name)}</h3>
                <p class="muted" style="margin:6px 0 10px;">${escapeHtml(t.description)}</p>
                <ul style="padding-left:18px;font-size:0.86rem;color:#eee;" class="stack">
                  ${(t.perks || []).map((pk) => `<li>${escapeHtml(pk)}</li>`).join('')}
                </ul>
              </div>
              ${isCurrent
                ? `<button class="btn-outline" disabled style="border-color:var(--green);color:var(--green);">Current Active Plan</button>`
                : `<button class="btn-primary" data-select-tier="${t.id}">${t.rank > rank ? 'Upgrade' : 'Select'} • ${formatCents(t.priceMonthlyCents)}/mo</button>`}
            </div>
          `;
        }).join('')}
      </div>

      ${!state.isAdmin && state.requests.length ? `
        <h3>Your Access Requests</h3>
        ${state.requests.slice(0, 6).map((r) => `
          <div class="card space-between">
            <div>
              <strong>${escapeHtml(r.requestedTierName)}</strong>
              <div class="muted">Preferred: ${escapeHtml(r.preferredProvider)} ${r.note ? '• ' + escapeHtml(r.note) : ''}</div>
            </div>
            <span class="pill ${r.status === 'APPROVED' ? 'pill-green' : r.status === 'DECLINED' ? 'pill-red' : ''}">${escapeHtml(r.status)}</span>
          </div>
        `).join('')}
      ` : ''}

      ${!state.isAdmin && state.transactions.length ? `
        <h3>Your Verified Payment History</h3>
        ${state.transactions.slice(0, 8).map((tx) => `
          <div class="card space-between">
            <div>
              <strong>${escapeHtml(tx.tierName)}</strong>
              <div class="muted">${escapeHtml(tx.paymentLabel)} • ${escapeHtml(tx.status)} • Fee: ${formatCents(tx.feeCents)}</div>
            </div>
            <strong style="color:var(--pink-bright);">${formatCents(tx.amountCents)}</strong>
          </div>
        `).join('')}
      ` : ''}
    </div>
  `;

  $('sub-pause-btn')?.addEventListener('click', () => updateMySubStatus('PAUSED'));
  $('sub-cancel-btn')?.addEventListener('click', () => updateMySubStatus('CANCELLED'));
  root.querySelectorAll('[data-select-tier]').forEach((b) => b.addEventListener('click', () => openBillingGateModal(b.dataset.selectTier)));
}

async function updateMySubStatus(status) {
  try {
    await updateDoc(doc(db, 'subscriptions', state.user.uid), {
      status,
      autoRenew: false,
      renewsAtLabel: status === 'PAUSED' ? 'Paused' : 'Ends at period close',
      updatedAt: serverTimestamp()
    });
    showToast(`Subscription updated to ${status}.`);
  } catch (err) {
    showToast(err.message);
  }
}

function renderMessages() {
  const root = $('tab-messages');
  const effectiveAdmin = state.isAdmin && !state.fanPreview;

  if (effectiveAdmin && !state.activeChatFanUid) {
    root.innerHTML = `
      <div class="stack">
        <h2>Maarya's VIP Inbox</h2>
        <p class="muted">Direct messages from registered fans and subscribers.</p>
        ${state.conversations.length === 0 ? `<div class="card muted">No fan messages yet.</div>` : state.conversations.map((c) => `
          <div class="card space-between" style="cursor:pointer;" data-open-conv="${escapeHtml(c.fanUid)}">
            <div>
              <strong>${escapeHtml(c.fanName)}</strong> <span class="pill">${escapeHtml(c.fanTierName)}</span>
              <div class="muted">${escapeHtml(c.lastMessage)}</div>
            </div>
            ${c.unreadByCreator > 0 ? `<span class="pill">${c.unreadByCreator} new</span>` : `<span class="muted">Open →</span>`}
          </div>
        `).join('')}
      </div>
    `;
    root.querySelectorAll('[data-open-conv]').forEach((el) => el.addEventListener('click', () => {
      state.activeChatFanUid = el.dataset.openConv;
      listenMessages(state.activeChatFanUid);
      updateDoc(doc(db, 'conversations', state.activeChatFanUid), { unreadByCreator: 0, updatedAt: serverTimestamp() }).catch(() => {});
      renderMessages();
    }));
    return;
  }

  const targetUid = state.activeChatFanUid || state.user.uid;
  const activeConv = state.conversations.find((c) => c.fanUid === targetUid);

  root.innerHTML = `
    <div class="card stack">
      <div class="space-between">
        <div>
          <h3>${effectiveAdmin ? escapeHtml(activeConv?.fanName || 'Fan Thread') : 'Direct Line to Maarya (@maarya)'}</h3>
          <div class="muted">${effectiveAdmin ? escapeHtml(activeConv?.fanTierName || 'Member') : `Your tier: ${escapeHtml(currentFanTierName())}`}</div>
        </div>
        ${effectiveAdmin ? `<button class="btn-outline" id="dm-back-btn">← All Threads</button>` : ''}
      </div>
      <div class="chat-stream" id="chat-stream">
        ${state.messages.length === 0 ? `<div class="muted">No messages yet. Say hello!</div>` : state.messages.map((m) => {
          const isMe = effectiveAdmin ? m.senderRole === 'MAARYA' : m.senderRole === 'FAN';
          return `<div class="bubble ${isMe ? 'me' : 'them'}">
            <div style="font-size:0.72rem;opacity:0.8;font-weight:700;">${escapeHtml(m.senderName)}</div>
            <div>${escapeHtml(m.body)}</div>
          </div>`;
        }).join('')}
      </div>
      <form id="dm-form" class="row-gap">
        <input id="dm-text" type="text" placeholder="Write a private message..." maxlength="1000" style="flex:1;" required />
        <button class="btn-primary" style="width:auto;" type="submit">Send</button>
      </form>
    </div>
  `;

  $('dm-back-btn')?.addEventListener('click', () => {
    state.activeChatFanUid = null;
    renderMessages();
  });

  $('dm-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = $('dm-text').value.trim();
    if (!body) return;
    $('dm-text').value = '';
    const convRef = doc(db, 'conversations', targetUid);
    const snap = await getDoc(convRef);
    const fanName = activeConv?.fanName || state.user.displayName || 'Maarya Fan';
    const fanTierName = activeConv?.fanTierName || currentFanTierName();
    try {
      if (!snap.exists()) {
        await setDoc(convRef, {
          fanUid: targetUid,
          fanName: fanName.slice(0, 80),
          fanTierName: fanTierName.slice(0, 40),
          lastMessage: body.slice(0, 500),
          unreadByCreator: effectiveAdmin ? 0 : 1,
          unreadByFan: effectiveAdmin ? 1 : 0,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
      } else {
        await updateDoc(convRef, {
          lastMessage: body.slice(0, 500),
          unreadByCreator: effectiveAdmin ? 0 : increment(1),
          unreadByFan: effectiveAdmin ? increment(1) : 0,
          updatedAt: serverTimestamp()
        });
      }
      await addDoc(collection(db, 'conversations', targetUid, 'messages'), {
        fanUid: targetUid,
        senderId: state.user.uid,
        senderRole: effectiveAdmin ? 'MAARYA' : 'FAN',
        senderName: effectiveAdmin ? 'Maarya' : fanName.slice(0, 80),
        body,
        createdAt: serverTimestamp()
      });
    } catch (err) {
      showToast(err.message);
    }
  });
}

function renderAccount() {
  const root = $('tab-account');
  const sub = state.mySubscription;
  root.innerHTML = `
    <div class="stack">
      <h2>Fan Account Dashboard</h2>
      <div class="card card-accent space-between">
        <div>
          <h3>${escapeHtml(state.user.displayName || 'Maarya Fan')}</h3>
          <div class="muted">${escapeHtml(state.user.email || '')}</div>
          <div style="margin-top:8px;"><span class="pill">${escapeHtml(currentFanTierName())}</span></div>
        </div>
        <button class="btn-outline" id="acct-pw-reset">Send Password Reset Email</button>
      </div>

      ${state.isAdmin ? `
        <div class="card space-between" style="border-color: var(--pink);">
          <div>
            <strong>Maarya Super Admin Preview Mode</strong>
            <div class="muted">Toggle between Creator Admin privileges and normal Fan view</div>
          </div>
          <button class="btn-outline" id="toggle-preview-btn">${state.fanPreview ? 'Switch to Admin View' : 'Preview as Fan'}</button>
        </div>
      ` : ''}

      <div class="card form-stack">
        <h3>Update Display Name</h3>
        <input id="acct-name-input" type="text" value="${escapeHtml(state.user.displayName || '')}" maxlength="60" />
        <button class="btn-primary" id="acct-save-name" style="width:auto;">Save Profile</button>
      </div>

      <div class="card">
        <h3>Membership & Billing Summary</h3>
        <p class="muted" style="margin-top:6px;">
          ${sub ? `Active Plan: ${escapeHtml(sub.tierName)} (${formatCents(sub.monthlyPriceCents)}/mo) • ${escapeHtml(sub.status)} • Provider: ${escapeHtml(sub.paymentLabel)}`
                : 'No active paid tier. Open Club Tiers ($50–$500) to review membership options.'}
        </p>
      </div>

      <button class="btn-danger" id="acct-signout-btn">Sign Out</button>
    </div>
  `;

  $('acct-pw-reset')?.addEventListener('click', async () => {
    if (!state.user.email) return showToast('No email linked to account.');
    await sendPasswordResetEmail(auth, state.user.email);
    showToast(`Password recovery email sent to ${state.user.email}.`);
  });

  $('toggle-preview-btn')?.addEventListener('click', () => {
    state.fanPreview = !state.fanPreview;
    attachStreams(state.user);
    showToast(state.fanPreview ? 'Fan Preview Mode enabled.' : 'Creator Admin view restored.');
    renderAccount();
  });

  $('acct-save-name')?.addEventListener('click', async () => {
    const newName = $('acct-name-input').value.trim();
    if (!newName) return;
    await updateProfile(state.user, { displayName: newName });
    await updateDoc(doc(db, 'users', state.user.uid), { displayName: newName, updatedAt: serverTimestamp() });
    showToast('Display name updated.');
    renderAccount();
  });

  $('acct-signout-btn')?.addEventListener('click', () => signOut(auth));
}

function renderStudio() {
  if (!state.isAdmin) return;
  const root = $('tab-studio');
  const activeSubs = state.allSubscriptions.filter((s) => s.status === 'ACTIVE');
  const mrr = activeSubs.reduce((acc, s) => acc + (s.monthlyPriceCents || 0), 0);
  const succeeded = state.transactions.filter((t) => t.status === 'SUCCEEDED');
  const gross = succeeded.reduce((a, t) => a + (t.amountCents || 0), 0);
  const fees = succeeded.reduce((a, t) => a + (t.feeCents || 0), 0);
  const net = succeeded.reduce((a, t) => a + (t.netEarningsCents || 0), 0);
  const withdrawn = state.withdrawals.reduce((a, w) => a + (w.amountCents || 0), 0);
  const available = Math.max(0, net - withdrawn);
  const pendingReqs = state.requests.filter((r) => r.status === 'PENDING').length;
  const openReps = state.reports.filter((r) => r.status === 'OPEN').length;

  const tabs = [
    ['earnings', 'Earnings'],
    ['payouts', 'Payouts (PK)'],
    ['content', 'Content'],
    ['tiers', 'Tiers'],
    ['requests', `Requests & Fans (${pendingReqs})`],
    ['moderation', `Moderation (${openReps})`],
    ['profile', 'Profile']
  ];

  let bodyHtml = '';
  if (state.studioTab === 'earnings') {
    bodyHtml = `
      <div class="grid-2">
        <div class="card"><span class="muted">Available Balance</span><div class="metric-val" style="color:var(--green);">${formatCents(available)}</div></div>
        <div class="card"><span class="muted">Monthly Recurring</span><div class="metric-val" style="color:var(--pink-bright);">${formatCents(mrr)}</div></div>
        <div class="card"><span class="muted">Gross Verified Revenue</span><div class="metric-val">${formatCents(gross)}</div></div>
        <div class="card"><span class="muted">Fees / Net Earnings</span><div class="metric-val" style="color:var(--purple-bright);">${formatCents(fees)} / ${formatCents(net)}</div></div>
      </div>
      <h3 style="margin-top:16px;">Verified Transaction Ledger</h3>
      <div class="stack" style="margin-top:8px;">
        ${state.transactions.length === 0 ? `<div class="card muted">No verified webhook/admin transactions yet.</div>` : state.transactions.slice(0, 15).map((tx) => `
          <div class="card space-between">
            <div>
              <strong>${escapeHtml(tx.fanName)} • ${escapeHtml(tx.tierName)}</strong>
              <div class="muted">${escapeHtml(tx.paymentLabel)} • ${escapeHtml(tx.status)} • Fee: ${formatCents(tx.feeCents)} • Net: ${formatCents(tx.netEarningsCents)}</div>
            </div>
            <strong style="color:var(--pink-bright);">${formatCents(tx.amountCents)}</strong>
          </div>
        `).join('')}
      </div>
    `;
  } else if (state.studioTab === 'payouts') {
    const pc = state.payoutConfig;
    bodyHtml = `
      <div class="card card-accent" style="margin-bottom:12px;">
        <strong>Pakistan Settlement Rule (Payoneer -> JazzCash)</strong>
        <p class="muted" style="margin-top:4px;">Personal Payoneer accounts cannot automate website checkout or recurring subscriptions. Keep Payoneer Checkout disabled until commercial business merchant approval is verified. International earnings land in Payoneer USD and withdraw to Pakistan JazzCash.</p>
      </div>
      <form id="payout-config-form" class="card form-stack">
        <h3>Payoneer & JazzCash Settlement Config</h3>
        <input id="pc-payoneer-email" type="email" value="${escapeHtml(pc.payoneerReceivingEmail || '')}" placeholder="Payoneer Receiving Email" required />
        <input id="pc-payoneer-type" type="text" value="${escapeHtml(pc.payoneerAccountType || '')}" placeholder="Payoneer Account Classification" required />
        <input id="pc-jazz-wallet" type="text" value="${escapeHtml(pc.jazzcashWalletNumber || '')}" placeholder="Pakistan JazzCash Mobile Wallet (03xx...)" />
        <input id="pc-jazz-title" type="text" value="${escapeHtml(pc.jazzcashAccountTitle || 'Maarya')}" placeholder="JazzCash Account Title" required />
        <input id="pc-jazz-merchant" type="text" value="${escapeHtml(pc.jazzcashMerchantId || '')}" placeholder="JazzCash Merchant ID (if approved)" />
        <button class="btn-primary" type="submit">Save Pakistan Settlement Settings</button>
      </form>
      <form id="withdrawal-form" class="card form-stack" style="margin-top:12px;">
        <h3>Record Payoneer -> JazzCash Withdrawal Batch</h3>
        <div class="muted">Available Verified Balance: ${formatCents(available)}</div>
        <select id="wd-channel">
          <option value="PAYONEER_TO_JAZZCASH">Payoneer USD -> Pakistan JazzCash</option>
          <option value="JAZZCASH_DIRECT">JazzCash Merchant -> Wallet Settlement</option>
        </select>
        <div class="grid-2">
          <input id="wd-amount" type="number" min="1" placeholder="Amount ($ USD)" required />
          <input id="wd-fee" type="number" min="0" value="0" placeholder="FX / Fee ($ USD)" required />
        </div>
        <input id="wd-ref" type="text" placeholder="Transfer Reference Code" />
        <button class="btn-primary" type="submit">Log Withdrawal Batch</button>
      </form>
      <h3 style="margin-top:14px;">Withdrawal History</h3>
      <div class="stack" style="margin-top:8px;">
        ${state.withdrawals.map((w) => `
          <div class="card space-between">
            <div>
              <strong>${escapeHtml(w.channel)}</strong>
              <div class="muted">${escapeHtml(w.destinationLabel)} • ${escapeHtml(w.status)} ${w.referenceCode ? '• Ref ' + escapeHtml(w.referenceCode) : ''}</div>
            </div>
            <div class="row-gap">
              <strong>${formatCents(w.netPayoutCents)}</strong>
              ${w.status !== 'SETTLED' ? `<button class="btn-outline" data-settle-wd="${w.id}">Mark Settled</button>` : ''}
            </div>
          </div>
        `).join('') || `<div class="card muted">No withdrawals recorded yet.</div>`}
      </div>
    `;
  } else if (state.studioTab === 'content') {
    bodyHtml = `
      <form id="new-post-form" class="card form-stack">
        <h3>Publish Exclusive Drop</h3>
        <input id="np-title" type="text" placeholder="Post Title" required maxlength="120" />
        <textarea id="np-caption" rows="3" placeholder="Full subscriber-only note/editorial body..." required maxlength="2000"></textarea>
        <div class="grid-2">
          <select id="np-media-type">
            <option value="PHOTO">PHOTO</option>
            <option value="VIDEO">VIDEO</option>
            <option value="NOTE">NOTE</option>
          </select>
          <select id="np-tier">
            ${state.tiers.map((t) => `<option value="${t.rank}">${escapeHtml(t.badgeLabel)}</option>`).join('')}
          </select>
        </div>
        <select id="np-media-key">
          <option value="maarya_hero">Peach Silk Studio Portrait</option>
          <option value="maarya_portrait">Golden Floral Portrait</option>
        </select>
        <button class="btn-primary" type="submit">Publish Protected Drop</button>
      </form>
      <div class="stack" style="margin-top:14px;">
        ${state.posts.map((p) => `
          <div class="card space-between">
            <div>
              <span class="pill">${escapeHtml(p.requiredTierName)}</span>
              <span class="pill ${p.published ? 'pill-green' : 'pill-red'}">${p.published ? 'LIVE' : 'DRAFT'}</span>
              <strong style="display:block;margin-top:6px;">${escapeHtml(p.title)}</strong>
            </div>
            <button class="btn-danger" data-del-post="${p.id}">Delete</button>
          </div>
        `).join('')}
      </div>
    `;
  } else if (state.studioTab === 'tiers') {
    bodyHtml = `
      <div class="stack">
        ${state.tiers.map((t) => `
          <form class="card form-stack" data-edit-tier="${t.id}">
            <div class="space-between">
              <span class="pill">${escapeHtml(t.badgeLabel)}</span>
              <strong>${formatCents(t.priceMonthlyCents)}/mo</strong>
            </div>
            <input name="name" type="text" value="${escapeHtml(t.name)}" required />
            <input name="dollars" type="number" min="1" max="2000" value="${Math.round(t.priceMonthlyCents / 100)}" required />
            <textarea name="desc" rows="2" required>${escapeHtml(t.description)}</textarea>
            <button class="btn-outline" type="submit">Save Tier Pricing</button>
          </form>
        `).join('')}
      </div>
    `;
  } else if (state.studioTab === 'requests') {
    bodyHtml = `
      <div class="stack">
        <h3>Membership Requests (${pendingReqs} Pending)</h3>
        ${state.requests.map((r) => `
          <div class="card space-between">
            <div>
              <strong>${escapeHtml(r.fanName)}</strong> <span class="muted">(${escapeHtml(r.fanEmail)})</span>
              <div class="muted">${escapeHtml(r.requestedTierName)} via ${escapeHtml(r.preferredProvider)}</div>
            </div>
            ${r.status === 'PENDING' ? `
              <div class="row-gap">
                <button class="btn-primary" style="width:auto;" data-approve-req="${r.id}">Approve & Activate</button>
                <button class="btn-danger" data-decline-req="${r.id}">Decline</button>
              </div>
            ` : `<span class="pill">${escapeHtml(r.status)}</span>`}
          </div>
        `).join('') || `<div class="card muted">No access requests yet.</div>`}

        <h3 style="margin-top:10px;">Active & Past Subscribers (${state.allSubscriptions.length})</h3>
        ${state.allSubscriptions.map((s) => `
          <div class="card space-between">
            <div>
              <strong>${escapeHtml(s.fanName)}</strong> • <span class="pill">${escapeHtml(s.tierName)}</span>
              <div class="muted">${escapeHtml(s.fanEmail)} • ${escapeHtml(s.paymentLabel)} • ${escapeHtml(s.status)}</div>
            </div>
            <strong>${formatCents(s.monthlyPriceCents)}/mo</strong>
          </div>
        `).join('') || `<div class="card muted">No subscribers yet.</div>`}
      </div>
    `;
  } else if (state.studioTab === 'moderation') {
    bodyHtml = `
      <div class="stack">
        <h3>Moderation Reports</h3>
        ${state.reports.map((r) => `
          <div class="card space-between">
            <div>
              <strong>${escapeHtml(r.postTitle)}</strong> • <span class="pill pill-red">${escapeHtml(r.reason)}</span>
              <div class="muted">By ${escapeHtml(r.reporterName)} • Status: ${escapeHtml(r.status)}</div>
              ${r.details ? `<div class="muted">${escapeHtml(r.details)}</div>` : ''}
            </div>
            ${r.status === 'OPEN' ? `
              <div class="row-gap">
                <button class="btn-outline" data-unpublish-rep="${r.id}|${r.postId}">Unpublish Post</button>
                <button class="btn-danger" data-dismiss-rep="${r.id}">Dismiss</button>
              </div>
            ` : ''}
          </div>
        `).join('') || `<div class="card muted">No open content reports.</div>`}
      </div>
    `;
  } else if (state.studioTab === 'profile') {
    bodyHtml = `
      <form id="studio-profile-form" class="card form-stack">
        <h3>Maarya Public Profile</h3>
        <input id="sp-tagline" type="text" value="${escapeHtml(state.profile.tagline)}" required maxlength="160" />
        <textarea id="sp-bio" rows="3" required maxlength="1000">${escapeHtml(state.profile.bio)}</textarea>
        <input id="sp-location" type="text" value="${escapeHtml(state.profile.location)}" required maxlength="80" />
        <button class="btn-primary" type="submit">Save Creator Profile</button>
      </form>
    `;
  }

  root.innerHTML = `
    <div class="stack">
      <div>
        <h2>Maarya Creator Studio <span class="pill">Super Admin</span></h2>
        <p class="muted">Single-creator control center exclusively for @maarya</p>
      </div>
      <div class="filter-row">
        ${tabs.map(([k, l]) => `<button class="filter-chip ${state.studioTab === k ? 'active' : ''}" data-studio-tab="${k}">${l}</button>`).join('')}
      </div>
      ${bodyHtml}
    </div>
  `;

  root.querySelectorAll('[data-studio-tab]').forEach((b) => b.addEventListener('click', () => {
    state.studioTab = b.dataset.studioTab;
    renderStudio();
  }));

  $('payout-config-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    await setDoc(doc(db, 'payout_configs', 'maarya'), {
      payoneerReceivingEmail: $('pc-payoneer-email').value.trim(),
      payoneerAccountType: $('pc-payoneer-type').value.trim(),
      payoneerBusinessApproved: false,
      jazzcashWalletNumber: $('pc-jazz-wallet').value.trim(),
      jazzcashAccountTitle: $('pc-jazz-title').value.trim() || 'Maarya',
      jazzcashMerchantId: $('pc-jazz-merchant').value.trim(),
      settlementSchedule: 'Manual (Payoneer USD -> JazzCash PK)',
      updatedAt: serverTimestamp()
    });
    showToast('Pakistan Payoneer -> JazzCash settings saved.');
  });

  $('withdrawal-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const amountCents = Math.round(Number($('wd-amount').value) * 100);
    const feeCents = Math.min(amountCents, Math.round(Number($('wd-fee').value || 0) * 100));
    if (amountCents < 100) return showToast('Minimum withdrawal is $1.');
    await addDoc(collection(db, 'withdrawals'), {
      channel: $('wd-channel').value,
      amountCents,
      feeCents,
      netPayoutCents: amountCents - feeCents,
      destinationLabel: state.payoutConfig.jazzcashWalletNumber ? `JazzCash ${state.payoutConfig.jazzcashWalletNumber}` : 'Pakistan JazzCash',
      referenceCode: $('wd-ref').value.trim() || 'MANUAL',
      status: 'PENDING',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    showToast('Withdrawal batch recorded.');
  });

  root.querySelectorAll('[data-settle-wd]').forEach((b) => b.addEventListener('click', async () => {
    await updateDoc(doc(db, 'withdrawals', b.dataset.settleWd), { status: 'SETTLED', updatedAt: serverTimestamp() });
    showToast('Withdrawal marked as settled.');
  }));

  $('new-post-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const title = $('np-title').value.trim();
    const fullCaption = $('np-caption').value.trim();
    const rank = Number($('np-tier').value);
    const tier = state.tiers.find((t) => t.rank === rank) || state.tiers[0];
    const preview = fullCaption.length > 110 ? fullCaption.slice(0, 110) + '...' : fullCaption;
    const postRef = doc(collection(db, 'posts'));
    await setDoc(postRef, {
      creatorHandle: '@maarya',
      authorId: state.user.uid,
      title,
      caption: preview,
      mediaType: $('np-media-type').value,
      mediaKey: $('np-media-key').value,
      requiredTierRank: rank,
      requiredTierName: tier.badgeLabel,
      ppvPriceCents: 0,
      likesCount: 0,
      commentsCount: 0,
      published: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    await setDoc(doc(db, 'posts', postRef.id, 'protected', 'payload'), {
      postId: postRef.id,
      requiredTierRank: rank,
      fullCaption,
      updatedAt: serverTimestamp()
    });
    showToast('Exclusive drop published!');
  });

  root.querySelectorAll('[data-del-post]').forEach((b) => b.addEventListener('click', async () => {
    await deleteDoc(doc(db, 'posts', b.dataset.delPost));
    showToast('Post deleted.');
  }));

  root.querySelectorAll('[data-edit-tier]').forEach((form) => form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const tierId = form.dataset.editTier;
    const tier = state.tiers.find((t) => t.id === tierId);
    const cents = Math.round(Number(form.dollars.value) * 100);
    await updateDoc(doc(db, 'membership_tiers', tierId), {
      name: form.name.value.trim(),
      priceMonthlyCents: cents,
      description: form.desc.value.trim(),
      badgeLabel: tier.badgeLabel,
      updatedAt: serverTimestamp()
    });
    showToast('Tier pricing updated.');
  }));

  root.querySelectorAll('[data-approve-req]').forEach((b) => b.addEventListener('click', async () => {
    const req = state.requests.find((r) => r.id === b.dataset.approveReq);
    if (!req) return;
    const tier = state.tiers.find((t) => t.id === req.requestedTierId) || state.tiers[0];
    await setDoc(doc(db, 'subscriptions', req.userId), {
      userId: req.userId,
      fanName: req.fanName,
      fanEmail: req.fanEmail,
      tierId: tier.id,
      tierName: tier.name,
      tierRank: tier.rank,
      monthlyPriceCents: tier.priceMonthlyCents,
      status: 'ACTIVE',
      autoRenew: false,
      renewsAtLabel: 'Granted by Maarya Studio',
      paymentMethod: 'ADMIN_GRANT',
      paymentLabel: 'Verified Studio Grant',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
    await updateDoc(doc(db, 'membership_requests', req.id), { status: 'APPROVED' });
    showToast(`Activated ${req.fanName} on ${tier.name}.`);
  }));

  root.querySelectorAll('[data-decline-req]').forEach((b) => b.addEventListener('click', async () => {
    await updateDoc(doc(db, 'membership_requests', b.dataset.declineReq), { status: 'DECLINED' });
    showToast('Request declined.');
  }));

  root.querySelectorAll('[data-unpublish-rep]').forEach((b) => b.addEventListener('click', async () => {
    const [repId, postId] = b.dataset.unpublishRep.split('|');
    await updateDoc(doc(db, 'posts', postId), { published: false, updatedAt: serverTimestamp() });
    await updateDoc(doc(db, 'reports', repId), { status: 'RESOLVED', updatedAt: serverTimestamp() });
    showToast('Post unpublished & report resolved.');
  }));

  root.querySelectorAll('[data-dismiss-rep]').forEach((b) => b.addEventListener('click', async () => {
    await updateDoc(doc(db, 'reports', b.dataset.dismissRep), { status: 'DISMISSED', updatedAt: serverTimestamp() });
    showToast('Report dismissed.');
  }));

  $('studio-profile-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    await setDoc(doc(db, 'creator_profile', 'maarya'), {
      displayName: 'Maarya',
      handle: '@maarya',
      tagline: $('sp-tagline').value.trim(),
      bio: $('sp-bio').value.trim(),
      location: $('sp-location').value.trim(),
      verified: true,
      subscriberCount: state.profile.subscriberCount || 0,
      postCount: state.posts.length || 0,
      updatedAt: serverTimestamp()
    });
    showToast('Creator profile saved.');
  });
}

// --- Modals ---
function closeModal() {
  $('modal-root').classList.add('hidden');
  $('modal-root').innerHTML = '';
}
$('modal-root').addEventListener('click', (e) => {
  if (e.target === $('modal-root')) closeModal();
});

function openBillingGateModal(tierId) {
  const tier = state.tiers.find((t) => t.id === tierId);
  if (!tier) return;
  const root = $('modal-root');
  root.classList.remove('hidden');
  root.innerHTML = `
    <div class="modal-card stack">
      <div class="space-between">
        <h3>${escapeHtml(tier.name)}</h3>
        <strong style="color:var(--pink-bright);">${formatCents(tier.priceMonthlyCents)}/mo</strong>
      </div>
      <div class="card" style="border-color: var(--red);">
        <span class="pill pill-red">Web Gateways Disabled</span>
        <p class="muted" style="margin-top:6px;">
          Real checkout via <strong>Payoneer (Pakistan Business eligible)</strong>, <strong>JazzCash Merchant</strong>, and <strong>Visa Debit/Credit</strong> stays disabled until API credentials & merchant approval are verified.
        </p>
      </div>
      <form id="req-access-form" class="form-stack">
        <label class="muted">Preferred Payment Gateway Once Live</label>
        <select id="req-provider">
          <option value="PAYONEER">Payoneer (International -> PK)</option>
          <option value="JAZZCASH">JazzCash Merchant (Pakistan)</option>
          <option value="VISA_CARD">Visa Debit / Credit Card</option>
        </select>
        <textarea id="req-note" rows="2" placeholder="Optional note for Maarya..." maxlength="280"></textarea>
        <div class="row-gap">
          <button class="btn-primary" style="flex:1;" type="submit">Submit Access Request</button>
          <button class="btn-outline" type="button" id="modal-close-btn">Close</button>
        </div>
      </form>
    </div>
  `;
  $('modal-close-btn').addEventListener('click', closeModal);
  $('req-access-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await addDoc(collection(db, 'membership_requests'), {
        userId: state.user.uid,
        fanName: (state.user.displayName || 'Maarya Fan').slice(0, 80),
        fanEmail: (state.user.email || `${state.user.uid}@member.maarya`).slice(0, 160),
        requestedTierId: tier.id,
        requestedTierName: tier.name.slice(0, 40),
        preferredProvider: $('req-provider').value,
        note: $('req-note').value.trim() || undefined,
        status: 'PENDING',
        createdAt: serverTimestamp()
      });
      closeModal();
      showToast(`Access request for ${tier.name} submitted.`);
    } catch (err) {
      showToast(err.message);
    }
  });
}

function openReportModal(postId) {
  const post = state.posts.find((p) => p.id === postId);
  if (!post) return;
  const root = $('modal-root');
  root.classList.remove('hidden');
  root.innerHTML = `
    <div class="modal-card stack">
      <h3>Report Content</h3>
      <p class="muted">Flag "${escapeHtml(post.title)}" for moderation review.</p>
      <form id="report-form" class="form-stack">
        <select id="rep-reason">
          <option value="POLICY_VIOLATION">Platform Policy Violation</option>
          <option value="COPYRIGHT">Copyright / Unauthorized Reupload</option>
          <option value="HARASSMENT">Harassment or Abuse</option>
          <option value="SPAM">Spam or Scams</option>
          <option value="OTHER">Other Concern</option>
        </select>
        <textarea id="rep-details" rows="3" placeholder="Additional details..." maxlength="450"></textarea>
        <div class="row-gap">
          <button class="btn-danger" style="flex:1;" type="submit">Submit Report</button>
          <button class="btn-outline" type="button" id="rep-close">Cancel</button>
        </div>
      </form>
    </div>
  `;
  $('rep-close').addEventListener('click', closeModal);
  $('report-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await addDoc(collection(db, 'reports'), {
        postId: post.id,
        postTitle: post.title.slice(0, 120),
        reporterUid: state.user.uid,
        reporterName: (state.user.displayName || 'Member').slice(0, 80),
        reason: $('rep-reason').value,
        details: $('rep-details').value.trim() || undefined,
        status: 'OPEN',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      closeModal();
      showToast('Report submitted for moderation review.');
    } catch (err) {
      showToast(err.message);
    }
  });
}

async function openCommentsModal(postId) {
  const root = $('modal-root');
  root.classList.remove('hidden');
  root.innerHTML = `<div class="modal-card stack"><h3>Member Comments</h3><div id="comments-list" class="stack muted">Loading...</div><form id="comment-form" class="row-gap"><input id="c-input" type="text" placeholder="Write a comment..." maxlength="500" style="flex:1;" required /><button class="btn-primary" style="width:auto;" type="submit">Post</button><button class="btn-outline" type="button" id="c-close">Close</button></form></div>`;
  $('c-close').addEventListener('click', closeModal);

  const loadComments = async () => {
    const snap = await getDocs(collection(db, 'posts', postId, 'comments'));
    const list = snap.docs.map((d) => d.data()).sort((a, b) => (a.createdAt?.seconds || 0) - (b.createdAt?.seconds || 0));
    $('comments-list').innerHTML = list.length === 0
      ? '<div class="muted">No comments yet.</div>'
      : list.map((c) => `<div class="card"><strong>${escapeHtml(c.authorName)}</strong> <span class="pill">${escapeHtml(c.authorTierBadge)}</span><div style="margin-top:4px;">${escapeHtml(c.body)}</div></div>`).join('');
  };
  await loadComments();

  $('comment-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const body = $('c-input').value.trim();
    if (!body) return;
    await addDoc(collection(db, 'posts', postId, 'comments'), {
      postId,
      userId: state.user.uid,
      authorName: (state.isAdmin && !state.fanPreview ? 'Maarya' : (state.user.displayName || 'Member')).slice(0, 60),
      authorTierBadge: state.isAdmin && !state.fanPreview ? 'CREATOR' : currentFanTierName().slice(0, 40),
      body,
      createdAt: serverTimestamp()
    });
    await updateDoc(doc(db, 'posts', postId), { commentsCount: increment(1), updatedAt: serverTimestamp() });
    $('c-input').value = '';
    await loadComments();
  });
}
