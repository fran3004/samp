/* =====================================================================
   SAMP · Navegación, overlays, mega menú, paleta Ctrl+K y toasts
   ===================================================================== */
(() => {
  const $ = (s, r = document) => r.querySelector(s);
  const I = window.SAMP?.I || (() => '');

  const nav = $('#nav');
  if (nav) {
    const onScroll = () => nav.classList.toggle('s', window.scrollY > 4);
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  let openPop = null;
  function closePop() {
    if (!openPop) return;
    openPop.pop.classList.remove('open');
    openPop.btn.setAttribute('aria-expanded', 'false');
    openPop = null;
  }

  document.addEventListener('click', e => {
    const btn = e.target.closest('[data-pop]');
    if (btn) {
      e.preventDefault();
      const pop = $('#' + btn.dataset.pop);
      if (!pop) return;
      if (openPop && openPop.pop === pop) return closePop();
      closePop();
      const rect = btn.getBoundingClientRect();
      pop.style.top = (rect.bottom + window.scrollY + 8) + 'px';
      pop.style.left = Math.max(12, Math.min(rect.right - pop.offsetWidth, window.innerWidth - pop.offsetWidth - 12)) + 'px';
      pop.classList.add('open');
      btn.setAttribute('aria-expanded', 'true');
      openPop = { pop, btn };
      return;
    }
    if (openPop && !e.target.closest('.pop')) closePop();
  });

  const mega = $('#mega');
  const megaBtn = $('[data-mega]');
  if (mega && megaBtn) {
    let timer;
    const toggle = open => {
      mega.classList.toggle('open', open);
      megaBtn.setAttribute('aria-expanded', String(open));
    };
    megaBtn.addEventListener('click', () => toggle(!mega.classList.contains('open')));
    megaBtn.addEventListener('pointerenter', e => {
      if (e.pointerType === 'mouse') { clearTimeout(timer); timer = setTimeout(() => toggle(true), 120); }
    });
    nav && nav.addEventListener('pointerleave', () => { clearTimeout(timer); timer = setTimeout(() => toggle(false), 200); });
    mega.addEventListener('pointerenter', () => clearTimeout(timer));
  }

  const backdrop = $('#bd');
  let activeLayer = null;
  function openLayer(el) {
    closeLayer();
    activeLayer = el;
    el.classList.add('open');
    backdrop && backdrop.classList.add('open');
    document.body.style.overflow = 'hidden';
    const focusable = el.querySelector('input,button,[href]');
    focusable && setTimeout(() => focusable.focus({ preventScroll: true }), 60);
  }
  function closeLayer() {
    if (!activeLayer) return;
    activeLayer.classList.remove('open');
    backdrop && backdrop.classList.remove('open');
    document.body.style.overflow = '';
    activeLayer = null;
  }

  document.addEventListener('click', e => {
    const trigger = e.target.closest('[data-drawer],[data-modal],[data-sheet],[data-palette]');
    if (trigger) {
      e.preventDefault();
      const id = trigger.dataset.drawer || trigger.dataset.modal || trigger.dataset.sheet || 'pal';
      const target = $('#' + id);
      target && openLayer(target);
      return;
    }
    if (e.target === backdrop || e.target.closest('[data-close]')) closeLayer();
  });

  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      closeLayer();
      closePop();
      mega && mega.classList.remove('open');
    }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      const palette = $('#pal');
      if (palette) activeLayer === palette ? closeLayer() : openLayer(palette);
    }
  });

  function toast(msg, opts = {}) {
    const container = $('#toasts');
    if (!container) return;
    const el = document.createElement('div');
    el.className = 'toast enter';
    el.innerHTML = `${I(opts.icon || 'check')}<span>${msg}</span>${opts.action ? `<button type="button">${opts.action}</button>` : ''}`;
    container.append(el);
    requestAnimationFrame(() => requestAnimationFrame(() => el.classList.remove('enter')));
    const dismiss = () => { el.classList.add('leave'); setTimeout(() => el.remove(), 380); };
    let timer = setTimeout(dismiss, opts.ms || 4000);
    el.addEventListener('pointerenter', () => clearTimeout(timer));
    el.addEventListener('pointerleave', () => { timer = setTimeout(dismiss, 2000); });
    const actionBtn = el.querySelector('button');
    actionBtn && actionBtn.addEventListener('click', () => { opts.onAction && opts.onAction(); clearTimeout(timer); dismiss(); });
  }

  window.toast = toast;
  window.SAMP = Object.assign(window.SAMP || {}, { openLayer, closeLayer, toast });

  document.addEventListener('click', e => {
    const trigger = e.target.closest('[data-toast]');
    if (trigger) {
      e.preventDefault();
      toast(trigger.dataset.toast, { icon: trigger.dataset.toastIcon });
    }
  });
})();
