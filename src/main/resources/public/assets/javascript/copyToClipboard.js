'use strict';

// Wires up buttons with data-copy-target="<element id>" to copy that element's text to the clipboard. Buttons are
// rendered hidden and only revealed here, so users without JavaScript never see a button that does nothing.
document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('[data-copy-target]').forEach(function (button) {
    const target = document.getElementById(button.dataset.copyTarget);
    const status = document.getElementById(button.dataset.copyStatus);
    if (!target) {
      return;
    }

    const originalLabel = button.textContent;
    let resetTimer;

    function showResult(message) {
      button.textContent = message;
      if (status) {
        status.textContent = message;
      }
      clearTimeout(resetTimer);
      resetTimer = setTimeout(function () {
        button.textContent = originalLabel;
        if (status) {
          status.textContent = '';
        }
      }, 2000);
    }

    // navigator.clipboard is only available in secure contexts, so fall back to selecting the text and using the
    // legacy copy command when the service is served over plain HTTP
    function fallbackCopy(text) {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.setAttribute('readonly', '');
      textarea.style.position = 'absolute';
      textarea.style.left = '-9999px';
      document.body.appendChild(textarea);
      textarea.select();
      try {
        return document.execCommand('copy');
      } finally {
        document.body.removeChild(textarea);
      }
    }

    button.addEventListener('click', function () {
      const text = target.textContent;

      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text)
          .then(function () { showResult('Copied'); })
          .catch(function () { showResult(fallbackCopy(text) ? 'Copied' : 'Copy failed'); });
      } else {
        showResult(fallbackCopy(text) ? 'Copied' : 'Copy failed');
      }
    });

    button.hidden = false;
  });
});
