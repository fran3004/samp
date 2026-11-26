/* =====================================================================
   SAMP · Interactividad de la ficha de producto (gráfica SVG 30 días)
   ===================================================================== */
(() => {
  const fmt = n => Math.round(n).toLocaleString('es-CO');
  document.querySelectorAll('.chart[data-points]').forEach(chart => {
    const points = JSON.parse(chart.dataset.points || '[]');
    if (points.length < 2) return;
    const svg = chart.querySelector('svg');
    const W = +svg.viewBox.baseVal.width;
    const H = +svg.viewBox.baseVal.height;
    const pad = 8;
    const min = Math.min(...points.map(p => p[1])) * 0.97;
    const max = Math.max(...points.map(p => p[1])) * 1.02;
    const X = i => pad + i * (W - pad * 2) / (points.length - 1);
    const Y = v => H - 28 - (v - min) / Math.max(1, max - min) * (H - 48);

    const pathD = points.map((p, i) => (i ? 'L' : 'M') + X(i).toFixed(1) + ' ' + Y(p[1]).toFixed(1)).join(' ');
    const line = svg.querySelector('.line');
    const area = svg.querySelector('.area');
    line.setAttribute('d', pathD);
    area.setAttribute('d', pathD + ` L${X(points.length - 1)} ${H - 28} L${X(0)} ${H - 28} Z`);
    line.style.setProperty('--len', line.getTotalLength());

    const cursor = svg.querySelector('.cursor');
    const knob = svg.querySelector('.knob');
    const tip = chart.querySelector('.ctip');

    const moveTo = idx => {
      const x = X(idx);
      const y = Y(points[idx][1]);
      cursor.setAttribute('x1', x);
      cursor.setAttribute('x2', x);
      knob.setAttribute('cx', x);
      knob.setAttribute('cy', y);
      const rect = svg.getBoundingClientRect();
      tip.style.left = (x / W * rect.width) + 'px';
      tip.style.top = (y / H * rect.height) + 'px';
      tip.innerHTML = `<b>$ ${fmt(points[idx][1])} COP</b>${points[idx][0]}`;
    };

    svg.addEventListener('pointermove', e => {
      const rect = svg.getBoundingClientRect();
      const idx = Math.round(((e.clientX - rect.left) / rect.width * W - pad) / ((W - pad * 2) / (points.length - 1)));
      moveTo(Math.max(0, Math.min(points.length - 1, idx)));
    });

    const minIndex = points.reduce((best, p, i) => p[1] < points[best][1] ? i : best, 0);
    moveTo(minIndex);
    chart.classList.add('show');
    window.addEventListener('resize', () => moveTo(minIndex));
  });
})();
