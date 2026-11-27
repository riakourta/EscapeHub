(() => {
    const SLOT_ID = 'inventory-slot';
    const LIST_ID = 'personal-inventory-list';
    const COUNT_ID = 'personal-inventory-count';

    function getAuthHeaders() {
        const tokenKey = (typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS && API_CONFIG.STORAGE_KEYS.TOKEN)
            ? API_CONFIG.STORAGE_KEYS.TOKEN
            : 'jwtToken';
        const token = localStorage.getItem(tokenKey);
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    }

    async function apiRequest(url, method = 'GET', body = null) {
        const options = {
            method,
            headers: getAuthHeaders()
        };
        if (body !== null) {
            options.body = JSON.stringify(body);
        }

        const response = await fetch(url, options);
        const data = await response.json().catch(() => null);
        if (!response.ok) {
            const msg = (data && (data.error || data.message)) || `HTTP ${response.status}`;
            throw new Error(msg);
        }
        return data;
    }

    function updateSharedJournalUI(entries) {
        if (!Array.isArray(entries)) return;
        const list = document.getElementById('journal-list');
        if (!list) return;

        const nodes = entries.map(entry => {
            const li = document.createElement('li');
            li.textContent = entry.message;
            return li;
        });
        list.replaceChildren(...nodes);
        list.scrollTop = list.scrollHeight;
    }

    function ensurePanelMounted() {
        const slot = document.getElementById(SLOT_ID);
        if (!slot) return null;

        if (!slot.querySelector('.inventory-panel')) {
            const panel = document.createElement('section');
            panel.className = 'inventory-panel';
            panel.innerHTML = `
                <div class="inventory-panel-header">
                    <h3 class="inventory-panel-title">Προσωπικό Inventory</h3>
                    <span id="${COUNT_ID}" class="inventory-count-badge">0</span>
                </div>
                <ul id="${LIST_ID}" class="inventory-list">
                    <li class="inventory-empty">Κανένα προσωπικό στοιχείο ακόμα...</li>
                </ul>
            `;
            slot.appendChild(panel);
        }
        return slot;
    }

    function renderItems(items) {
        ensurePanelMounted();
        const listEl = document.getElementById(LIST_ID);
        const countEl = document.getElementById(COUNT_ID);
        if (!listEl) return;

        const safeItems = Array.isArray(items) ? items : [];
        if (countEl) {
            countEl.textContent = String(safeItems.length);
        }

        listEl.replaceChildren();

        if (safeItems.length === 0) {
            const emptyLi = document.createElement('li');
            emptyLi.className = 'inventory-empty';
            emptyLi.textContent = 'Κανένα προσωπικό στοιχείο ακόμα...';
            listEl.appendChild(emptyLi);
            return;
        }

        for (const item of safeItems) {
            const li = document.createElement('li');
            li.className = 'inventory-item';

            const titleEl = document.createElement('div');
            titleEl.className = 'inventory-item-title';
            titleEl.textContent = item.title || item.id;
            li.appendChild(titleEl);

            if (item.description) {
                const descEl = document.createElement('div');
                descEl.className = 'inventory-item-desc';
                descEl.textContent = item.description;
                li.appendChild(descEl);
            }

            const shareBtn = document.createElement('button');
            shareBtn.type = 'button';
            shareBtn.className = 'inventory-share-btn';
            shareBtn.disabled = Boolean(item.shared);
            shareBtn.textContent = item.shared
                ? 'Κοινοποιήθηκε'
                : 'Μεταφορά στο Κοινό Ημερολόγιο';

            if (!item.shared) {
                shareBtn.addEventListener('click', async () => {
                    shareBtn.disabled = true;
                    try {
                        const result = await apiRequest(
                            `/api/inventory/share/${encodeURIComponent(item.id)}`,
                            'POST'
                        );
                        if (result && Array.isArray(result.inventory)) {
                            renderItems(result.inventory);
                        }
                        if (result && Array.isArray(result.entries)) {
                            updateSharedJournalUI(result.entries);
                        }
                    } catch (err) {
                        console.error('Αποτυχία μεταφοράς στο Κοινό Ημερολόγιο:', err);
                        shareBtn.disabled = false;
                    }
                });
            }

            li.appendChild(shareBtn);
            listEl.appendChild(li);
        }
    }

    async function refresh() {
        try {
            const items = await apiRequest('/api/inventory', 'GET');
            renderItems(items);
            return items;
        } catch (err) {
            console.error('Αποτυχία φόρτωσης inventory:', err);
            renderItems([]);
            return [];
        }
    }

    async function addToInventory(item) {
        const items = await apiRequest('/api/inventory/add', 'POST', item);
        renderItems(items);
        return items;
    }

    async function addToJournal(item) {
        const result = await apiRequest('/api/journal/add', 'POST', item);
        if (result && Array.isArray(result.entries)) {
            updateSharedJournalUI(result.entries);
        }
        return result;
    }

    window.InventoryActions = {
        addToInventory,
        addToJournal,
        refresh
    };

    window.addEventListener('ws:message', (event) => {
        const msg = event.detail;
        if (!msg || typeof msg !== 'object') return;
        if (msg.type === 'journal_state' && Array.isArray(msg.entries)) {
            updateSharedJournalUI(msg.entries);
        }
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            ensurePanelMounted();
            refresh();
        });
    } else {
        ensurePanelMounted();
        refresh();
    }
})();