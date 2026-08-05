import { clear, el } from '/operations/js/dom.js';
import { statusLabel } from '/operations/js/format.js';

let opener = null;

export function setPageMeta(title, subtitle) {
  const t = document.getElementById('page-title');
  const s = document.getElementById('page-subtitle');
  if (t) t.textContent = title;
  if (s) s.textContent = subtitle;
}

export function badge(status) {
  return el('span', { className: `badge badge-${status || 'info'}`, text: `${statusLabel(status)} (${status || '—'})` });
}

export function loadingBox(text = 'Зареждане…') {
  return el('div', { className: 'loading', text });
}

export function emptyBox(text) {
  return el('div', { className: 'empty', text });
}

export function errorBox(message, onRetry) {
  return el('div', { className: 'error-box stack' }, [
    el('p', { text: message }),
    onRetry ? el('button', { type: 'button', className: 'btn btn-primary', onClick: onRetry, text: 'Опитай отново' }) : null
  ]);
}

export function openDialog({ title, body, footer, openerEl }) {
  opener = openerEl || document.activeElement;
  const overlay = document.getElementById('overlay');
  const dialog = document.getElementById('dialog');
  const titleEl = document.getElementById('dialog-title');
  const bodyEl = document.getElementById('dialog-body');
  const footerEl = document.getElementById('dialog-footer');
  titleEl.textContent = title || 'Диалог';
  clear(bodyEl);
  clear(footerEl);
  if (body) bodyEl.appendChild(body);
  if (footer) footerEl.appendChild(footer);
  overlay.hidden = false;
  dialog.hidden = false;
  dialog.querySelector('button, input, select, textarea')?.focus();
}

export function closeDialog() {
  document.getElementById('overlay').hidden = true;
  document.getElementById('dialog').hidden = true;
  clear(document.getElementById('dialog-body'));
  clear(document.getElementById('dialog-footer'));
  if (opener && typeof opener.focus === 'function') opener.focus();
  opener = null;
}

export function wireDialogChrome() {
  const close = () => closeDialog();
  document.getElementById('dialog-close').addEventListener('click', close);
  document.getElementById('overlay').addEventListener('click', close);
  document.addEventListener('keydown', (ev) => {
    if (ev.key === 'Escape' && !document.getElementById('dialog').hidden) {
      close();
    }
  });
}