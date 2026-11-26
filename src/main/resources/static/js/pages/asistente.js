/* =====================================================================
   SAMP · Interactividad del asistente y armador general de IA
   ===================================================================== */
(() => {
  const form = document.getElementById('ai-form');
  const input = document.getElementById('ai-input');
  const msgs = document.getElementById('msgs');
  const modeSel = document.getElementById('ai-mode');
  const catInp = document.getElementById('ai-cat');
  const budInp = document.getElementById('ai-budget');
  if (!form || !input || !msgs) return;

  // Historial reciente para mantener conversación humana con contexto
  const history = [];
  let pending = false;

  async function sendPrompt(text) {
    const clean = text.trim();
    if (!clean || pending) return;
    pending = true;

    const uDiv = document.createElement('div');
    uDiv.className = 'm u';
    const uBub = document.createElement('div');
    uBub.className = 'bub';
    uBub.textContent = clean;
    uDiv.appendChild(uBub);
    msgs.appendChild(uDiv);

    const typingDiv = document.createElement('div');
    typingDiv.className = 'm a';
    typingDiv.innerHTML = `<span class="av"><svg class="ic"><use href="#i-spark"/></svg></span>
      <div class="body"><div class="txt" style="opacity:.7">Pensando...</div></div>`;
    msgs.appendChild(typingDiv);
    typingDiv.scrollIntoView({ behavior: 'smooth', block: 'end' });
    msgs.scrollTop = msgs.scrollHeight;

    const payload = {
      message: clean,
      mode: modeSel ? modeSel.value : 'CHAT',
      category: catInp?.value || 'GENERAL',
      budgetCop: budInp?.value ? Number(budInp.value) : null,
      history: history.slice(-6)
    };

    try {
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      typingDiv.remove();

      const aDiv = document.createElement('div');
      aDiv.className = 'm a';
      const toolsHtml = (data.toolsInvoked || []).map(t => `<span class="tc">${t} ✓</span>`).join('');
      const toolsBlock = toolsHtml ? `<div class="tools">${toolsHtml}</div>` : '';
      aDiv.innerHTML = `<span class="av"><svg class="ic"><use href="#i-spark"/></svg></span>
        <div class="body">${toolsBlock}<div class="txt" style="white-space:pre-line"></div>
        <div class="val"><svg class="ic sm"><use href="#i-shield"/></svg><span>Validado (${data.providerUsed} · ${data.modelUsed} · ${data.latencyMs} ms)</span></div></div>`;
      aDiv.querySelector('.txt').textContent = data.answer;
      msgs.appendChild(aDiv);
      aDiv.scrollIntoView({ behavior: 'smooth', block: 'end' });
      msgs.scrollTop = msgs.scrollHeight;

      history.push({ role: 'user', text: clean });
      history.push({ role: 'model', text: data.answer });

      const cp = document.getElementById('ctx-prov');
      if (cp) cp.textContent = data.providerUsed;
      const cm = document.getElementById('ctx-mod');
      if (cm) cm.textContent = data.modelUsed;
    } catch {
      typingDiv.remove();
      window.toast && window.toast('No se pudo conectar con el asistente.', { icon: 'alert' });
    } finally {
      pending = false;
    }
  }

  form.addEventListener('submit', e => {
    e.preventDefault();
    const val = input.value;
    input.value = '';
    sendPrompt(val);
  });

  // Enter envía el mensaje; Shift+Enter agrega salto de línea
  input.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      const val = input.value;
      input.value = '';
      sendPrompt(val);
    }
  });

  document.querySelectorAll('[data-prompt]').forEach(btn => {
    btn.addEventListener('click', () => {
      if (btn.dataset.build && modeSel) modeSel.value = 'BUILD';
      sendPrompt(btn.dataset.prompt);
    });
  });
})();
