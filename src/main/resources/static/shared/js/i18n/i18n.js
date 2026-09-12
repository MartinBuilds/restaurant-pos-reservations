import { bg } from './bg.js?v=fix-tables-board-1';
import { en } from './en.js?v=fix-tables-board-1';
import { getLanguage, LANGUAGE_CHANGE, setLanguage as persistLanguage } from '../ui-preferences.js';

const dictionaries = { bg, en };

export function t(key, params = {}) {
  const lang = getLanguage();
  const dict = dictionaries[lang] || dictionaries.bg;
  let value = dict[key] ?? dictionaries.bg[key] ?? key;
  Object.entries(params).forEach(([name, raw]) => {
    value = value.replaceAll(`{${name}}`, String(raw ?? ''));
  });
  return value;
}

export function statusLabel(code) {
  if (code == null || code === '') return '';
  const key = `status.${code}`;
  const translated = t(key);
  return translated === key ? String(code) : translated;
}

export function availabilityReasonLabel(code) {
  if (code == null || code === '') return '';
  const key = `availability.${code}`;
  const translated = t(key);
  return translated === key ? statusLabel(code) : translated;
}

export function unitLabel(code) {
  if (code == null || code === '') return '';
  const key = `unit.${code}`;
  const translated = t(key);
  return translated === key ? String(code) : translated;
}

export function setLanguage(lang) {
  return persistLanguage(lang);
}

export function applyDomI18n(root = document) {
  root.querySelectorAll('[data-i18n]').forEach((node) => {
    const key = node.getAttribute('data-i18n');
    if (!key) return;
    const label = node.querySelector('.nav-label, .i18n-text');
    if (label) label.textContent = t(key);
    else node.textContent = t(key);
  });
  root.querySelectorAll('[data-i18n-aria]').forEach((node) => {
    const key = node.getAttribute('data-i18n-aria');
    if (key) node.setAttribute('aria-label', t(key));
  });
  root.querySelectorAll('[data-i18n-title]').forEach((node) => {
    const key = node.getAttribute('data-i18n-title');
    if (key) node.setAttribute('title', t(key));
  });
  root.querySelectorAll('[data-i18n-placeholder]').forEach((node) => {
    const key = node.getAttribute('data-i18n-placeholder');
    if (key) node.setAttribute('placeholder', t(key));
  });
}

export function onLanguageChange(handler) {
  const listener = (event) => handler(event.detail?.language || getLanguage());
  window.addEventListener(LANGUAGE_CHANGE, listener);
  return () => window.removeEventListener(LANGUAGE_CHANGE, listener);
}

export function hasKey(key) {
  return Object.prototype.hasOwnProperty.call(dictionaries.bg, key)
    && Object.prototype.hasOwnProperty.call(dictionaries.en, key);
}

export function dictionaryKeys() {
  return Object.keys(dictionaries.bg);
}
