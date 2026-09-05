import { api, ApiClientError } from '/operations/js/api.js';
import { el } from '/operations/js/dom.js';
import { dateTime, money, text } from '/operations/js/format.js';
import { handleError, toast } from '/operations/js/notifications.js';
import { closeDialog, openDialog } from './ui-shared.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=pr17-4';

function receiptView(payment) {
  const items = payment.items || [];
  return el('div', { className: 'stack' }, [
    el('div', { className: 'sim-note' }, [
      el('strong', { text: t('payment.simulationWarning') })
    ]),
    el('p', { text: t('payment.receipt', { number: text(payment.receiptNumber) }) }),
    el('p', { text: t('payment.order', { number: text(payment.orderNumber) }) }),
    el('p', { text: t('payment.table', { number: text(payment.tableNumber) }) }),
    el('p', { text: t('payment.method', {
      method: payment.method ? `${statusLabel(payment.method)} (${payment.method})` : '—'
    }) }),
    el('p', { text: t('payment.amount', { amount: money(payment.amount) }) }),
    el('p', { text: t('payment.processedBy', { name: text(payment.processedByName) }) }),
    el('p', { text: t('payment.paidAt', { time: dateTime(payment.paidAt) }) }),
    el('p', { text: t('payment.simulated', {
      value: payment.simulated === true ? t('common.yes') : t('common.no')
    }) }),
    el('div', { className: 'table-wrap' }, [
      el('table', { className: 'data' }, [
        el('thead', {}, [el('tr', {}, [
          el('th', { text: t('col.item') }), el('th', { text: t('col.qtyShort') }),
          el('th', { text: t('col.unitPrice') }), el('th', { text: t('col.lineTotal') })
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
    title: t('page.payments.title'),
    body: receiptView(payment),
    footer: el('button', { type: 'button', className: 'btn btn-primary', onClick: () => closeDialog(), text: t('common.close') }),
    openerEl
  });
}

export async function openPaymentDialog(order, { onDone, openerEl }) {
  let submitting = false;
  const methodCash = el('input', { type: 'radio', name: 'pay-method', value: 'CASH', id: 'pay-cash', checked: true });
  const methodCard = el('input', { type: 'radio', name: 'pay-method', value: 'CARD', id: 'pay-card' });

  const body = el('div', { className: 'stack' }, [
    el('div', { className: 'sim-note' }, [
      el('strong', { text: t('payment.simulationWarning') })
    ]),
    el('p', { text: t('payment.orderSummary', {
      number: order.orderNumber,
      amount: money(order.totalAmount)
    }) }),
    el('label', { className: 'row' }, [methodCash, el('span', { text: `${statusLabel('CASH')} (CASH)` })]),
    el('label', { className: 'row' }, [methodCard, el('span', { text: `${statusLabel('CARD')} (CARD)` })])
  ]);

  const submitBtn = el('button', { type: 'button', className: 'btn btn-primary', text: t('common.confirm') });
  submitBtn.addEventListener('click', async () => {
    if (submitting) return;
    if (!window.confirm(t('payment.simulationWarning'))) return;
    const method = methodCard.checked ? 'CARD' : 'CASH';
    submitting = true;
    submitBtn.disabled = true;
    try {
      const payment = await api.post(`/api/waiter/orders/${order.id}/payment`, { method });
      toast(t('msg.paymentSaved'), 'success');
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
        handleError(err, t('payment.failed'));
      }
    } finally {
      submitting = false;
      submitBtn.disabled = false;
    }
  });

  openDialog({
    title: t('action.pay'),
    body,
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn', onClick: () => closeDialog(), text: t('common.cancel') }),
      submitBtn
    ]),
    openerEl
  });
}
