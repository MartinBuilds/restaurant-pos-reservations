export function money(value) {
  if (value === null || value === undefined || value === '') return '—';
  const n = Number(value);
  if (Number.isNaN(n)) return String(value);
  return n.toFixed(2) + ' лв.';
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

export function statusLabel(status) {
  const map = {
    AVAILABLE: 'Свободна',
    OCCUPIED: 'Заета',
    RESERVED: 'Резервирана',
    OUT_OF_SERVICE: 'Извън употреба',
    ACCEPTED: 'Приета',
    COOKING: 'Готви се',
    READY: 'Готова',
    SERVED: 'Сервирана',
    CANCELLED: 'Отказана',
    PENDING: 'Чакаща',
    CONFIRMED: 'Потвърдена',
    COMPLETED: 'Завършена',
    NO_SHOW: 'Неявяване',
    CASH: 'В брой',
    CARD: 'Карта'
  };
  return map[status] || String(status || '—');
}