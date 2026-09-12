/**
 * Themed datetime picker (BG/EN). Value format matches datetime-local: YYYY-MM-DDTHH:mm
 */
import { t } from '/shared/js/i18n/i18n.js?v=fix-datetime-1';
import { getLanguage } from '/shared/js/ui-preferences.js';

function pad(n) {
  return String(n).padStart(2, '0');
}

function localeTag() {
  return getLanguage() === 'en' ? 'en-GB' : 'bg-BG';
}

export function parseLocalDateTime(value) {
  if (!value) return null;
  const m = String(value).match(/^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?/);
  if (!m) return null;
  return {
    y: Number(m[1]),
    m: Number(m[2]),
    d: Number(m[3]),
    h: Number(m[4] ?? 12),
    min: Number(m[5] ?? 0)
  };
}

export function formatLocalDateTime(parts) {
  if (!parts) return '';
  return `${parts.y}-${pad(parts.m)}-${pad(parts.d)}T${pad(parts.h)}:${pad(parts.min)}`;
}

export function formatDisplayDateTime(value) {
  const parts = typeof value === 'object' && value ? value : parseLocalDateTime(value);
  if (!parts) return '';
  if (getLanguage() === 'en') {
    return `${pad(parts.d)}/${pad(parts.m)}/${parts.y} ${pad(parts.h)}:${pad(parts.min)}`;
  }
  return `${pad(parts.d)}.${pad(parts.m)}.${parts.y} ${pad(parts.h)}:${pad(parts.min)}`;
}

function monthLabel(year, monthIndex0) {
  return new Intl.DateTimeFormat(localeTag(), { month: 'long', year: 'numeric' })
    .format(new Date(year, monthIndex0, 1));
}

function weekdayLabels() {
  const base = new Date(2024, 0, 1); // Monday
  const fmt = new Intl.DateTimeFormat(localeTag(), { weekday: 'short' });
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(base);
    d.setDate(base.getDate() + i);
    return fmt.format(d);
  });
}

function daysInMonth(year, month1) {
  return new Date(year, month1, 0).getDate();
}

/** Monday=0 … Sunday=6 */
function mondayFirstDow(year, month1, day) {
  const js = new Date(year, month1 - 1, day).getDay(); // Sun=0
  return (js + 6) % 7;
}

let openPicker = null;

function closeOpenPicker() {
  if (openPicker) openPicker.close();
}

