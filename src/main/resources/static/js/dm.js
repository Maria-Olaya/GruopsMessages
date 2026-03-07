// ============================================================
//  dm.js — Mensajes directos 1:1
//  Depende de: api.js (state, apiCall, toast, escHtml, avColor, getInitials)
//              messaging.js (stompClient, waitForWS)
// ============================================================

const dm = {
  conversations: [],
  current:       null,   // ConversationResponseDTO activa
  chatSub:       null,   // suscripción WS del chat activo
  reqSub:        null,   // suscripción WS de solicitudes entrantes
};

// ── Init — llamado desde _applySession() en auth.js ──────────
function initDm() {
  loadDmList();
  _subscribeDmRequests();
}

// ── Suscribir a notificaciones de solicitudes ─────────────────
function _subscribeDmRequests() {
  if (!stompClient?.connected) { setTimeout(_subscribeDmRequests, 600); return; }
  if (dm.reqSub) { try { dm.reqSub.unsubscribe(); } catch (_) {} }
  dm.reqSub = stompClient.subscribe(
    `/topic/dm.requests.${state.userId}`,
    frame => _onRequestEvent(JSON.parse(frame.body))
  );
}

// ── Cargar lista de chats activos + badge de pendientes ───────
async function loadDmList() {
  const { ok, data } = await apiCall('GET', '/dm');
  if (!ok) return;
  dm.conversations = Array.isArray(data) ? data : [];
  _renderDmList();
  _refreshDmBadge();
}

async function _refreshDmBadge() {
  const { ok, data } = await apiCall('GET', '/dm/requests');
  if (!ok) return;
  const count = Array.isArray(data) ? data.length : 0;
  // Reutilizamos el badge de invitaciones ya que van en el mismo panel
  const badge = document.getElementById('inv-badge');
  if (!badge) return;
  // Sumar al conteo de invitaciones de grupo
  const current = parseInt(badge.textContent) || 0;
  const groupInvCount = parseInt(badge.dataset.groupCount || '0');
  const total = groupInvCount + count;
  badge.textContent = total;
  total > 0 ? badge.classList.remove('hidden') : badge.classList.add('hidden');
  badge.dataset.dmCount = count;
}

function _renderDmList() {
  const el = document.getElementById('dm-list');
  if (!el) return;

  const active   = dm.conversations.filter(c => c.status === 'ACTIVE');
  const outgoing = dm.conversations.filter(c => c.status === 'PENDING' && !c.isIncoming);

  if (!active.length && !outgoing.length) {
    el.innerHTML = `<div class="empty-state"><p>Sin chats directos.<br>Pulsa <b>+</b> para iniciar uno.</p></div>`;
    return;
  }

  let html = '';
  active.forEach(conv => {
    const sel = dm.current?.conversationId === conv.conversationId;
    html += `<div class="group-item${sel ? ' active' : ''}" id="dm-item-${conv.conversationId}"
                  onclick="openDmChat(${conv.conversationId})">
               <div class="gi-avatar ${avColor(conv.otherUserName)}">${getInitials(conv.otherUserName)}</div>
               <div class="gi-info">
                 <div class="gi-name">${escHtml(conv.otherUserName)}</div>
                 <div class="gi-sub">Mensaje directo</div>
               </div>
             </div>`;
  });

  if (outgoing.length) {
    html += `<div class="gsidebar-label" style="margin-top:6px"><span>Pendientes (enviadas)</span></div>`;
    outgoing.forEach(conv => {
      html += `<div class="group-item" style="opacity:0.45;pointer-events:none">
                 <div class="gi-avatar ${avColor(conv.otherUserName)}">${getInitials(conv.otherUserName)}</div>
                 <div class="gi-info">
                   <div class="gi-name" style="color:var(--text3)">${escHtml(conv.otherUserName)}</div>
                   <div class="gi-sub">Esperando respuesta…</div>
                 </div>
               </div>`;
    });
  }

  el.innerHTML = html;
}

// ── Sección DM dentro del panel Invitaciones ──────────────────
async function loadDmRequestsSection() {
  const { ok, data } = await apiCall('GET', '/dm/requests');
  const el = document.getElementById('dm-requests-section');
  if (!el) return;

  if (!ok || !Array.isArray(data) || !data.length) {
    el.innerHTML = '';
    return;
  }

  el.innerHTML = `
    <div class="gsidebar-label" style="margin-top:12px;color:var(--accent)">
      <span>● Solicitudes de chat (${data.length})</span>
    </div>
    ${data.map(conv => `
      <div class="inv-item" id="dm-req-${conv.conversationId}">
        <div class="inv-group">${escHtml(conv.otherUserName)}</div>
        <div class="inv-by">${escHtml(conv.otherUserEmail)} quiere chatear contigo</div>
        <div class="inv-actions">
          <button class="btn-accept" onclick="acceptDmRequest(${conv.conversationId},'${escHtml(conv.otherUserName)}')">✓ Aceptar</button>
          <button class="btn-reject" onclick="declineDmRequest(${conv.conversationId})">✗ Rechazar</button>
        </div>
      </div>`).join('')}`;
}

