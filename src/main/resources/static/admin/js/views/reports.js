import { api, queryString } from '../api.js';
import { money, percent, quantity, toDateTimeLocalValue, fromDateTimeLocalValue, dateTime } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, loading, errorBox, emptyState,
  handleError, field, toast, reloadButton, setPageRefresh
} from '../ui.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=fix-reports-3';
import { createDatetimePicker } from '/shared/js/datetime-picker.js?v=fix-datetime-2';

let lastFilters = null;

function paymentsCountLabel(count) {
  const n = Number(count) || 0;
  return n === 1 ? t('reports.paymentsOne') : t('reports.paymentsMany', { count: n });
}

export async function renderReports() {
  setPageMeta(t('page.reports.title'), t('page.reports.subtitle'));
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 1, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  const toLocal = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  await loadReports({ from: `${toLocal(start)}:00`, to: `${toLocal(end)}:00` });
}

async function loadReports(filters, { soft = false } = {}) {
  lastFilters = filters;
  if (!soft) mount(loading(t('common.loading')));
  const fromInput = createDatetimePicker({ value: toDateTimeLocalValue(filters.from) });
  const toInput = createDatetimePicker({ value: toDateTimeLocalValue(filters.to) });

  try {
    const q = queryString(filters);
    const [summary, byItem, byMethod] = await Promise.all([
      api.get(`/api/admin/reports/sales/summary${q}`),
      api.get(`/api/admin/reports/sales/by-item${q}`),
      api.get(`/api/admin/reports/sales/by-payment-method${q}`)
    ]);

    const itemRows = (byItem.items || []).map((row) => [
      String(row.menuItemId),
      row.menuItemName || '—',
      quantity(row.quantitySold),
      money(row.revenue),
      String(row.paidOrdersCount)
    ]);

    const methodCards = (byMethod.methods || []).map((m) => {
      const width = Math.max(0, Math.min(100, Number(m.percentageOfRevenue) || 0));
      const kind = String(m.method || '').toLowerCase() === 'card' ? 'card' : 'cash';
      const bar = el('div', { className: 'bar-track' }, [
        el('div', { className: `bar-fill bar-fill-${kind}` })
      ]);
      bar.firstChild.style.width = `${width}%`;
      return el('div', { className: `card report-method report-method-${kind}` }, [
        el('div', { className: 'report-method-top' }, [
          el('div', { className: 'card-label', text: m.method ? statusLabel(m.method) : '—' }),
          el('span', { className: `report-pill report-pill-${kind}`, text: percent(m.percentageOfRevenue) })
        ]),
        el('div', { className: 'card-value', text: money(m.amount) }),
        el('p', { className: 'muted report-method-meta', text: paymentsCountLabel(m.paymentCount) }),
        bar
      ]);
    });

    const refresh = () => loadReports(lastFilters || filters, { soft: true });

    mount(el('div', { className: 'stack reports-page' }, [
      el('div', { className: 'note report-sim-note' }, [
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
        el('p', { className: 'muted report-period-line', text: `${t('msg.period')}: ${dateTime(summary.period?.from)} → ${dateTime(summary.period?.to)}` })
      ], [reloadButton(refresh)]),
      el('div', { className: 'report-metrics' }, [
        metric(t('reports.revenue'), money(summary.totalRevenue), 'revenue'),
        metric(t('reports.paidOrders'), String(summary.paidOrdersCount), 'orders'),
        metric(t('reports.soldItems'), String(summary.soldItemsCount), 'items'),
        metric(t('reports.avgOrder'), money(summary.averageOrderValue), 'avg')
      ]),
      panel(t('reports.byMethod'), [
        el('p', { className: 'muted', text: t('reports.total', { amount: money(byMethod.totalRevenue) }) }),
        el('div', { className: 'grid grid-2 report-methods' }, methodCards.length ? methodCards : [
          emptyState(t('common.empty'))
        ])
      ]),
      panel(t('reports.byItem'), [
        itemRows.length
          ? table(t('reports.salesByItem'), [t('col.id'), t('col.snapshotName'), t('col.qtyShort'), t('col.revenue'), t('col.orders')], itemRows)
          : emptyState(t('msg.noResults'))
      ])
    ]));
    setPageRefresh(refresh);
  } catch (err) {
    mount(el('div', { className: 'stack reports-page' }, [
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
      ], [reloadButton(() => loadReports(filters, { soft: true }))]),
      errorBox(handleError(err), () => loadReports(filters))
    ]));
    setPageRefresh(() => loadReports(filters, { soft: true }));
  }
}

function metric(label, value, tone = 'revenue') {
  return el('div', { className: `report-metric report-metric-${tone}` }, [
    el('p', { className: 'report-metric-label', text: label }),
    el('p', { className: 'report-metric-value', text: value })
  ]);
}
