import { api, ApiClientError } from '/operations/js/api.js';
import { clear, el } from '/operations/js/dom.js';
import { dateTime, text } from '/operations/js/format.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=pr17-4';

let abort = null;
let busyIds = new Set();

function column(title, orders, renderCard) {
  const list = el('div');
  if (!orders.length) {
    list.appendChild(el('div', { className: 'empty', text: t('msg.ordersEmpty') }));
  } else {
    orders.forEach((o) => list.appendChild(renderCard(o)));
  }
  return el('section', { className: 'column', 'aria-label': title }, [
    el('h2', { className: 'column-title', text: title }),
    list
  ]);
}

async function patchStatus(order, status, btn) {
  if (busyIds.has(order.id)) return;
  busyIds.add(order.id);
  btn.disabled = true;
  try {
    await api.patch(`/api/kitchen/orders/${order.id}/status`, { status });
    toast(t('kitchen.orderMoved', {
      number: order.orderNumber,
      status: `${statusLabel(status)} (${status})`
    }), 'success');
    await renderQueue();
  } catch (err) {
    if (err instanceof ApiClientError && (err.status === 409 || err.status === 400)) {
      handleError(err);
      await renderQueue();
    } else {
      handleError(err, t('msg.statusNotUpdated'));
      btn.disabled = false;
      busyIds.delete(order.id);
    }
  }
}

function orderCard(order) {
  const items = order.items || [];
  const actions = el('div', { className: 'actions' });
  if (order.status === 'ACCEPTED') {
    const btn = el('button', {
      type: 'button',
      className: 'btn btn-primary btn-lg',
      text: t('action.startCooking')
    });
    btn.addEventListener('click', () => patchStatus(order, 'COOKING', btn));
    actions.appendChild(btn);
  } else if (order.status === 'COOKING') {
    const btn = el('button', {
      type: 'button',
      className: 'btn btn-accent btn-lg',
      text: t('action.markReady')
    });
    btn.addEventListener('click', () => patchStatus(order, 'READY', btn));
    actions.appendChild(btn);
  }

  return el('article', { className: 'order-card', dataset: { orderId: order.id } }, [
    el('h3', { text: text(order.orderNumber) }),
    el('p', { className: 'meta', text: t('kitchen.tableMeta', {
      table: text(order.tableNumber),
      time: dateTime(order.createdAt)
    }) }),
    el('p', { className: 'meta', text: t('kitchen.statusMeta', {
      status: `${statusLabel(order.status)} (${order.status})`
    }) }),
    el('ul', {}, items.map((it) => el('li', {
      text: `${text(it.menuItemName)} × ${text(it.quantity)}`
    }))),
    actions
  ]);
}

export async function renderQueue() {
  const content = document.getElementById('content');
  if (abort) abort.abort();
  abort = new AbortController();
  busyIds = new Set();

  clear(content);
  content.appendChild(el('div', { className: 'loading', text: t('kitchen.loading') }));

  try {
    const orders = await api.get('/api/kitchen/orders', { signal: abort.signal });
    const list = Array.isArray(orders) ? orders : [];
    const accepted = list.filter((o) => o.status === 'ACCEPTED');
    const cooking = list.filter((o) => o.status === 'COOKING');
    const ready = list.filter((o) => o.status === 'READY');

    clear(content);
    setBanner('');
    if (!list.length) {
      content.appendChild(el('div', { className: 'empty', text: t('kitchen.emptyActive') }));
      return;
    }
    content.appendChild(column(`${t('kitchen.col.accepted')} (ACCEPTED)`, accepted, orderCard));
    content.appendChild(column(`${t('kitchen.col.cooking')} (COOKING)`, cooking, orderCard));
    content.appendChild(column(`${t('kitchen.col.ready')} (READY)`, ready, orderCard));
  } catch (err) {
    if (err && err.name === 'AbortError') return;
    clear(content);
    handleError(err, t('kitchen.loadError'));
    const retry = el('button', { type: 'button', className: 'btn btn-primary', text: t('common.retry') });
    retry.addEventListener('click', () => renderQueue());
    content.appendChild(el('div', { className: 'error-box stack' }, [
      el('p', { text: err.message || t('common.error') }),
      retry
    ]));
  }
}
