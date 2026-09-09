/**
 * Page refresh: top indicator animation, reload-button wiring, mobile pull-to-refresh.
 * Views register a handler that refetches their API data.
 */
import { t } from '/shared/js/i18n/i18n.js?v=fix-refresh-1';

const PULL_THRESHOLD = 72;
const PULL_MAX = 128;
const LOADING_OFFSET = 56;

let handler = null;
let busy = false;
let bar = null;
let pill = null;
let label = null;
let spinner = null;
let pullArmed = false;
let pullStartY = 0;
let pullDistance = 0;
let wired = false;

export function setPageRefresh(fn) {
  handler = typeof fn === 'function' ? fn : null;
}

export function clearPageRefresh() {
  handler = null;
}

export function isPageRefreshing() {
  return busy;
}

function canPull() {
  if (busy || !handler) return false;
  if (document.body.classList.contains('dialog-open')) return false;
  if (document.body.classList.contains('account-sheet-open')) return false;
  if (document.body.classList.contains('sidebar-drawer-open')) return false;
  return getScrollTop() <= 1;
}

function getScrollTop() {
  const main = document.querySelector('.main-column');
  if (main && main.scrollHeight > main.clientHeight + 2) {
    return main.scrollTop;
  }
  const content = document.getElementById('content');
  if (content && content.scrollHeight > content.clientHeight + 2) {
    return content.scrollTop;
  }
  return window.scrollY || document.documentElement.scrollTop || 0;
}

function getShiftTarget() {
  return document.querySelector('.app-shell') || document.getElementById('content') || document.body;
}

function setContentOffset(px, { animate = false } = {}) {
  const target = getShiftTarget();
  if (!target) return;
  target.classList.toggle('page-refresh-shifting', !animate && px > 0);
  target.classList.toggle('page-refresh-settling', animate);
  if (px <= 0) {
    target.style.setProperty('--page-refresh-shift', '0px');
    // Keep settling class briefly so spring-back animates, then clear.
    if (animate) {
      window.setTimeout(() => {
        target.style.removeProperty('--page-refresh-shift');
        target.classList.remove('page-refresh-shifting', 'page-refresh-settling');
      }, 340);
    } else {
      target.style.removeProperty('--page-refresh-shift');
      target.classList.remove('page-refresh-shifting', 'page-refresh-settling');
    }
    return;
  }
  target.style.setProperty('--page-refresh-shift', `${px}px`);
}

function ensureBar() {
  if (bar) return bar;
  bar = document.createElement('div');
  bar.id = 'page-refresh-bar';
  bar.className = 'page-refresh-bar';
  bar.setAttribute('aria-hidden', 'true');

  pill = document.createElement('div');
  pill.className = 'page-refresh-pill';

  spinner = document.createElement('span');
  spinner.className = 'page-refresh-spinner';
  spinner.setAttribute('aria-hidden', 'true');

  label = document.createElement('span');
  label.className = 'page-refresh-label';
  label.textContent = t('common.refreshing');

  pill.append(spinner, label);
  bar.appendChild(pill);
  document.body.appendChild(bar);
  return bar;
}

function setPullVisual(distance, loading = false) {
  ensureBar();
  const clamped = Math.min(PULL_MAX, Math.max(0, distance));
  const progress = Math.min(1, clamped / PULL_THRESHOLD);
  bar.classList.toggle('is-pulling', clamped > 0 && !loading);
  bar.classList.toggle('is-loading', loading);
  bar.classList.toggle('is-visible', loading || clamped > 8);
  bar.style.setProperty('--pull', String(progress));
  bar.style.transform = loading
    ? 'translate3d(0, 0, 0)'
    : `translate3d(0, ${clamped - 64}px, 0)`;
  label.textContent = loading
    ? t('common.refreshing')
    : (clamped >= PULL_THRESHOLD ? t('common.releaseToRefresh') : t('common.pullToRefresh'));
  spinner.classList.toggle('is-spinning', loading || clamped >= PULL_THRESHOLD);
  bar.setAttribute('aria-hidden', loading || clamped > 8 ? 'false' : 'true');

  if (loading) {
    setContentOffset(LOADING_OFFSET, { animate: true });
  } else {
    setContentOffset(clamped, { animate: false });
  }
}

