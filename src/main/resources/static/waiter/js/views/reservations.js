import { api, queryString } from '/operations/js/api.js';
import { clear, el, responsiveDataTable } from '/operations/js/dom.js';
import { dateTime, text, toLocalDateTimeInputValue } from '/operations/js/format.js';
import { handleError, setBanner } from '/operations/js/notifications.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=fix-refresh-1';
import { refreshPage, setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-2';

let abort = null;

function defaultRange() {
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  const to = new Date(from);
  to.setDate(to.getDate() + 1);
  return {
    from: toLocalDateTimeInputValue(from),
    to: toLocalDateTimeInputValue(to)
  };
}

export async function renderReservations() {
  setPageMeta(t('page.reservations.title'), t('page.reservations.waiterSubtitle'));
  const content = document.getElementById('content');
  clear(content);

  const range = defaultRange();
  const fromInput = el('input', { type: 'datetime-local', id: 'res-from', value: range.from });
  const toInput = el('input', { type: 'datetime-local', id: 'res-to', value: range.to });
  const tableInput = el('input', { type: 'number', id: 'res-table', min: '1', placeholder: t('col.tableId') });
  const statusInput = el('select', { id: 'res-status' }, [
    el('option', { value: '', text: t('filter.allStatuses') }),
    ...['CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'].map((s) => el('option', {
      value: s,
      text: statusLabel(s)
    }))
  ]);
  const resultHost = el('div', { id: 'res-results' });
  const loadBtn = el('button', {
    type: 'button',
    className: 'btn btn-primary btn-reload',
    text: t('action.reload')
  });

  const load = async ({ soft = false } = {}) => {
    if (!soft) {
      clear(resultHost);
      resultHost.appendChild(loadingBox());
    }
    if (abort) abort.abort();
    abort = new AbortController();
    try {
      const q = queryString({
        from: fromInput.value,
        to: toInput.value,
        tableId: tableInput.value || undefined,
        status: statusInput.value || undefined
      });
      const rows = await api.get(`/api/waiter/reservations/schedule${q}`, { signal: abort.signal });
      clear(resultHost);
      setBanner('');
      if (!rows || !rows.length) {
        resultHost.appendChild(emptyBox(t('msg.noResults')));
        return;
      }
      const headers = [
        t('col.number'), t('col.table'), t('col.client'),
        t('col.start'), t('col.end'), t('msg.guests'), t('col.status')
      ];
      resultHost.appendChild(responsiveDataTable(headers, rows.map((r) => [
        text(r.reservationNumber),
        text(r.tableNumber),
        text(r.clientName),
        dateTime(r.startTime),
        dateTime(r.endTime),
        text(r.guestCount),
        badge(r.status)
      ])));
    } catch (err) {
      if (err && err.name === 'AbortError') return;
      clear(resultHost);
      handleError(err, t('reservations.scheduleLoadError'));
      resultHost.appendChild(errorBox(err.message || t('common.error'), () => load()));
    }
  };

  setPageRefresh(() => load({ soft: true }));
  loadBtn.addEventListener('click', () => { refreshPage({ source: 'button' }); });

  content.appendChild(el('div', { className: 'panel stack' }, [
    el('p', { className: 'muted', text: t('reservations.waiterReadonlyNote') }),
    el('div', { className: 'grid grid-filters' }, [
      el('label', { className: 'field' }, [el('span', { text: t('col.from') }), fromInput]),
      el('label', { className: 'field' }, [el('span', { text: t('col.to') }), toInput]),
      el('label', { className: 'field' }, [el('span', { text: t('col.tableId') }), tableInput]),
      el('label', { className: 'field' }, [el('span', { text: t('col.status') }), statusInput])
    ]),
    el('div', { className: 'actions' }, [loadBtn]),
    resultHost
  ]));

  await load();
}
