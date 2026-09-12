import { api, ApiClientError } from '/operations/js/api.js';
import { clear, el } from '/operations/js/dom.js';
import { dateTime, text } from '/operations/js/format.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=fix-tables-board-1';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';

let abort = null;
let busyIds = new Set();
let highlightOrderId = null;

function badgeFor(status) {
  return el('span', {
    className: `badge badge-${status || 'muted'}`,
    text: statusLabel(status) || '—'
  });
}

function column(key, title, orders, renderCard) {
  const count = el('span', { className: 'column-count', text: String(orders.length) });
  const list = el('div', { className: 'column-list' });
  if (!orders.length) {
    list.appendChild(el('div', {
      className: 'column-empty',
      text: t(`kitchen.empty.${key}`)
    }));
  } else {
    orders.forEach((o) => list.appendChild(renderCard(o)));
  }
  return el('section', {
    className: `column column-${key.toLowerCase()}`,
    'aria-label': title
  }, [
    el('div', { className: 'column-head' }, [
      el('h2', { className: 'column-title', text: title }),
      count
    ]),
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
      status: statusLabel(status)
    }), 'success');
    highlightOrderId = order.id;
    await renderQueue({ soft: true });
  } catch (err) {
    if (err && err.name === 'AbortError') {
      // Realtime/refresh may abort the follow-up GET after a successful PATCH.
      try {
        await renderQueue({ soft: true });
      } catch {
        /* ignore secondary abort */
      }
      return;
    }
    if (err instanceof ApiClientError && (err.status === 409 || err.status === 400)) {
      handleError(err);
      try {
        await renderQueue({ soft: true });
      } catch {
        /* ignore */
      }
    } else {
      handleError(err, t('msg.statusNotUpdated'));
      btn.disabled = false;
    }
  } finally {
    busyIds.delete(order.id);
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
  } else if (order.status === 'READY') {
    actions.appendChild(el('p', {
      className: 'ready-note',
      text: t('kitchen.readyWaiting')
    }));
  }

  const flash = highlightOrderId != null && String(highlightOrderId) === String(order.id);
  return el('article', {
    className: flash ? 'order-card flash' : 'order-card',
    dataset: { orderId: order.id }
  }, [
    el('div', { className: 'order-card-head' }, [
      el('h3', { text: text(order.orderNumber) }),
      badgeFor(order.status)
    ]),
    el('p', { className: 'meta', text: t('kitchen.tableMeta', {
      table: text(order.tableNumber),
      time: dateTime(order.createdAt)
    }) }),
    el('ul', { className: 'order-items' }, items.map((it) => el('li', {
      text: `${text(it.menuItemName)} × ${text(it.quantity)}`
    }))),
    actions
  ]);
}

function emptyGuide() {
  return el('div', { className: 'kitchen-empty-guide' }, [
    el('h2', { text: t('kitchen.emptyActive') }),
    el('p', { className: 'muted', text: t('kitchen.emptyGuide') }),
    el('ol', { className: 'kitchen-steps' }, [
      el('li', { text: t('kitchen.step1') }),
      el('li', { text: t('kitchen.step2') }),
      el('li', { text: t('kitchen.step3') })
    ]),
    el('p', { className: 'kitchen-hint', text: t('kitchen.emptyLiveHint') })
  ]);
}

export async function renderQueue(options = {}) {
  setPageRefresh(() => renderQueue({ soft: true }));
  const content = document.getElementById('content');
  if (abort) abort.abort();
  abort = new AbortController();
  const keepBusy = options.soft === true;
  if (!keepBusy) busyIds = new Set();

  if (!options.soft || !content.querySelector('.board-columns, .kitchen-empty-guide')) {
    clear(content);
    content.appendChild(el('div', { className: 'loading', text: t('kitchen.loading') }));
  }

  try {
    const orders = await api.get('/api/kitchen/orders', { signal: abort.signal });
    const list = Array.isArray(orders) ? orders : [];
    const accepted = list.filter((o) => o.status === 'ACCEPTED');
    const cooking = list.filter((o) => o.status === 'COOKING');
    const ready = list.filter((o) => o.status === 'READY');

    clear(content);
    setBanner('');

    const flow = el('div', { className: 'kitchen-flow', 'aria-hidden': 'true' }, [
      el('span', { className: 'kitchen-flow-step is-accepted', text: t('kitchen.col.accepted') }),
      el('span', { className: 'kitchen-flow-sep', text: '→' }),
      el('span', { className: 'kitchen-flow-step is-cooking', text: t('kitchen.col.cooking') }),
      el('span', { className: 'kitchen-flow-sep', text: '→' }),
      el('span', { className: 'kitchen-flow-step is-ready', text: t('kitchen.col.ready') })
    ]);

    if (!list.length) {
      content.append(flow, emptyGuide());
      highlightOrderId = null;
      return;
    }

    const columns = el('div', { className: 'board-columns' }, [
      column('accepted', t('kitchen.col.accepted'), accepted, orderCard),
      column('cooking', t('kitchen.col.cooking'), cooking, orderCard),
      column('ready', t('kitchen.col.ready'), ready, orderCard)
    ]);
    content.append(flow, columns);

    if (highlightOrderId != null) {
      const id = highlightOrderId;
      highlightOrderId = null;
      requestAnimationFrame(() => {
        const card = content.querySelector(`[data-order-id="${id}"]`);
        if (card) {
          card.classList.add('flash');
          card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
      });
    }
  } catch (err) {
    if (err && err.name === 'AbortError') throw err;
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

export function markOrderHighlight(orderId) {
  if (orderId != null) highlightOrderId = orderId;
}
