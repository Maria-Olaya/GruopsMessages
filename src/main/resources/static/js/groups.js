// ===================== SIDEBAR PANELS =====================
function showPanel(panel) {
  document.getElementById('panel-groups').classList.toggle('hidden', panel !== 'groups');
  document.getElementById('panel-invitations').classList.toggle('hidden', panel !== 'invitations');
  document.getElementById('nav-groups').classList.toggle('active', panel === 'groups');
  document.getElementById('nav-invitations').classList.toggle('active', panel !== 'groups');
  if (panel === 'invitations') loadInvitations();
}

// ===================== GROUPS LIST =====================
async function loadGroups() {
  const { ok, data } = await apiCall('GET', '/groups');
  if (!ok) return;
  state.groups = Array.isArray(data) ? data : [];
  renderGroupsList(state.groups);
}

function renderGroupsList(groups) {
  const el = document.getElementById('groups-list');
  if (!groups.length) {
    el.innerHTML = `<div class="empty-state"><p>No perteneces a ningún grupo.<br>¡Crea uno o espera una invitación!</p></div>`;
    return;
  }
  el.innerHTML = groups.map(g => `
    <div class="group-item ${state.currentGroup?.groupId === g.groupId ? 'active' : ''}"
         onclick="selectGroup(${g.groupId})">
      <div class="gi-avatar ${avColor(g.name)}">${getInitials(g.name)}</div>
      <div class="gi-info">
        <div class="gi-name">${escHtml(g.name)}${g.isPrivate ? '<span class="tag-private">privado</span>' : ''}</div>
        <div class="gi-sub">${g.memberCount||1} miembros · ${g.channelCount||0} canales</div>
      </div>
    </div>`).join('');
}

function filterGroups(q) {
  renderGroupsList(state.groups.filter(g => g.name.toLowerCase().includes(q.toLowerCase())));
}

// ===================== SELECT GROUP =====================
async function selectGroup(groupId) {
  const { ok, data } = await apiCall('GET', `/groups/${groupId}`);
  if (!ok) return toast('Error al cargar el grupo', 'error');
  state.currentGroup = data;

  // Determine current user role in this group
  await loadMembers();

  // Header
  const av = document.getElementById('gv-avatar');
  av.textContent = getInitials(data.name);
  av.className = `group-avatar-lg ${avColor(data.name)}`;
  document.getElementById('gv-name').textContent = data.name + (data.isPrivate ? ' 🔒' : '');
  document.getElementById('gv-meta').textContent =
    `${data.memberCount||1} miembros · ${data.channelCount||0} canales · creado por ${data.createdByName}`;

  // Header actions based on role
  renderGroupActions();

  // Show group view
  document.getElementById('welcome-state').classList.add('hidden');
  document.getElementById('group-view').classList.remove('hidden');

  // Load channels + update add-channel button visibility
  await loadChannels();

  // Show/hide "add channel" button
  document.getElementById('btn-add-channel').classList.toggle('hidden', state.currentRole !== 'ADMIN');

  // Highlight in sidebar
  renderGroupsList(state.groups);
  subscribePresence(data.groupId);
  // Reset chat area
  document.getElementById('chat-area').innerHTML = `
    <div class="chat-empty">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      <p>Selecciona un canal para chatear</p>
    </div>`;
}

function renderGroupActions() {
  const el = document.getElementById('gv-actions');
  const isAdmin = state.currentRole === 'ADMIN';
  const isCreator = state.currentGroup?.createdByUserId === state.userId;

  let html = '';

  if (isAdmin) {
    html += `<button class="header-btn" onclick="openInviteModal()" title="Invitar usuario">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      Invitar
    </button>`;
    html += `<button class="header-btn" onclick="openEditGroupModal()" title="Editar grupo">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
      Editar
    </button>`;
  }

  if (!isCreator) {
    html += `<button class="header-btn" onclick="leaveGroup()" title="Salir del grupo">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
      Salir
    </button>`;
  }

  if (isCreator) {
    html += `<button class="header-btn danger" onclick="openDeleteGroupModal()" title="Eliminar grupo">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
      Eliminar
    </button>`;
  }

  el.innerHTML = html;
}

// ===================== CREATE GROUP =====================
async function createGroup() {
  const name = document.getElementById('ng-name').value.trim();
  const desc = document.getElementById('ng-desc').value.trim();
  const priv = document.getElementById('ng-private').checked;
  if (!name) return toast('El nombre del grupo es requerido', 'warn');

  const { ok, data } = await apiCall('POST', '/groups', { name, description: desc, isPrivate: priv });
  if (!ok) return toast(data?.message || 'Error al crear el grupo', 'error');

  closeModal('modal-create-group');
  toast(`Grupo "${name}" creado`, 'success');
  await loadGroups();
  selectGroup(data.groupId);
}

