// ===================== AUTH =====================

function switchAuthTab(tab) {
  document.getElementById('form-login').classList.toggle('hidden', tab !== 'login');
  document.getElementById('form-register').classList.toggle('hidden', tab !== 'register');
  document.getElementById('tab-login').classList.toggle('active', tab === 'login');
  document.getElementById('tab-register').classList.toggle('active', tab !== 'login');
  hideAuthError();
}

function showAuthError(msg) {
  const el = document.getElementById('auth-error');
  el.textContent = msg;
  el.classList.remove('hidden');
}
function hideAuthError() {
  document.getElementById('auth-error').classList.add('hidden');
}

async function doLogin() {
  const email = document.getElementById('login-email').value.trim();
  const pass  = document.getElementById('login-pass').value;
  if (!email || !pass) return showAuthError('Completa todos los campos');

  const { ok, data } = await apiCall('POST', '/users/login', { email, password: pass }, false);
  if (!ok) return showAuthError(data?.message || 'Credenciales incorrectas');

  startSession({ ...data, email });
}

async function doRegister() {
  const name  = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const pass  = document.getElementById('reg-pass').value;

  if (!name || !email || !pass) return showAuthError('Completa todos los campos');
  if (pass.length < 6) return showAuthError('La contraseña debe tener al menos 6 caracteres');

  const reg = await apiCall('POST', '/users/register', { name, email, password: pass }, false);
  if (!reg.ok) return showAuthError(reg.data?.message || 'Error al crear la cuenta');

  const login = await apiCall('POST', '/users/login', { email, password: pass }, false);
  if (!login.ok) return showAuthError('Cuenta creada. Por favor inicia sesión.');

  startSession({ ...login.data, email });
}

function startSession(data) {
  state.token     = data.token;
  state.userId    = data.userId;
  state.userName  = data.name;
  state.userEmail = data.email;
  state.userRole  = data.role;

  // Persistir en sessionStorage para sobrevivir reloads
  saveSession();

  _applySession();
}

// Aplica el estado de sesión a la UI (usado tanto en login como en restore)
function _applySession() {
  document.getElementById('my-name').textContent  = state.userName;
  document.getElementById('my-email').textContent = state.userEmail;
  const av = document.getElementById('my-avatar');
  av.textContent = getInitials(state.userName);
  av.className = `user-avatar ${avColor(state.userName)}`;

  document.getElementById('screen-auth').classList.remove('active');
  document.getElementById('screen-app').classList.add('active');

  connectWebSocket();
  loadGroups();
  pollInvitations();
}

function logout() {
  disconnectWebSocket();

  // Limpiar sessionStorage
  clearSession();

  Object.assign(state, {
    token: null, userId: null, userName: null, userEmail: null,
    groups: [], currentGroup: null, currentRole: null, members: []
  });

  document.getElementById('screen-app').classList.remove('active');
  document.getElementById('screen-auth').classList.add('active');
  document.getElementById('group-view').classList.add('hidden');
  document.getElementById('welcome-state').classList.remove('hidden');
  document.getElementById('groups-list').innerHTML = '';
  document.getElementById('invitations-list').innerHTML = '';
  document.getElementById('login-email').value = '';
  document.getElementById('login-pass').value  = '';
  hideAuthError();
  switchAuthTab('login');
}

// ===================== RESTORE AL CARGAR =====================
// Si hay sesión guardada, restaurar la UI sin pedir login de nuevo
document.addEventListener('DOMContentLoaded', () => {
  document.querySelector('[onclick="openModal(\'modal-profile\')"]')
    ?.setAttribute('onclick', 'openProfileModal()');

  if (state.token) {
    // Ya se restauró el state en api.js — solo aplicar a la UI
    _applySession();
  } else {
    document.getElementById('login-email')?.focus();
  }
});

// ===================== PROFILE MODAL =====================

function openProfileModal() {
  document.getElementById('p-name').value  = state.userName || '';
  document.getElementById('p-email').value = state.userEmail || '';
  openModal('modal-profile');
  switchProfileTab('info', document.querySelector('.profile-tab'));
}

function switchProfileTab(tab, el) {
  ['info','password','danger'].forEach(t => {
    document.getElementById(`ptab-${t}`).classList.add('hidden');
  });
  document.getElementById(`ptab-${tab}`).classList.remove('hidden');
  document.querySelectorAll('.profile-tab').forEach(t => t.classList.remove('active'));
  if (el) el.classList.add('active');
}

async function saveProfile() {
  const name  = document.getElementById('p-name').value.trim();
  const email = document.getElementById('p-email').value.trim();
  const phone = document.getElementById('p-phone').value.trim();
  if (!name || !email) return toast('Nombre y email son requeridos', 'warn');

  const { ok, data } = await apiCall('PUT', `/users/${state.userId}`, { name, email, phoneNumber: phone || null });
  if (!ok) return toast(data?.message || 'Error al guardar cambios', 'error');

  state.userName  = name;
  state.userEmail = email;
  saveSession(); // Actualizar sessionStorage con nuevo nombre/email

  document.getElementById('my-name').textContent  = name;
  document.getElementById('my-email').textContent = email;
  const av = document.getElementById('my-avatar');
  av.textContent = getInitials(name);
  av.className = `user-avatar ${avColor(name)}`;

  closeModal('modal-profile');
  toast('Perfil actualizado', 'success');
}

async function changePassword() {
  const oldPass = document.getElementById('p-old-pass').value;
  const newPass = document.getElementById('p-new-pass').value;
  if (!oldPass || !newPass) return toast('Completa ambos campos', 'warn');
  if (newPass.length < 6) return toast('La nueva contraseña debe tener al menos 6 caracteres', 'warn');

  const { ok, data } = await apiCall(
    'PATCH',
    `/users/${state.userId}/password?oldPassword=${encodeURIComponent(oldPass)}&newPassword=${encodeURIComponent(newPass)}`
  );
  if (!ok) return toast(data?.message || 'Contraseña actual incorrecta', 'error');

  closeModal('modal-profile');
  toast('Contraseña actualizada', 'success');
}

async function deleteAccount() {
  if (!confirm('¿Estás seguro? Esta acción desactivará tu cuenta permanentemente.')) return;

  const { ok, data } = await apiCall('DELETE', `/users/${state.userId}`);
  if (!ok) return toast(data?.message || 'Error al eliminar la cuenta', 'error');

  toast('Cuenta eliminada', 'info');
  setTimeout(logout, 1000);
}