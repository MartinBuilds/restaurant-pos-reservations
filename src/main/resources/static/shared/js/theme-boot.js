/**
 * Early theme bootstrap (no credentials / no API data).
 * Allowed as a blocking head script to avoid light-theme flash.
 */
(function () {
  try {
    var KEY = 'restaurant.ui.theme';
    var raw = localStorage.getItem(KEY);
    var pref = raw === 'light' || raw === 'dark' || raw === 'system' ? raw : 'system';
    var dark = pref === 'dark'
      || (pref === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    document.documentElement.setAttribute('data-theme-pref', pref);
  } catch (e) {
    document.documentElement.setAttribute('data-theme', 'light');
    document.documentElement.setAttribute('data-theme-pref', 'system');
  }
})();
