(() => {
    const STORAGE_TOKEN_KEY = 'jwtToken';

    let nextRoomUrl = 'room2.html';
    let redirectTriggered = false;
    let lastSolvedIds = new Set();

    function getAuthHeaders() {
        const token = localStorage.getItem(STORAGE_TOKEN_KEY);
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    }

    function getModalsRoot() {
        let root = document.getElementById('modals-root');
        if (!root) {
            root = document.createElement('div');
            root.id = 'modals-root';
            document.body.appendChild(root);
        }
        return root;
    }

    async function checkJournalDoorStatus() {
        if (redirectTriggered) return;
        try {
            const res = await fetch('/api/puzzles', {
                method: 'GET',
                headers: getAuthHeaders()
            });
            if (!res.ok) return;
            const data = await res.json();

            if (Array.isArray(data.puzzles)) {
                data.puzzles.forEach(p => {
                    if (p.solved && !lastSolvedIds.has(p.id)) {
                        lastSolvedIds.add(p.id);
                        window.dispatchEvent(new CustomEvent('puzzle:solved', {
                            detail: {
                                puzzleId: p.id,
                                doorUnlocked: Boolean(data.doorUnlocked)
                            }
                        }));
                    }
                });
            }

            if (data.nextRoom) {
                nextRoomUrl = data.nextRoom;
            }

            if (data.doorUnlocked) {
                showDoorUnlockedModal(nextRoomUrl);
            }
        } catch (err) {
            // Αγνοούμε προσωρινά σφάλματα δικτύου κατά τον έλεγχο
        }
    }

    function showDoorUnlockedModal(targetRoom) {
        if (redirectTriggered) return;
        redirectTriggered = true;

        const target = targetRoom || 'room2.html';
        const root = getModalsRoot();
        root.innerHTML = '';

        const backdrop = document.createElement('div');
        backdrop.className = 'puzzle-modal-backdrop';

        const card = document.createElement('div');
        card.className = 'puzzle-modal-card door-unlocked-card';
        card.innerHTML = `
            <h2 class="door-unlocked-title">🚪 Η πόρτα ξεκλείδωσε!</h2>
            <p class="door-unlocked-text">
                Οι δύο κωδικοί καταχωρήθηκαν στο <strong>Κοινό Ημερολόγιο</strong> από δύο διαφορετικούς παίκτες.<br>
                Η έξοδος <strong>«Προς Πτέρυγα Sully»</strong> άνοιξε. Μετάβαση στο Δωμάτιο 2 σε 3 δευτερόλεπτα...
            </p>
            <button type="button" class="door-proceed-btn">Μετάβαση τώρα →</button>
        `;

        const goNext = () => {
            window.location.href = target;
        };

        card.querySelector('.door-proceed-btn').addEventListener('click', goNext);
        backdrop.appendChild(card);
        root.appendChild(backdrop);

        setTimeout(goNext, 3000);
    }

    window.addEventListener('ws:message', (e) => {
        const msg = e.detail;
        if (!msg || !msg.type) return;
        const normalizedType = String(msg.type).toUpperCase();
        if (normalizedType === 'DOOR_UNLOCKED') {
            const target = msg.nextRoom || (msg.payload && msg.payload.nextRoom) || 'room2.html';
            showDoorUnlockedModal(target);
        } else if (normalizedType === 'JOURNAL_UPDATED' || normalizedType === 'CLUE_ADDED') {
            checkJournalDoorStatus();
        }
    });

    document.addEventListener('DOMContentLoaded', () => {
        checkJournalDoorStatus();
        setInterval(checkJournalDoorStatus, 1500);
    });
})();