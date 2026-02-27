// ===================== INIT =====================
document.addEventListener('DOMContentLoaded', () => {
  // Wire up profile modal button
  document.querySelector('[onclick="openModal(\'modal-profile\')"]')?.setAttribute('onclick', 'openProfileModal()');

  // Auto focus
  const emailInput = document.getElementById('login-email');
  if (emailInput) emailInput.focus();
});