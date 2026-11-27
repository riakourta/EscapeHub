// exhibitModal.js - Δυναμική φόρτωση εκθεμάτων (GET /api/exhibits) & Modal πληροφοριών με πλήκτρο I

const ExhibitInspector = (() => {
    const exhibitsById = new Map();
    let isLoaded = false;
    let activeExhibitId = null;

    function getAuthHeaders() {
        const tokenKey =
            typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS
                ? API_CONFIG.STORAGE_KEYS.TOKEN
                : 'jwtToken';
        const token = localStorage.getItem(tokenKey);
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    }

    async function loadExhibitsFromApi() {
        try {
            const response = await fetch('/api/exhibits', {
                method: 'GET',
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const list = await response.json();
            if (!Array.isArray(list)) return;

            exhibitsById.clear();
            for (const item of list) {
                exhibitsById.set(item.id, item);

                // Ενημερώνουμε δυναμικά και το roomConfig.obstacles ώστε να μην υπάρχουν hardcoded ονόματα
                if (typeof roomConfig !== 'undefined' && Array.isArray(roomConfig.obstacles)) {
                    const localObs = roomConfig.obstacles.find(o => o.id === item.id);
                    if (localObs && item.label && !localObs.collected) {
                        localObs.label = item.label;
                    }
                }
            }
            isLoaded = true;
        } catch (err) {
            console.error('Αποτυχία φόρτωσης δεδομένων εκθεμάτων από /api/exhibits:', err);
        }
    }

    function isTypingInInput(target) {
        if (!target) return false;
        const tag = target.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') {
            return true;
        }
        if (target.isContentEditable) {
            return true;
        }
        return Boolean(target.closest('#chat-container, #search-bar-slot'));
    }

    function isInspectKey(event) {
        if (!event || !event.key) return false;
        const k = event.key.toLowerCase();
        // Υποστήριξη και για αγγλικό 'i' και για ελληνικό 'ι'
        return k === 'i' || k === 'ι' || event.code === 'KeyI';
    }

    function setFeedback(element, message, isError = false) {
        if (!element) return;
        element.textContent = message;
        element.classList.toggle('error', isError);
    }

    function buildInventoryItem(exhibit, clue, index) {
            return {
                id: clue.id || clue.clueId || `${exhibit.id}-clue-${index + 1}`,
                title: clue.title || exhibit.name || exhibit.label || 'Στοιχείο',
                description: clue.description || clue.text || '',
                shared: false
            };
    }

    async function handleAddToInventory(clue, feedbackEl) {
        try {
            if (window.InventoryActions && typeof window.InventoryActions.addToInventory === 'function') {
                await window.InventoryActions.addToInventory(clue);
            } else {
                const res = await fetch('/api/inventory/add', {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    body: JSON.stringify(clue)
                });
                if (!res.ok) {
                    throw new Error('Δεν ήταν δυνατή η προσθήκη στο Inventory.');
                }
            }
            setFeedback(feedbackEl, '✔ Το στοιχείο προστέθηκε στο προσωπικό σου Inventory!', false);
        } catch (err) {
            setFeedback(feedbackEl, err.message || 'Σφάλμα κατά την προσθήκη στο Inventory.', true);
        }
    }

    async function handleAddToJournal(clue, feedbackEl) {
        try {
            if (window.InventoryActions && typeof window.InventoryActions.addToJournal === 'function') {
                await window.InventoryActions.addToJournal(clue);
            } else {
                const res = await fetch('/api/journal/add', {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    body: JSON.stringify(clue)
                });
                if (!res.ok) {
                    throw new Error('Δεν ήταν δυνατή η προσθήκη στο Κοινό Ημερολόγιο.');
                }
            }
            setFeedback(feedbackEl, '✔ Το στοιχείο καταχωρήθηκε στο Κοινό Ημερολόγιο!', false);
        } catch (err) {
            setFeedback(feedbackEl, err.message || 'Σφάλμα κατά την καταχώρηση στο Ημερολόγιο.', true);
        }
    }

    function closeModal() {
        const root = document.getElementById('modals-root');
        if (root) {
            const existing = root.querySelector('.exhibit-modal-backdrop');
            if (existing) existing.remove();
        }
        activeExhibitId = null;
    }

    function renderModal(exhibit) {
        const root = document.getElementById('modals-root');
        if (!root || !exhibit) return;

        closeModal();
        activeExhibitId = exhibit.id;

        const backdrop = document.createElement('div');
        backdrop.className = 'exhibit-modal-backdrop';
        backdrop.setAttribute('role', 'dialog');
        backdrop.setAttribute('aria-modal', 'true');

        backdrop.addEventListener('click', (e) => {
            if (e.target === backdrop) closeModal();
        });

        const card = document.createElement('div');
        card.className = 'exhibit-modal-card';

        // Header
        const header = document.createElement('div');
        header.className = 'exhibit-modal-header';

        const titleWrap = document.createElement('div');
        const badge = document.createElement('span');
        badge.className = 'exhibit-category-badge';
        badge.textContent = exhibit.category || 'Έκθεμα Μουσείου';

        const title = document.createElement('h2');
        title.className = 'exhibit-modal-title';
        title.textContent = exhibit.name || exhibit.label;

        titleWrap.appendChild(badge);
        titleWrap.appendChild(title);

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'exhibit-close-btn';
        closeBtn.setAttribute('aria-label', 'Κλείσιμο');
        closeBtn.textContent = '✕';
        closeBtn.addEventListener('click', closeModal);

        header.appendChild(titleWrap);
        header.appendChild(closeBtn);
        card.appendChild(header);

        // Εικόνα + Βασικά Πεδία (Όνομα εκθέματος, Καλλιτέχνης, Ημερομηνία)
        const bodyGrid = document.createElement('div');
        bodyGrid.className = 'exhibit-body-grid';

        const imgBox = document.createElement('div');
        imgBox.className = 'exhibit-image-box';

        if (exhibit.imageUrl) {
            const img = document.createElement('img');
            img.src = exhibit.imageUrl;
            img.alt = exhibit.name || exhibit.label;
            img.onerror = () => {
                img.remove();
                const fallback = document.createElement('div');
                fallback.className = 'exhibit-image-fallback';
                fallback.textContent = `🖼️ ${exhibit.name || exhibit.label}`;
                imgBox.appendChild(fallback);
            };
            imgBox.appendChild(img);
        } else {
            const fallback = document.createElement('div');
            fallback.className = 'exhibit-image-fallback';
            fallback.textContent = `🖼️ ${exhibit.name || exhibit.label}`;
            imgBox.appendChild(fallback);
        }

        const metaList = document.createElement('div');
        metaList.className = 'exhibit-meta-list';

        const fields = [
            { label: 'Όνομα Εκθέματος', value: exhibit.name || exhibit.label },
            { label: 'Καλλιτέχνης / Δημιουργός', value: exhibit.artist || 'Άγνωστος' },
            { label: 'Ημερομηνία / Περίοδος', value: exhibit.date || 'Άγνωστη' }
        ];

        for (const field of fields) {
            const row = document.createElement('div');
            row.className = 'exhibit-meta-row';

            const lbl = document.createElement('span');
            lbl.className = 'exhibit-meta-label';
            lbl.textContent = field.label;

            const val = document.createElement('span');
            val.className = 'exhibit-meta-value';
            val.textContent = field.value;

            row.appendChild(lbl);
            row.appendChild(val);
            metaList.appendChild(row);
        }

        bodyGrid.appendChild(imgBox);
        bodyGrid.appendChild(metaList);
        card.appendChild(bodyGrid);

        // Περιγραφή
        if (exhibit.description) {
            const desc = document.createElement('p');
            desc.className = 'exhibit-description';
            desc.textContent = exhibit.description;
            card.appendChild(desc);
        }

        // Σημείωση Ασφαλείας
        if (exhibit.securityNote) {
            const note = document.createElement('div');
            note.className = 'exhibit-security-note';
            note.textContent = `🔍 Σημείωση Ασφαλείας: ${exhibit.securityNote}`;
            card.appendChild(note);
        }

        // Στοιχεία / Clues με τα 2 κουμπιά
        const clues = Array.isArray(exhibit.clues) ? exhibit.clues : [];
        if (clues.length > 0) {
            const cluesSection = document.createElement('div');
            cluesSection.className = 'exhibit-clues-section';

            const heading = document.createElement('div');
            heading.className = 'exhibit-clues-heading';
            heading.textContent = '📌 Διαθέσιμα Στοιχεία (Clues) για Καταχώρηση:';
            cluesSection.appendChild(heading);

            const feedbackEl = document.createElement('div');
            feedbackEl.className = 'exhibit-feedback';

            clues.forEach((clue, index) => {
                            const itemPayload = buildInventoryItem(exhibit, clue, index);

                            const clueCard = document.createElement('div');
                            clueCard.className = 'exhibit-clue-card';

                            const cTitle = document.createElement('div');
                            cTitle.className = 'exhibit-clue-title';
                            cTitle.textContent = clue.title;

                            const cText = document.createElement('div');
                            cText.className = 'exhibit-clue-text';
                            cText.textContent = clue.text || clue.description || '';

                            const actions = document.createElement('div');
                            actions.className = 'exhibit-clue-actions';

                            const btnInv = document.createElement('button');
                            btnInv.type = 'button';
                            btnInv.className = 'exhibit-btn exhibit-btn-inventory';
                            btnInv.textContent = '🎒 Προσθήκη στο Inventory';
                            btnInv.addEventListener('click', () => handleAddToInventory(itemPayload, feedbackEl));

                            const btnJournal = document.createElement('button');
                            btnJournal.type = 'button';
                            btnJournal.className = 'exhibit-btn exhibit-btn-journal';
                            btnJournal.textContent = '📜 Προσθήκη στο Κοινό Ημερολόγιο';
                            btnJournal.addEventListener('click', () => handleAddToJournal(itemPayload, feedbackEl));

                            actions.appendChild(btnInv);
                            actions.appendChild(btnJournal);

                            clueCard.appendChild(cTitle);
                            clueCard.appendChild(cText);
                            clueCard.appendChild(actions);
                            cluesSection.appendChild(clueCard);
                        });

            cluesSection.appendChild(feedbackEl);
            card.appendChild(cluesSection);
        }

        backdrop.appendChild(card);
        root.appendChild(backdrop);
    }

    async function inspectNearbyExhibit() {
        if (typeof getNearbyExhibit !== 'function') return;

        const nearby = getNearbyExhibit();
        if (!nearby) return;

        if (!isLoaded) {
            await loadExhibitsFromApi();
        }

        const details = exhibitsById.get(nearby.id);
        if (details) {
            renderModal(details);
        }
    }

    function initKeyListener() {
        window.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && activeExhibitId) {
                closeModal();
                return;
            }

            if (!isInspectKey(event) || event.repeat) return;
            if (isTypingInInput(event.target)) return;

            if (activeExhibitId) {
                event.preventDefault();
                closeModal();
                return;
            }

            if (
                (typeof isPanicSequenceRunning !== 'undefined' && isPanicSequenceRunning) ||
                (typeof isIntroAutoWalking !== 'undefined' && isIntroAutoWalking)
            ) {
                return;
            }

            if (typeof getNearbyExhibit === 'function' && getNearbyExhibit()) {
                event.preventDefault();
                inspectNearbyExhibit();
            }
        });
    }

    function init() {
        loadExhibitsFromApi();
        initKeyListener();
    }

    return {
        init,
        loadExhibitsFromApi,
        inspectNearbyExhibit,
        closeModal,
        isTypingInInput
    };
})();

if (typeof window !== 'undefined') {
    window.ExhibitInspector = ExhibitInspector;
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => ExhibitInspector.init());
    } else {
        ExhibitInspector.init();
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = { ExhibitInspector };
}