export function createDatetimePicker({ value = '', placeholder } = {}) {
  const root = document.createElement('div');
  root.className = 'dtp';

  let parts = parseLocalDateTime(value);
  let viewY = parts?.y ?? new Date().getFullYear();
  let viewM = parts?.m ?? (new Date().getMonth() + 1);
  let panelOpen = false;

  const trigger = document.createElement('button');
  trigger.type = 'button';
  trigger.className = 'dtp-trigger';
  trigger.setAttribute('aria-haspopup', 'dialog');
  trigger.setAttribute('aria-expanded', 'false');

  const panel = document.createElement('div');
  panel.className = 'dtp-panel';
  panel.hidden = true;
  panel.setAttribute('role', 'dialog');

  Object.defineProperty(root, 'id', {
    configurable: true,
    enumerable: true,
    get() { return trigger.id; },
    set(v) { trigger.id = v || ''; }
  });

  Object.defineProperty(root, 'value', {
    configurable: true,
    enumerable: true,
    get() { return formatLocalDateTime(parts); },
    set(v) {
      parts = parseLocalDateTime(v);
      if (parts) {
        viewY = parts.y;
        viewM = parts.m;
      }
      syncTrigger();
      if (panelOpen) renderPanel();
      root.dispatchEvent(new Event('change', { bubbles: true }));
    }
  });

  function syncTrigger() {
    const text = formatDisplayDateTime(parts);
    trigger.textContent = text || (placeholder || t('datetime.placeholder'));
    trigger.classList.toggle('is-empty', !text);
  }

  function emitChange() {
    root.dispatchEvent(new Event('change', { bubbles: true }));
    root.dispatchEvent(new Event('input', { bubbles: true }));
  }

  function positionPanel() {
    const margin = 12;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const narrow = vw <= 640;
    const width = narrow
      ? Math.max(260, vw - margin * 2)
      : Math.min(312, vw - margin * 2);

    panel.style.position = 'fixed';
    panel.style.zIndex = '120';
    panel.style.boxSizing = 'border-box';
    panel.style.width = `${width}px`;
    panel.style.maxWidth = `calc(100vw - ${margin * 2}px)`;
    panel.style.maxHeight = `calc(100dvh - ${margin * 2}px)`;
    panel.style.overflow = 'auto';
    panel.style.right = 'auto';

    if (narrow) {
      const rect = trigger.getBoundingClientRect();
      const safeBottom = 12;
      panel.style.left = `${margin}px`;
      panel.style.right = `${margin}px`;
      panel.style.width = 'auto';
      if (rect.bottom < vh * 0.45) {
        const top = Math.max(margin, Math.min(rect.bottom + 8, vh * 0.2));
        panel.style.top = `${top}px`;
        panel.style.bottom = 'auto';
        panel.style.maxHeight = `calc(100dvh - ${top + margin}px)`;
      } else {
        panel.style.top = 'auto';
        panel.style.bottom = `${safeBottom}px`;
        panel.style.maxHeight = `calc(100dvh - ${safeBottom * 2}px)`;
      }
      return;
    }

    const rect = trigger.getBoundingClientRect();
    let left = rect.left;
    if (left + width > vw - margin) left = vw - width - margin;
    if (left < margin) left = margin;

    const panelHeight = Math.min(panel.offsetHeight || 360, vh - margin * 2);
    const spaceBelow = vh - rect.bottom - margin;
    const spaceAbove = rect.top - margin;
    const preferBelow = spaceBelow >= Math.min(panelHeight, 280) || spaceBelow >= spaceAbove;

    panel.style.left = `${left}px`;
    if (preferBelow) {
      const top = Math.min(rect.bottom + 8, vh - margin - 120);
      panel.style.top = `${Math.max(margin, top)}px`;
      panel.style.bottom = 'auto';
      panel.style.maxHeight = `${Math.max(180, vh - Number.parseFloat(panel.style.top) - margin)}px`;
    } else {
      panel.style.top = 'auto';
      panel.style.bottom = `${Math.max(margin, vh - rect.top + 8)}px`;
      panel.style.maxHeight = `${Math.max(180, spaceAbove)}px`;
    }
  }

  function close() {
    if (!panelOpen) return;
    panelOpen = false;
    panel.hidden = true;
    trigger.setAttribute('aria-expanded', 'false');
    root.classList.remove('is-open');
    if (panel.parentElement !== root) root.appendChild(panel);
    if (openPicker === api) openPicker = null;
  }

  function open() {
    if (panelOpen) return;
    closeOpenPicker();
    panelOpen = true;
    document.body.appendChild(panel);
    panel.hidden = false;
    trigger.setAttribute('aria-expanded', 'true');
    root.classList.add('is-open');
    if (parts) {
      viewY = parts.y;
      viewM = parts.m;
    }
    renderPanel();
    positionPanel();
    // Second pass after layout so height-based clamping is accurate
    requestAnimationFrame(() => {
      if (panelOpen) positionPanel();
    });
    openPicker = api;
  }

  function selectDay(day) {
    const h = parts?.h ?? 12;
    const min = parts?.min ?? 0;
    parts = { y: viewY, m: viewM, d: day, h, min };
    syncTrigger();
    renderPanel();
    emitChange();
  }

  function renderPanel() {
    while (panel.firstChild) panel.removeChild(panel.firstChild);

    const head = document.createElement('div');
    head.className = 'dtp-head';

    const prev = document.createElement('button');
    prev.type = 'button';
    prev.className = 'dtp-nav';
    prev.setAttribute('aria-label', t('datetime.prevMonth'));
    prev.textContent = '‹';
    prev.addEventListener('click', (e) => {
      e.stopPropagation();
      viewM -= 1;
      if (viewM < 1) { viewM = 12; viewY -= 1; }
      renderPanel();
      positionPanel();
    });

    const title = document.createElement('div');
    title.className = 'dtp-month';
    title.textContent = monthLabel(viewY, viewM - 1);

    const next = document.createElement('button');
    next.type = 'button';
    next.className = 'dtp-nav';
    next.setAttribute('aria-label', t('datetime.nextMonth'));
    next.textContent = '›';
    next.addEventListener('click', (e) => {
      e.stopPropagation();
      viewM += 1;
      if (viewM > 12) { viewM = 1; viewY += 1; }
      renderPanel();
      positionPanel();
    });

    head.append(prev, title, next);

    const week = document.createElement('div');
    week.className = 'dtp-weekdays';
    weekdayLabels().forEach((label) => {
      const cell = document.createElement('span');
      cell.textContent = label;
      week.appendChild(cell);
    });

    const grid = document.createElement('div');
    grid.className = 'dtp-grid';
    const firstDow = mondayFirstDow(viewY, viewM, 1);
    const total = daysInMonth(viewY, viewM);
    const today = new Date();
    for (let i = 0; i < firstDow; i += 1) {
      const empty = document.createElement('span');
      empty.className = 'dtp-day is-empty';
      grid.appendChild(empty);
    }
    for (let day = 1; day <= total; day += 1) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'dtp-day';
      btn.textContent = String(day);
      const selected = parts && parts.y === viewY && parts.m === viewM && parts.d === day;
      const isToday = today.getFullYear() === viewY
        && (today.getMonth() + 1) === viewM
        && today.getDate() === day;
      if (selected) btn.classList.add('is-selected');
      if (isToday) btn.classList.add('is-today');
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        selectDay(day);
      });
      grid.appendChild(btn);
    }

    const timeRow = document.createElement('div');
    timeRow.className = 'dtp-time';

    const hourSelect = document.createElement('select');
    hourSelect.className = 'dtp-select';
    hourSelect.setAttribute('aria-label', t('datetime.hour'));
    for (let h = 0; h < 24; h += 1) {
      const opt = document.createElement('option');
      opt.value = String(h);
      opt.textContent = pad(h);
      if ((parts?.h ?? 12) === h) opt.selected = true;
      hourSelect.appendChild(opt);
    }
    hourSelect.addEventListener('change', () => {
      if (!parts) {
        parts = { y: viewY, m: viewM, d: Math.min(new Date().getDate(), daysInMonth(viewY, viewM)), h: 12, min: 0 };
      }
      parts = { ...parts, h: Number(hourSelect.value) };
      syncTrigger();
      emitChange();
    });

    const sep = document.createElement('span');
    sep.className = 'dtp-time-sep';
    sep.textContent = ':';

    const minSelect = document.createElement('select');
    minSelect.className = 'dtp-select';
    minSelect.setAttribute('aria-label', t('datetime.minute'));
    for (let min = 0; min < 60; min += 1) {
      const opt = document.createElement('option');
      opt.value = String(min);
      opt.textContent = pad(min);
      if ((parts?.min ?? 0) === min) opt.selected = true;
      minSelect.appendChild(opt);
    }
    minSelect.addEventListener('change', () => {
      if (!parts) {
        parts = { y: viewY, m: viewM, d: Math.min(new Date().getDate(), daysInMonth(viewY, viewM)), h: 12, min: 0 };
      }
      parts = { ...parts, min: Number(minSelect.value) };
      syncTrigger();
      emitChange();
    });

    const timeLabel = document.createElement('span');
    timeLabel.className = 'dtp-time-label';
    timeLabel.textContent = t('datetime.time');

    timeRow.append(timeLabel, hourSelect, sep, minSelect);

    const actions = document.createElement('div');
    actions.className = 'dtp-actions';

    const clearBtn = document.createElement('button');
    clearBtn.type = 'button';
    clearBtn.className = 'btn btn-secondary dtp-action';
    clearBtn.textContent = t('datetime.clear');
    clearBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      parts = null;
      syncTrigger();
      renderPanel();
      emitChange();
    });

    const todayBtn = document.createElement('button');
    todayBtn.type = 'button';
    todayBtn.className = 'btn btn-secondary dtp-action';
    todayBtn.textContent = t('datetime.today');
    todayBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const now = new Date();
      parts = {
        y: now.getFullYear(),
        m: now.getMonth() + 1,
        d: now.getDate(),
        h: parts?.h ?? now.getHours(),
        min: parts?.min ?? now.getMinutes()
      };
      viewY = parts.y;
      viewM = parts.m;
      syncTrigger();
      renderPanel();
      emitChange();
    });

    const doneBtn = document.createElement('button');
    doneBtn.type = 'button';
    doneBtn.className = 'btn dtp-action';
    doneBtn.textContent = t('common.confirm');
    doneBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (!parts) {
        const now = new Date();
        parts = {
          y: viewY,
          m: viewM,
          d: Math.min(now.getDate(), daysInMonth(viewY, viewM)),
          h: Number(hourSelect.value),
          min: Number(minSelect.value)
        };
        syncTrigger();
        emitChange();
      }
      close();
    });

    actions.append(clearBtn, todayBtn, doneBtn);
    panel.append(head, week, grid, timeRow, actions);
  }

  trigger.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (panelOpen) close();
    else open();
  });

  root.append(trigger, panel);
  syncTrigger();

  const api = { root, panel, close, open, positionPanel };
  root._dtp = api;
  return root;
}

export function wireDatetimePickers() {
  if (wireDatetimePickers._wired) return;
  wireDatetimePickers._wired = true;
  document.addEventListener('click', (event) => {
    if (!openPicker) return;
    const { root, panel } = openPicker;
    if (root.contains(event.target) || panel.contains(event.target)) return;
    openPicker.close();
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') closeOpenPicker();
  });
  window.addEventListener('resize', () => {
    if (openPicker && typeof openPicker.positionPanel === 'function') {
      openPicker.positionPanel();
    }
  });
}
