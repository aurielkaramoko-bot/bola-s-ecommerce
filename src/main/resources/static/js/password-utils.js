/**
 * BOLA — Password utilities
 * - Toggle œil (show/hide password)
 * - Strength meter (faible/moyen/fort)
 *
 * Usage:
 *   <div class="input-group bolas-pw-group">
 *     <input type="password" class="form-control bolas-pw-input" id="myPw"/>
 *     <button type="button" class="btn bolas-pw-toggle input-group-text" data-target="myPw">
 *       <i class="bi bi-eye"></i>
 *     </button>
 *   </div>
 *   <!-- optional strength bar (auto-attached if present) -->
 *   <div class="bolas-pw-strength" data-for="myPw"></div>
 */
(function () {
  'use strict';

  /* ─── Toggle eye ─── */
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('.bolas-pw-toggle');
    if (!btn) return;
    var targetId = btn.getAttribute('data-target');
    var input = document.getElementById(targetId);
    if (!input) return;
    var isPassword = input.type === 'password';
    input.type = isPassword ? 'text' : 'password';
    var icon = btn.querySelector('i');
    if (icon) {
      icon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
    }
  });

  /* ─── Strength meter ─── */
  function calcStrength(pw) {
    if (!pw) return { score: 0, label: '', cls: '' };
    var score = 0;
    if (pw.length >= 6) score++;
    if (pw.length >= 10) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;

    if (score <= 1) return { score: 20,  label: 'Faible',  cls: 'pw-weak' };
    if (score <= 2) return { score: 40,  label: 'Faible',  cls: 'pw-weak' };
    if (score === 3) return { score: 60,  label: 'Moyen',   cls: 'pw-medium' };
    if (score === 4) return { score: 80,  label: 'Fort',    cls: 'pw-strong' };
    return { score: 100, label: 'Très fort', cls: 'pw-very-strong' };
  }

  function updateStrengthBar(input) {
    var barEl = document.querySelector('.bolas-pw-strength[data-for="' + input.id + '"]');
    if (!barEl) return;

    var result = calcStrength(input.value);
    var fill = barEl.querySelector('.pw-fill');
    var labelEl = barEl.querySelector('.pw-label');

    if (!fill) {
      // Build DOM on first use
      barEl.innerHTML =
        '<div class="pw-bar-track">' +
          '<div class="pw-fill"></div>' +
        '</div>' +
        '<div class="pw-hints">' +
          '<span class="pw-label"></span>' +
          '<span class="pw-tips text-muted">Min 6 car. • 1 majuscule • 1 chiffre</span>' +
        '</div>';
      fill = barEl.querySelector('.pw-fill');
      labelEl = barEl.querySelector('.pw-label');
    }

    fill.style.width = result.score + '%';
    fill.className = 'pw-fill ' + result.cls;
    labelEl.textContent = result.label;
    labelEl.className = 'pw-label ' + result.cls;
  }

  document.addEventListener('input', function (e) {
    if (e.target.classList.contains('bolas-pw-input')) {
      updateStrengthBar(e.target);
    }
  });
})();
