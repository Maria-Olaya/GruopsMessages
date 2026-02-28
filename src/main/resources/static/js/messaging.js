// ===================== WEBSOCKET =====================
let stompClient  = null;
let currentSub   = null;
let receiptSub = null; // agregar junto a currentSub al inicio del archivo
let chatCtx      = null;

function connectWebSocket() {
  if (stompClient?.connected) return;
  const sock = new SockJS('/ws');
  stompClient = Stomp.over(sock);
  stompClient.debug = null;
  stompClient.connect(
    { Authorization: `Bearer ${state.token}` },
    () => { console.log('[WS] connected'); },
    (err) => {
      console.error('[WS] error', err);
      setTimeout(connectWebSocket, 3000);
    }
  );
}

function disconnectWebSocket() {
  if (stompClient) { stompClient.disconnect(); stompClient = null; }
  currentSub = null;
  chatCtx = null;
}

function waitForWS(timeout = 4000) {
  return new Promise(resolve => {
    if (stompClient?.connected) return resolve();
    const iv = setInterval(() => {
      if (stompClient?.connected) { clearInterval(iv); resolve(); }
    }, 100);
    setTimeout(() => { clearInterval(iv); resolve(); }, timeout);
  });
}

// ===================== OPEN CHAT =====================
async function openChannelChat(channelId, channelName, groupId) {
  chatCtx = { type: 'channel', id: channelId, name: channelName, groupId };
  setActiveChannel(channelId);
  await _openChat(`/topic/channel.${channelId}`, '#' + channelName);
  loadHistory('channel', channelId);
}

async function openGeneralChat() {
  if (!state.currentGroup) return;
  chatCtx = { type: 'group', id: state.currentGroup.groupId, name: 'general', groupId: state.currentGroup.groupId };
  setActiveGeneral();
  await _openChat(`/topic/group.${state.currentGroup.groupId}`, '# general');
  loadHistory('group', state.currentGroup.groupId);
}


async function _openChat(topic, title) {
  if (!stompClient?.connected) { connectWebSocket(); await waitForWS(); }
  if (currentSub)  { currentSub.unsubscribe();  currentSub  = null; }
  if (receiptSub)  { receiptSub.unsubscribe();  receiptSub  = null; }

  currentSub = stompClient.subscribe(topic, frame => {
    const msg = JSON.parse(frame.body);
    appendMessage(msg, true);
  });

  // Suscribirse al topic de receipts del mismo canal/grupo
  const receiptTopic = topic.startsWith('/topic/channel.')
    ? `/topic/receipts.channel.${chatCtx.id}`
    : `/topic/receipts.group.${chatCtx.id}`;

  receiptSub = stompClient.subscribe(receiptTopic, frame => {
    const receipt = JSON.parse(frame.body);
    updateMessageStatus(receipt.messageId, receipt.status);
  });

  renderChatUI(title);
}

// ===================== HISTORY =====================
async function loadHistory(type, id) {
  const path = type === 'channel'
    ? `/messages/channel/${id}?size=50`
    : `/messages/group/${id}?size=50`;
  const { ok, data } = await apiCall('GET', path);
  if (!ok) return;
  const container = document.getElementById('chat-messages');
  if (!container) return;
  container.innerHTML = '';
  (data || []).forEach(m => appendMessage(m, false));
  scrollBottom();
}

// ===================== SEND =====================
function sendMessage() {
  const input = document.getElementById('chat-input');
  const text = input?.value.trim();
  if (!text || !stompClient?.connected || !chatCtx) return;
  stompClient.send('/app/chat.send', {}, JSON.stringify({
    groupId:   chatCtx.groupId,
    channelId: chatCtx.type === 'channel' ? chatCtx.id : null,
    type:      'TEXT',
    content:   text,
    fileUrl:   null,
    fileName:  null,
  }));
  input.value = '';
}

