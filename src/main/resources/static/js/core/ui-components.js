/* =====================================================================
   SAMP · Componentes de interfaz compartidos (pestañas, favoritos, reveal)
   ===================================================================== */
(() => {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const REDUCE = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const I = window.SAMP?.I || (() => '');

  function initSlider(group, selector, indicatorClass) {
    const bar = document.createElement('span');
    bar.className = indicatorClass;
    group.prepend(bar);
    const buttons = $$(selector, group);
    const place = (btn, instant) => {
      if (instant) bar.style.transition = 'none';
      bar.style.width = btn.offsetWidth + 'px';
      bar.style.transform = `translateX(${btn.offsetLeft - (indicatorClass === 'pill' ? 4 : 0)}px)`;
      if (instant) { bar.offsetWidth; bar.style.transition = ''; }
    };
    buttons.forEach(btn => btn.addEventListener('click', () => {
      buttons.forEach(x => x.setAttribute('aria-selected', String(x === btn)));
      place(btn);
      const pane = btn.dataset.show;
      if (pane) {
        const scope = group.closest('[data-tabs-scope]') || document;
        $$('[data-pane]', scope).forEach(p => { p.hidden = p.dataset.pane !== pane; });
      }
    }));
    const active = buttons.find(b => b.getAttribute('aria-selected') === 'true') || buttons[0];
    if (active) {
      active.setAttribute('aria-selected', 'true');
      requestAnimationFrame(() => place(active, true));
    }
    window.addEventListener('resize', () => {
      const current = buttons.find(b => b.getAttribute('aria-selected') === 'true');
      current && place(current, true);
    });
  }
  $$('.seg').forEach(g => initSlider(g, 'button', 'pill'));
  $$('.tabs').forEach(g => initSlider(g, 'button', 'bar'));

  document.addEventListener('click', e => {
    const fav = e.target.closest('.fav');
    if (!fav) return;
    e.preventDefault();
    e.stopPropagation();
    fav.classList.toggle('on');
    if (fav.classList.contains('on') && window.toast) {
      window.toast('Guardado en favoritos.', { action: 'Deshacer', onAction: () => fav.classList.remove('on'), icon: 'heart' });
    }
  });

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('in');
      observer.unobserve(entry.target);
    });
  }, { rootMargin: '0px 0px -8% 0px' });
  $$('.io').forEach(el => REDUCE ? el.classList.add('in') : observer.observe(el));

  $$('[data-type]').forEach(inp => {
    const words = inp.dataset.type.split('|');
    if (REDUCE) { inp.placeholder = words[0]; return; }
    let w = 0, i = 0, del = false, stop = false;
    ['focus', 'keydown', 'pointerdown'].forEach(ev => inp.addEventListener(ev, () => {
      stop = true;
      inp.placeholder = 'Escribe un producto o código de barras';
    }));
    (function step() {
      if (stop) return;
      const word = words[w];
      inp.placeholder = word.slice(0, i) + '▍';
      if (!del && i < word.length) i++;
      else if (!del) { del = true; return setTimeout(step, 1500); }
      else if (i > 0) i--;
      else { del = false; w = (w + 1) % words.length; }
      setTimeout(step, del ? 35 : 75);
    })();
  });

  $$('[data-progress]').forEach(box => {
    const items = $$('.pstore', box);
    const finish = item => {
      const failed = item.dataset.fail != null;
      item.classList.add(failed ? 'fail' : 'done');
      const st = $('.st', item);
      if (st) {
        st.className = 'st ' + (failed ? 'bad' : 'ok');
        st.innerHTML = I(failed ? 'alert' : 'check');
      }
      const lbl = $('.lbl2', item);
      if (lbl && item.dataset.result) lbl.textContent = item.dataset.result;
    };
    if (REDUCE) return items.forEach(finish);
    items.forEach((p, idx) => setTimeout(() => finish(p), 450 + idx * 280));
  });

  const views = $$('[data-view]');
  if (views.length) {
    const syncView = () => {
      const hash = window.location.hash.slice(1) || views[0].dataset.view;
      views.forEach(v => { v.hidden = v.dataset.view !== hash; });
      $$('[data-nav]').forEach(a => a.setAttribute('aria-current', a.dataset.nav === hash ? 'page' : 'false'));
    };
    window.addEventListener('hashchange', syncView);
    syncView();
  }

  document.addEventListener('click', e => {
    const copyBtn = e.target.closest('[data-copy]');
    if (copyBtn) {
      navigator.clipboard && navigator.clipboard.writeText(copyBtn.dataset.copy);
      window.toast && window.toast('Copiado: ' + copyBtn.dataset.copy, { icon: 'copy' });
    }
    const revealBtn = e.target.closest('[data-reveal]');
    if (revealBtn) {
      const target = $(revealBtn.dataset.reveal);
      if (target) {
        target.type = target.type === 'password' ? 'text' : 'password';
        revealBtn.setAttribute('aria-pressed', String(target.type === 'text'));
      }
    }
  });
})();
