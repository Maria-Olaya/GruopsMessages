// ===================== FILES PANEL =====================

let _filesCache     = [];
let _filesPanelOpen = false;

// ── Carga archivos de un canal ─────────────────────────────
async function loadChannelFiles(channelId) {
  _resetFilesPanel();
  const { ok, data } = await apiCall('GET', `/messages/channel/${channelId}/files`);
  if (!ok) return;
  _filesCache = data || [];
  if (_filesPanelOpen) _renderFilesPanel(_filesCache);
}

// ── Carga archivos del general (por groupId) ───────────────
async function loadGeneralFiles(groupId) {
  _resetFilesPanel();
  const { ok, data } = await apiCall('GET', `/messages/group/${groupId}/files`);
  if (!ok) return;
  _filesCache = data || [];
  if (_filesPanelOpen) _renderFilesPanel(_filesCache);
}

function _resetFilesPanel() {
  _filesCache = [];
  _filesPanelOpen = false;
  const panel = document.getElementById('files-panel');
  if (panel) panel.classList.remove('open');
}

// ── Nuevo archivo recibido por WS ──────────────────────────
function onNewFileMessage(msg) {
  if (msg.type !== 'IMAGE' && msg.type !== 'FILE') return;
  if (msg.deleted) return;
  // Evitar duplicados: si ya existe el messageId, no agregar
  const already = _filesCache.some(f => f.messageId === msg.messageId);
  if (already) return;
  _filesCache.unshift(msg);
  if (_filesPanelOpen) _renderFilesPanel(_filesCache);
}

// ── Abrir / cerrar panel ───────────────────────────────────
function toggleFilesPanel() {
  const panel = document.getElementById('files-panel');
  const btn   = document.getElementById('btn-files-panel');
  if (!panel) return;

  _filesPanelOpen = !_filesPanelOpen;
  panel.classList.toggle('open', _filesPanelOpen);
  btn?.classList.toggle('active', _filesPanelOpen);

  if (_filesPanelOpen) {
    // Posicionar el panel debajo del botón usando coordenadas del viewport
    _positionPanel(panel, btn);
    _renderFilesPanel(_filesCache);
    const input = document.getElementById('fp-search');
    if (input) { input.value = ''; input.focus(); }
    // Listener para cerrar al hacer clic fuera
    setTimeout(() => document.addEventListener('click', _closePanelOutside), 50);
  } else {
    document.removeEventListener('click', _closePanelOutside);
  }
}

function _positionPanel(panel, btn) {
  if (!btn) return;
  const rect = btn.getBoundingClientRect();
  // Alinear borde derecho del panel con borde derecho del botón
  const panelW = 300;
  let left = rect.right - panelW;
  let top  = rect.bottom + 8;

  // No salir de la pantalla por la izquierda
  if (left < 8) left = 8;
  // No salir de la pantalla por abajo
  const maxTop = window.innerHeight - 400;
  if (top > maxTop) top = rect.top - 8 - Math.min(400, panel.scrollHeight || 400);

  panel.style.left = left + 'px';
  panel.style.top  = top  + 'px';
  panel.style.right = 'auto';
}

function _closePanelOutside(e) {
  const panel = document.getElementById('files-panel');
  const btn   = document.getElementById('btn-files-panel');
  if (!panel || !_filesPanelOpen) return;
  if (!panel.contains(e.target) && !btn?.contains(e.target)) {
    _filesPanelOpen = false;
    panel.classList.remove('open');
    btn?.classList.remove('active');
    document.removeEventListener('click', _closePanelOutside);
  }
}

// ── Búsqueda en tiempo real ────────────────────────────────
function filterFilesPanel(q) {
  const term = q.toLowerCase().trim();
  const filtered = term
    ? _filesCache.filter(f => (f.fileName || '').toLowerCase().includes(term))
    : _filesCache;
  _renderFilesPanel(filtered, term);
}

// ── Renderizar lista ───────────────────────────────────────
function _renderFilesPanel(files, highlight) {
  const list = document.getElementById('fp-list');
  if (!list) return;

  if (!files || !files.length) {
    list.innerHTML = `<div class="fp-empty">${highlight ? 'Sin resultados para "' + escHtml(highlight) + '"' : 'Sin archivos aún'}</div>`;
    return;
  }

  // Agrupar por fecha
  const groups = {};
  files.forEach(f => {
    const d   = new Date(f.sentAt);
    const key = d.toLocaleDateString('es', { day: 'numeric', month: 'long', year: 'numeric' });
    if (!groups[key]) groups[key] = [];
    groups[key].push(f);
  });

  list.innerHTML = Object.entries(groups).map(([date, items]) => `
    <div class="fp-date-header">${date}</div>
    ${items.map(_buildFileRow).join('')}
  `).join('');
}

function _buildFileRow(f) {
  const icon   = _fileIcon(f.type, f.fileName);
  const name   = escHtml(f.fileName || 'archivo');
  const time   = new Date(f.sentAt).toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' });
  const sender = escHtml(f.senderName || '');
  const url    = escHtml(f.fileUrl || '');
  return `
    <div class="fp-row" onclick="window.open('${url}', '_blank')" title="${name}">
      <span class="fp-icon">${icon}</span>
      <div class="fp-info">
        <div class="fp-name">${name}</div>
        <div class="fp-meta">${sender} · ${time}</div>
      </div>
    </div>`;
}

function _fileIcon(type, fileName) {
  if (type === 'IMAGE') return '🖼️';
  const ext = (fileName || '').split('.').pop().toLowerCase();
  const map = {
    pdf: '📕', doc: '📝', docx: '📝',
    xls: '📗', xlsx: '📗',
    ppt: '📙', pptx: '📙',
    zip: '🗜️', rar: '🗜️',
    mp4: '🎬', mov: '🎬', avi: '🎬',
    mp3: '🎵', wav: '🎵',
    txt: '📄',
  };
  return map[ext] || '📎';
}