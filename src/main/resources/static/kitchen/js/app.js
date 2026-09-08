import { ensureCsrf, logout, setUnauthorizedHandler, api } from '/operations/js/api.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { onRealtimeRefresh, startKitchenRealtime, stopRealtime } from './realtime.js';
import { renderQueue } from './queue.js';
import { mountShellChrome } from '/shared/js/account-shell.js?v=fix-toasts-2';
import { consumeSignedInWelcome } from '/shared/js/session-flash.js?v=fix-toasts-3';
import { t } from '/shared/js/i18n/i18n.js?v=fix-toasts-3';

async function boot() {
  setUnauthorizedHandler(() => {
    stopRealtime();
    window.location.assign('/login');
  });

  document.getElementById('refresh-btn').addEventListener('click', () => {
    renderQueue().catch((err) => handleError(err));
  });

  let csrfOk = false;
  try {
    await ensureCsrf();
    csrfOk = true;
  } catch (err) {
    handleError(err, t('session.csrfError'));
    setBanner(t('session.problem'), 'error');
  }

  try {
    await mountShellChrome({
      apiGet: (path) => api.get(path),
      onLogout: async () => {
        try {
          stopRealtime();
          await logout();
        } catch { /* ignore */ }
        window.location.assign('/login');
      },
      onLanguageApplied: () => {
        renderQueue().catch((err) => handleError(err));
      }
    });
  } catch (err) {
    console.error(err);
    handleError(err, t('boot.kitchenError'));
  }

  if (csrfOk && consumeSignedInWelcome()) {
    toast(t('session.activeShort'), 'success');
  }

  onRealtimeRefresh(async () => {
    await renderQueue();
  });

  await renderQueue();

  if (csrfOk) {
    try {
      await startKitchenRealtime();
    } catch (err) {
      handleError(err, t('session.wsError'));
    }
  }
}

boot().catch((err) => {
  console.error(err);
  toast(t('boot.kitchenError'), 'error');
});
