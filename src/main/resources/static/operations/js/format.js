import { t } from '/shared/js/i18n/i18n.js?v=fix-currency-1';

export { statusLabel } from '/shared/js/i18n/i18n.js?v=fix-currency-1';

export function money(value) {
  if (value === null || value === undefined || value === '') return '—';
  const n = Number(value);
  if (Number.isNaN(n)) return String(value);
  return n.toFixed(2) + t('fmt.currencySuffix');
}

export function text(value) {
  if (value === null || value === undefined || value === '') return '—';
  return String(value);
}

/** Format LocalDateTime-like string without timezone conversion. */
export function dateTime(value) {
  if (!value) return '—';
  const s = String(value);
  if (s.includes('T')) {
    const [d, t] = s.split('T');
    const time = (t || '').slice(0, 8);
    return `${d} ${time}`;
  }
  return s;
}

export function toLocalDateTimeInputValue(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