// ── Aceptar / Rechazar solicitud ──────────────────────────────
async function acceptDmRequest(conversationId, otherName) {
  const { ok, data } = await apiCall('POST', `/dm/${conversationId}/accept`);
  if (!ok) return toast(data?.message || 'Error al aceptar', 'error');
  toast(`Chat con ${otherName} activado`, 'success');
  await loadDmList();
  await loadDmRequestsSection();
  _updateInvBadge();
  showPanel('dm');
  openDmChat(conversationId);
}

async function declineDmRequest(conversationId) {
  const { ok, data } = await apiCall('DELETE', `/dm/${conversationId}/decline`);
  if (!ok) return toast(data?.message || 'Error al rechazar', 'error');
  toast('Solicitud rechazada', 'info');
  document.getElementById(`dm-req-${conversationId}`)?.remove();
  await loadDmList();
  _updateInvBadge();
}

function _updateInvBadge() {
  // Recalcular badge sumando grupos + dm pendientes
  loadInvitations(); // ya actualiza inv-badge con grupo
  _refreshDmBadge();
}

// ── Iniciar DM desde modal ────────────────────────────────────
async function startDm() {
  const input = document.getElementById('dm-target-email');
  const email = input?.value.trim();
  if (!email) return toast('Ingresa el email del usuario', 'warn');
  if (email === state.userEmail) return toast('No puedes enviarte mensajes a ti mismo', 'warn');

  closeModal('modal-new-dm');

  const { ok, data } = await apiCall('POST', `/dm/start?targetEmail=${encodeURIComponent(email)}`);
  if (!ok) return toast(data?.message || 'Usuario no encontrado', 'error');

  await loadDmList();
  showPanel('dm');

  if (data.status === 'ACTIVE') {
    toast(`Chat con ${data.otherUserName} abierto`, 'success');
    openDmChat(data.conversationId);
  } else {
    toast(`Solicitud enviada a ${data.otherUserName}. Esperando que acepte.`, 'info');
  }
}

// ── Abrir chat ────────────────────────────────────────────────
async function openDmChat(conversationId) {
  let conv = dm.conversations.find(c => c.conversationId === conversationId);
  if (!conv) { await loadDmList(); conv = dm.conversations.find(c => c.conversationId === conversationId); }
  if (!conv) return;
  if (conv.status === 'PENDING' && !conv.isIncoming) {
    toast('Esperando que el otro usuario acepte tu solicitud', 'warn');
    return;
  }

  dm.current = conv;

  // Mostrar vista DM
  document.getElementById('welcome-state')?.classList.add('hidden');
  document.getElementById('group-view')?.classList.add('hidden');
  document.getElementById('dm-view')?.classList.remove('hidden');

  // Header
  const av = document.getElementById('dm-header-avatar');
  if (av) { av.textContent = getInitials(conv.otherUserName); av.className = `group-avatar-lg ${avColor(conv.otherUserName)}`; }
  const nameEl = document.getElementById('dm-header-name');
  if (nameEl) nameEl.textContent = conv.otherUserName;
  const metaEl = document.getElementById('dm-header-meta');
  if (metaEl) metaEl.textContent = conv.otherUserEmail;

  // Suscribir WS
  if (dm.chatSub) { try { dm.chatSub.unsubscribe(); } catch (_) {} dm.chatSub = null; }
  const _sub = () => {
    if (!stompClient?.connected) { setTimeout(_sub, 500); return; }
    dm.chatSub = stompClient.subscribe(`/topic/dm.${conversationId}`,
      frame => _onDmMessage(JSON.parse(frame.body)));
  };
  _sub();

  _buildDmChatUI(conv);
  _renderDmList();
}

function _buildDmChatUI(conv) {
  const area = document.getElementById('dm-chat-body');
  if (!area) return;
  area.innerHTML = `
    <div class="chat-messages" id="dm-messages"></div>
    <div class="chat-input-wrap">
      <div class="chat-input-row">
        <label class="attach-btn" title="Adjuntar archivo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
          </svg>
          <input type="file" style="display:none" accept="image/*,.pdf,.txt,.doc,.docx" onchange="handleDmFile(event)">
        </label>
        <input type="text" class="chat-input" id="dm-input"
               placeholder="Mensaje a ${escHtml(conv.otherUserName)}…"
               onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();sendDmMessage()}">
        <button class="send-btn" onclick="sendDmMessage()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
    </div>`;
  _loadDmHistory(conv.conversationId);
}

async function _loadDmHistory(conversationId) {
  const { ok, data } = await apiCall('GET', `/dm/${conversationId}/messages?size=50`);
  const el = document.getElementById('dm-messages');
  if (!ok || !el) return;
  el.innerHTML = '';
  _dmDates = {};
  (Array.isArray(data) ? data : []).forEach(m => _appendDmMsg(m, false));
  el.scrollTop = el.scrollHeight;
}

// ── Enviar ────────────────────────────────────────────────────
function sendDmMessage() {
  const input = document.getElementById('dm-input');
  const text  = input?.value.trim();
  if (!text || !dm.current || !stompClient?.connected) return;
  stompClient.send('/app/dm.send', {}, JSON.stringify({
    conversationId: dm.current.conversationId,
    type: 'TEXT', content: text, fileUrl: null, fileName: null
  }));
  input.value = '';
}

