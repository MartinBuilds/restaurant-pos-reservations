import { clear, el } from '/operations/js/dom.js';
import { statusLabel } from '/operations/js/format.js';
import { closeOverlayDialog, openOverlayDialog } from '/shared/js/dialog-sheet.js?v=fix-receipt-dialog-1';
import { t } from '/shared/js/i18n/i18n.js?v=fix-orders-board-1';

let opener = null;

export function setPageMeta(title, subtitle) {
  const titleEl = document.getElementById('page-title');
  const s = document.getElementById('page-subtitle');
  if (titleEl) titleEl.textContent = title;
  if (s) s.textContent = subtitle;
  document.title = `${title} — ${t('waiter.titleSuffix')}`;
}

export function badge(status) {
  return el('span', { className: `badge badge-${status || 'info'}`, text: statusLabel(status) || '—' });
}

export function loadingBox(text) {
  return el('div', { className: 'loading', text: text || t('common.loading') });
}

export function emptyBox(text) {
  return el('div', { className: 'empty', text: text || t('common.empty') });
}

export function errorBox(message, onRetry) {
  return el('div', { className: 'error-box' }, [
    el('p', { text: message || t('common.error') }),
    onRetry ? el('button', {
      type: 'button',
      className: 'btn',
      onClick: onRetry,
      text: t('common.retry')
    }) : null
  ]);
}

export function openDialog({ title, body, footer, openerEl }) {
  opener = openerEl || document.activeElement;
  const overlay = document.getElementById('overlay');
  const dialog = document.getElementById('dialog');
  const titleEl = document.getElementById('dialog-title');
  const bodyEl = document.getElementById('dialog-body');
  const footerEl = document.getElementById('dialog-footer');
  titleEl.textContent = title || t('common.dialog');
  clear(bodyEl);
  clear(footerEl);
  if (body) bodyEl.appendChild(body);
  if (footer) footerEl.appendChild(footer);
  openOverlayDialog(dialog, overlay);
  document.body.classList.add('dialog-open');
  dialog.querySelector('button, input, select, textarea')?.focus();
}

export function closeDialog() {
  const overlay = document.getElementById('overlay');
  const dialog = document.getElementById('dialog');
  document.body.classList.remove('dialog-open');
  const focus = opener;
  opener = null;
  return closeOverlayDialog(dialog, overlay).then(() => {
    // Skip clearing if another openDialog already reopened this dialog.
    if (!dialog.hidden) return;
    clear(document.getElementById('dialog-body'));
    clear(document.getElementById('dialog-footer'));
    if (focus && typeof focus.focus === 'function') focus.focus();
  });
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
