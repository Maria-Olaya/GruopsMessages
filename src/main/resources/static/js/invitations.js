// ===================== INVITACIONES =====================
// Depende de: api.js (state, apiCall, showToast, getInitials, getAvatarColor)

// Carga las invitaciones pendientes del usuario autenticado
async function loadPendingInvitations() {
  // GET /api/groups/invitations/pending — userId del token JWT
  // retorna List<InvitationResponseDTO>:
  // { invitationId, groupId, groupName, invitedByUserId, invitedByName,
  //   invitedUserId, invitedUserName, status, sentAt, respondedAt }
  const { ok, data } = await apiCall('GET', '/groups/invitations/pending');
  if (!ok) return;

  state.pendingInvitations = Array.isArray(data) ? data : [];

  // Actualizar badge del botón 🔔
  const badge = document.getElementById('invite-badge');
  if (state.pendingInvitations.length > 0) {
    badge.textContent    = state.pendingInvitations.length;
    badge.style.display  = 'block';
  } else {
    badge.style.display  = 'none';
  }

  renderInvitationsList();
}

function renderInvitationsList() {
  const container = document.getElementById('invitations-sidebar-list');

  if (!state.pendingInvitations.length) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">🔔</div>
        <p>No tienes invitaciones pendientes</p>
      </div>`;
    return;
  }

  container.innerHTML = state.pendingInvitations.map(inv => `
    <div class="invite-item">
      <div class="avatar avatar-sm ${getAvatarColor(inv.groupName || 'G')}">
        ${getInitials(inv.groupName || 'G')}
      </div>
      <div style="flex:1;min-width:0">
        <div style="font-size:13px;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
          ${inv.groupName || 'Grupo'}
        </div>
        <div style="font-size:11px;color:var(--text2)">
          Invitado por ${inv.invitedByName || 'Admin'}
        </div>
      </div>
      <div class="invite-actions">
        <button class="btn-accept" onclick="respondInvitation(${inv.invitationId}, true)"  title="Aceptar">✓</button>
        <button class="btn-reject" onclick="respondInvitation(${inv.invitationId}, false)" title="Rechazar">✗</button>
      </div>
    </div>`).join('');
}

// Acepta o rechaza una invitación
async function respondInvitation(invitationId, accept) {
  // PATCH /api/groups/invitations/{invitationId}?accept=true|false
  // userId del token JWT (valida que sea el invitado)
  // retorna InvitationResponseDTO
  const { ok, data } = await apiCall('PATCH', `/groups/invitations/${invitationId}?accept=${accept}`);
  if (!ok) return showToast(data?.message || 'Error al responder la invitación', 'error');

  showToast(accept ? '¡Invitación aceptada!' : 'Invitación rechazada', accept ? 'success' : 'info');

  // Refrescar invitaciones y grupos si fue aceptada
  await loadPendingInvitations();
  if (accept) loadGroups();
}

// Muestra el panel de invitaciones en el sidebar
function showInvitationsPanel() {
  const invTab = document.querySelectorAll('.nav-tab')[1];
  switchSidePanel('invitations', invTab);
}

// Alterna entre el panel de grupos y el de invitaciones
function switchSidePanel(panel, el) {
  document.getElementById('panel-groups').style.display      = panel === 'groups'      ? '' : 'none';
  document.getElementById('panel-invitations').style.display = panel === 'invitations' ? '' : 'none';

  document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');

  if (panel === 'invitations') loadPendingInvitations();
}