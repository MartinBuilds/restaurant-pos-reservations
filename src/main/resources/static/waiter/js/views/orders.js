import { api, ApiClientError } from '/operations/js/api.js';
import { clear, el, responsiveDataTable } from '/operations/js/dom.js';
import { dateTime, money, text } from '/operations/js/format.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { openAddItemsDialog } from './order-form.js';
import { openPaymentDialog, showReceipt } from './payment.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';
import { t } from '/shared/js/i18n/i18n.js?v=fix-orders-board-1';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';

let abort = null;

async function markServed(order, btn, reload) {
  if (!window.confirm(t('orders.confirmServe', { number: order.orderNumber }))) return;
  btn.disabled = true;
  try {
    await api.patch(`/api/waiter/orders/${order.id}/status`, { status: 'SERVED' });
    toast(t('msg.orderServed'), 'success');
    await reload();
  } catch (err) {
    if (err instanceof ApiClientError && err.status === 409) {
      handleError(err);
      await reload();
    } else {
      handleError(err, t('msg.statusNotUpdated'));
      btn.disabled = false;
    }
  }
}

function buildActiveCard(order, reload) {
  const items = order.items || [];
  const actions = el('div', { className: 'actions' });

  if (!order.closed && (order.status === 'ACCEPTED' || order.status === 'COOKING' || order.status === 'READY')) {
    const addBtn = el('button', { type: 'button', className: 'btn', text: t('action.addItems') });
    addBtn.addEventListener('click', () => openAddItemsDialog(order, {
      openerEl: addBtn,
      onDone: reload
    }));
    actions.appendChild(addBtn);
  }

  if (order.status === 'SERVED' && order.closed === false) {
    const payBtn = el('button', { type: 'button', className: 'btn btn-primary btn-lg', text: t('action.pay') });
    payBtn.addEventListener('click', () => openPaymentDialog(order, {
      openerEl: payBtn,
      onDone: reload
    }));
    actions.appendChild(payBtn);
  }

  return el('article', { className: 'panel' }, [
    el('div', { className: 'panel-header' }, [
      el('div', {}, [
        el('h2', { text: text(order.orderNumber) }),
        el('p', { className: 'muted', text: t('label.tableMeta', {
          table: text(order.tableNumber),
          time: dateTime(order.createdAt)
        }) })
      ]),
      badge(order.status)
    ]),
    responsiveDataTable(
      [t('col.item'), t('col.qtyShort'), t('col.unitPrice'), t('col.lineTotal')],
      items.map((it) => [
        text(it.menuItemName),
        text(it.quantity),
        money(it.unitPrice),
        money(it.lineTotal)
      ])
    ),
    el('p', { text: t('orders.totalClosed', {
      total: money(order.totalAmount),
      closed: order.closed ? t('common.yes') : t('common.no')
    }) }),
    actions
  ]);
}

function actionCell(order, reload) {
  const wrap = el('div', { className: 'actions' });

  if (order.status === 'READY' && !order.closed) {
    const addBtn = el('button', { type: 'button', className: 'btn', text: t('action.addItems') });
    addBtn.addEventListener('click', () => openAddItemsDialog(order, {
      openerEl: addBtn,
      onDone: reload
    }));
    wrap.appendChild(addBtn);

    const serveBtn = el('button', { type: 'button', className: 'btn btn-accent', text: t('action.serve') });
    serveBtn.addEventListener('click', () => markServed(order, serveBtn, reload));
    wrap.appendChild(serveBtn);
  }

  if (order.status === 'SERVED' && !order.closed) {
    const payBtn = el('button', { type: 'button', className: 'btn btn-primary', text: t('action.pay') });
    payBtn.addEventListener('click', () => openPaymentDialog(order, {
      openerEl: payBtn,
      onDone: reload
    }));
    wrap.appendChild(payBtn);
  }

  if (order.closed) {
    const receiptBtn = el('button', { type: 'button', className: 'btn', text: t('action.viewReceipt') });
    receiptBtn.addEventListener('click', async () => {
      try {
        const payment = await api.get(`/api/waiter/orders/${order.id}/payment`);
        showReceipt(payment, receiptBtn);
      } catch (err) {
        handleError(err, t('orders.receiptNotFound'));
      }
    });
    wrap.appendChild(receiptBtn);
  }

  if (!wrap.childNodes.length) {
    wrap.appendChild(el('span', { className: 'muted', text: '—' }));
  }
  return wrap;
}

function buildReadyPaidTable(orders, reload) {
  if (!orders.length) {
    return el('div', { className: 'empty', text: t('orders.readyPaidEmpty') });
  }

  return responsiveDataTable(
    [
      t('col.order'),
      t('col.table'),
      t('col.status'),
      t('col.total'),
      t('col.updated'),
      t('common.actions')
    ],
    orders.map((order) => [
      text(order.orderNumber),
      text(order.tableNumber),
      badge(order.closed ? 'PAID' : order.status),
      money(order.totalAmount),
      dateTime(order.updatedAt || order.createdAt),
      actionCell(order, reload)
    ]),
    { caption: t('orders.readyPaidTitle') }
  );
}

export async function renderOrders({ soft = false } = {}) {
  setPageRefresh(() => renderOrders({ soft: true }));
  setPageMeta(t('page.orders.title'), t('page.orders.subtitle'));
  const content = document.getElementById('content');
  if (!soft) {
    clear(content);
    content.appendChild(loadingBox());
  }

  if (abort) abort.abort();
  abort = new AbortController();
  const { signal } = abort;
  const reload = () => renderOrders({ soft: true });

  try {
    const [openOrders, history] = await Promise.all([
      api.get('/api/waiter/orders', { signal }),
      api.get('/api/waiter/orders/history?limit=40', { signal })
    ]);

    clear(content);
    setBanner('');

    const open = Array.isArray(openOrders) ? openOrders : [];
    const closed = Array.isArray(history) ? history : [];

    const active = open.filter((o) => !o.closed && o.status !== 'READY');
    const ready = open.filter((o) => !o.closed && o.status === 'READY');
    const readyPaid = [...ready, ...closed];

    const root = el('div', { className: 'orders-board stack' });

    root.appendChild(el('section', { className: 'orders-section' }, [
      el('div', { className: 'orders-section-head' }, [
        el('h2', { className: 'orders-section-title', text: t('orders.activeTitle') }),
        el('span', { className: 'orders-section-count', text: String(active.length) })
      ]),
      active.length
        ? el('div', { className: 'stack' }, active.map((order) => buildActiveCard(order, reload)))
        : emptyBox(t('orders.activeEmpty'))
    ]));

    root.appendChild(el('section', { className: 'orders-section' }, [
      el('div', { className: 'orders-section-head' }, [
        el('h2', { className: 'orders-section-title', text: t('orders.readyPaidTitle') }),
        el('span', { className: 'orders-section-count', text: String(readyPaid.length) })
      ]),
      el('p', { className: 'muted orders-section-hint', text: t('orders.readyPaidHint') }),
      buildReadyPaidTable(readyPaid, reload)
    ]));

    content.appendChild(root);
  } catch (err) {
    if (err && err.name === 'AbortError') return;
    clear(content);
    handleError(err, t('orders.loadError'));
    content.appendChild(errorBox(err.message || t('common.error'), () => renderOrders()));
  }
}
