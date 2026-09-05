import { el, clear } from './dom.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr17-4';

export function toast(message, type = 'info') {
  const region = document.getElementById('toast-region');
  if (!region) return;
  const item = el('div', { className: `toast toast-${type}`, role: 'status' }, [String(message)]);
  region.appendChild(item);
  setTimeout(() => item.remove(), 4500);
}

export function setBanner(message, type = 'info') {
  const region = document.getElementById('status-region');
  if (!region) return;
  clear(region);
  if (!message) return;
  region.appendChild(el('div', { className: `banner banner-${type}` }, [String(message)]));
}

export function handleError(err, fallback = t('common.error')) {
  if (err && err.name === 'AbortError') return;
  const msg = (err && err.message) ? err.message : fallback;
  toast(msg, 'error');
  setBanner(msg, 'error');
}