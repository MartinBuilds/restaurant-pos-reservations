import { ApiClientError } from './api.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr20-4';
import { closeOverlayDialog, openOverlayDialog } from '/shared/js/dialog-sheet.js?v=pr20-4';

const content = () => document.getElementById('content');
const toastRegion = () => document.getElementById('toast-region');
const statusRegion = () => document.getElementById('status-region');
const dialog = () => document.getElementById('dialog');
const dialogBody = () => document.getElementById('dialog-body');
const dialogFooter = () => document.getElementById('dialog-footer');
const dialogTitle = () => document.getElementById('dialog-title');
const overlay = () => document.getElementById('overlay');

let lastFocus = null;
let escapeHandler = null;

export function setPageMeta(title, subtitle) {
  document.getElementById('page-title').textContent = title;
  document.getElementById('page-subtitle').textContent = subtitle || '';
  document.title = `${title} — ${t('admin.titleSuffix')}`;
}

export function clear(node) {
  while (node.firstChild) node.removeChild(node.firstChild);
}

export function el(tag, props = {}, children = []) {
  const node = document.createElement(tag);
  Object.entries(props).forEach(([key, value]) => {
    if (value == null || value === false) return;
    if (key === 'className') node.className = value;
    else if (key === 'text') node.textContent = value;
    else if (key === 'htmlFor') node.htmlFor = value;
    else if (key.startsWith('on') && typeof value === 'function') {
      node.addEventListener(key.slice(2).toLowerCase(), value);
    } else if (key === 'dataset') {
      Object.entries(value).forEach(([dKey, dVal]) => {
        node.dataset[dKey] = dVal;
      });
    } else {
      node.setAttribute(key, value === true ? '' : String(value));
    }
  });
  (Array.isArray(children) ? children : [children]).forEach((child) => {
    if (child == null || child === false) return;
    if (typeof child === 'string' || typeof child === 'number') {
      node.appendChild(document.createTextNode(String(child)));
    } else {
      node.appendChild(child);
    }
  });
  return node;
}

export function mount(viewRoot) {
  const root = content();
  clear(root);
  root.appendChild(viewRoot);
  root.focus({ preventScroll: true });
}

export function loading(message) {
  return el('div', { className: 'loading', role: 'status' }, message || t('common.loading'));
}

export function emptyState(message, action) {
  const box = el('div', { className: 'empty' }, [el('p', { text: message })]);
  if (action) box.appendChild(action);
  return box;
}

export function errorBox(message, onRetry) {
  const box = el('div', { className: 'error-box', role: 'alert' }, [
    el('p', { text: message })
  ]);
  if (onRetry) {
    box.appendChild(el('button', {
      type: 'button',
      className: 'btn btn-secondary',
      onClick: onRetry,
      text: t('common.retry')
    }));
  }
  return box;
}

export function toast(message, type = 'info') {
  const region = toastRegion();
  const item = el('div', { className: `toast toast-${type}`, role: 'status' }, message);
  region.appendChild(item);
  setTimeout(() => item.remove(), 4500);
}

export function setBanner(message, type = 'info') {
  const region = statusRegion();
  clear(region);
  if (!message) return;
  region.appendChild(el('div', { className: `banner banner-${type}`, role: 'status' }, message));
}

export function badge(text, kind = 'muted') {
  return el('span', { className: `badge badge-${kind}`, text: String(text) });
}

export function panel(title, bodyNodes, actions = []) {
  return el('section', { className: 'panel' }, [
    el('div', { className: 'panel-header' }, [
      el('h2', { className: 'panel-title', text: title }),
      el('div', { className: 'row-actions' }, actions)
    ]),
    ...(Array.isArray(bodyNodes) ? bodyNodes : [bodyNodes])
  ]);
}

export function field(labelText, control, errorText) {
  const id = control.id || `field-${Math.random().toString(36).slice(2, 9)}`;
  control.id = id;
  return el('div', { className: 'field' }, [
    el('label', { htmlFor: id, text: labelText }),
    control,
    errorText ? el('div', { className: 'field-error', text: errorText }) : null
  ]);
}

