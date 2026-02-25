// ===================== ESTADO GLOBAL =====================
const state = {
  token:               null,
  userId:              null,
  userName:            null,
  userEmail:           null,
  groups:              [],
  currentGroup:        null,
  currentGroupTab:     'members',
  pendingInvitations:  [],
};

// ===================== FETCH HELPER =====================
// Todas las llamadas al API pasan por aquí.
// Retorna { ok, status, data } siempre — nunca lanza excepciones.
async function apiCall(method, path, body = null, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && state.token) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  try {
    const res  = await fetch('/api' + path, opts);
    const text = await res.text();
    let data = null;
    try { data = JSON.parse(text); } catch (_) {}
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    // Error de red (backend caído, CORS, etc.)
    console.error('[API] Error de red:', err);
    return { ok: false, status: 0, data: { message: 'No se pudo conectar con el servidor' } };
  }
}

// ===================== UTILS =====================

// Genera iniciales a partir de un nombre
function getInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(' ');
  return parts.length >= 2
    ? (parts[0][0] + parts[1][0]).toUpperCase()
    : name.slice(0, 2).toUpperCase();
}

// Asigna un color de avatar determinístico según el nombre
function getAvatarColor(str) {
  const colors = ['av-blue', 'av-purple', 'av-green', 'av-orange', 'av-pink', 'av-teal'];
  let hash = 0;
  for (const c of (str || '')) hash = c.charCodeAt(0) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
}

// ===================== TOAST =====================
function showToast(msg, type = 'info') {
  const icons = { success: '✅', error: '❌', info: '💬', warn: '⚠️' };
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<span class="toast-icon">${icons[type] || 'ℹ️'}</span><span>${msg}</span>`;
  document.getElementById('toast-container').appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

// ===================== MODALES =====================
function openModal(id) {
  document.getElementById(id).classList.add('open');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('open');
  // Limpiar inputs del modal
  document.querySelectorAll(`#${id} input, #${id} textarea`).forEach(el => {
    if (el.type !== 'checkbox') el.value = '';
  });
}

// Cerrar modal al hacer click fuera
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.modal-overlay').forEach(m => {
    m.addEventListener('click', e => {
      if (e.target === m) m.classList.remove('open');
    });
  });

  // Cerrar con ESC
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.open').forEach(m => m.classList.remove('open'));
    }
  });
});