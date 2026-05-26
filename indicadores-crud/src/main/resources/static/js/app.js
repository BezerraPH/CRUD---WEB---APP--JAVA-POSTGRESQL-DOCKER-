/**
 * UX: confirmações, foco, feedback de envio e auto-dismiss de alertas.
 */
(function () {
    'use strict';

    function initSidebar() {
        var btn = document.getElementById('btn-sidebar-toggle');
        var sidebar = document.getElementById('app-sidebar');
        var overlay = document.getElementById('sidebar-overlay');
        if (!btn || !sidebar) {
            return;
        }

        function setOpen(open) {
            document.body.classList.toggle('sidebar-open', open);
            btn.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (overlay) {
                overlay.hidden = !open;
                overlay.setAttribute('aria-hidden', open ? 'false' : 'true');
            }
        }

        btn.addEventListener('click', function () {
            setOpen(!document.body.classList.contains('sidebar-open'));
        });

        if (overlay) {
            overlay.addEventListener('click', function () {
                setOpen(false);
            });
        }

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                setOpen(false);
            }
        });

        sidebar.querySelectorAll('a').forEach(function (link) {
            link.addEventListener('click', function () {
                setOpen(false);
            });
        });
    }

    initSidebar();

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            var msg = form.getAttribute('data-confirm');
            if (msg && !window.confirm(msg)) {
                e.preventDefault();
            }
        });
    });

    var lancamentoForm = document.getElementById('form-lancamento');
    if (lancamentoForm) {
        var firstInput = lancamentoForm.querySelector('input:not([type="hidden"]):not([disabled])');
        if (firstInput && !document.querySelector('.alert-app-danger')) {
            setTimeout(function () { firstInput.focus(); }, 200);
        }
    }

    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"].btn-app-primary');
            if (btn && !btn.disabled) {
                btn.disabled = true;
                if (!btn.dataset.originalText) {
                    btn.dataset.originalText = btn.textContent;
                }
                btn.textContent = 'Salvando…';
            }
        });
    });

    document.querySelectorAll('.alert-dismissible').forEach(function (alert) {
        setTimeout(function () {
            var close = alert.querySelector('.btn-close');
            if (close) {
                close.click();
            }
        }, 8000);
    });
})();
