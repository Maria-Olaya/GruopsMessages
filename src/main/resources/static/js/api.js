// ===================== ESTADO GLOBAL =====================
const state = {
  token:          null,
  userId:         null,
  userName:       null,
  userEmail:      null,
  userRole:       null,
  groups:         [],
  currentGroup:   null,
  currentRole:    null,
  currentCreator: null,
  members:        [],
};

// ===================== PERSISTENCIA DE SESIÓN =====================
// Restaurar sesión desde sessionStorage al cargar la página
// sessionStorage sobrevive el reload pero se limpia al cerrar la pestaña
(function restoreSession() {
  const saved = sessionStorage.getItem('gruopChat_session');
  if (!saved) return;
  try {
    const s = JSON.parse(saved);
    state.token     = s.token     || null;
    state.userId    = s.userId    || null;
    state.userName  = s.userName  || null;
    state.userEmail = s.userEmail || null;
    state.userRole  = s.userRole  || null;
  } catch(_) {
    sessionStorage.removeItem('gruopChat_session');
  }
})();

function saveSession() {
  sessionStorage.setItem('gruopChat_session', JSON.stringify({
    token:     state.token,
    userId:    state.userId,
    userName:  state.userName,
    userEmail: state.userEmail,
    userRole:  state.userRole,
  }));
}

function clearSession() {
  sessionStorage.removeItem('gruopChat_session');
}

// ===================== FETCH HELPER =====================
async function apiCall(method, path, body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && state.token) headers['Authorization'] = `Bearer ${state.token}`;

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  try {
    const res  = await fetch('/api' + path, opts);
    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch(_) {}

    // Si el token expiró, limpiar sesión y volver al login
    if (res.status === 401) {
      clearSession();
      Object.assign(state, { token: null, userId: null, userName: null, userEmail: null });
    }

    return { ok: res.ok, status: res.status, data };
  } catch(err) {
    console.error('[API] Network error:', err);
    return { ok: false, status: 0, data: { message: 'No se pudo conectar con el servidor' } };
  }
}

// ===================== AVATAR UTILS =====================
function getInitials(name) {
  if (!name) return '?';
  const p = name.trim().split(' ');
  return p.length >= 2 ? (p[0][0] + p[1][0]).toUpperCase() : name.slice(0,2).toUpperCase();
}
function avColor(str) {
  let h = 0;
  for (const c of (str||'')) h = c.charCodeAt(0) + ((h<<5) - h);
  return 'av-' + (Math.abs(h) % 6);
}

// ===================== TOASTS =====================
function toast(msg, type = 'info') {
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<div class="toast-dot"></div><span>${msg}</span>`;
  document.getElementById('toast-container').appendChild(el);
  setTimeout(() => {
    el.style.animation = 'none';
    el.style.opacity = '0';
    el.style.transform = 'translateX(20px)';
    el.style.transition = 'all 0.2s ease';
    setTimeout(() => el.remove(), 200);
  }, 3200);
}

// ===================== MODALES =====================
function openModal(id) {
  document.getElementById(id).classList.remove('hidden');
}
function closeModal(id) {
  document.getElementById(id).classList.add('hidden');
  document.querySelectorAll(`#${id} input:not([type=checkbox]), #${id} textarea`)
    .forEach(el => el.value = '');
}
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.modal-overlay').forEach(m => {
    m.addEventListener('click', e => {
      if (e.target === m) m.classList.add('hidden');
    });
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay:not(.hidden)').forEach(m => m.classList.add('hidden'));
      document.getElementById('msg-context-menu')?.classList.add('hidden');
    }
  });
  document.addEventListener('click', () => {
    document.getElementById('msg-context-menu')?.classList.add('hidden');
  });
});