import { statusLabel as i18nStatus } from '/shared/js/i18n/i18n.js?v=fix-client-ui-1';
import { formatDisplayDateTime, parseLocalDateTime } from '/shared/js/datetime-picker.js?v=fix-client-ui-1';

export function text(value) {
  if (value === null || value === undefined || value === '') return '—';
  return String(value);
}

export function dateTime(value) {
  if (!value) return '—';
  const parts = parseLocalDateTime(value);
  if (!parts) return String(value).replace('T', ' ').slice(0, 19);
  return formatDisplayDateTime(parts);
}

/** Keep LocalDateTime as local form value — no timezone conversion. */
export function toDateTimeLocalValue(value) {
  if (!value) return '';
  const s = String(value);
  if (s.length >= 16) return s.slice(0, 16);
  return s;
}

export function fromDateTimeLocalValue(value) {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
}

export function statusLabel(status) {
  if (!status) return '—';
  return i18nStatus(status);
}
