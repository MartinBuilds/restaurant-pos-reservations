import { el, clear } from './dom.js';

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

export function handleError(err, fallback = 'Възникна грешка.') {
  if (err && err.name === 'AbortError') return;
  const msg = (err && err.message) ? err.message : fallback;
  toast(msg, 'error');
  setBanner(msg, 'error');
}