async function handleFile(event) {
  const file = event.target.files[0];
  if (!file || !chatCtx) return;
  event.target.value = '';

  if (file.size > 10 * 1024 * 1024) {
    return toast('El archivo supera el límite de 10 MB', 'error');
  }

  toast('Subiendo archivo...', 'info');

  const formData = new FormData();
  formData.append('file', file);

  let uploadData;
  try {
    const res = await fetch('/api/files/upload', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${state.token}` },
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return toast(err.message || 'Error al subir el archivo', 'error');
    }
    uploadData = await res.json();
  } catch(e) {
    return toast('No se pudo conectar con el servidor', 'error');
  }

  const isImg = file.type.startsWith('image/');
  stompClient.send('/app/chat.send', {}, JSON.stringify({
    groupId:   chatCtx.groupId,
    channelId: chatCtx.type === 'channel' ? chatCtx.id : null,
    type:      isImg ? 'IMAGE' : 'FILE',
    content:   null,
    fileUrl:   uploadData.fileUrl,
    fileName:  uploadData.fileName,
  }));
}


// ===================== DELETE MESSAGE =====================
async function deleteMessage(messageId) {
  if (!confirm('¿Eliminar este mensaje?')) return;
  const { ok, data } = await apiCall('DELETE', `/messages/${messageId}`);
  if (!ok) return toast(data?.message || 'No puedes eliminar ese mensaje', 'error');

  // Reemplazar contenido por "Mensaje eliminado" — visible para el autor en su sesión
  // Para los demás aparece así al recargar el historial (deleted=true en el DTO)
  const el = document.querySelector(`.msg-row[data-id="${messageId}"]`);
  if (el) {
    const bubble = el.querySelector('.msg-bubble');
    if (bubble) {
      bubble.innerHTML = '<div class="msg-deleted">Mensaje eliminado</div>';
      bubble.classList.add('deleted-bubble');
    }
    el.querySelector('.msg-actions')?.remove();
  }
  toast('Mensaje eliminado', 'info');
}

// ===================== EDIT MESSAGE =====================
// El content se guarda en data-content del boton (NO en onclick)
// para evitar que comillas o caracteres especiales rompan el HTML

function startEditMessage(btn) {
  const messageId   = btn.dataset.id;
  const currentText = btn.dataset.content;

  // Evitar doble edicion simultanea
  if (document.querySelector('.edit-input-wrap')) return;

  const el = document.querySelector(`.msg-row[data-id="${messageId}"]`);
  if (!el) return;

  const bubble = el.querySelector('.msg-bubble');
  bubble.dataset.originalHtml = bubble.innerHTML;

  bubble.innerHTML = `
    <div class="edit-input-wrap">
      <input type="text" class="edit-input" id="edit-input-${messageId}"
        onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();confirmEdit('${messageId}');}
                   if(event.key==='Escape'){cancelEdit('${messageId}');}">
      <div class="edit-actions">
        <button class="edit-btn-cancel" onclick="cancelEdit('${messageId}')">Cancelar</button>
        <button class="edit-btn-save"   onclick="confirmEdit('${messageId}')">Guardar</button>
      </div>
    </div>`;

  // Setear value por JS para evitar problemas de escaping en el HTML
  const input = document.getElementById(`edit-input-${messageId}`);
  if (input) {
    input.value = currentText;
    input.focus();
    input.setSelectionRange(input.value.length, input.value.length);
  }
}

async function confirmEdit(messageId) {
  const input = document.getElementById(`edit-input-${messageId}`);
  const newContent = input?.value.trim();
  if (!newContent) return toast('El mensaje no puede estar vacío', 'warn');

  const { ok, data } = await apiCall(
    'PATCH', `/messages/${messageId}?content=${encodeURIComponent(newContent)}`
  );
  if (!ok) return toast(data?.message || 'Error al editar el mensaje', 'error');

  const el = document.querySelector(`.msg-row[data-id="${messageId}"]`);
  if (el) {
    const bubble = el.querySelector('.msg-bubble');
    bubble.innerHTML = `<div class="msg-text">${escHtml(newContent)}</div><div class="msg-edited">editado</div>`;
    delete bubble.dataset.originalHtml;
    // Actualizar data-content del boton para ediciones futuras
    const editBtn = el.querySelector('.msg-action-btn[data-id]');
    if (editBtn) editBtn.dataset.content = newContent;
  }
  toast('Mensaje editado', 'success');
}

function cancelEdit(messageId) {
  const el = document.querySelector(`.msg-row[data-id="${messageId}"]`);
  if (!el) return;
  const bubble = el.querySelector('.msg-bubble');
  if (bubble?.dataset.originalHtml) {
    bubble.innerHTML = bubble.dataset.originalHtml;
    delete bubble.dataset.originalHtml;
  }
}

// ===================== RENDER CHAT UI =====================
function renderChatUI(title) {
  document.getElementById('chat-area').innerHTML = `
    <div class="chat-header">
      <span class="chat-channel-name">${escHtml(title)}</span>
    </div>
    <div class="chat-messages" id="chat-messages"></div>
    <div class="chat-input-wrap">
      <div class="chat-input-row">
        <label class="attach-btn" title="Adjuntar archivo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/>
          </svg>
          <input type="file" style="display:none" accept="image/*,.pdf,.txt,.doc,.docx" onchange="handleFile(event)">
        </label>
        <input type="text" id="chat-input" class="chat-input" placeholder="Escribe un mensaje..."
          onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();sendMessage()}" autofocus>
        <button class="send-btn" onclick="sendMessage()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"/>
            <polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
    </div>`;
}

// ===================== APPEND MESSAGE =====================
function appendMessage(msg, scroll = true) {
  const container = document.getElementById('chat-messages');
  if (!container) return;

  const isOwn = msg.senderId === state.userId;
  const time  = new Date(msg.sentAt).toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' });

  let bodyHtml = '';
  if (msg.deleted) {
    // Aparece igual para todos — mismo estilo, mismo texto
    bodyHtml = '<div class="msg-deleted">Mensaje eliminado</div>';
  } else if (msg.type === 'TEXT') {
    bodyHtml = `<div class="msg-text">${escHtml(msg.content)}</div>`;
    if (msg.editedAt) bodyHtml += '<div class="msg-edited">editado</div>';
  } else if (msg.type === 'IMAGE') {
    bodyHtml = `<img class="msg-image" src="${msg.fileUrl}" alt="${escHtml(msg.fileName || 'imagen')}" onclick="window.open(this.src)">`;
  } else {
    bodyHtml = `
      <div class="msg-file" onclick="window.open('${msg.fileUrl}')">
        <span class="file-icon">📄</span>
        <span class="file-name">${escHtml(msg.fileName || 'archivo')}</span>
      </div>`;
  }

  // Botones solo en mensajes propios no eliminados
  let actionsHtml = '';
  if (isOwn && !msg.deleted) {
    // Boton de editar: el content va en data-content, se setea por JS tras insertar al DOM
    const editBtn = msg.type === 'TEXT' ? `
      <button class="msg-action-btn" title="Editar"
              data-id="${msg.messageId}" data-content=""
              onclick="startEditMessage(this)">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
        </svg>
      </button>` : '';

    const deleteBtn = `
      <button class="msg-action-btn danger" title="Eliminar" onclick="deleteMessage(${msg.messageId})">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
        </svg>
      </button>`;

    actionsHtml = `<div class="msg-actions">${editBtn}${deleteBtn}</div>`;
  }

  const el = document.createElement('div');
  el.className = `msg-row ${isOwn ? 'own' : ''}`;
  el.dataset.id = msg.messageId;

  if (isOwn) {
    el.innerHTML = `
      ${actionsHtml}
      <div class="msg-body">
        <div class="msg-meta">
          <span class="msg-time">${time}</span>
          <span class="msg-status">${statusIcon(msg.status)}</span>
        </div>
        <div class="msg-bubble own-bubble${msg.deleted ? ' deleted-bubble' : ''}">
          ${bodyHtml}
        </div>
      </div>`;
  } else {
    el.innerHTML = `
      <div class="msg-av ${avColor(msg.senderName || '?')}">${getInitials(msg.senderName || '?')}</div>
      <div class="msg-body">
        <div class="msg-meta">
          <span class="msg-sender">${escHtml(msg.senderName)}</span>
          <span class="msg-time">${time}</span>
        </div>
        <div class="msg-bubble other${msg.deleted ? ' deleted-bubble' : ''}">
          ${bodyHtml}
        </div>
      </div>`;
  }

  container.appendChild(el);

  // IMPORTANTE: setear data-content por JS despues de insertar al DOM
  // Si lo ponemos en el atributo HTML, comillas en el mensaje rompen el parsing
  if (isOwn && !msg.deleted && msg.type === 'TEXT') {
    const editBtn = el.querySelector('.msg-action-btn[data-id]');
    if (editBtn) editBtn.dataset.content = msg.content || '';
  }

  if (scroll) scrollBottom();

  // Al agregar un mensaje visible, notificar lectura al servidor
  if (!isOwn && msg.messageId && stompClient?.connected && chatCtx) {
    stompClient.send('/app/chat.read', {}, JSON.stringify({
      messageId: msg.messageId,
      groupId:   chatCtx.groupId,
      channelId: chatCtx.type === 'channel' ? chatCtx.id : null
    }));
  }
}

function scrollBottom() {
  const c = document.getElementById('chat-messages');
  if (c) c.scrollTop = c.scrollHeight;
}

// Actualiza el ícono de status en un mensaje propio
function updateMessageStatus(messageId, status) {
  const el = document.querySelector(`.msg-row[data-id="${messageId}"]`);
  if (!el) return;
  const tick = el.querySelector('.msg-status');
  if (!tick) return;

  const icons = {
    SENT:      '✓',
    DELIVERED: '✓✓',
    READ:      '<span style="color:#4fc3f7">✓✓</span>'
  };
  tick.innerHTML = icons[status] || '';
}
function statusIcon(status) {
  if (status === 'READ')      return '<span style="color:#4fc3f7">✓✓</span>';
  if (status === 'DELIVERED') return '✓✓';
  return '✓'; // SENT o undefined
}

