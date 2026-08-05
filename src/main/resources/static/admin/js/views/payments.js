import { api, queryString } from '../api.js';
import { money, dateTime, toDateTimeLocalValue, fromDateTimeLocalValue } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, handleError, field
} from '../ui.js';

export async function renderPayments() {
  setPageMeta('Плащания', 'Read-only история на симулационни CASH/CARD плащания');
  mount(loading());
  await reload({});
}

async function reload(filters) {
  try {
    const payments = await api.get(`/api/admin/payments${queryString(filters)}`);
    const method = el('select', {}, [
      el('option', { value: '', text: 'Всички методи' }),
      el('option', { value: 'CASH', text: 'CASH', selected: filters.method === 'CASH' ? 'true' : null }),
      el('option', { value: 'CARD', text: 'CARD', selected: filters.method === 'CARD' ? 'true' : null })
    ]);
    const from = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.from) });
    const to = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.to) });
    const processedById = el('input', { type: 'number', min: '1', value: filters.processedById || '' });

    const rows = payments.map((p) => [
      p.receiptNumber || '—',
      p.orderNumber || String(p.orderId),
      badge(p.method || '—', p.method === 'CASH' ? 'ok' : 'info'),
      money(p.amount),
      p.processedByName || String(p.processedById),
      dateTime(p.paidAt),
      el('button', {
        type: 'button', className: 'btn btn-secondary', text: 'Детайли',
        onClick: () => openReceipt(p.id)
      })
    ]);

    mount(el('div', { className: 'stack' }, [
      el('div', { className: 'note' }, [
        el('strong', { text: 'Симулационно плащане. ' }),
        document.createTextNode('Не е реален фискален бон, не е банкова транзакция. CARD е само симулация — няма card fields.')
      ]),
      panel('Филтри', [
        el('div', { className: 'filters' }, [
          field('Метод', method),
          field('От', from),
          field('До', to),
          field('Processed by ID', processedById),
          el('button', {
            type: 'button', className: 'btn', text: 'Приложи',
            onClick: () => reload({
              method: method.value || undefined,
              from: fromDateTimeLocalValue(from.value),
              to: fromDateTimeLocalValue(to.value),
              processedById: processedById.value || undefined
            })
          })
        ])
      ]),
      panel('Плащания', [
        payments.length
          ? table('Плащания', ['Касова бележка', 'Поръчка', 'Метод', 'Сума', 'Оператор', 'Платено на', ''], rows)
          : emptyState('Няма плащания за избраните филтри.')
      ], [
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Презареди', onClick: () => reload(filters) })
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
      title: `Бележка ${p.receiptNumber}`,
      body: el('div', { className: 'stack' }, [
        el('div', { className: 'note' }, 'Симулационно плащане · не е фискален бон · не е банкова транзакция'),
        el('p', { text: `simulated=${String(p.simulated)}` }),
        el('p', { text: `Поръчка: ${p.orderNumber} · маса #${p.tableNumber}` }),
        el('p', { text: `Метод: ${p.method} · сума: ${money(p.amount)}` }),
        el('p', { text: `Оператор: ${p.processedByName} · ${dateTime(p.paidAt)}` }),
        el('p', { text: `Статус на поръчката: ${p.orderStatus} · closed=${String(p.orderClosed)}` }),
        itemRows.length
          ? table('Позиции (snapshot)', ['Ястие', 'Ед. цена', 'К-во', 'Ред'], itemRows)
          : emptyState('Няма позиции.')
      ]),
      footerButtons: [
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Затвори', onClick: () => closeDialog() })
      ]
    });
  } catch (err) {
    handleError(err);
  }
}