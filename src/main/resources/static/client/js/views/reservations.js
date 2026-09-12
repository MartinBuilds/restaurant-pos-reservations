import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text } from '../format.js';
import { badge, emptyBox, errorBox, handleError, loadingBox, setBanner, setPageMeta } from '../ui.js';
import { navigate } from '../router.js';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';
import { t } from '/shared/js/i18n/i18n.js?v=fix-res-boards-2';

let abortController = null;

const TERMINAL = new Set(['CANCELLED', 'COMPLETED', 'NO_SHOW']);

function startMs(reservation) {
  const raw = reservation?.startTime;
  if (!raw) return NaN;
  const normalized = String(raw).includes('T') ? String(raw) : String(raw).replace(' ', 'T');
  const ms = Date.parse(normalized);
  return Number.isFinite(ms) ? ms : NaN;
}

function endMs(reservation) {
  const raw = reservation?.endTime;
  if (!raw) return NaN;
  const normalized = String(raw).includes('T') ? String(raw) : String(raw).replace(' ', 'T');
  const ms = Date.parse(normalized);
  return Number.isFinite(ms) ? ms : NaN;
}

function isHistory(reservation, now = Date.now()) {
  if (!reservation) return true;
  if (TERMINAL.has(reservation.status)) return true;
  if (reservation.status === 'CONFIRMED') {
    const end = endMs(reservation);
    return Number.isFinite(end) && end <= now;
  }
  return false;
}

function displayStatus(reservation, now = Date.now()) {
  if (reservation?.status === 'CONFIRMED' && isHistory(reservation, now)) {
    return 'ELAPSED';
  }
  return reservation?.status;
}

function metaChip(label, value) {
  return el('div', { className: 'res-chip' }, [
    el('span', { className: 'res-chip-label', text: label }),
    el('span', { className: 'res-chip-value', text: value })
  ]);
}

function reservationCard(reservation, now, { history = false } = {}) {
  const status = displayStatus(reservation, now);

  return el('article', {
    className: `res-card${history ? ' is-history' : ' is-upcoming'}`
  }, [
    el('div', { className: 'res-card-top' }, [
      el('div', { className: 'res-card-id' }, [
        el('p', { className: 'res-card-kicker', text: t('col.number') }),
        el('h3', { className: 'res-card-title wrap', text: text(reservation.reservationNumber) })
      ]),
      badge(status)
    ]),
    el('p', {
      className: 'res-card-table',
      text: t('label.tableNamed', {
        number: text(reservation.tableNumber),
        name: text(reservation.tableDisplayName)
      })
    }),
    el('div', { className: 'res-card-meta' }, [
      metaChip(t('col.start'), dateTime(reservation.startTime)),
      metaChip(t('col.end'), dateTime(reservation.endTime)),
      metaChip(t('msg.guests'), text(reservation.guestCount))
    ]),
    reservation.notes
      ? el('p', { className: 'res-card-notes muted wrap', text: text(reservation.notes) })
      : null,
    el('div', { className: 'res-card-footer' }, [
      el('button', {
        type: 'button',
        className: 'btn btn-primary btn-sm',
        text: t('action.details'),
        onClick: () => navigate(`#/reservations/${reservation.id}`)
      })
    ])
  ]);
}

function buildGrid(items, now, { history = false } = {}) {
  if (!items.length) {
    return el('div', {
      className: 'res-empty',
      text: history ? t('reservations.historyEmpty') : t('reservations.upcomingEmpty')
    });
  }
  return el('div', { className: 'res-grid' }, items.map((r) => reservationCard(r, now, { history })));
}

function section({ kind, title, hint, count, body }) {
  return el('section', { className: `res-panel res-panel-${kind}` }, [
    el('header', { className: 'res-panel-head' }, [
      el('div', { className: 'res-panel-copy' }, [
        el('h2', { className: 'res-panel-title', text: title }),
        hint ? el('p', { className: 'res-panel-hint muted', text: hint }) : null
      ]),
      el('span', {
        className: 'res-panel-count',
        text: String(count),
        'aria-label': String(count)
      })
    ]),
    body
  ]);
}

export async function renderReservations(root) {
  setPageRefresh(() => renderReservations(root));
  setPageMeta(t('page.myReservations.title'), t('page.myReservations.subtitle'));
  setBanner('');
  clear(root);

  if (abortController) abortController.abort();
  abortController = new AbortController();
  root.appendChild(loadingBox(t('common.loading')));

  try {
    const list = await api.get('/api/client/reservations', { signal: abortController.signal });
    clear(root);

    const items = Array.isArray(list) ? list : [];
    if (!items.length) {
      root.appendChild(emptyBox(t('common.empty')));
      root.appendChild(el('div', { className: 'actions' }, [
        el('button', {
          type: 'button',
          className: 'btn btn-primary',
          text: t('page.availability.title'),
          onClick: () => navigate('#/availability')
        })
      ]));
      return;
    }

    const now = Date.now();
    const upcoming = items
      .filter((r) => !isHistory(r, now))
      .sort((a, b) => startMs(a) - startMs(b) || String(a.reservationNumber).localeCompare(String(b.reservationNumber)));
    const history = items
      .filter((r) => isHistory(r, now))
      .sort((a, b) => endMs(b) - endMs(a) || String(b.reservationNumber).localeCompare(String(a.reservationNumber)));

    const board = el('div', { className: 'reservations-board' });
    board.appendChild(section({
      kind: 'upcoming',
      title: t('reservations.upcomingTitle'),
      hint: t('reservations.upcomingHint'),
      count: upcoming.length,
      body: buildGrid(upcoming, now, { history: false })
    }));
    board.appendChild(section({
      kind: 'history',
      title: t('reservations.historyTitle'),
      hint: t('reservations.historyHint'),
      count: history.length,
      body: buildGrid(history, now, { history: true })
    }));
    root.appendChild(board);
  } catch (e) {
    if (e && e.name === 'AbortError') return;
    clear(root);
    root.appendChild(errorBox(e.message || t('common.error'), () => renderReservations(root)));
    handleError(e);
  }
}

export function abortReservations() {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
}
