// ===================== PRESENCE =====================

const presenceCache = {}; // userId → { online, lastSeen }
let presenceSub = null;

async function subscribePresence(groupId) {
  if (presenceSub) { presenceSub.unsubscribe(); presenceSub = null; }

  // Estado inicial via REST
  const { ok, data } = await apiCall('GET', `/presence/group/${groupId}`);
  if (ok && Array.isArray(data)) {
    data.forEach(p => {
      presenceCache[p.userId] = { online: p.online, lastSeen: p.lastSeen };
    });
    applyPresenceToMembersList();
  }

  // Cambios en tiempo real via WS
  if (!stompClient?.connected) return;
  presenceSub = stompClient.subscribe(`/topic/presence.${groupId}`, frame => {
    const event = JSON.parse(frame.body);
    presenceCache[event.userId] = { online: event.online, lastSeen: event.lastSeen };
    applyPresenceToMembersList();
  });
}

function applyPresenceToMembersList() {
  document.querySelectorAll('.member-item[data-user-id]').forEach(el => {
    const uid = parseInt(el.dataset.userId);
    const p   = presenceCache[uid];
    if (!p) return;

    // Buscar o crear el dot en el avatar
    const av = el.querySelector('.mi-avatar');
    if (!av) return;

    let dot = av.querySelector('.presence-dot');
    if (!dot) {
      dot = document.createElement('span');
      dot.className = 'presence-dot';
      av.style.position = 'relative';
      av.appendChild(dot);
    }

    dot.className = `presence-dot ${p.online ? 'online' : 'offline'}`;
    dot.title = p.online ? 'En línea' : formatLastSeen(p.lastSeen);
  });
}

function formatLastSeen(lastSeen) {
  if (!lastSeen) return 'Nunca conectado';
  const date = new Date(lastSeen);
  const now  = new Date();
  const diff = now - date;
  const mins = Math.floor(diff / 60000);
  const hrs  = Math.floor(diff / 3600000);
  if (mins < 1)  return 'Visto hace un momento';
  if (mins < 60) return `Visto hace ${mins} min`;
  if (hrs  < 24) return `Visto hoy a las ${date.toLocaleTimeString('es', {hour:'2-digit',minute:'2-digit'})}`;
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString())
    return `Visto ayer a las ${date.toLocaleTimeString('es', {hour:'2-digit',minute:'2-digit'})}`;
  return `Visto el ${date.toLocaleDateString('es', {day:'numeric',month:'short'})}`;
}