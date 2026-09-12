import { clearBanner, setBanner, toast } from '/shared/js/toasts.js?v=fix-toasts-2';
import { t } from '/shared/js/i18n/i18n.js?v=fix-toasts-1';

export { toast, setBanner, clearBanner };

export function handleError(err, fallback = t('common.error')) {
  if (err && err.name === 'AbortError') return;
  const msg = (err && err.message) ? err.message : fallback;
  clearBanner();
  toast(msg, 'error');
  return msg;
}
