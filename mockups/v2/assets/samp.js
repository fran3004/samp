/* =====================================================================
   SAMP · comportamiento compartido de los mockups v2
   - Inyecta iconos, barra superior, navegación, pie, barra móvil y overlays.
   - En Thymeleaf cada bloque de render*() se convierte en un th:fragment.
   - ?capture deja todo en su estado final (para exportar a Figma).
   ===================================================================== */
(() => {
const $ = (s, r = document) => r.querySelector(s), $$ = (s, r = document) => [...r.querySelectorAll(s)];
const CAP = /[?&]capture/.test(location.search);
const REDUCE = matchMedia('(prefers-reduced-motion: reduce)').matches;
const STILL = CAP || REDUCE;
if (CAP) document.documentElement.classList.add('cap');
// captura a Figma: carga el script oficial solo cuando la URL lo pide
if (location.hash.includes('figmacapture')) { const sc = document.createElement('script'); sc.src = 'https://mcp.figma.com/mcp/html-to-design/capture.js'; sc.async = true; document.head.appendChild(sc); }
const QS = new URLSearchParams(location.search);
const HV = (location.hash.startsWith('#figma') ? '' : location.hash.slice(1)) || QS.get('view') || '';
const page = document.body.dataset.page || '', layout = document.body.dataset.layout || 'public';
const root = location.pathname.includes('/admin/') || location.pathname.includes('/cuenta/') ? '../' : '';

/* ---------------- iconos (sprite) ---------------- */
const P = {
  logo:'',
  search:'<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  heart:'<path d="M12 20s-7-4.4-9-9a5 5 0 0 1 9-3 5 5 0 0 1 9 3c-2 4.6-9 9-9 9z"/>',
  bell:'<path d="M6 16v-5a6 6 0 0 1 12 0v5l2 2H4zM10 21h4"/>',
  user:'<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
  arrow:'<path d="M7 17 17 7M8 7h9v9"/>',
  right:'<path d="M5 12h14M13 6l6 6-6 6"/>',
  check:'<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  x:'<path d="M6 6l12 12M18 6 6 18"/>',
  chev:'<path d="m6 9 6 6 6-6"/>',
  chevr:'<path d="m9 6 6 6-6 6"/>',
  chevl:'<path d="m15 6-6 6 6 6"/>',
  menu:'<path d="M4 7h16M4 12h16M4 17h10"/>',
  camera:'<path d="M4 8h3l2-3h6l2 3h3v11H4z"/><circle cx="12" cy="13" r="3.5"/>',
  barcode:'<path d="M4 6v12M7 6v12M10 6v12M14 6v12M16 6v12M20 6v12M4 4h3M17 4h3M4 20h3M17 20h3"/>',
  spark:'<path d="M12 3c.4 3.8 2.2 5.6 6 6-3.8.4-5.6 2.2-6 6-.4-3.8-2.2-5.6-6-6 3.8-.4 5.6-2.2 6-6zM19 15c.2 1.6 1 2.4 2.5 2.5-1.6.2-2.3 1-2.5 2.5-.2-1.6-1-2.3-2.5-2.5 1.6-.2 2.3-1 2.5-2.5z"/>',
  cpu:'<rect x="6" y="6" width="12" height="12" rx="2"/><rect x="9.5" y="9.5" width="5" height="5" rx="1"/><path d="M9 3v3M15 3v3M9 18v3M15 18v3M3 9h3M3 15h3M18 9h3M18 15h3"/>',
  sliders:'<path d="M4 7h10M18 7h2M4 17h4M12 17h8"/><circle cx="16" cy="7" r="2"/><circle cx="10" cy="17" r="2"/>',
  grid:'<rect x="4" y="4" width="7" height="7" rx="2"/><rect x="13" y="4" width="7" height="7" rx="2"/><rect x="4" y="13" width="7" height="7" rx="2"/><rect x="13" y="13" width="7" height="7" rx="2"/>',
  list:'<path d="M9 6h11M9 12h11M9 18h11M4 6h1M4 12h1M4 18h1"/>',
  truck:'<path d="M3 6h11v10H3zM14 10h4l3 3v3h-7"/><circle cx="7" cy="18" r="2"/><circle cx="17" cy="18" r="2"/>',
  receipt:'<path d="M6 3h12v18l-3-2-3 2-3-2-3 2zM9 8h6M9 12h6"/>',
  clock:'<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  info:'<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5M12 8h.01"/>',
  alert:'<path d="M12 4 2.5 20h19zM12 10v4M12 17h.01"/>',
  down:'<path d="M3 7l6 6 4-4 8 8M21 11v6h-6"/>',
  up:'<path d="M3 17l6-6 4 4 8-8M21 13V7h-6"/>',
  ext:'<path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"/>',
  share:'<circle cx="6" cy="12" r="2.5"/><circle cx="18" cy="6" r="2.5"/><circle cx="18" cy="18" r="2.5"/><path d="m8.2 10.8 7.6-3.6M8.2 13.2l7.6 3.6"/>',
  cols:'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M9 4v16M15 4v16"/>',
  trash:'<path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13"/>',
  plus:'<path d="M12 5v14M5 12h14"/>',
  minus:'<path d="M5 12h14"/>',
  gear:'<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-2.7-1.1l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.1-2.7l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 2.7-1.1V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 2.7 1.1l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0 1.1 2.7H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1.3z"/>',
  logout:'<path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10"/>',
  globe:'<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.5 2.6 3.5 5.4 3.5 8.5s-1 5.9-3.5 8.5c-2.5-2.6-3.5-5.4-3.5-8.5s1-5.9 3.5-8.5z"/>',
  home:'<path d="M4 11 12 4l8 7v9h-5v-6H9v6H4z"/>',
  send:'<path d="M4 12 20 4l-6 16-3-7z"/>',
  mic:'<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/>',
  image:'<rect x="3" y="4" width="18" height="16" rx="3"/><circle cx="9" cy="10" r="2"/><path d="m21 16-5-5-9 9"/>',
  upload:'<path d="M12 16V4M7 9l5-5 5 5M4 16v4h16v-4"/>',
  refresh:'<path d="M20 11a8 8 0 0 0-14.5-4.5L4 8M4 4v4h4M4 13a8 8 0 0 0 14.5 4.5L20 16M20 20v-4h-4"/>',
  db:'<ellipse cx="12" cy="6" rx="7" ry="3"/><path d="M5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6"/>',
  shield:'<path d="M12 3 5 6v6c0 4.4 3 7.6 7 9 4-1.4 7-4.6 7-9V6z"/><path d="m9 12 2 2 4-4"/>',
  zap:'<path d="M13 3 5 13h6l-1 8 8-10h-6z"/>',
  play:'<path d="M7 5v14l12-7z"/>',
  pause:'<path d="M8 5v14M16 5v14"/>',
  eye:'<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  mail:'<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m4 7 8 6 8-6"/>',
  lock:'<rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
  users:'<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0M16 4.5a3.5 3.5 0 0 1 0 7M18 14a6 6 0 0 1 3.5 6"/>',
  layers:'<path d="m12 3 9 5-9 5-9-5zM3 13l9 5 9-5"/>',
  act:'<path d="M3 12h4l3-8 4 16 3-8h4"/>',
  grip:'<circle cx="9" cy="6" r="1"/><circle cx="15" cy="6" r="1"/><circle cx="9" cy="12" r="1"/><circle cx="15" cy="12" r="1"/><circle cx="9" cy="18" r="1"/><circle cx="15" cy="18" r="1"/>',
  more:'<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
  copy:'<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V5a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3"/>',
  hist:'<path d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5M12 7v5l3 2"/>',
  box:'<path d="m12 3 8 4.5v9L12 21l-8-4.5v-9z"/><path d="m4 7.5 8 4.5 8-4.5M12 12v9"/>',
  pad:'<path d="M7 9v4M5 11h4M15 10h.01M18 12h.01"/><path d="M6.5 6h11A4.5 4.5 0 0 1 22 10.5v2A5.5 5.5 0 0 1 16.5 18c-1.5 0-2.5-1-3.5-2h-2c-1 1-2 2-3.5 2A5.5 5.5 0 0 1 2 12.5v-2A4.5 4.5 0 0 1 6.5 6z"/>',
  monitor:'<rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/>',
  head:'<path d="M4 15v-3a8 8 0 0 1 16 0v3"/><rect x="3" y="14" width="4" height="6" rx="2"/><rect x="17" y="14" width="4" height="6" rx="2"/>',
  phone:'<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
  laptop:'<rect x="5" y="5" width="14" height="10" rx="1.5"/><path d="M2 19h20"/>',
  watch:'<rect x="6" y="6" width="12" height="12" rx="3"/><path d="M9 6V3h6v3M9 18v3h6v-3M12 10v2.5l1.5 1"/>',
  speaker:'<rect x="6" y="3" width="12" height="18" rx="3"/><circle cx="12" cy="14" r="3"/><path d="M12 7h.01"/>',
  cart:'<path d="M3 4h2l2.5 11h11l2-8H6.5"/><circle cx="9" cy="19.5" r="1.5"/><circle cx="17" cy="19.5" r="1.5"/>',
  tag:'<path d="M3 12V4h8l10 10-8 8z"/><circle cx="7.5" cy="8.5" r="1.5"/>',
  gpu:'<rect x="2" y="7" width="20" height="10" rx="2"/><circle cx="9" cy="12" r="2.5"/><circle cx="16" cy="12" r="2.5"/><path d="M5 17v3M2 10h-0"/>',
  ram:'<path d="M3 7h18v9H3zM6 16v3M10 16v3M14 16v3M18 16v3"/><path d="M7 10h2v3H7zM11 10h2v3h-2zM15 10h2v3h-2z"/>',
  hdd:'<rect x="3" y="6" width="18" height="12" rx="2"/><path d="M7 14h.01M11 14h6"/>',
  psu:'<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="10" cy="12" r="4"/><path d="M10 8v8M6 12h8M17 9v6"/>',
  case:'<rect x="6" y="2.5" width="12" height="19" rx="2"/><circle cx="12" cy="15" r="3"/><path d="M9 6h6"/>',
  fan:'<circle cx="12" cy="12" r="2"/><path d="M12 10c0-4 1-6 4-6s2 4-2 7M14 12c4 0 6 1 6 4s-4 2-7-2M12 14c0 4-1 6-4 6s-2-4 2-7M10 12c-4 0-6-1-6-4s4-2 7 2"/>',
  board:'<rect x="3" y="3" width="18" height="18" rx="2"/><rect x="7" y="7" width="6" height="6" rx="1"/><path d="M16 7v10M7 16h6"/>',
  filter:'<path d="M3 5h18l-7 8v6l-4 2v-8z"/>',
  sort:'<path d="M7 4v16M3 8l4-4 4 4M17 20V4M13 16l4 4 4-4"/>',
  star:'<path d="m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>',
  github:'<path d="M9 19c-4.3 1.4-4.3-2.5-6-3M15 21v-3.5c0-1 .1-1.4-.5-2 2.8-.3 5.5-1.4 5.5-6a4.6 4.6 0 0 0-1.3-3.2 4.2 4.2 0 0 0-.1-3.2s-1.1-.3-3.5 1.3a12.3 12.3 0 0 0-6.2 0C6.5 2.8 5.4 3.1 5.4 3.1a4.2 4.2 0 0 0-.1 3.2A4.6 4.6 0 0 0 4 9.5c0 4.6 2.7 5.7 5.5 6-.6.6-.6 1.2-.5 2V21"/>',
  key:'<circle cx="8" cy="15" r="4"/><path d="m11 12 9-9M17 6l3 3M14 9l2 2"/>',
  scale:'<path d="M12 4v16M6 20h12M4 8h16M7 8l-3 7a3 3 0 0 0 6 0zM17 8l-3 7a3 3 0 0 0 6 0z"/>',
  flag:'<path d="M5 21V4h11l-2 4 2 4H5"/>',
  wand:'<path d="m4 20 11-11M14 4v2M18 8h2M17.5 5.5l1.5-1.5M10 7h-.01M17 13h-.01"/>'
};
const LOGO = '<symbol id="logo" viewBox="0 0 64 64"><path d="M6 31 34 9c5-4 12-5 22-5l-5 24-31 6z" fill="#0B7BF2"/><path d="M6 31 20 34l31-6-5 9-26 5z" fill="#1447C0" opacity=".55"/><path d="M30 22 44 12" stroke="#fff" stroke-width="4" stroke-linecap="round"/><rect x="5" y="33" width="46" height="25" rx="12.5" fill="#22E3D2"/><rect x="16" y="42" width="24" height="7" rx="3.5" fill="#fff"/></symbol>';
const sprite = document.createElement('div');
sprite.innerHTML = '<svg width="0" height="0" style="position:absolute" aria-hidden="true"><defs>' + LOGO +
  Object.entries(P).filter(([k]) => k !== 'logo').map(([k, v]) => `<symbol id="i-${k}" viewBox="0 0 24 24">${v}</symbol>`).join('') +
  '<linearGradient id="ga" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="#0A62C4" stop-opacity=".18"/><stop offset="1" stop-color="#0A62C4" stop-opacity="0"/></linearGradient></defs></svg>';
document.body.prepend(sprite.firstChild);
const I = (n, c = '') => `<svg class="ic ${c}" aria-hidden="true"><use href="#i-${n}"/></svg>`;
window.SAMP = { I, HV, QS };

/* ---------------- fragmentos ---------------- */
const logo = (dark) => `<a class="logo" href="${root}index.html" aria-label="SAMP, inicio"><svg><use href="#logo"/></svg><span>SAMP<small>MEJORES PRECIOS</small></span></a>`;
const signedIn = document.body.dataset.user !== 'anon';

function renderPublicNav() {
  const cur = (p) => page === p ? ' aria-current="page"' : '';
  return `
<div class="live" role="status"><span class="sdot ok pulse"></span><span><b>12.480 ofertas</b> actualizadas hoy a las 10:42 · 6 tiendas conectadas · Próxima consulta en <b class="num" data-countdown="5520">1 h 32 min</b></span></div>
<header class="nav" id="nav"><div class="wrap">
  <button class="ibtn burger" data-drawer="mnav" aria-label="Abrir menú">${I('menu')}</button>
  ${logo()}
  <nav class="menu" aria-label="Principal">
    <button class="nl" data-mega aria-expanded="false">Categorías ${I('chev', 'chev')}</button>
    <a class="nl opt" href="${root}armador-pc.html"${cur('pc')}>Armar PC</a>
    <a class="nl" href="${root}asistente.html"${cur('ia')}>${I('spark', 'sm')}Asistente <span class="new">IA</span></a>
  </nav>
  <button class="nsearch" data-palette aria-label="Buscar">${I('search')}<span>Busca un producto o código de barras</span><span class="kbd">Ctrl</span><span class="kbd">K</span></button>
  <div class="r">
    <a class="ibtn hide-m tip" data-tip="Escanear código" href="${root}escanear.html" aria-label="Escanear código">${I('barcode')}</a>
    <a class="ibtn hide-m tip" data-tip="Favoritos" href="${root}cuenta.html#favoritos" aria-label="Favoritos">${I('heart')}</a>
    <button class="ibtn" data-pop="p-notif" aria-label="Notificaciones">${I('bell')}<span class="dot">3</span></button>
    <button class="cur hide-m" data-pop="p-cur" aria-label="Moneda">${I('globe', 'sm')}COP ${I('chev', 'sm')}</button>
    ${signedIn ? `<button class="ibtn" data-pop="p-user" aria-label="Tu cuenta" style="margin-left:4px"><span class="avatar">LM</span></button>`
               : `<a class="btn btn-p sm" href="${root}entrar.html">${I('user', 'sm')}Entrar</a>`}
  </div>
</div>
${megaMenu()}
</header>`;
}

function megaMenu(stat) {
  const cats = [['phone', 'Celulares'], ['laptop', 'Computadores'], ['cpu', 'Componentes PC'], ['head', 'Audio'], ['pad', 'Consolas y juegos'], ['watch', 'Relojes'], ['speaker', 'Hogar inteligente'], ['cart', 'Supermercado']];
  return `<div class="mega${stat ? ' static' : ''}" id="mega" role="dialog" aria-label="Categorías"><div class="in wrap">
  <div class="cats">${cats.map(([i, n], k) => `<a class="mi${k === 1 ? ' hl' : ''}" href="${root}resultados.html">${I(i)}${n}${k === 1 ? I('chevr', 'sm end') : ''}</a>`).join('')}</div>
  <div class="lk"><h4>Computadores</h4>${['Portátiles', 'Ultralivianos', 'Gamer', 'MacBook', 'Todo en uno', 'Monitores', 'Teclados y ratones'].map(t => `<a href="${root}resultados.html">${t}</a>`).join('')}</div>
  <div class="lk"><h4>Marcas</h4>${['Apple', 'Lenovo', 'Asus', 'Dell', 'HP', 'Huawei', 'Acer'].map(t => `<a href="${root}resultados.html">${t}</a>`).join('')}</div>
  <a class="promo" href="${root}armador-pc.html"><span class="chip teal">${I('cpu')}Nuevo</span><h3 style="font-size:22px">Arma tu PC gamer con precios reales</h3><p class="ink2" style="font-size:14px">Comprobamos compatibilidad, consumo y el mejor precio de cada pieza.</p><img src="https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/thumbnail.webp" alt=""></a>
</div></div>`;
}

function overlays() {
  return `
<div class="pop" id="p-notif" style="--o:top right">${notifPanel()}</div>
<div class="pop" id="p-user" style="--o:top right">${userMenu()}</div>
<div class="pop" id="p-cur" style="--o:top right;min-width:260px">${curMenu()}</div>
<div class="backdrop" id="bd"></div>
<aside class="drawer" id="mnav" aria-label="Menú">${mobileNav()}</aside>
<div class="palette" id="pal" role="dialog" aria-label="Buscar">${palette()}</div>
<div class="toasts" id="toasts" aria-live="polite"></div>`;
}
const notifPanel = () => `<div class="notif"><div class="hd"><h4>Notificaciones</h4><button class="btn btn-q sm">Marcar leídas</button></div>
  <a class="it unread" href="${root}producto.html"><span class="ico" style="background:var(--ok-50);color:var(--ok)">${I('down')}</span><div><p><b>AirPods Max bajó a $ 2.116.000</b> en Mercado Libre. Tu alerta era de $ 2.200.000.</p><time>Hace 12 min</time></div></a>
  <a class="it unread" href="${root}cuenta.html#alertas"><span class="ico" style="background:var(--act-50);color:var(--act)">${I('bell')}</span><div><p><b>MacBook Pro 14"</b> está a 6 % de tu precio objetivo.</p><time>Hace 2 h</time></div></a>
  <a class="it unread" href="${root}armador-pc.html"><span class="ico" style="background:var(--teal-50);color:var(--teal-ink)">${I('cpu')}</span><div><p>Tu armado <b>«Gamer 1440p»</b> bajó $ 184.000 en total.</p><time>Ayer</time></div></a>
  <a class="it" href="#"><span class="ico" style="background:var(--warn-50);color:var(--warn)">${I('alert')}</span><div><p><b>Tienda demo</b> no respondió en la última consulta. Mostramos datos del 16 nov.</p><time>16 nov</time></div></a>
  <div class="ft"><a class="link" href="${root}cuenta.html#notificaciones">Ver todas ${I('right', 'sm')}</a></div></div>`;
const userMenu = () => `<div class="row" style="padding:10px 12px 12px;--g:12px"><span class="avatar">LM</span><div><b style="display:block">Laura Méndez</b><span class="mut" style="font-size:13px">laura@correo.co</span></div></div><div class="msep"></div>
  <a class="mi" href="${root}cuenta.html#favoritos">${I('heart')}Favoritos<span class="end">12</span></a>
  <a class="mi" href="${root}cuenta.html#alertas">${I('bell')}Alertas de precio<span class="end">4</span></a>
  <a class="mi" href="${root}cuenta.html#armados">${I('cpu')}Mis armados<span class="end">2</span></a>
  <a class="mi" href="${root}cuenta.html#preferencias">${I('gear')}Preferencias</a>
  <div class="msep"></div><a class="mi" href="${root}admin.html">${I('shield')}Panel de administración<span class="end chip dark" style="height:20px;font-size:11px">Admin</span></a>
  <div class="msep"></div><a class="mi dg" href="${root}entrar.html">${I('logout')}Cerrar sesión</a>`;
const curMenu = () => `<div class="mhead">Moneda de visualización</div>
  ${[['COP', 'Peso colombiano', 1], ['USD', 'Dólar estadounidense', 0], ['MXN', 'Peso mexicano', 0], ['EUR', 'Euro', 0]].map(([c, n, on]) => `<button class="mi${on ? ' hl' : ''}"><b class="mono" style="width:38px">${c}</b>${n}${on ? I('check', 'sm end') : ''}</button>`).join('')}
  <div class="msep"></div><p class="hint" style="padding:6px 12px 8px">Guardamos en USD. Tasa del 17 nov: 1 USD = $ 3.912 COP.</p>`;
const mobileNav = () => `<div class="dh">${logo()}<button class="ibtn" data-close aria-label="Cerrar">${I('x')}</button></div><div class="db">
  <button class="nsearch" data-palette style="max-width:none;width:100%;margin:4px 0 12px">${I('search')}<span>Buscar productos</span></button>
  ${[['home', 'Inicio', 'index.html'], ['grid', 'Categorías', 'resultados.html'], ['cpu', 'Armar PC', 'armador-pc.html'], ['spark', 'Asistente IA', 'asistente.html'], ['barcode', 'Escanear código', 'escanear.html'], ['cols', 'Comparar', 'comparar.html']].map(([i, n, h]) => `<a class="mi" href="${root}${h}" style="min-height:52px;font-size:16px">${I(i)}${n}</a>`).join('')}
  <div class="msep"></div>
  ${[['heart', 'Favoritos', 'cuenta.html#favoritos'], ['bell', 'Alertas', 'cuenta.html#alertas'], ['gear', 'Preferencias', 'cuenta.html#preferencias']].map(([i, n, h]) => `<a class="mi" href="${root}${h}" style="min-height:52px;font-size:16px">${I(i)}${n}</a>`).join('')}
  <div class="msep"></div><div class="row between" style="padding:10px 12px"><span class="ink2" style="font-weight:600">Moneda</span><span class="chip line">${I('globe')}COP</span></div>
  </div><div style="padding:16px 20px;border-top:1px solid var(--line)"><a class="btn btn-p block" href="${root}entrar.html">${I('user', 'sm')}Entrar o crear cuenta</a></div>`;
const palette = () => `<div class="pin">${I('search', 'lg')}<input placeholder="Busca un producto, marca o código de barras" value="airpods" aria-label="Buscar"><span class="kbd">Esc</span></div>
  <div class="pb"><div class="mhead">Productos</div>
  ${[['apple-airpods-max-silver', 'mobile-accessories', 'Apple AirPods Max', '$ 2.116.000', '5 tiendas'], ['apple-airpods', 'mobile-accessories', 'Apple AirPods (3.ª gen.)', '$ 498.000', '4 tiendas'], ['beats-flex-wireless-earphones', 'mobile-accessories', 'Beats Flex', '$ 219.000', '3 tiendas']].map(([s, c, n, p, t], k) =>
   `<a class="res${k === 0 ? ' hl' : ''}" href="${root}producto.html"><img src="https://cdn.dummyjson.com/product-images/${c}/${s}/thumbnail.webp" alt=""><div class="grow"><b>${n.replace('AirPods', '<mark style="background:var(--teal-50);color:inherit;border-radius:4px">AirPods</mark>')}</b><div class="mut" style="font-size:13px">${t}</div></div><b class="num">${p}</b></a>`).join('')}
  <div class="mhead">Búsquedas recientes</div>
  ${['rtx 4070', 'iphone 13 pro', '7702004003508'].map(t => `<a class="mi" href="${root}resultados.html">${I('hist')}${t}<span class="end">${I('arrow', 'sm')}</span></a>`).join('')}
  <div class="mhead">Acciones</div>
  <a class="mi" href="${root}escanear.html">${I('barcode')}Escanear código de barras</a>
  <a class="mi" href="${root}escanear.html#foto">${I('camera')}Buscar con una foto</a>
  <a class="mi" href="${root}asistente.html">${I('spark')}Preguntarle al asistente IA</a></div>
  <div class="pf"><span><span class="kbd">↑</span> <span class="kbd">↓</span> navegar</span><span><span class="kbd">Enter</span> abrir</span><span style="margin-left:auto">Resultados de 6 tiendas</span></div>`;

function renderFooter() {
  return `<footer class="foot"><div class="wrap"><div class="cols">
  <div>${logo()}<p style="margin-top:18px;max-width:40ch">Sistema automático de mejores precios. Comparamos el mismo producto en varias tiendas y te mostramos el total real, con su fecha y su fuente.</p>
    <form class="nl-form" onsubmit="event.preventDefault();toast('Listo. Te avisaremos de las mejores bajadas.')"><input class="input" placeholder="Tu correo" aria-label="Correo"><button class="btn btn-t">Suscribirme</button></form></div>
  <div><h4>Explorar</h4><ul><li><a href="${root}resultados.html">Categorías</a></li><li><a href="${root}armador-pc.html">Armar PC</a></li><li><a href="${root}asistente.html">Asistente IA</a></li><li><a href="${root}escanear.html">Escanear código</a></li></ul></div>
  <div><h4>Tu cuenta</h4><ul><li><a href="${root}cuenta.html#favoritos">Favoritos</a></li><li><a href="${root}cuenta.html#alertas">Alertas</a></li><li><a href="${root}cuenta.html#preferencias">Preferencias</a></li></ul></div>
  <div><h4>Confianza</h4><ul><li><a href="#">Cómo calculamos el total</a></li><li><a href="#">Fuentes y permisos</a></li><li><a href="#">Datos de demostración</a></li></ul></div>
  <div><h4>Legal</h4><ul><li><a href="#">Privacidad</a></li><li><a href="#">Términos</a></li><li><a href="#">Aviso de monedas</a></li></ul></div>
  </div><div class="bt"><span>© 2026 SAMP · Proyecto académico de Programación II. SAMP no vende productos ni procesa pagos.</span><span>Tasas: open.er-api.com · Actualizado 17 nov 2026, 10:42</span></div></div></footer>`;
}
function renderTabbar() {
  const t = [['home', 'Inicio', 'index.html', 'inicio'], ['search', 'Buscar', 'resultados.html', 'resultados'], ['spark', 'IA', 'asistente.html', 'ia'], ['heart', 'Favoritos', 'cuenta.html#favoritos', 'cuenta'], ['user', 'Cuenta', 'cuenta.html#preferencias', 'perfil']];
  return `<nav class="tabbar" aria-label="Accesos">${t.map(([i, n, h, p]) => `<a href="${root}${h}"${page === p ? ' aria-current="page"' : ''}>${I(i, 'lg')}${n}${p === 'cuenta' ? '<span class="dot"></span>' : ''}</a>`).join('')}</nav>`;
}

/* ---------------- montaje ---------------- */
if (layout === 'public' || layout === 'account') {
  document.body.insertAdjacentHTML('afterbegin', renderPublicNav());
  if (!document.body.hasAttribute('data-nofoot')) document.body.insertAdjacentHTML('beforeend', renderFooter());
  document.body.insertAdjacentHTML('beforeend', renderTabbar() + overlays());
} else if (layout === 'admin' || layout === 'bare') {
  document.body.insertAdjacentHTML('beforeend', overlays());
}
window.SAMP.frag = { megaMenu, notifPanel, userMenu, curMenu, mobileNav, palette };
$$('[data-frag]').forEach(el => { const f = window.SAMP.frag[el.dataset.frag]; if (f) el.innerHTML = f(true); });

/* ---------------- interacciones ---------------- */
const nav = $('#nav');
if (nav) { const f = () => nav.classList.toggle('s', scrollY > 4); addEventListener('scroll', f, { passive: true }); f(); }

// popovers anclados al disparador
let openPop = null;
function closePop() { if (!openPop) return; openPop.pop.classList.remove('open'); openPop.btn.setAttribute('aria-expanded', 'false'); openPop = null; }
document.addEventListener('click', e => {
  const b = e.target.closest('[data-pop]');
  if (b) {
    e.preventDefault(); const pop = $('#' + b.dataset.pop); if (!pop) return;
    if (openPop && openPop.pop === pop) return closePop();
    closePop(); const r = b.getBoundingClientRect();
    pop.style.top = (r.bottom + scrollY + 8) + 'px';
    pop.style.left = Math.max(12, Math.min(r.right - pop.offsetWidth, innerWidth - pop.offsetWidth - 12)) + 'px';
    pop.classList.add('open'); b.setAttribute('aria-expanded', 'true'); openPop = { pop, btn: b }; return;
  }
  if (openPop && !e.target.closest('.pop')) closePop();
});
// mega menú (clic, con intención en hover)
const mega = $('#mega'), mb = $('[data-mega]'); let mt;
if (mega && mb) {
  const set = v => { mega.classList.toggle('open', v); mb.setAttribute('aria-expanded', v) };
  mb.addEventListener('click', () => set(!mega.classList.contains('open')));
  mb.addEventListener('pointerenter', e => { if (e.pointerType === 'mouse') { clearTimeout(mt); mt = setTimeout(() => set(true), 120) } });
  nav.addEventListener('pointerleave', () => { clearTimeout(mt); mt = setTimeout(() => set(false), 200) });
  mega.addEventListener('pointerenter', () => clearTimeout(mt));
}
// drawer, modal, sheet, palette
const bd = $('#bd');
let layer = null;
function openLayer(el) { closeLayer(true); layer = el; el.classList.add('open'); bd && bd.classList.add('open'); document.body.style.overflow = 'hidden'; const f = el.querySelector('input,button,[href]'); f && setTimeout(() => f.focus({ preventScroll: true }), 60); }
function closeLayer(silent) { if (!layer) return; layer.classList.remove('open'); bd && bd.classList.remove('open'); document.body.style.overflow = ''; layer = null; }
window.SAMP.openLayer = openLayer; window.SAMP.closeLayer = closeLayer;
document.addEventListener('click', e => {
  const t = e.target.closest('[data-drawer],[data-modal],[data-sheet],[data-palette]');
  if (t) { e.preventDefault(); const id = t.dataset.drawer || t.dataset.modal || t.dataset.sheet || 'pal'; const el = $('#' + id); el && openLayer(el); return; }
  if (e.target === bd || e.target.closest('[data-close]')) closeLayer();
});
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') { closeLayer(); closePop(); mega && mega.classList.remove('open'); }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); const p = $('#pal'); if (p) { layer === p ? closeLayer() : openLayer(p); } } // sin animación: acción de teclado
});

