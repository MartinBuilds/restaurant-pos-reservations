import { api, queryString } from '../api.js';
import { money, percent, quantity, toDateTimeLocalValue, fromDateTimeLocalValue, dateTime } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, loading, errorBox, emptyState,
  handleError, field, toast
} from '../ui.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=pr17-4';

export async function renderReports() {
  setPageMeta(t('page.reports.title'), t('page.reports.subtitle'));
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 1, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  const toLocal = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  await loadReports({ from: `${toLocal(start)}:00`, to: `${toLocal(end)}:00` });
}

async function loadReports(filters) {
  mount(loading(t('common.loading')));
  const fromInput = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.from) });
  const toInput = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.to) });

  try {
    const q = queryString(filters);
    const [summary, byItem, byMethod] = await Promise.all([
      api.get(`/api/admin/reports/sales/summary${q}`),
      api.get(`/api/admin/reports/sales/by-item${q}`),
      api.get(`/api/admin/reports/sales/by-payment-method${q}`)
    ]);

    const tz = summary?.period?.timeZone || byItem?.period?.timeZone || 'Europe/Sofia';
    const itemRows = (byItem.items || []).map((row) => [
      String(row.menuItemId),
      row.menuItemName || '—',
      quantity(row.quantitySold),
      money(row.revenue),
      String(row.paidOrdersCount)
    ]);

    const methodCards = (byMethod.methods || []).map((m) => {
      const width = Math.max(0, Math.min(100, Number(m.percentageOfRevenue) || 0));
      const bar = el('div', { className: 'bar-track' }, [
        el('div', { className: 'bar-fill' })
      ]);
      bar.firstChild.style.width = `${width}%`;
      return el('div', { className: 'card' }, [
        el('div', { className: 'card-label', text: m.method ? `${statusLabel(m.method)} (${m.method})` : '—' }),
        el('div', { className: 'card-value', text: money(m.amount) }),
        el('p', { className: 'muted', text: `${m.paymentCount} · ${percent(m.percentageOfRevenue)}` }),
        bar
      ]);
    });

    mount(el('div', { className: 'stack' }, [
      el('div', { className: 'note-info note' }, [
        document.createTextNode(t('payment.simulationWarning'))
      ]),
      panel(t('msg.period'), [
        el('div', { className: 'filters' }, [
          field(t('col.from'), fromInput),
          field(t('col.to'), toInput),
          el('button', {
            type: 'button', className: 'btn', text: t('common.search'),
            onClick: () => {
              const from = fromDateTimeLocalValue(fromInput.value);
              const to = fromDateTimeLocalValue(toInput.value);
              if (!from || !to) {
                toast(t('msg.requiredFields'), 'error');
                return;
              }
              loadReports({ from, to });
            }
          })
        ]),
        el('p', { className: 'muted', text: `${t('msg.period')}: ${dateTime(summary.period?.from)} → ${dateTime(summary.period?.to)} (${tz})` })
      ]),
      el('div', { className: 'grid grid-4' }, [
        metric(t('reports.revenue'), money(summary.totalRevenue)),
        metric(t('reports.paidOrders'), String(summary.paidOrdersCount)),
        metric(t('reports.soldItems'), String(summary.soldItemsCount)),
        metric(t('reports.avgOrder'), money(summary.averageOrderValue))
      ]),
      panel(t('reports.byMethod'), [
        el('p', { className: 'muted', text: t('reports.total', { amount: money(byMethod.totalRevenue) }) }),
        el('div', { className: 'grid grid-2' }, methodCards.length ? methodCards : [
          emptyState(t('common.empty'))
        ])
      ]),
      panel(t('reports.byItem'), [
        itemRows.length
          ? table(t('reports.salesByItem'), [t('col.id'), t('col.snapshotName'), t('col.qtyShort'), t('col.revenue'), t('col.orders')], itemRows)
          : emptyState(t('msg.noResults'))
      ])
    ]));
  } catch (err) {
    mount(el('div', { className: 'stack' }, [
      panel(t('msg.period'), [
        el('div', { className: 'filters' }, [
          field(t('col.from'), fromInput),
          field(t('col.to'), toInput),
          el('button', {
            type: 'button', className: 'btn', text: t('common.search'),
            onClick: () => loadReports({
              from: fromDateTimeLocalValue(fromInput.value),
              to: fromDateTimeLocalValue(toInput.value)
            })
          })
        ])
      ]),
      errorBox(handleError(err), () => loadReports(filters))
    ]));
  }
}

function metric(label, value) {
  return el('div', { className: 'card' }, [
    el('div', { className: 'card-label', text: label }),
    el('div', { className: 'card-value', text: value })
  ]);
}
