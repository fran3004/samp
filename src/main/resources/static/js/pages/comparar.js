/* =====================================================================
   SAMP · Interactividad de comparación lado a lado
   ===================================================================== */
(() => {
  const toggle = document.getElementById('only-diff-toggle');
  const grid = document.getElementById('cmp');
  if (toggle && grid) {
    toggle.addEventListener('change', () => {
      grid.classList.toggle('only', toggle.checked);
    });
  }
})();
