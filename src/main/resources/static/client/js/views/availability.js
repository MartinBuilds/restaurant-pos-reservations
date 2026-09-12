import { api, queryString } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text, toDateTimeLocalValue } from '../format.js';
import { emptyBox, errorBox, handleError, loadingBox, setBanner, setPageMeta } from '../ui.js';
import { navigate } from '../router.js';
import { createDatetimePicker } from '/shared/js/datetime-picker.js?v=fix-client-ui-1';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';
import { t } from '/shared/js/i18n/i18n.js?v=fix-client-ui-1';

let abortController = null;

function defaultStart() {
  const d = new Date();
  d.setMinutes(0, 0, 0);
  d.setHours(d.getHours() + 2);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function defaultEnd(start) {
  if (!start) return '';
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

function guestStepper(id, value = 2) {
  const input = el('input', {
    id,
    type: 'number',
    min: '1',
    step: '1',
    required: true,
    value: String(value),
    className: 'guest-input',
    inputmode: 'numeric'
  });
  const dec = el('button', {
    type: 'button',
    className: 'guest-btn',
    'aria-label': '−',
    text: '−',
    onClick: () => {
      input.value = String(Math.max(1, Number(input.value || 1) - 1));
      input.dispatchEvent(new Event('input', { bubbles: true }));
    }
  });
  const inc = el('button', {
    type: 'button',
    className: 'guest-btn',
    'aria-label': '+',
    text: '+',
    onClick: () => {
      input.value = String(Number(input.value || 1) + 1);
      input.dispatchEvent(new Event('input', { bubbles: true }));
    }
  });
  return {
    root: el('div', { className: 'guest-stepper' }, [dec, input, inc]),
    input
  };
}

function renderResults(host, data) {
  const summary = el('div', { className: 'search-summary' }, [
    el('div', { className: 'search-summary-item' }, [
      el('span', { className: 'search-summary-label', text: t('msg.period') }),
      el('strong', { text: `${dateTime(data.startTime)} – ${dateTime(data.endTime)}` })
    ]),
    el('div', { className: 'search-summary-item' }, [
      el('span', { className: 'search-summary-label', text: t('msg.guests') }),
      el('strong', { text: text(data.guestCount) })
    ])
  ]);
  host.appendChild(summary);

  const tables = data.availableTables || [];
  if (!tables.length) {
    host.appendChild(emptyBox(t('msg.noResults')));
    return;
  }

  host.appendChild(el('h3', { className: 'results-title', text: t('msg.availableTables') }));

  const cards = el('div', { className: 'card-grid' });
  tables.forEach((table) => {
    const card = el('article', { className: 'table-card' }, [
      el('div', { className: 'table-card-top' }, [
        el('p', { className: 'table-card-kicker', text: t('label.table', { number: text(table.tableNumber) }) }),
        el('h3', { text: text(table.displayName) || t('label.table', { number: text(table.tableNumber) }) })
      ]),
      el('p', { className: 'table-card-meta', text: t('label.capacity', { n: text(table.capacity) }) }),
      el('button', {
        type: 'button',
        className: 'btn btn-primary table-card-cta',
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
  setPageRefresh(() => renderAvailability(root));
  setPageMeta(t('page.availability.title'), t('page.availability.subtitle'));
  setBanner('');
  clear(root);

  const startDefault = defaultStart();
  const startPicker = createDatetimePicker({ value: toDateTimeLocalValue(startDefault) });
  startPicker.id = 'av-start';
  const endPicker = createDatetimePicker({ value: toDateTimeLocalValue(defaultEnd(startDefault)) });
  endPicker.id = 'av-end';
  const guests = guestStepper('av-guests', 2);

  const fieldError = el('p', { className: 'field-error', id: 'av-error', hidden: true });
  const results = el('div', { id: 'av-results', className: 'results-stack' });
  const submitBtn = el('button', {
    type: 'submit',
    className: 'btn btn-primary search-submit',
    text: t('action.search')
  });

  startPicker.addEventListener('change', () => {
    const start = startPicker.value;
    if (!start) return;
    if (!endPicker.value || start >= endPicker.value) {
      endPicker.value = defaultEnd(start);
    }
  });

  const form = el('form', { className: 'search-panel', novalidate: true }, [
    el('div', { className: 'search-panel-head' }, [
      el('h3', { text: t('client.searchTitle') }),
      el('p', { className: 'muted', text: t('client.searchHint') })
    ]),
    el('div', { className: 'search-fields' }, [
      el('div', { className: 'field' }, [
        el('label', { for: 'av-start', text: t('col.startRequired') }),
        startPicker
      ]),
      el('div', { className: 'field' }, [
        el('label', { for: 'av-end', text: t('col.endRequired') }),
        endPicker
      ]),
      el('div', { className: 'field field-guests' }, [
        el('label', { for: 'av-guests', text: `${t('msg.guests')} *` }),
        guests.root
      ])
    ]),
    fieldError,
    el('div', { className: 'search-actions' }, [submitBtn])
  ]);

  root.append(
    form,
    results
  );

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    fieldError.hidden = true;
    fieldError.textContent = '';
    const start = startPicker.value;
    const end = endPicker.value;
    const guestCount = guests.input.value;
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
    submitBtn.disabled = true;

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
    } finally {
      submitBtn.disabled = false;
    }
  });
}

export function abortAvailability() {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
}