// segmentados y pestañas con indicador deslizante
function initSlider(group, sel, ind) {
  const bar = document.createElement('span'); bar.className = ind; group.prepend(bar);
  const btns = $$(sel, group);
  const place = (b, inst) => { if (inst) bar.style.transition = 'none'; bar.style.width = b.offsetWidth + 'px'; bar.style.transform = `translateX(${b.offsetLeft - (ind === 'pill' ? 4 : 0)}px)`; if (inst) { bar.offsetWidth; bar.style.transition = ''; } };
  btns.forEach(b => b.addEventListener('click', () => {
    btns.forEach(x => x.setAttribute('aria-selected', x === b)); place(b);
    const tgt = b.dataset.show; if (tgt) { const scope = group.closest('[data-tabs-scope]') || document; $$('[data-pane]', scope).forEach(p => p.hidden = p.dataset.pane !== tgt); }
    group.dispatchEvent(new CustomEvent('change', { detail: b }));
  }));
  const on = btns.find(b => b.getAttribute('aria-selected') === 'true') || btns[0]; on && on.setAttribute('aria-selected', 'true');
  requestAnimationFrame(() => on && place(on, true));
  addEventListener('resize', () => { const o = btns.find(b => b.getAttribute('aria-selected') === 'true'); o && place(o, true) });
  document.fonts && document.fonts.ready.then(() => { const o = btns.find(b => b.getAttribute('aria-selected') === 'true'); o && place(o, true) });
}
$$('.seg').forEach(g => initSlider(g, 'button', 'pill'));
$$('.tabs').forEach(g => initSlider(g, 'button', 'bar'));

