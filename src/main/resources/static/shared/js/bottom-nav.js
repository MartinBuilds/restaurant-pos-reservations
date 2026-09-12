/**
 * Phone bottom navigation: replaces the sidebar drawer on small screens.
 * Equal-width tabs (routes + account), no horizontal scroll.
 */
import { icon } from '/shared/js/icons.js';
import { onLanguageChange, t } from '/shared/js/i18n/i18n.js?v=fix-bottom-nav-3';
import { THEME_CHANGE } from '/shared/js/ui-preferences.js';

const MOBILE_MQ = '(max-width: 768px)';

function ensureBar() {
  let bar = document.getElementById('bottom-nav');
  if (bar) return bar;
  bar = document.createElement('nav');
  bar.id = 'bottom-nav';
  bar.className = 'bottom-nav';
  bar.setAttribute('aria-label', 'Primary');
  bar.hidden = true;

  const accountSlot = document.createElement('div');
  accountSlot.className = 'bottom-nav-account';
  accountSlot.id = 'bottom-account-slot';

  bar.appendChild(accountSlot);
  document.body.appendChild(bar);
  return bar;
}

function rebuildLinks(bar, mainNav) {
  bar.querySelectorAll('a.bottom-nav-item').forEach((node) => node.remove());
  const accountSlot = document.getElementById('bottom-account-slot');
  mainNav.querySelectorAll('a.nav-link').forEach((link) => {
    const item = link.cloneNode(true);
    item.classList.add('bottom-nav-item');
    item.removeAttribute('id');
    bar.insertBefore(item, accountSlot);
  });
}

function placeConnectionStatus(mobile, sidebarFooter) {
  const conn = document.getElementById('connection-status');
  if (!conn) return;
  const topbar = document.querySelector('.topbar');
  if (mobile && topbar) {
    if (!topbar.contains(conn)) {
      topbar.appendChild(conn);
      conn.classList.add('conn-topbar');
    }
    return;
  }
  conn.classList.remove('conn-topbar');
  if (sidebarFooter && !sidebarFooter.contains(conn)) {
    sidebarFooter.insertBefore(conn, sidebarFooter.firstChild);
  }
}

function placeAccount(mobile, accountSlot, sidebarFooter) {
  const accountMount = document.getElementById('account-mount');
  if (!accountMount) return;
  if (mobile) {
    if (!accountSlot.contains(accountMount)) accountSlot.appendChild(accountMount);
    polishBottomAccount();
    return;
  }
  if (sidebarFooter && !sidebarFooter.contains(accountMount)) {
    sidebarFooter.appendChild(accountMount);
  }
}

/** Make account look like the other equal-width tab items. Idempotent — no observer loops. */
export function polishBottomAccount() {
  const mount = document.querySelector('#bottom-account-slot #account-mount');
  const trigger = mount?.querySelector('#account-trigger');
  if (!trigger) return false;

  const labelText = t('nav.account');
  if (trigger.dataset.bottomTab === '1') {
    const label = trigger.querySelector('.nav-label');
    if (label && label.textContent !== labelText) label.textContent = labelText;
    trigger.setAttribute('aria-label', labelText);
    return true;
  }

  trigger.dataset.bottomTab = '1';
  trigger.classList.add('bottom-nav-item', 'bottom-nav-account-btn');
  const label = document.createElement('span');
  label.className = 'nav-label';
  label.textContent = labelText;
  trigger.replaceChildren(icon('account'), label);
  trigger.setAttribute('aria-label', labelText);
  return true;
}

function closeDrawerArtifacts() {
  const sidebar = document.getElementById('sidebar');
  const scrim = document.getElementById('sidebar-scrim');
  sidebar?.classList.remove('open');
  document.body.classList.remove('sidebar-drawer-open');
  if (scrim) {
    scrim.hidden = true;
    scrim.classList.remove('is-open');
    scrim.setAttribute('aria-hidden', 'true');
  }
}

/**
 * Call after decorateNavIcons(). Safe to call once per page.
 * Call polishBottomAccount() again after mountShellChrome().
 */
export function wireBottomNav() {
  const mainNav = document.getElementById('main-nav');
  const sidebarFooter = document.querySelector('.sidebar-footer');
  if (!mainNav) return;

  const bar = ensureBar();
  const accountSlot = document.getElementById('bottom-account-slot');
  rebuildLinks(bar, mainNav);

  const mq = window.matchMedia(MOBILE_MQ);
  const sync = () => {
    const mobile = mq.matches;
    document.body.classList.toggle('has-bottom-nav', mobile);
    bar.hidden = !mobile;
    if (mobile) closeDrawerArtifacts();
    placeAccount(mobile, accountSlot, sidebarFooter);
    placeConnectionStatus(mobile, sidebarFooter);
  };

  mq.addEventListener('change', sync);
  onLanguageChange(() => {
    if (mq.matches) polishBottomAccount();
  });
  window.addEventListener(THEME_CHANGE, () => {
    if (mq.matches) queueMicrotask(() => polishBottomAccount());
  });
  sync();
}
