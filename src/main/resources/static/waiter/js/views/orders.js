import { api, ApiClientError } from '/operations/js/api.js';
import { clear, el, responsiveDataTable } from '/operations/js/dom.js';
import { dateTime, money, text } from '/operations/js/format.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { openAddItemsDialog } from './order-form.js';
import { openPaymentDialog, showReceipt } from './payment.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';
import { t } from '/shared/js/i18n/i18n.js?v=fix-refresh-1';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-2';

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
  try {
    const orders = await api.get('/api/waiter/orders', { signal: abort.signal });
    clear(content);
    setBanner('');
    if (!orders || !orders.length) {
      content.appendChild(emptyBox(t('msg.ordersEmpty')));
      return;
    }
    const list = el('div', { className: 'stack' });
    orders.forEach((order) => {
      const items = order.items || [];
      const actions = el('div', { className: 'actions' });

      if (!order.closed && (order.status === 'ACCEPTED' || order.status === 'COOKING' || order.status === 'READY')) {
        const addBtn = el('button', { type: 'button', className: 'btn', text: t('action.addItems') });
        addBtn.addEventListener('click', () => openAddItemsDialog(order, {
          openerEl: addBtn,
          onDone: () => renderOrders()
        }));
        actions.appendChild(addBtn);
      }

      if (order.status === 'READY' && !order.closed) {
        const serveBtn = el('button', { type: 'button', className: 'btn btn-accent btn-lg', text: t('action.serve') });
        serveBtn.addEventListener('click', () => markServed(order, serveBtn, () => renderOrders()));
        actions.appendChild(serveBtn);
      }

      if (order.status === 'SERVED' && order.closed === false) {
        const payBtn = el('button', { type: 'button', className: 'btn btn-primary btn-lg', text: t('action.pay') });
        payBtn.addEventListener('click', () => openPaymentDialog(order, {
          openerEl: payBtn,
          onDone: () => renderOrders()
        }));
        actions.appendChild(payBtn);
      }

      if (order.closed === true) {
        const receiptBtn = el('button', { type: 'button', className: 'btn', text: t('action.viewReceipt') });
        receiptBtn.addEventListener('click', async () => {
          try {
            const payment = await api.get(`/api/waiter/orders/${order.id}/payment`);
            showReceipt(payment, receiptBtn);
          } catch (err) {
            handleError(err, t('orders.receiptNotFound'));
          }
        });
        actions.appendChild(receiptBtn);
      }

      list.appendChild(el('article', { className: 'panel' }, [
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
      ]));
    });
    content.appendChild(list);
  } catch (err) {
    if (err && err.name === 'AbortError') return;
    clear(content);
    handleError(err, t('orders.loadError'));
    content.appendChild(errorBox(err.message || t('common.error'), () => renderOrders()));
  }
}