// favoritos con deshacer
document.addEventListener('click', e => {
  const f = e.target.closest('.fav'); if (!f) return; e.preventDefault(); e.stopPropagation();
  f.classList.toggle('on');
  if (f.classList.contains('on')) toast('Guardado en favoritos. Te avisaremos si baja.', { action: 'Deshacer', onAction: () => f.classList.remove('on'), icon: 'heart' });
});

// toasts (transiciones, interrumpibles)
function toast(msg, o = {}) {
  const box = $('#toasts'); if (!box) return;
  const t = document.createElement('div'); t.className = 'toast enter'; t.innerHTML = `${I(o.icon || 'check')}<span>${msg}</span>${o.action ? `<button>${o.action}</button>` : ''}`;
  box.append(t); requestAnimationFrame(() => requestAnimationFrame(() => t.classList.remove('enter')));
  let timer; const kill = () => { t.classList.add('leave'); setTimeout(() => t.remove(), 400) };
  const arm = () => timer = setTimeout(kill, o.ms || 4200); arm();
  t.addEventListener('pointerenter', () => clearTimeout(timer)); t.addEventListener('pointerleave', arm);
  const b = t.querySelector('button'); b && b.addEventListener('click', () => { o.onAction && o.onAction(); clearTimeout(timer); kill() });
}
window.toast = toast;
document.addEventListener('click', e => { const t = e.target.closest('[data-toast]'); if (t) { e.preventDefault(); toast(t.dataset.toast, { icon: t.dataset.toastIcon }) } });