export function openDialog({ title, body, footerButtons = [], onClose }) {
  lastFocus = document.activeElement;
  dialogTitle().textContent = title;
  clear(dialogBody());
  dialogBody().appendChild(body);
  clear(dialogFooter());
  footerButtons.forEach((btn) => dialogFooter().appendChild(btn));
  openOverlayDialog(dialog(), overlay());
  document.body.classList.add('dialog-open');

  if (escapeHandler) {
    document.removeEventListener('keydown', escapeHandler);
    escapeHandler = null;
  }

  const close = () => {
    closeDialog();
    if (onClose) onClose();
  };
  document.getElementById('dialog-close').onclick = close;
  overlay().onclick = close;
  escapeHandler = (event) => {
    if (event.key === 'Escape') close();
  };
  document.addEventListener('keydown', escapeHandler);
  const focusable = dialog().querySelector('button, [href], input, select, textarea');
  if (focusable) focusable.focus();
}

export function closeDialog() {
  if (escapeHandler) {
    document.removeEventListener('keydown', escapeHandler);
    escapeHandler = null;
  }
  document.body.classList.remove('dialog-open');
  const focus = lastFocus;
  lastFocus = null;
  closeOverlayDialog(dialog(), overlay()).then(() => {
    clear(dialogBody());
    clear(dialogFooter());
    if (focus && typeof focus.focus === 'function') focus.focus();
  });
}

export function confirmDialog({ title, message, confirmLabel, danger = false }) {
  return new Promise((resolve) => {
    const wasOpen = !dialog().hidden;
    const snapshot = wasOpen
      ? {
        title: dialogTitle().textContent,
        body: Array.from(dialogBody().childNodes),
        footer: Array.from(dialogFooter().childNodes)
      }
      : null;

    let settled = false;
    const settle = (result) => {
      if (settled) return;
      settled = true;
      if (snapshot) {
        dialogTitle().textContent = snapshot.title;
        clear(dialogBody());
        snapshot.body.forEach((node) => dialogBody().appendChild(node));
        clear(dialogFooter());
        snapshot.footer.forEach((node) => dialogFooter().appendChild(node));
        openOverlayDialog(dialog(), overlay());
        document.body.classList.add('dialog-open');
        if (escapeHandler) {
          document.removeEventListener('keydown', escapeHandler);
          escapeHandler = null;
        }
        const closeParent = () => closeDialog();
        document.getElementById('dialog-close').onclick = closeParent;
        overlay().onclick = closeParent;
        escapeHandler = (event) => {
          if (event.key === 'Escape') closeParent();
        };
        document.addEventListener('keydown', escapeHandler);
      } else {
        closeDialog();
      }
      resolve(result);
    };

    openDialog({
      title,
      body: el('p', { text: message }),
      footerButtons: [
        el('button', {
          type: 'button',
          className: 'btn btn-secondary',
          text: t('common.cancel'),
          onClick: () => settle(false)
        }),
        el('button', {
          type: 'button',
          className: danger ? 'btn btn-danger' : 'btn',
          text: confirmLabel || t('common.confirm'),
          onClick: () => settle(true)
        })
      ],
      onClose: () => settle(false)
    });
  });
}

export function handleError(err, fallback) {
  if (err && err.name === 'AbortError') return;
  const message = err instanceof ApiClientError ? err.message : (fallback || t('common.error'));
  toast(message, 'error');
  return message;
}

export function table(captionText, headers, rows) {
  const tableEl = el('table', { className: 'responsive-data-table' });
  tableEl.appendChild(el('caption', { text: captionText }));
  const thead = el('thead', {}, [
    el('tr', {}, headers.map((h) => el('th', { scope: 'col', text: h })))
  ]);
  const tbody = el('tbody');
  const labels = headers.map((h) => String(h || ''));
  rows.forEach((cells) => {
    tbody.appendChild(el('tr', {}, cells.map((cell, index) => {
      const label = labels[index] || '';
      const isActions = !label || /actions|действия/i.test(label);
      const td = el('td', {
        className: isActions ? 'is-actions' : null,
        'data-label': label
      });
      if (cell == null) td.textContent = '—';
      else if (typeof cell === 'string' || typeof cell === 'number') td.textContent = String(cell);
      else td.appendChild(cell);
      return td;
    })));
  });
  tableEl.appendChild(thead);
  tableEl.appendChild(tbody);
  return el('div', { className: 'table-wrap' }, [tableEl]);
}