// ===================== EDIT GROUP =====================
function openEditGroupModal() {
  const g = state.currentGroup;
  document.getElementById('eg-name').value = g.name;
  document.getElementById('eg-desc').value = g.description || '';
  document.getElementById('eg-private').checked = g.isPrivate;
  openModal('modal-edit-group');
}
async function saveEditGroup() {
  const name = document.getElementById('eg-name').value.trim();
  const desc = document.getElementById('eg-desc').value.trim();
  const priv = document.getElementById('eg-private').checked;
  if (!name) return toast('El nombre es requerido', 'warn');

  const { ok, data } = await apiCall(
    'PUT', `/groups/${state.currentGroup.groupId}`,
    { name, description: desc, isPrivate: priv }
  );
  if (!ok) return toast(data?.message || 'Error al guardar', 'error');

  state.currentGroup = data;
  closeModal('modal-edit-group');
  toast('Grupo actualizado', 'success');
  document.getElementById('gv-name').textContent = data.name + (data.isPrivate ? ' 🔒' : '');
  await loadGroups();
}

// ===================== DELETE GROUP =====================
function openDeleteGroupModal() {
  document.getElementById('dg-name').textContent = state.currentGroup.name;
  openModal('modal-delete-group');
}
async function confirmDeleteGroup() {
  const { ok, data } = await apiCall('DELETE', `/groups/${state.currentGroup.groupId}`);
  if (!ok) return toast(data?.message || 'Error al eliminar', 'error');

  closeModal('modal-delete-group');
  toast('Grupo eliminado', 'info');
  state.currentGroup = null;
  document.getElementById('group-view').classList.add('hidden');
  document.getElementById('welcome-state').classList.remove('hidden');
  await loadGroups();
}

// ===================== LEAVE GROUP =====================
async function leaveGroup() {
  if (!confirm(`¿Salir del grupo "${state.currentGroup.name}"?`)) return;
  const { ok, data } = await apiCall('POST', `/groups/${state.currentGroup.groupId}/leave`);
  if (!ok) return toast(data?.message || 'No puedes salir de este grupo', 'error');

  toast('Saliste del grupo', 'info');
  state.currentGroup = null;
  document.getElementById('group-view').classList.add('hidden');
  document.getElementById('welcome-state').classList.remove('hidden');
  await loadGroups();
}

// ===================== MEMBERS =====================
async function loadMembers() {
  if (!state.currentGroup) return;
  const { ok, data } = await apiCall('GET', `/groups/${state.currentGroup.groupId}/members`);
  if (!ok) return;

  state.members = data;

  // Find current user's role
  const me = data.find(m => m.userId === state.userId);
  state.currentRole = me?.role || 'MEMBER';
  state.currentCreator = state.currentGroup.createdByUserId;

  renderMembersList(data);
}

function renderMembersList(members) {
  const el = document.getElementById('members-list');
  if (!members.length) {
    el.innerHTML = '<div class="empty-state"><p>Sin miembros</p></div>';
    return;
  }

  const isAdmin = state.currentRole === 'ADMIN';
  const creatorId = state.currentGroup?.createdByUserId;

  el.innerHTML = members.map(m => {
    const isMe = m.userId === state.userId;
    const isCreatorMember = m.userId === creatorId;
    const canManage = isAdmin && !isMe;

    let actions = '';
    if (canManage) {
      const newRole = m.role === 'ADMIN' ? 'MEMBER' : 'ADMIN';
      const roleLabel = m.role === 'ADMIN' ? '↓ Quitar admin' : '↑ Hacer admin';
      actions += `<button class="mi-btn" title="${roleLabel}" onclick="changeMemberRole(${m.userId}, '${newRole}')">
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          ${m.role === 'ADMIN' ? '<polyline points="18 15 12 9 6 15"/>' : '<polyline points="6 9 12 15 18 9"/>'}
        </svg>
      </button>`;
      if (!isCreatorMember) {
        actions += `<button class="mi-btn danger" title="Expulsar" onclick="openRemoveMemberModal(${m.userId}, '${escHtml(m.name)}')">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>`;
      }
    }

    return `<div class="member-item" data-user-id="${m.userId}">
      <div class="mi-avatar ${avColor(m.name)}">${getInitials(m.name)}</div>
      <div class="mi-info">
        <div class="mi-name">${escHtml(m.name)}${isMe ? ' <span style="color:var(--text3)">(tú)</span>' : ''}</div>
        <span class="mi-role ${m.role === 'ADMIN' ? 'admin' : 'member'}">${m.role}</span>
      </div>
      ${actions ? `<div class="mi-actions">${actions}</div>` : ''}
    </div>`;
  }).join('');
}

