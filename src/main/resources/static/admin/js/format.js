const moneyFmt = new Intl.NumberFormat('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const numberFmt = new Intl.NumberFormat('bg-BG', { maximumFractionDigits: 3 });
const percentFmt = new Intl.NumberFormat('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function money(value) {
  if (value == null || value === '') return '—';
  return moneyFmt.format(Number(value));
}

export function quantity(value) {
  if (value == null || value === '') return '—';
  return numberFmt.format(Number(value));
}

export function percent(value) {
  if (value == null || value === '') return '—';
  return `${percentFmt.format(Number(value))}%`;
}

export function dateTime(value) {
  if (!value) return '—';
  return String(value).replace('T', ' ').slice(0, 19);
}

export function toDateTimeLocalValue(value) {
  if (!value) return '';
  return String(value).slice(0, 16);
}

export function fromDateTimeLocalValue(value) {
  if (!value) return null;
  return value.length === 16 ? `${value}:00` : value;
}

export function boolLabel(value) {
  return value ? 'Да' : 'Не';
}