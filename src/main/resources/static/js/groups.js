// ===================== GRUPOS =====================
// Depende de: api.js (state, apiCall, showToast, getInitials, getAvatarColor, openModal, closeModal)

// ---- Cargar y renderizar ----

async function loadGroups() {
  // GET /api/groups — retorna List<GroupResponseDTO>
  // GroupResponseDTO: { groupId, name, description, createdByUserId, createdByName,
  //                     isPrivate, memberCount, channelCount, createdAt }
  const { ok, data } = await apiCall('GET', '/groups');
  if (!ok) return;
  state.groups = Array.isArray(data) ? data : [];
  renderGroupsList(state.groups);
}

function renderGroupsList(groups) {
  const container = document.getElementById('groups-list');

  if (!groups.length) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">💬</div>
        <p>Aún no perteneces a ningún grupo.<br>¡Crea uno o espera una invitación!</p>
      </div>`;
    return;
  }

  container.innerHTML = groups.map(g => {
    const members  = g.memberCount  || 1;
    const channels = g.channelCount || 0;
    const isActive = state.currentGroup?.groupId === g.groupId;
    return `
      <div class="group-item ${isActive ? 'active' : ''}" onclick="selectGroup(${g.groupId})">
        <div class="avatar ${getAvatarColor(g.name)}">${getInitials(g.name)}</div>
        <div class="group-info">
          <div class="group-name">${g.name}</div>
          <div class="group-sub">
            ${members} miembro${members !== 1 ? 's' : ''} · ${channels} canal${channels !== 1 ? 'es' : ''}
          </div>
        </div>
        ${g.isPrivate ? '<span class="tag-private">🔒</span>' : ''}
      </div>`;
  }).join('');
}

function filterGroups(query) {
  const filtered = state.groups.filter(g =>
    g.name.toLowerCase().includes(query.toLowerCase())
  );
  renderGroupsList(filtered);
}

// ---- Seleccionar un grupo ----

async function selectGroup(groupId) {
  // GET /api/groups/{groupId} — retorna GroupResponseDTO
  const { ok, data } = await apiCall('GET', `/groups/${groupId}`);
  if (!ok) return showToast('Error al cargar el grupo', 'error');

  state.currentGroup = data;

  // Header del panel
  const av = document.getElementById('gv-avatar');
  av.textContent = getInitials(data.name);
  av.className   = `avatar ${getAvatarColor(data.name)}`;

  document.getElementById('gv-name').textContent = data.name + (data.isPrivate ? ' 🔒' : '');
  document.getElementById('gv-meta').textContent =
    `${data.memberCount || 1} miembros · ${data.channelCount || 0} canales`;

  // Mostrar la vista de grupo
  document.getElementById('welcome-state').style.display = 'none';
  document.getElementById('group-view').classList.add('active');

  // Resetear a pestaña miembros
  document.querySelectorAll('.group-tab').forEach((t, i) => t.classList.toggle('active', i === 0));
  state.currentGroupTab = 'members';
  loadMembers();

  // Resaltar en sidebar
  renderGroupsList(state.groups);
}

function switchGroupTab(tab, el) {
  state.currentGroupTab = tab;
  document.querySelectorAll('.group-tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  if (tab === 'members') loadMembers();
  else                   loadChannels();
}

// ---- Crear grupo ----

async function createGroup() {
  const name  = document.getElementById('new-group-name').value.trim();
  const desc  = document.getElementById('new-group-desc').value.trim();
  const priv  = document.getElementById('new-group-private').checked;

  if (!name) return showToast('El nombre del grupo es requerido', 'warn');

  // POST /api/groups — body: GroupRequestDTO { name, description, isPrivate }
  // retorna GroupResponseDTO con 201
  const { ok, data } = await apiCall('POST', '/groups', { name, description: desc, isPrivate: priv });
  if (!ok) return showToast(data?.message || 'Error al crear el grupo', 'error');

  closeModal('modal-create-group');
  showToast(`Grupo "${name}" creado`, 'success');
  await loadGroups();
  selectGroup(data.groupId); // abrir el grupo recién creado
}

// ---- Salir del grupo ----

async function leaveCurrentGroup() {
  if (!state.currentGroup) return;
  if (!confirm(`¿Salir del grupo "${state.currentGroup.name}"?`)) return;

  // POST /api/groups/{groupId}/leave — userId del token JWT
  const { ok, data } = await apiCall('POST', `/groups/${state.currentGroup.groupId}/leave`);
  if (!ok) return showToast(data?.message || 'No puedes salir de este grupo', 'error');

  showToast('Saliste del grupo', 'info');
  state.currentGroup = null;
  document.getElementById('group-view').classList.remove('active');
  document.getElementById('welcome-state').style.display = '';
  loadGroups();
}

// ---- Miembros ----

async function loadMembers() {
  if (!state.currentGroup) return;

  // GET /api/groups/{groupId}/members
  // retorna List<GroupMemberResponseDTO>:
  // { userId, userName, userEmail, role (ADMIN|MEMBER), joinedAt }
  const { ok, data } = await apiCall('GET', `/groups/${state.currentGroup.groupId}/members`);
  const content = document.getElementById('group-content');

  if (!ok) {
    content.innerHTML = `<div class="empty-state"><p>Error al cargar los miembros</p></div>`;
    return;
  }

  content.innerHTML = `
    <div class="section-title">Miembros del grupo</div>
    ${data.map(m => `
      <div class="member-item">
        <div class="avatar avatar-sm ${getAvatarColor(m.name || 'U')}">
          ${getInitials(m.name || 'U')}
        </div>
        <div>
          <div style="font-size:14px;font-weight:500">${m.name || 'Usuario'}</div>
          <div style="font-size:12px;color:var(--text2)">${m.userEmail || ''}</div>
        </div>
        <span class="member-role ${m.role === 'ADMIN' ? 'role-admin' : 'role-member'}">${m.role}</span>
      </div>
    `).join('')}`;
}

// ---- Canales ----

async function loadChannels() {
  if (!state.currentGroup) return;

  // GET /api/groups/{groupId}/channels
  // retorna List<ChannelResponseDTO>:
  // { channelId, groupId, name, description, createdByUserId, createdByName, createdAt }
  const { ok, data } = await apiCall('GET', `/groups/${state.currentGroup.groupId}/channels`);
  const content = document.getElementById('group-content');

  const channelsHtml = (ok && data.length)
    ? data.map(c => `
        <div class="channel-item">
          <div class="channel-hash">#</div>
          <div>
            <div style="font-size:14px;font-weight:500">${c.name}</div>
            <div style="font-size:12px;color:var(--text2)">${c.description || 'Sin descripción'}</div>
          </div>
        </div>`).join('')
    : `<div class="empty-state"><div class="empty-icon">📢</div><p>Aún no hay canales en este grupo.</p></div>`;

  content.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px">
      <div class="section-title" style="margin:0">Canales</div>
      <button class="btn-modal-primary" style="padding:7px 14px;font-size:12px;border-radius:8px"
              onclick="openModal('modal-create-channel')">+ Nuevo canal</button>
    </div>
    ${channelsHtml}`;
}