// Remove member
let _removeMemberId = null;
function openRemoveMemberModal(userId, name) {
  _removeMemberId = userId;
  document.getElementById('rm-member-name').textContent = name;
  openModal('modal-remove-member');
}
async function confirmRemoveMember() {
  if (!_removeMemberId) return;
  const { ok, data } = await apiCall('DELETE', `/groups/${state.currentGroup.groupId}/members/${_removeMemberId}`);
  if (!ok) return toast(data?.message || 'Error al expulsar', 'error');

  closeModal('modal-remove-member');
  toast('Miembro expulsado', 'info');
  _removeMemberId = null;
  await loadMembers();

  // Update group meta
  const meta = await apiCall('GET', `/groups/${state.currentGroup.groupId}`);
  if (meta.ok) {
    state.currentGroup = meta.data;
    document.getElementById('gv-meta').textContent =
      `${meta.data.memberCount||1} miembros · ${meta.data.channelCount||0} canales · creado por ${meta.data.createdByName}`;
    await loadGroups();
  }
}

// Change member role
async function changeMemberRole(userId, newRole) {
  const { ok, data } = await apiCall(
    'PATCH',
    `/groups/${state.currentGroup.groupId}/members/${userId}/role?role=${newRole}`
  );
  if (!ok) return toast(data?.message || 'Error al cambiar rol', 'error');
  toast(`Rol cambiado a ${newRole}`, 'success');
  await loadMembers();
}

// ===================== INVITE =====================
function openInviteModal() {
  openModal('modal-invite');
}
async function sendInvitation() {
  const email = document.getElementById('invite-email').value.trim();
  if (!email) return toast('Ingresa el email del usuario', 'warn');

  const { ok: uok, data: udata } = await apiCall('GET', `/users/email/${encodeURIComponent(email)}`);
  if (!uok) return toast('Usuario no encontrado', 'error');

  const { ok, data } = await apiCall(
    'POST',
    `/groups/${state.currentGroup.groupId}/invitations?invitedUserId=${udata.userId}`
  );
  if (!ok) return toast(data?.message || 'Error al enviar invitación', 'error');

  closeModal('modal-invite');
  toast(`Invitación enviada a ${email}`, 'success');
}

// ===================== INVITATIONS =====================
let _invPollTimer = null;
function pollInvitations() {
  loadInvitations();
  _invPollTimer = setInterval(() => { if (state.token) loadInvitations(); }, 30000);
}

async function loadInvitations() {
  const { ok, data } = await apiCall('GET', '/groups/invitations/pending');
  if (!ok) return;
  const list = Array.isArray(data) ? data : [];

  const badge = document.getElementById('inv-badge');
  if (list.length > 0) {
    badge.textContent = list.length;
    badge.classList.remove('hidden');
  } else {
    badge.classList.add('hidden');
  }
  renderInvitationsList(list);
}

function renderInvitationsList(list) {
  const el = document.getElementById('invitations-list');
  if (!list.length) {
    el.innerHTML = '<div class="empty-state"><p>No tienes invitaciones pendientes</p></div>';
    return;
  }
  el.innerHTML = list.map(inv => `
    <div class="inv-item">
      <div class="inv-group">${escHtml(inv.groupName || 'Grupo')}</div>
      <div class="inv-by">Invitado por ${escHtml(inv.invitedByName || 'Admin')}</div>
      <div class="inv-actions">
        <button class="btn-accept" onclick="respondInvitation(${inv.invitationId}, true)">✓ Aceptar</button>
        <button class="btn-reject" onclick="respondInvitation(${inv.invitationId}, false)">✗ Rechazar</button>
      </div>
    </div>`).join('');
}

async function respondInvitation(id, accept) {
  const { ok, data } = await apiCall('PATCH', `/groups/invitations/${id}?accept=${accept}`);
  if (!ok) return toast(data?.message || 'Error al responder', 'error');

  toast(accept ? '¡Invitación aceptada!' : 'Invitación rechazada', accept ? 'success' : 'info');
  await loadInvitations();
  if (accept) await loadGroups();
}

// ===================== HTML ESCAPE =====================
function escHtml(str) {
  if (!str) return '';
  return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}