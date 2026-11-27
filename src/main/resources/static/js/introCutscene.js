(() => {
    'use strict';

    let stage = 'waiting';
    let pendingAlarm = null;
    let alarmEventSent = false;
    let animationFrame = null;
    let lastFrameTime = null;
    let lastSendTime = 0;

    const blockedKeys = new Set([
        'arrowup',
        'arrowdown',
        'arrowleft',
        'arrowright',
        'e',
        'i'
    ]);

    function isLocked() {
        return stage !== 'finished' && stage !== 'failed';
    }

    function blockControls(event) {
        if (!isLocked()) return;

        const target = event.target;

        if (
            target &&
            (
                ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) ||
                target.isContentEditable
            )
        ) {
            return;
        }

        if (blockedKeys.has(event.key.toLowerCase())) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }

    window.addEventListener('keydown', blockControls, true);
    window.addEventListener('keyup', blockControls, true);

    function movieFinished() {
        return (
            !document.getElementById('movie-title-screen') &&
            !document.getElementById('cutscene-overlay')
        );
    }

    function getTarget() {
        return roomConfig.obstacles.find(exhibit => exhibit.isTarget);
    }

    function fail(message) {
        if (animationFrame !== null) {
            cancelAnimationFrame(animationFrame);
        }

        animationFrame = null;
        stage = 'failed';
        isIntroAutoWalking = false;
        document.body.classList.remove('cinema-mode');

        const hud = document.getElementById('hud-text');

        if (hud) hud.textContent = message;

        console.error('[intro cutscene]', message);
        renderGame();
    }

    function finish() {
        if (stage !== 'modal') return;

        stage = 'finished';
        isIntroAutoWalking = false;
        document.body.classList.remove('cinema-mode');
        renderGame();

        if (!alarmEventSent) {
            alarmEventSent = true;
            window.dispatchEvent(new CustomEvent('alarm:triggered'));
        }
    }

    function showAlarm(target) {
        if (stage === 'modal' || stage === 'finished') return;

        stage = 'modal';
        isIntroAutoWalking = true;

        if (roomConfig.alarmTriggered) {
            const modal = document.getElementById('intro-modal');

            if (modal) modal.style.display = 'flex';
        } else {
            playAlarmEffects(target);
        }
    }

    function requestAlarm(target) {
        if (!target) return;

        pendingAlarm = target;

        // Περιμένουμε να ολοκληρωθεί το περπάτημα αυτού του browser.
        if (
            movieFinished() &&
            (stage === 'awaiting-server' || stage === 'failed')
        ) {
            showAlarm(target);
        }
    }

    function approach(current, goal, distance) {
        const difference = goal - current;

        if (Math.abs(difference) <= distance) return goal;

        return current + Math.sign(difference) * distance;
    }

    function startWalking(target) {
        stage = 'walking';

        const playerIds = [
            String(localPlayerId),
            ...networkPlayers.keys()
        ].sort();

        const index = playerIds.indexOf(String(localPlayerId));
        const offset = (index - (playerIds.length - 1) / 2) * 16;

        const goalX = Math.max(
            target.x - playerState.size + 1,
            Math.min(
                target.x + target.width - 1,
                target.x + (target.width - playerState.size) / 2 + offset
            )
        );

        // Η πάνω πλευρά του παίκτη ακουμπά την κάτω πλευρά του εκθέματος.
        const goalY = target.y + target.height;
        const speed = 110;

        function frame(time) {
            if (stage !== 'walking') return;

            if (lastFrameTime === null) lastFrameTime = time;

            const elapsed = Math.min(time - lastFrameTime, 50) / 1000;
            lastFrameTime = time;

            if (playerState.x !== goalX) {
                playerState.direction =
                    playerState.x < goalX ? 'right' : 'left';

                playerState.x = approach(
                    playerState.x,
                    goalX,
                    speed * elapsed
                );
            } else {
                playerState.direction =
                    playerState.y < goalY ? 'down' : 'up';

                playerState.y = approach(
                    playerState.y,
                    goalY,
                    speed * elapsed
                );
            }

            const arrived =
                playerState.x === goalX &&
                playerState.y === goalY;

            if (arrived) playerState.direction = 'up';

            if (arrived || time - lastSendTime >= 80) {
                if (!gameSocket.sendMove(playerState)) {
                    fail('Η σύνδεση διακόπηκε. Κάνε ανανέωση της σελίδας.');
                    return;
                }

                lastSendTime = time;
            }

            renderGame();

            if (arrived) {
                animationFrame = null;
                stage = 'awaiting-server';

                if (pendingAlarm || target.collected) {
                    showAlarm(pendingAlarm || target);
                    return;
                }

                if (!gameSocket.sendExhibitCollected(target)) {
                    fail('Δεν στάλθηκε η συλλογή. Κάνε ανανέωση της σελίδας.');
                    return;
                }

                window.setTimeout(() => {
                    if (stage === 'awaiting-server') {
                        fail(
                            'Δεν επιβεβαιώθηκε η συλλογή. ' +
                            'Δοκίμασε το E κοντά στο έκθεμα ή έλεγξε τη σύνδεση.'
                        );
                    }
                }, 8000);

                return;
            }

            animationFrame = requestAnimationFrame(frame);
        }

        animationFrame = requestAnimationFrame(frame);
    }

    function tryStart() {
        if (stage !== 'waiting') return;

        if (!movieFinished() || localPlayerId === null) return;

        const target = getTarget();

        if (!target) {
            fail('Δεν βρέθηκε το έκθεμα της εισαγωγής.');
            return;
        }

        // Παίκτης που μπήκε αφού είχε ήδη συλλεχθεί το έκθεμα.
        if (pendingAlarm || target.collected || roomConfig.alarmTriggered) {
            showAlarm(pendingAlarm || target);
            return;
        }

        startWalking(target);
    }

    window.IntroCutscene = Object.freeze({
        requestAlarm,
        isLocked
    });

    function initialize() {
        const startButton = document.getElementById('start-btn');

        if (!startButton || !document.getElementById('intro-modal')) {
            fail('Λείπει το παράθυρο συναγερμού ή το κουμπί συνέχειας.');
            return;
        }

        startButton.addEventListener('click', finish);

        if (pendingIntroAlarm) {
            requestAlarm(pendingIntroAlarm);
        }

        // Το event bus εκπέμπει πριν ενημερωθεί η τοπική κατάσταση.
        window.addEventListener('ws:message', () => {
            queueMicrotask(tryStart);
        });

        const observer = new MutationObserver(tryStart);

        observer.observe(document.body, {
            childList: true
        });

        const readinessTimer = window.setInterval(() => {
            tryStart();

            if (stage !== 'waiting') {
                window.clearInterval(readinessTimer);
                observer.disconnect();
            }
        }, 250);

        tryStart();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize, {
            once: true
        });
    } else {
        initialize();
    }
})();