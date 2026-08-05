import { api, ApiClientError } from '/operations/js/api.js';
import { el } from '/operations/js/dom.js';
import { dateTime, money, text } from '/operations/js/format.js';
import { handleError, toast } from '/operations/js/notifications.js';
import { closeDialog, openDialog } from './ui-shared.js';

function receiptView(payment) {
  const items = payment.items || [];
  return el('div', { className: 'stack' }, [
    el('div', { className: 'sim-note' }, [
      el('strong', { text: 'Симулационно плащане' }),
      el('p', { text: 'Това не е реален фискален бон или банкова транзакция.' })
    ]),
    el('p', { text: `Бон: ${text(payment.receiptNumber)}` }),
    el('p', { text: `Поръчка: ${text(payment.orderNumber)}` }),
    el('p', { text: `Маса: ${text(payment.tableNumber)}` }),
    el('p', { text: `Метод: ${text(payment.method)}` }),
    el('p', { text: `Сума: ${money(payment.amount)}` }),
    el('p', { text: `Обработено от: ${text(payment.processedByName)}` }),
    el('p', { text: `Платено на: ${dateTime(payment.paidAt)}` }),
    el('p', { text: `simulated=${payment.simulated === true ? 'true' : String(payment.simulated)}` }),
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
    ])
  ]);
}

export function showReceipt(payment, openerEl) {
  openDialog({
    title: 'Симулационен бон',
    body: receiptView(payment),
    footer: el('button', { type: 'button', className: 'btn btn-primary', onClick: () => closeDialog(), text: 'Затвори' }),
    openerEl
  });
}

export async function openPaymentDialog(order, { onDone, openerEl }) {
  let submitting = false;
  const methodCash = el('input', { type: 'radio', name: 'pay-method', value: 'CASH', id: 'pay-cash', checked: true });
  const methodCard = el('input', { type: 'radio', name: 'pay-method', value: 'CARD', id: 'pay-card' });

  const body = el('div', { className: 'stack' }, [
    el('div', { className: 'sim-note' }, [
      el('strong', { text: 'Симулационно плащане' }),
      el('p', { text: 'CASH и CARD са симулация. Няма данни за карта.' })
    ]),
    el('p', { text: `Поръчка ${order.orderNumber} · сума ${money(order.totalAmount)}` }),
    el('label', { className: 'row' }, [methodCash, el('span', { text: 'CASH — симулация' })]),
    el('label', { className: 'row' }, [methodCard, el('span', { text: 'CARD — симулация' })])
  ]);

  const submitBtn = el('button', { type: 'button', className: 'btn btn-primary', text: 'Потвърди плащане' });
  submitBtn.addEventListener('click', async () => {
    if (submitting) return;
    if (!window.confirm('Потвърждавате ли симулационното плащане?')) return;
    const method = methodCard.checked ? 'CARD' : 'CASH';
    submitting = true;
    submitBtn.disabled = true;
    try {
      const payment = await api.post(`/api/waiter/orders/${order.id}/payment`, { method });
      toast('Плащането е записано.', 'success');
      closeDialog();
      showReceipt(payment, openerEl);
      if (onDone) await onDone();
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 409) {
        handleError(err);
        try {
          const existing = await api.get(`/api/waiter/orders/${order.id}/payment`);
          closeDialog();
          showReceipt(existing, openerEl);
        } catch { /* ignore */ }
        if (onDone) await onDone();
      } else {
        handleError(err, 'Плащането неуспешно.');
      }
    } finally {
      submitting = false;
      submitBtn.disabled = false;
    }
  });

  openDialog({
    title: 'Симулационно плащане',
    body,
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn', onClick: () => closeDialog(), text: 'Отказ' }),
      submitBtn
    ]),
    openerEl
  });
}