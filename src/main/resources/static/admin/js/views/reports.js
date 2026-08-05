import { api, queryString } from '../api.js';
import { money, percent, quantity, toDateTimeLocalValue, fromDateTimeLocalValue, dateTime } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  handleError, field, toast
} from '../ui.js';

export async function renderReports() {
  setPageMeta('Отчети', 'Оборот само от paid + closed + SERVED · период [from, to)');
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 1, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  const toLocal = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  await loadReports({ from: `${toLocal(start)}:00`, to: `${toLocal(end)}:00` });
}

async function loadReports(filters) {
  mount(loading('Зареждане на отчети...'));
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
        el('div', { className: 'card-label', text: m.method }),
        el('div', { className: 'card-value', text: money(m.amount) }),
        el('p', { className: 'muted', text: `${m.paymentCount} плащания · ${percent(m.percentageOfRevenue)}` }),
        bar
      ]);
    });

    mount(el('div', { className: 'stack' }, [
      el('div', { className: 'note-info note' }, [
        document.createTextNode(`Периодът е [from, to). Часова зона: ${tz}. Включват се само платени, затворени и SERVED поръчки. Данните са от симулационни CASH/CARD payments. Това не е счетоводен или данъчен отчет.`)
      ]),
      panel('Период', [
        el('div', { className: 'filters' }, [
          field('От', fromInput),
          field('До', toInput),
          el('button', {
            type: 'button', className: 'btn', text: 'Генерирай',
            onClick: () => {
              const from = fromDateTimeLocalValue(fromInput.value);
              const to = fromDateTimeLocalValue(toInput.value);
              if (!from || !to) {
                toast('from и to са задължителни.', 'error');
                return;
              }
              loadReports({ from, to });
            }
          })
        ]),
        el('p', { className: 'muted', text: `Избран период: ${dateTime(summary.period?.from)} → ${dateTime(summary.period?.to)} (${tz})` })
      ]),
      el('div', { className: 'grid grid-4' }, [
        metric('Приходи', money(summary.totalRevenue)),
        metric('Платени поръчки', String(summary.paidOrdersCount)),
        metric('Продадени позиции', String(summary.soldItemsCount)),
        metric('Средна стойност', money(summary.averageOrderValue))
      ]),
      panel('По метод на плащане', [
        el('p', { className: 'muted', text: `Общо: ${money(byMethod.totalRevenue)}` }),
        el('div', { className: 'grid grid-2' }, methodCards.length ? methodCards : [
          emptyState('Няма методи.')
        ])
      ]),
      panel('По ястие (исторически snapshot)', [
        itemRows.length
          ? table('Продажби по ястие', ['ID', 'Snapshot име', 'К-во', 'Приход', 'Поръчки'], itemRows)
          : emptyState('Няма продажби за периода.')
      ])
    ]));
  } catch (err) {
    mount(el('div', { className: 'stack' }, [
      panel('Период', [
        el('div', { className: 'filters' }, [
          field('От', fromInput),
          field('До', toInput),
          el('button', {
            type: 'button', className: 'btn', text: 'Генерирай',
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