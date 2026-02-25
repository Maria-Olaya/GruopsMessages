// ===================== AUTH =====================
// Depende de: api.js (state, apiCall, showToast)

function switchTab(tab) {
  document.getElementById('form-login').style.display    = tab === 'login'    ? '' : 'none';
  document.getElementById('form-register').style.display = tab === 'register' ? '' : 'none';
  document.querySelectorAll('.auth-tab').forEach((t, i) => {
    t.classList.toggle('active', (i === 0 && tab === 'login') || (i === 1 && tab === 'register'));
  });
  hideAuthError();
}

function showAuthError(msg) {
  const el = document.getElementById('auth-error');
  el.textContent = msg;
  el.style.display = 'block';
}

function hideAuthError() {
  document.getElementById('auth-error').style.display = 'none';
}

// Login con email + contraseña
async function doLogin() {
  const email = document.getElementById('login-email').value.trim();
  const pass  = document.getElementById('login-pass').value;

  if (!email || !pass) return showAuthError('Completa todos los campos');

  const { ok, data } = await apiCall('POST', '/users/login', { email, password: pass }, false);
  if (!ok) return showAuthError(data?.message || 'Credenciales incorrectas');

  // login retorna AuthResponseDTO: { token, userId, name, role, expiresIn }
  startSession({ ...data, email });
}

// Registro: crea cuenta y hace auto-login
async function doRegister() {
  const name  = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const pass  = document.getElementById('reg-pass').value;

  if (!name || !email || !pass) return showAuthError('Completa todos los campos');
  if (pass.length < 6)          return showAuthError('La contraseña debe tener al menos 6 caracteres');

  // 1. Registrar — UserRequestDTO: { name, email, password }
  const reg = await apiCall('POST', '/users/register', { name, email, password: pass }, false);
  if (!reg.ok) return showAuthError(reg.data?.message || 'Error al crear la cuenta');

  // 2. Auto-login
  const login = await apiCall('POST', '/users/login', { email, password: pass }, false);
  if (!login.ok) return showAuthError('Cuenta creada. Por favor inicia sesión.');

  startSession({ ...login.data, email });
}

// Establece la sesión y cambia a la pantalla principal
function startSession(data) {
  state.token     = data.token;
  state.userId    = data.userId;
  state.userName  = data.name;
  state.userEmail = data.email;

  // Actualizar UI del sidebar
  document.getElementById('user-name-sidebar').textContent  = state.userName;
  document.getElementById('user-email-sidebar').textContent = state.userEmail;

  const av = document.getElementById('user-avatar-sm');
  av.textContent = getInitials(state.userName);
  av.className   = `avatar avatar-sm ${getAvatarColor(state.userName)}`;

  // Cambiar pantalla
  document.getElementById('screen-auth').classList.remove('active');
  document.getElementById('screen-app').classList.add('active');

  // Cargar datos iniciales
  loadGroups();
  loadPendingInvitations();
}

// Cierra la sesión y vuelve al login
function logout() {
  // Resetear estado
  state.token              = null;
  state.userId             = null;
  state.userName           = null;
  state.userEmail          = null;
  state.groups             = [];
  state.currentGroup       = null;
  state.pendingInvitations = [];

  // Resetear UI
  document.getElementById('screen-app').classList.remove('active');
  document.getElementById('screen-auth').classList.add('active');
  document.getElementById('login-email').value = '';
  document.getElementById('login-pass').value  = '';
  document.getElementById('group-view').classList.remove('active');
  document.getElementById('welcome-state').style.display = '';
  document.getElementById('groups-list').innerHTML = '';
  hideAuthError();
  switchTab('login');
}