// cifras que suben
const fmt = n => Math.round(n).toLocaleString('es-CO');
function count(el) {
  const n = +el.dataset.count, pre = el.dataset.pre || '', suf = el.dataset.suf || '', dec = +el.dataset.dec || 0;
  const out = v => el.textContent = pre + (dec ? v.toFixed(dec).replace('.', ',') : fmt(v)) + suf;
  if (STILL) return out(n);
  const t0 = performance.now(), d = +el.dataset.ms || 1100;
  (function f(t) { const k = Math.min(1, (t - t0) / d), e = 1 - Math.pow(1 - k, 4); out(n * e); if (k < 1) requestAnimationFrame(f) })(t0);
}
// revelado al entrar en pantalla
const io = new IntersectionObserver(es => es.forEach(e => { if (!e.isIntersecting) return; const el = e.target; el.classList.add('in'); if (el.dataset.count != null) count(el); $$('[data-count]', el).forEach(c => { if (!c.dataset.done) { c.dataset.done = 1; count(c) } }); io.unobserve(el) }), { rootMargin: '0px 0px -8% 0px' });
$$('.io,[data-count]').forEach(el => STILL ? (el.classList.add('in'), el.dataset.count != null && count(el)) : io.observe(el));

// buscador que se escribe solo (se detiene si la persona interactúa)
$$('[data-type]').forEach(inp => {
  const words = inp.dataset.type.split('|'); if (STILL) { inp.placeholder = words[0]; return; }
  let w = 0, i = 0, del = false, stop = false;
  ['focus', 'keydown', 'pointerdown'].forEach(ev => inp.addEventListener(ev, () => { stop = true; inp.placeholder = 'Escribe un producto o código de barras' }));
  (function step() { if (stop) return; const s = words[w]; inp.placeholder = s.slice(0, i) + '▍';
    if (!del && i < s.length) i++; else if (!del) { del = true; return setTimeout(step, 1600) } else if (i > 0) i--; else { del = false; w = (w + 1) % words.length }
    setTimeout(step, del ? 35 : 70 + Math.random() * 50) })();
});

