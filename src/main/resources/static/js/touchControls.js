// Use the existing keyboard path for collisions, modal guards and network updates.
(() => {
    const controls = document.getElementById('touch-controls');
    if (!controls) return;
    let activePointer = null;
    let timer = null;
    function stop() {
        if (timer !== null) window.clearInterval(timer);
        timer = null;
        activePointer = null;
    }
    controls.querySelectorAll('button[data-key]').forEach(button => {
        button.addEventListener('pointerdown', event => {
            if (event.button !== 0 || activePointer !== null) return;
            event.preventDefault();
            activePointer = event.pointerId;
            button.setPointerCapture(event.pointerId);
            const step = () => {
                if (typeof window.canMoveInMuseum === 'function' && !window.canMoveInMuseum()) {
                    stop();
                    return;
                }
                window.dispatchEvent(new KeyboardEvent('keydown', {key: button.dataset.key, cancelable: true}));
            };
            step();
            if (activePointer !== null) timer = window.setInterval(step, 80);
        });
        for (const type of ['pointerup', 'pointercancel', 'lostpointercapture']) {
            button.addEventListener(type, event => {
                if (event.pointerId === activePointer) stop();
            });
        }
        button.addEventListener('contextmenu', event => event.preventDefault());
    });
    window.addEventListener('blur', stop);
    window.addEventListener('pagehide', stop);
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) stop();
    });
})();
