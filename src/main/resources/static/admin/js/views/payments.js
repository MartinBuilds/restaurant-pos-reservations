import { api, queryString } from '../api.js';
import { money, dateTime, toDateTimeLocalValue, fromDateTimeLocalValue } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, handleError, field
} from '../ui.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=pr19-1';

export async function renderPayments() {
  setPageMeta(t('page.payments.title'), t('page.payments.subtitle'));
  mount(loading(t('common.loading')));
  await reload({});
}

async function reload(filters) {
  try {
    const payments = await api.get(`/api/admin/payments${queryString(filters)}`);
    const method = el('select', {}, [
      el('option', { value: '', text: t('filter.allMethods') }),
      el('option', { value: 'CASH', text: statusLabel('CASH'), selected: filters.method === 'CASH' ? 'true' : null }),
      el('option', { value: 'CARD', text: statusLabel('CARD'), selected: filters.method === 'CARD' ? 'true' : null })
    ]);
    const from = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.from) });
    const to = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.to) });
    const processedById = el('input', { type: 'number', min: '1', value: filters.processedById || '' });

    const rows = payments.map((p) => [
      p.receiptNumber || '—',
      p.orderNumber || String(p.orderId),
      badge(p.method ? statusLabel(p.method) : '—', p.method === 'CASH' ? 'ok' : 'info'),
      money(p.amount),
      p.processedByName || String(p.processedById),
      dateTime(p.paidAt),
      el('button', {
        type: 'button', className: 'btn btn-secondary', text: t('action.details'),
        onClick: () => openReceipt(p.id)
      })
    ]);

    mount(el('div', { className: 'stack' }, [
      el('div', { className: 'note' }, [
        el('strong', { text: t('payment.simulationWarning') })
      ]),
      panel(t('panel.filters'), [
        el('div', { className: 'filters' }, [
          field(t('col.method'), method),
          field(t('col.from'), from),
          field(t('col.to'), to),
          field(t('col.processedById'), processedById),
          el('button', {
            type: 'button', className: 'btn', text: t('common.confirm'),
            onClick: () => reload({
              method: method.value || undefined,
              from: fromDateTimeLocalValue(from.value),
              to: fromDateTimeLocalValue(to.value),
              processedById: processedById.value || undefined
            })
          })
        ])
      ]),
      panel(t('page.payments.title'), [
        payments.length
          ? table(t('page.payments.title'), [t('col.receipt'), t('col.order'), t('col.method'), t('col.amount'), t('col.operator'), t('col.paidAt'), t('common.actions')], rows)
          : emptyState(t('msg.noResults'))
      ], [
        el('button', { type: 'button', className: 'btn btn-secondary', text: t('action.reload'), onClick: () => reload(filters) })
      ])
    ]));
  } catch (err) {
    mount(errorBox(handleError(err), () => reload(filters)));
  }
}

async function openReceipt(id) {
  try {
    const p = await api.get(`/api/admin/payments/${id}`);
    const itemRows = (p.items || []).map((item) => [
      item.menuItemName || '—',
      money(item.unitPrice),
      String(item.quantity),
      money(item.lineTotal)
    ]);
    openDialog({
      title: t('payments.receiptTitle', { number: p.receiptNumber }),
      body: el('div', { className: 'stack' }, [
        el('div', { className: 'note' }, t('payment.simulationWarning')),
        el('p', { text: t('payment.simulated', {
          value: p.simulated ? t('common.yes') : t('common.no')
        }) }),
        el('p', { text: t('payments.orderTableLine', { order: p.orderNumber, table: p.tableNumber }) }),
        el('p', { text: t('payments.methodAmountLine', { method: p.method ? statusLabel(p.method) : '—', amount: money(p.amount) }) }),
        el('p', { text: t('payments.operatorPaidLine', { name: p.processedByName, paidAt: dateTime(p.paidAt) }) }),
        el('p', { text: t('payments.orderStatusLine', {
          status: p.orderStatus ? statusLabel(p.orderStatus) : '—',
          closed: p.orderClosed ? t('common.yes') : t('common.no')
        }) }),
        itemRows.length
          ? table(t('payments.itemsSnapshot'), [t('col.item'), t('col.unitPrice'), t('col.qtyShort'), t('col.lineTotal')], itemRows)
          : emptyState(t('common.empty'))
      ]),
      footerButtons: [
        el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.close'), onClick: () => closeDialog() })
      ]
    });
  } catch (err) {
    handleError(err);
  }
}