// progreso de tiendas consultadas
$$('[data-progress]').forEach(box => {
  const items = $$('.pstore', box);
  const done = (p) => { const bad = p.dataset.fail != null; p.classList.add(bad ? 'fail' : 'done'); const st = $('.st', p); st.className = 'st ' + (bad ? 'bad' : 'ok'); st.innerHTML = I(bad ? 'alert' : 'check'); const l = $('.lbl2', p); if (l) l.textContent = p.dataset.result; };
  if (STILL) return items.forEach(done);
  items.forEach((p, k) => setTimeout(() => done(p), 700 + k * 420 + Math.random() * 200));
});

// skeleton → contenido
$$('[data-skel]').forEach(box => {
  const sk = $('.skel', box), real = $('.real', box); if (!sk || !real) return;
  if (STILL) { sk.remove(); real.hidden = false; return; }
  setTimeout(() => { sk.remove(); real.hidden = false; $$('.pcard', real).forEach((c, k) => { c.style.animation = `up .7s var(--ease) ${k * 50}ms both` }) }, +box.dataset.skel || 1300);
});

// cuenta regresiva
$$('[data-countdown]').forEach(el => { let s = +el.dataset.countdown; const f = () => { const h = Math.floor(s / 3600), m = Math.floor(s % 3600 / 60); el.textContent = `${h} h ${String(m).padStart(2, '0')} min`; }; f(); if (!STILL) setInterval(() => { s = Math.max(0, s - 60); f() }, 60000) });

