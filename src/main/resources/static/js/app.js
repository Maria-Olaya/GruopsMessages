// ===================== APP — INICIALIZACIÓN =====================
// Este archivo se carga de último.
// Aquí van solo las cosas que necesitan que el DOM esté listo.

document.addEventListener('DOMContentLoaded', () => {

  // Refrescar invitaciones cada 30 segundos (mientras hay sesión activa)
  setInterval(() => {
    if (state.token) loadPendingInvitations();
  }, 30_000);

  // Focus automático en el campo email al cargar
  const emailInput = document.getElementById('login-email');
  if (emailInput) emailInput.focus();

});