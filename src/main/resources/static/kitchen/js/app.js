import { ensureCsrf, logout, setUnauthorizedHandler, api } from '/operations/js/api.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { onRealtimeRefresh, startKitchenRealtime, stopRealtime } from './realtime.js?v=fix-kitchen-ready-1';
import { renderQueue } from './queue.js?v=fix-kitchen-ready-1';
import { mountShellChrome } from '/shared/js/account-shell.js?v=fix-toasts-2';
import { consumeSignedInWelcome } from '/shared/js/session-flash.js?v=fix-toasts-3';
import { refreshPage, setPageRefresh, wirePageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';
import { t } from '/shared/js/i18n/i18n.js?v=fix-tables-board-1';

async function boot() {
  wirePageRefresh();

  setUnauthorizedHandler(() => {
    stopRealtime();
    window.location.assign('/login');
  });

  const refreshBtn = document.getElementById('refresh-btn');
  if (refreshBtn) {
    refreshBtn.classList.add('btn-reload');
    refreshBtn.addEventListener('click', () => {
      refreshPage({ source: 'button' }).catch((err) => handleError(err));
    });
  }

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
        renderQueue().catch((err) => {
          if (err && err.name === 'AbortError') return;
          handleError(err);
        });
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
    try {
      await renderQueue({ soft: true });
    } catch (err) {
      if (err && err.name === 'AbortError') return;
      handleError(err);
    }
  });

  setPageRefresh(async () => {
    try {
      await renderQueue({ soft: true });
    } catch (err) {
      if (err && err.name === 'AbortError') return;
      throw err;
    }
  });
  try {
    await renderQueue();
  } catch (err) {
    if (!(err && err.name === 'AbortError')) {
      handleError(err, t('kitchen.loadError'));
    }
  }

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