// gráfico con cursor
$$('.chart[data-points]').forEach(ch => {
  const pts = JSON.parse(ch.dataset.points), svg = $('svg', ch), W = +svg.viewBox.baseVal.width, H = +svg.viewBox.baseVal.height, pad = 8;
  const min = Math.min(...pts.map(p => p[1])) * .97, max = Math.max(...pts.map(p => p[1])) * 1.02;
  const X = i => pad + i * (W - pad * 2) / (pts.length - 1), Y = v => H - 28 - (v - min) / (max - min) * (H - 48);
  let d = pts.map((p, i) => (i ? 'L' : 'M') + X(i).toFixed(1) + ' ' + Y(p[1]).toFixed(1)).join(' ');
  const line = $('.line', svg), area = $('.area', svg); line.setAttribute('d', d); area.setAttribute('d', d + ` L${X(pts.length - 1)} ${H - 28} L${X(0)} ${H - 28} Z`);
  const len = line.getTotalLength(); line.style.setProperty('--len', len);
  const cur = $('.cursor', svg), knob = $('.knob', svg), tip = $('.ctip', ch);
  const at = i => { const x = X(i), y = Y(pts[i][1]); cur.setAttribute('x1', x); cur.setAttribute('x2', x); knob.setAttribute('cx', x); knob.setAttribute('cy', y);
    const r = svg.getBoundingClientRect(); tip.style.left = (x / W * r.width) + 'px'; tip.style.top = (y / H * r.height) + 'px'; tip.innerHTML = `<b>$ ${fmt(pts[i][1])}</b>${pts[i][0]}`; };
  svg.addEventListener('pointermove', e => { const r = svg.getBoundingClientRect(); const i = Math.round(((e.clientX - r.left) / r.width * W - pad) / ((W - pad * 2) / (pts.length - 1))); at(Math.max(0, Math.min(pts.length - 1, i))) });
  const mi = pts.reduce((a, p, i) => p[1] < pts[a][1] ? i : a, 0); at(+ch.dataset.focus || mi); ch.classList.toggle('show', ch.dataset.show != null);
  addEventListener('resize', () => at(+ch.dataset.focus || mi));
});

