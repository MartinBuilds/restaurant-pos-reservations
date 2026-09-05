import { statusLabel as i18nStatus } from '/shared/js/i18n/i18n.js?v=pr17-4';

export function text(value) {
  if (value === null || value === undefined || value === '') return '—';
  return String(value);
}

export function dateTime(value) {
  if (!value) return '—';
  const s = String(value);
  if (s.includes('T')) {
    const [d, time] = s.split('T');
    return `${d} ${(time || '').slice(0, 8)}`;
  }
  return s;
}

/** Keep LocalDateTime as local form value — no timezone conversion. */
export function toDateTimeLocalValue(value) {
  if (!value) return '';
  const s = String(value);
  if (s.length >= 16) return s.slice(0, 16);
  return s;
}

export function statusLabel(status) {
  if (!status) return '—';
  return i18nStatus(status);
}
