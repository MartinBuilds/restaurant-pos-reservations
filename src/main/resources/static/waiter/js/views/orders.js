import { api, ApiClientError } from '/operations/js/api.js';
import { clear, el } from '/operations/js/dom.js';
import { dateTime, money, text } from '/operations/js/format.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { openAddItemsDialog } from './order-form.js';
import { openPaymentDialog, showReceipt } from './payment.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';

let abort = null;

async function markServed(order, btn, reload) {
  if (!window.confirm(`Маркиране на ${order.orderNumber} като SERVED?`)) return;
  btn.disabled = true;
  try {
    await api.patch(`/api/waiter/orders/${order.id}/status`, { status: 'SERVED' });
    toast('Поръчката е маркирана като сервирана.', 'success');
    await reload();
  } catch (err) {
    if (err instanceof ApiClientError && err.status === 409) {
      handleError(err);
      await reload();
    } else {
      handleError(err, 'Статусът не беше обновен.');
      btn.disabled = false;
    }
  }
}

export async function renderOrders() {
  setPageMeta('Поръчки', 'Отворени поръчки, сервиране и плащане');
  const content = document.getElementById('content');
  clear(content);
  content.appendChild(loadingBox());

  if (abort) abort.abort();
  abort = new AbortController();
  try {
    const orders = await api.get('/api/waiter/orders', { signal: abort.signal });
    clear(content);
    setBanner('');
    if (!orders || !orders.length) {
      content.appendChild(emptyBox('Няма отворени поръчки.'));
      return;
    }
    const list = el('div', { className: 'stack' });
    orders.forEach((order) => {
      const items = order.items || [];
      const actions = el('div', { className: 'actions' });

      if (!order.closed && (order.status === 'ACCEPTED' || order.status === 'COOKING' || order.status === 'READY')) {
        const addBtn = el('button', { type: 'button', className: 'btn', text: 'Добави артикули' });
        addBtn.addEventListener('click', () => openAddItemsDialog(order, {
          openerEl: addBtn,
          onDone: () => renderOrders()
        }));
        actions.appendChild(addBtn);
      }

      if (order.status === 'READY' && !order.closed) {
        const serveBtn = el('button', { type: 'button', className: 'btn btn-accent btn-lg', text: 'Маркирай като сервирана' });
        serveBtn.addEventListener('click', () => markServed(order, serveBtn, () => renderOrders()));
        actions.appendChild(serveBtn);
      }

      if (order.status === 'SERVED' && order.closed === false) {
        const payBtn = el('button', { type: 'button', className: 'btn btn-primary btn-lg', text: 'Плащане (симулация)' });
        payBtn.addEventListener('click', () => openPaymentDialog(order, {
          openerEl: payBtn,
          onDone: () => renderOrders()
        }));
        actions.appendChild(payBtn);
      }

      if (order.closed === true) {
        const receiptBtn = el('button', { type: 'button', className: 'btn', text: 'Виж бон' });
        receiptBtn.addEventListener('click', async () => {
          try {
            const payment = await api.get(`/api/waiter/orders/${order.id}/payment`);
            showReceipt(payment, receiptBtn);
          } catch (err) {
            handleError(err, 'Бонът не е намерен.');
          }
        });
        actions.appendChild(receiptBtn);
      }

      list.appendChild(el('article', { className: 'panel' }, [
        el('div', { className: 'panel-header' }, [
          el('div', {}, [
            el('h2', { text: text(order.orderNumber) }),
            el('p', { className: 'muted', text: `Маса ${text(order.tableNumber)} · ${dateTime(order.createdAt)}` })
          ]),
          badge(order.status)
        ]),
        el('div', { className: 'table-wrap' }, [
          el('table', { className: 'data' }, [
            el('thead', {}, [el('tr', {}, [
              el('th', { text: 'Артикул' }), el('th', { text: 'Кол.' }),
              el('th', { text: 'Ед. цена' }), el('th', { text: 'Ред' })
            ])]),
            el('tbody', {}, items.map((it) => el('tr', {}, [
              el('td', { text: text(it.menuItemName) }),
              el('td', { text: text(it.quantity) }),
              el('td', { text: money(it.unitPrice) }),
              el('td', { text: money(it.lineTotal) })
            ])))
          ])
        ]),
        el('p', { text: `Общо: ${money(order.totalAmount)} · closed=${order.closed}` }),
        actions
      ]));
    });
    content.appendChild(list);
  } catch (err) {
    if (err && err.name === 'AbortError') return;
    clear(content);
    handleError(err, 'Поръчките не можаха да се заредят.');
    content.appendChild(errorBox(err.message || 'Грешка', () => renderOrders()));
  }
}