async function createChannel() {
  const name = document.getElementById('new-channel-name').value.trim();
  const desc = document.getElementById('new-channel-desc').value.trim();

  if (!name) return showToast('El nombre del canal es requerido', 'warn');

  // POST /api/groups/{groupId}/channels — body: ChannelRequestDTO { name, description }
  // retorna ChannelResponseDTO con 201
  const { ok, data } = await apiCall(
    'POST',
    `/groups/${state.currentGroup.groupId}/channels`,
    { name, description: desc }
  );
  if (!ok) return showToast(data?.message || 'Error al crear el canal', 'error');

  closeModal('modal-create-channel');
  showToast(`Canal #${name} creado`, 'success');
  loadChannels();

  // Actualizar conteo en header
  const meta = await apiCall('GET', `/groups/${state.currentGroup.groupId}`);
  if (meta.ok) {
    state.currentGroup = meta.data;
    document.getElementById('gv-meta').textContent =
      `${meta.data.memberCount || 1} miembros · ${meta.data.channelCount || 0} canales`;
  }
}

// ---- Invitar usuario ----

function openInviteModal() {
  if (!state.currentGroup) return;
  openModal('modal-invite');
}

async function sendInvitation() {
  const email = document.getElementById('invite-email').value.trim();
  if (!email) return showToast('Ingresa el email del usuario', 'warn');

  // 1. Buscar userId por email — GET /api/users/email/{email}
  // retorna UserResponseDTO: { userId, name, email, ... }
  const { ok: uok, data: udata } = await apiCall('GET', `/users/email/${encodeURIComponent(email)}`);
  if (!uok) return showToast('Usuario no encontrado', 'error');

  // 2. Enviar invitación — POST /api/groups/{groupId}/invitations?invitedUserId={id}
  // retorna InvitationResponseDTO con 201
  const { ok, data } = await apiCall(
    'POST',
    `/groups/${state.currentGroup.groupId}/invitations?invitedUserId=${udata.userId}`
  );
  if (!ok) return showToast(data?.message || 'Error al enviar la invitación', 'error');

  closeModal('modal-invite');
  showToast(`Invitación enviada a ${email}`, 'success');
}