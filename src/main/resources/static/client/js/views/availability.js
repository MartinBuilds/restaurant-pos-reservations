import { api, queryString } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text } from '../format.js';
import { emptyBox, errorBox, handleError, loadingBox, setBanner, setPageMeta } from '../ui.js';
import { navigate } from '../router.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr19-1';

let abortController = null;

function defaultStart() {
  const d = new Date();
  d.setMinutes(0, 0, 0);
  d.setHours(d.getHours() + 2);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function defaultEnd(start) {
  const [date, time] = start.split('T');
  const [h, m] = time.split(':').map(Number);
  const endH = (h + 2) % 24;
  return `${date}T${String(endH).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

function validate(start, end, guestCount) {
  if (!start || !end || !guestCount) return t('msg.requiredFields');
  if (start >= end) return t('msg.startBeforeEnd');
  const n = Number(guestCount);
  if (!Number.isInteger(n) || n < 1) return t('msg.guestCountInvalid');
  return null;
}

function renderResults(host, data) {
  const meta = el('div', { className: 'meta-row' }, [
    el('span', { text: `${t('msg.period')}: ${dateTime(data.startTime)} – ${dateTime(data.endTime)}` }),
    el('span', { text: `${t('msg.guests')}: ${text(data.guestCount)}` })
  ]);
  host.appendChild(meta);

  const tables = data.availableTables || [];
  if (!tables.length) {
    host.appendChild(emptyBox(t('msg.noResults')));
    return;
  }

  host.appendChild(el('p', { className: 'muted', text: t('msg.availableTables') }));

  const cards = el('div', { className: 'card-grid' });
  tables.forEach((table) => {
    const card = el('article', { className: 'card' }, [
      el('h3', { text: t('label.table', { number: text(table.tableNumber) }) }),
      el('p', { className: 'muted', text: text(table.displayName) }),
      el('p', { text: t('label.capacity', { n: text(table.capacity) }) }),
      el('button', {
        type: 'button',
        className: 'btn btn-primary',
        text: t('action.book'),
        onClick: () => {
          const params = new URLSearchParams({
            diningTableId: String(table.diningTableId),
            tableNumber: String(table.tableNumber || ''),
            displayName: String(table.displayName || ''),
            startTime: String(data.startTime || ''),
            endTime: String(data.endTime || ''),
            guestCount: String(data.guestCount || '')
          });
          navigate(`#/new?${params.toString()}`);
        }
      })
    ]);
    cards.appendChild(card);
  });
  host.appendChild(cards);
}

export async function renderAvailability(root) {
  setPageMeta(t('page.availability.title'), t('page.availability.subtitle'));
  setBanner('');
  clear(root);

  const startDefault = defaultStart();
  const form = el('form', { className: 'form-grid', novalidate: true });
  const startInput = el('input', { id: 'av-start', type: 'datetime-local', required: true, value: startDefault });
  const endInput = el('input', { id: 'av-end', type: 'datetime-local', required: true, value: defaultEnd(startDefault) });
  const guestInput = el('input', { id: 'av-guests', type: 'number', min: '1', step: '1', required: true, value: '2' });
  const fieldError = el('p', { className: 'field-error', id: 'av-error', hidden: true });
  const results = el('div', { id: 'av-results', className: 'stack' });

  form.append(
    el('div', { className: 'field' }, [
      el('label', { for: 'av-start', text: t('col.startRequired') }),
      startInput,
      el('p', { className: 'hint', text: t('hint.localTimezone') })
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'av-end', text: t('col.endRequired') }),
      endInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'av-guests', text: `${t('msg.guests')} *` }),
      guestInput
    ]),
    fieldError,
    el('div', { className: 'actions' }, [
      el('button', { type: 'submit', className: 'btn btn-primary', text: t('action.search') })
    ])
  );

  root.append(form, results);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    fieldError.hidden = true;
    fieldError.textContent = '';
    const start = startInput.value;
    const end = endInput.value;
    const guestCount = guestInput.value;
    const err = validate(start, end, guestCount);
    if (err) {
      fieldError.textContent = err;
      fieldError.hidden = false;
      return;
    }

    if (abortController) abortController.abort();
    abortController = new AbortController();
    clear(results);
    results.appendChild(loadingBox(t('common.loading')));

    try {
      const data = await api.get(
        `/api/client/reservations/availability${queryString({ startTime: start, endTime: end, guestCount })}`,
        { signal: abortController.signal }
      );
      clear(results);
      renderResults(results, data);
    } catch (e) {
      if (e && e.name === 'AbortError') return;
      clear(results);
      results.appendChild(errorBox(e.message || t('common.error'), () => form.requestSubmit()));
      handleError(e);
    }
  });
}

export function abortAvailability() {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
}
