/**
 * Lightweight Sonner-like toasts (HerdMind: top-center, richColors, closeButton).
 * No external dependency — mirrors Sonner richColors tokens.
 */
import { clear, el } from '/shared/js/dom-lite.js';

const DEFAULT_MS = 4000;

const ICONS = {
  success: '<path d="M20 6 9 17l-5-5" fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round"/>',
  error: '<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><path d="M15 9 9 15M9 9l6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
  info: '<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2"/><path d="M12 8h.01M11 12h1v4h1" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>'
};

function ensureRegion() {
  let region = document.getElementById('toast-region');
  if (!region) {
    region = el('div', {
      id: 'toast-region',
      className: 'toast-region',
      role: 'region',
      'aria-live': 'polite',
      'aria-label': 'Notifications'
    });
    document.body.appendChild(region);
  }
  return region;
}

function iconSvg(kind) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('width', '18');
  svg.setAttribute('height', '18');
  svg.setAttribute('aria-hidden', 'true');
  svg.classList.add('toast-icon');
  svg.innerHTML = ICONS[kind] || ICONS.info;
  return svg;
}

function showToast(message, type = 'success') {
  const text = message == null ? '' : String(message);
  if (!text) return null;
  const kind = ['success', 'error', 'info'].includes(type) ? type : 'info';
  const region = ensureRegion();

  const closeBtn = el('button', {
    type: 'button',
    className: 'toast-close',
    'aria-label': 'Close'
  }, ['×']);

  const item = el('div', {
    className: `toast toast-${kind}`,
    role: kind === 'error' ? 'alert' : 'status',
    'data-type': kind
  }, [
    iconSvg(kind),
    el('div', { className: 'toast-title' }, [text]),
    closeBtn
  ]);

  const dismiss = () => {
    if (!item.isConnected) return;
    item.classList.remove('is-visible');
    item.classList.add('is-leaving');
    window.setTimeout(() => item.remove(), 220);
  };

  closeBtn.addEventListener('click', (event) => {
    event.stopPropagation();
    dismiss();
  });

  region.prepend(item);
  requestAnimationFrame(() => item.classList.add('is-visible'));
  const timer = window.setTimeout(dismiss, DEFAULT_MS);
  item.addEventListener('mouseenter', () => window.clearTimeout(timer), { once: true });
  return item;
}

export function toast(message, type = 'success') {
  return showToast(message, type);
}

toast.success = (message) => showToast(message, 'success');
toast.error = (message) => showToast(message, 'error');
toast.info = (message) => showToast(message, 'info');

/** Legacy status strip: clear only — feedback goes through top toasts. */
export function setBanner(message, type = 'info') {
  const region = document.getElementById('status-region');
  if (region) clear(region);
  if (message) {
    const kind = type === 'error' ? 'error' : type === 'success' ? 'success' : 'info';
    showToast(String(message), kind);
  }
}

export function clearBanner() {
  const region = document.getElementById('status-region');
  if (region) clear(region);
}

export function sameJson(a, b) {
  return JSON.stringify(a) === JSON.stringify(b);
}

/** Returns true when payload is unchanged (and shows blue info toast). */
export function toastIfUnchanged(baseline, next, message) {
  if (!sameJson(baseline, next)) return false;
  toast.info(message);
  return true;
}