function resetPullVisual({ animate = true } = {}) {
  if (bar) {
    bar.classList.remove('is-pulling', 'is-visible');
    if (!busy) {
      bar.classList.remove('is-loading');
      bar.style.transform = 'translate3d(0, -110%, 0)';
      bar.setAttribute('aria-hidden', 'true');
    }
  }
  setContentOffset(0, { animate });
  pullDistance = 0;
}

function markReloadButtons(active) {
  document.querySelectorAll('.btn-reload').forEach((btn) => {
    btn.classList.toggle('is-refreshing', active);
    if (active) btn.disabled = true;
    else btn.disabled = false;
  });
}

async function settleBar() {
  if (bar) {
    bar.classList.add('is-loading', 'is-visible');
    bar.style.transform = 'translate3d(0, 0, 0)';
  }
  setContentOffset(LOADING_OFFSET, { animate: true });
  await new Promise((r) => setTimeout(r, 220));
  if (bar) {
    bar.classList.remove('is-loading', 'is-visible', 'is-pulling');
    bar.style.transform = 'translate3d(0, -110%, 0)';
    bar.setAttribute('aria-hidden', 'true');
  }
  setContentOffset(0, { animate: true });
}

export async function refreshPage({ source = 'button' } = {}) {
  if (busy || typeof handler !== 'function') return false;
  busy = true;
  document.body.classList.add('is-page-refreshing');
  ensureBar();
  setPullVisual(source === 'pull' ? Math.max(pullDistance, PULL_THRESHOLD) : PULL_THRESHOLD, true);
  markReloadButtons(true);
  try {
    await handler();
  } finally {
    markReloadButtons(false);
    await settleBar();
    document.body.classList.remove('is-page-refreshing');
    busy = false;
    pullArmed = false;
    pullDistance = 0;
  }
  return true;
}

function onTouchStart(event) {
  if (!canPull() || event.touches.length !== 1) {
    pullArmed = false;
    return;
  }
  pullArmed = true;
  pullStartY = event.touches[0].clientY;
  pullDistance = 0;
}

function onTouchMove(event) {
  if (!pullArmed || busy || event.touches.length !== 1) return;
  if (!canPull() && pullDistance <= 0) {
    pullArmed = false;
    resetPullVisual({ animate: true });
    return;
  }
  const dy = event.touches[0].clientY - pullStartY;
  if (dy <= 0) {
    pullDistance = 0;
    resetPullVisual({ animate: false });
    return;
  }
  // Rubber-band: strong at first, then softens near PULL_MAX
  const raw = dy * 0.62;
  pullDistance = Math.min(PULL_MAX, raw - (raw * raw) / (PULL_MAX * 4));
  if (pullDistance > 4 && event.cancelable) {
    event.preventDefault();
  }
  setPullVisual(pullDistance, false);
}

function onTouchEnd() {
  if (!pullArmed) return;
  const shouldRefresh = pullDistance >= PULL_THRESHOLD && !busy && handler;
  pullArmed = false;
  if (shouldRefresh) {
    refreshPage({ source: 'pull' }).catch(() => {});
  } else {
    resetPullVisual({ animate: true });
  }
}

export function wirePageRefresh() {
  if (wired) return;
  wired = true;
  ensureBar();
  document.addEventListener('touchstart', onTouchStart, { passive: true });
  document.addEventListener('touchmove', onTouchMove, { passive: false });
  document.addEventListener('touchend', onTouchEnd, { passive: true });
  document.addEventListener('touchcancel', onTouchEnd, { passive: true });
}