async function handleDmFile(event) {
  const file = event.target.files?.[0];
  if (!file || !dm.current) return;
  event.target.value = '';
  if (file.size > 10 * 1024 * 1024) return toast('Máximo 10 MB', 'error');
  toast('Subiendo archivo…', 'info');
  const fd = new FormData();
  fd.append('file', file);
  try {
    const res = await fetch('/api/files/upload', {
      method: 'POST', headers: { Authorization: `Bearer ${state.token}` }, body: fd
    });
    if (!res.ok) { const e = await res.json().catch(() => ({})); return toast(e.message || 'Error', 'error'); }
    const up = await res.json();
    stompClient.send('/app/dm.send', {}, JSON.stringify({
      conversationId: dm.current.conversationId,
      type: file.type.startsWith('image/') ? 'IMAGE' : 'FILE',
      content: null, fileUrl: up.fileUrl, fileName: up.fileName
    }));
  } catch { toast('Error de conexión', 'error'); }
}

// ── Recibir mensaje WS ────────────────────────────────────────
function _onDmMessage(msg) {
  const existing = document.getElementById(`dm-msg-${msg.messageId}`);
  if (existing) { existing.outerHTML = _buildDmMsgHTML(msg); return; }
  _appendDmMsg(msg, true);
}

// ── Recibir evento de solicitud WS ───────────────────────────
function _onRequestEvent(conv) {
  loadDmList();
  if (conv.status === 'PENDING' && conv.isIncoming) {
    toast(`${conv.otherUserName} quiere chatear contigo — ve a Invitaciones`, 'info');
    loadDmRequestsSection();
    _refreshDmBadge();
  } else if (conv.status === 'ACTIVE' && !conv.isIncoming) {
    toast(`${conv.otherUserName} aceptó tu solicitud`, 'success');
    loadDmList();
  }
}

// ── Render mensajes ───────────────────────────────────────────
let _dmDates = {};

function _appendDmMsg(msg, scroll) {
  const el = document.getElementById('dm-messages');
  if (!el) return;
  const day = new Date(msg.sentAt).toDateString();
  if (!_dmDates[day]) {
    _dmDates[day] = true;
    el.insertAdjacentHTML('beforeend', `
      <div class="date-divider">
        <span>${new Date(msg.sentAt).toLocaleDateString('es', { weekday:'long', day:'numeric', month:'long' })}</span>
      </div>`);
  }
  el.insertAdjacentHTML('beforeend', _buildDmMsgHTML(msg));
  if (scroll) el.scrollTop = el.scrollHeight;
}

function _buildDmMsgHTML(msg) {
  const isOwn  = msg.senderId === state.userId;
  const time   = new Date(msg.sentAt).toLocaleTimeString('es', { hour:'2-digit', minute:'2-digit' });
  const bubble = isOwn ? 'own-bubble' : 'other';

  let body;
  if (msg.deleted)          body = `<div class="msg-deleted">Mensaje eliminado</div>`;
  else if (msg.type==='IMAGE') body = `<img class="msg-image" src="${escHtml(msg.fileUrl)}" alt="${escHtml(msg.fileName||'imagen')}" onclick="window.open(this.src,'_blank')">`;
  else if (msg.type==='FILE')  body = `<div class="msg-file" onclick="window.open('${escHtml(msg.fileUrl)}','_blank')"><span class="file-icon">📄</span><span class="file-name">${escHtml(msg.fileName||'archivo')}</span></div>`;
  else                         body = `<div class="msg-text">${escHtml(msg.content||'')}</div>`;

  const del = (isOwn && !msg.deleted) ? `
    <div class="msg-actions">
      <button class="msg-action-btn danger" title="Eliminar" onclick="deleteDmMessage(${msg.messageId})">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
        </svg>
      </button>
    </div>` : '';

  if (isOwn) return `
    <div class="msg-row own" id="dm-msg-${msg.messageId}">
      ${del}
      <div class="msg-body">
        <div class="msg-meta"><span class="msg-time">${time}</span></div>
        <div class="msg-bubble ${bubble}${msg.deleted?' deleted-bubble':''}">${body}</div>
      </div>
    </div>`;

  return `
    <div class="msg-row" id="dm-msg-${msg.messageId}">
      <div class="msg-av ${avColor(msg.senderName||'')}">${getInitials(msg.senderName||'?')}</div>
      <div class="msg-body">
        <div class="msg-meta">
          <span class="msg-sender">${escHtml(msg.senderName||'')}</span>
          <span class="msg-time">${time}</span>
        </div>
        <div class="msg-bubble ${bubble}${msg.deleted?' deleted-bubble':''}">${body}</div>
      </div>
    </div>`;
}

async function deleteDmMessage(messageId) {
  const { ok, data } = await apiCall('DELETE', `/dm/messages/${messageId}`);
  if (!ok) return toast(data?.message || 'Error al eliminar', 'error');
}