// enrutado por hash para páginas con vistas (cuenta, admin)
const views = $$('[data-view]');
if (views.length) {
  const go = () => { const hh = location.hash.startsWith('#figma') ? '' : location.hash.slice(1); const h = hh || QS.get('view') || views[0].dataset.view; views.forEach(v => { const on = v.dataset.view === h; v.hidden = !on; if (on && !STILL) { v.style.animation = 'none'; v.offsetWidth; v.style.animation = 'up .5s var(--ease) both' } });
    $$('[data-nav]').forEach(a => a.setAttribute('aria-current', a.dataset.nav === h ? 'page' : 'false')); scrollTo({ top: 0, behavior: 'instant' }); };
  addEventListener('hashchange', go); go();
}
// copiar
document.addEventListener('click', e => { const c = e.target.closest('[data-copy]'); if (c) { navigator.clipboard && navigator.clipboard.writeText(c.dataset.copy); toast('Copiado: ' + c.dataset.copy, { icon: 'copy' }) } });
// rangos con relleno
$$('.range').forEach(r => { const f = () => { r.style.setProperty('--v', ((r.value - r.min) / (r.max - r.min) * 100) + '%'); const o = r.dataset.out && $(r.dataset.out); o && (o.textContent = '$ ' + fmt(+r.value)) }; r.addEventListener('input', f); f() });
// mostrar/ocultar contraseña
document.addEventListener('click', e => { const b = e.target.closest('[data-reveal]'); if (!b) return; const i = $(b.dataset.reveal); i.type = i.type === 'password' ? 'text' : 'password'; b.setAttribute('aria-pressed', i.type === 'text') });
// modo captura: saca del documento las superposiciones cerradas para que Figma no las dibuje
if (CAP) setTimeout(() => {
  $$('.drawer,.sheet,.pop,.mega,.palette,.modal,.backdrop,.toasts').forEach(el => {
    if (el.classList.contains('static') || el.classList.contains('open') || el.closest('[data-frag]')) return;
    if (el.classList.contains('toasts') && el.children.length) return;
    el.parentNode.removeChild(el);
  });
  document.documentElement.classList.add('cap-ready');
}, 500);
})();
