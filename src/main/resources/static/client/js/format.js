export function text(value) {
  if (value === null || value === undefined || value === '') return '—';
  return String(value);
}

export function dateTime(value) {
  if (!value) return '—';
  const s = String(value);
  if (s.includes('T')) {
    const [d, t] = s.split('T');
    return `${d} ${(t || '').slice(0, 8)}`;
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
  const map = {
    CONFIRMED: 'Потвърдена',
    CANCELLED: 'Отказана',
    COMPLETED: 'Завършена',
    NO_SHOW: 'Неявяване'
  };
  return map[status] || String(status || '—');
}