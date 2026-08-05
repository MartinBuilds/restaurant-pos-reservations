import { api, queryString } from '/operations/js/api.js';
import { clear, el } from '/operations/js/dom.js';
import { dateTime, text, toLocalDateTimeInputValue } from '/operations/js/format.js';
import { handleError, setBanner } from '/operations/js/notifications.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';

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
  setPageMeta('Резервации', 'Read-only график (Europe/Sofia LocalDateTime, без timezone offset)');
  const content = document.getElementById('content');
  clear(content);

  const range = defaultRange();
  const fromInput = el('input', { type: 'datetime-local', id: 'res-from', value: range.from });
  const toInput = el('input', { type: 'datetime-local', id: 'res-to', value: range.to });
  const tableInput = el('input', { type: 'number', id: 'res-table', min: '1', placeholder: 'tableId' });
  const statusInput = el('select', { id: 'res-status' }, [
    el('option', { value: '', text: 'Всички статуси' }),
    ...['PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'].map((s) => el('option', { value: s, text: s }))
  ]);
  const resultHost = el('div', { id: 'res-results' });
  const loadBtn = el('button', { type: 'button', className: 'btn btn-primary', text: 'Зареди' });

  const load = async () => {
    clear(resultHost);
    resultHost.appendChild(loadingBox());
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
        resultHost.appendChild(emptyBox('Няма резервации за периода.'));
        return;
      }
      resultHost.appendChild(el('div', { className: 'table-wrap' }, [
        el('table', { className: 'data' }, [
          el('thead', {}, [el('tr', {}, [
            el('th', { text: 'Номер' }), el('th', { text: 'Маса' }), el('th', { text: 'Клиент' }),
            el('th', { text: 'Начало' }), el('th', { text: 'Край' }), el('th', { text: 'Гости' }), el('th', { text: 'Статус' })
          ])]),
          el('tbody', {}, rows.map((r) => el('tr', {}, [
            el('td', { text: text(r.reservationNumber) }),
            el('td', { text: text(r.tableNumber) }),
            el('td', { text: text(r.clientName) }),
            el('td', { text: dateTime(r.startTime) }),
            el('td', { text: dateTime(r.endTime) }),
            el('td', { text: text(r.guestCount) }),
            el('td', {}, [badge(r.status)])
          ])))
        ])
      ]));
    } catch (err) {
      if (err && err.name === 'AbortError') return;
      clear(resultHost);
      handleError(err, 'Графикът не можа да се зареди.');
      resultHost.appendChild(errorBox(err.message || 'Грешка', load));
    }
  };

  loadBtn.addEventListener('click', () => { load(); });

  content.appendChild(el('div', { className: 'panel stack' }, [
    el('p', { className: 'muted', text: 'Само преглед. Няма create/update/cancel от сервитьорския UI. Часовете се изпращат като LocalDateTime без Z.' }),
    el('div', { className: 'grid grid-filters' }, [
      el('label', { className: 'field' }, [el('span', { text: 'От' }), fromInput]),
      el('label', { className: 'field' }, [el('span', { text: 'До' }), toInput]),
      el('label', { className: 'field' }, [el('span', { text: 'tableId' }), tableInput]),
      el('label', { className: 'field' }, [el('span', { text: 'status' }), statusInput])
    ]),
    el('div', { className: 'actions' }, [loadBtn]),
    resultHost
  ]));

  await load();
}