// ===================== CHANNELS =====================

let _activeChannelId   = null;
let _activeChannelName = null;
let _deleteChannelId   = null;

async function loadChannels() {
  if (!state.currentGroup) return;
  const { ok, data } = await apiCall('GET', `/groups/${state.currentGroup.groupId}/channels`);
  if (!ok) return;
  renderChannelsList(Array.isArray(data) ? data : []);
}

function renderChannelsList(channels) {
  const el = document.getElementById('channels-list');
  const isAdmin = state.currentRole === 'ADMIN';

  if (!channels.length) {
    el.innerHTML = `<div style="padding:4px 14px;font-size:11px;color:var(--text3)">Sin canales aún</div>`;
    return;
  }

  el.innerHTML = channels.map(c => `
    <div class="channel-item ${_activeChannelId === c.channelId ? 'active' : ''}"
         onclick="openChannelChat(${c.channelId}, '${escHtml(c.name)}', ${c.groupId})">
      <span class="ch-hash">#</span>
      <span class="ch-name">${escHtml(c.name)}</span>
      ${isAdmin ? `
        <button class="ch-delete" title="Eliminar canal"
                onclick="event.stopPropagation(); openDeleteChannelModal(${c.channelId}, '${escHtml(c.name)}')">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
          </svg>
        </button>` : ''}
    </div>`).join('');
}

async function createChannel() {
  const name = document.getElementById('nc-name').value.trim();
  const desc = document.getElementById('nc-desc').value.trim();
  if (!name) return toast('El nombre del canal es requerido', 'warn');

  const { ok, data } = await apiCall(
    'POST',
    `/groups/${state.currentGroup.groupId}/channels`,
    { name, description: desc }
  );
  if (!ok) return toast(data?.message || 'Error al crear el canal', 'error');

  closeModal('modal-create-channel');
  toast(`Canal #${name} creado`, 'success');
  await loadChannels();

  // Refresh group meta
  const meta = await apiCall('GET', `/groups/${state.currentGroup.groupId}`);
  if (meta.ok) {
    state.currentGroup = meta.data;
    document.getElementById('gv-meta').textContent =
      `${meta.data.memberCount||1} miembros · ${meta.data.channelCount||0} canales · creado por ${meta.data.createdByName}`;
    await loadGroups();
  }
}

function openDeleteChannelModal(channelId, name) {
  _deleteChannelId = channelId;
  document.getElementById('dc-name').textContent = '#' + name;
  openModal('modal-delete-channel');
}

async function confirmDeleteChannel() {
  if (!_deleteChannelId) return;
  const { ok, data } = await apiCall('DELETE', `/groups/channels/${_deleteChannelId}`);
  if (!ok) return toast(data?.message || 'Error al eliminar el canal', 'error');

  // If we were chatting in this channel, reset chat area
  if (_activeChannelId === _deleteChannelId) {
    _activeChannelId = null;
    _activeChannelName = null;
    document.getElementById('chat-area').innerHTML = `
      <div class="chat-empty">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        <p>Selecciona un canal para chatear</p>
      </div>`;
    if (currentSub) { currentSub.unsubscribe(); currentSub = null; }
  }

  closeModal('modal-delete-channel');
  toast('Canal eliminado', 'info');
  _deleteChannelId = null;
  await loadChannels();

  const meta = await apiCall('GET', `/groups/${state.currentGroup.groupId}`);
  if (meta.ok) {
    state.currentGroup = meta.data;
    document.getElementById('gv-meta').textContent =
      `${meta.data.memberCount||1} miembros · ${meta.data.channelCount||0} canales · creado por ${meta.data.createdByName}`;
    await loadGroups();
  }
}

function setActiveChannel(channelId) {
  _activeChannelId = channelId;
  // Refresh highlight
  document.querySelectorAll('.channel-item').forEach(el => {
    el.classList.toggle('active',
      el.getAttribute('onclick')?.includes(`openChannelChat(${channelId},`) ||
      el.getAttribute('onclick')?.includes(`openChannelChat(${channelId} `)
    );
  });
  // Also reset general chat highlight
  document.getElementById('btn-general-chat').classList.remove('active');
}
function setActiveGeneral() {
  _activeChannelId = null;
  document.querySelectorAll('.channel-item').forEach(el => el.classList.remove('active'));
  document.getElementById('btn-general-chat').classList.add('active');
}