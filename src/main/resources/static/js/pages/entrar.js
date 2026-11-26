/* =====================================================================
   SAMP · Interactividad de la página de ingreso y registro
   ===================================================================== */
(() => {
  if (window.location.hash === '#crear') {
    const regTab = document.querySelector('[data-show="up"]');
    regTab && regTab.click();
  }
})();
