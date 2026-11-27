(function () {
    'use strict';

    const SLOT_ID = 'search-bar-slot';
    const API_ENDPOINT = '/api/search';
    const MIN_QUERY_LENGTH = 2;

    function getAuthToken() {
        const keys = ['token', 'jwt', 'jwtToken', 'authToken', 'accessToken'];
        for (const key of keys) {
            const value = localStorage.getItem(key);
            if (value && value.trim()) {
                return value.trim();
            }
        }
        return null;
    }

    function initExhibitSearchBar() {
        const slot = document.getElementById(SLOT_ID);
        if (!slot) {
            return;
        }

        // Καθαρίζουμε τυχόν προηγούμενο περιεχόμενο
        slot.innerHTML = '';

        // 1. Δημιουργία δομής DOM (ταιριάζει ακριβώς με το css/searchBar.css)
        const root = document.createElement('div');
        root.className = 'exhibit-search-root';

        const form = document.createElement('form');
        form.className = 'exhibit-search-form';
        form.setAttribute('role', 'search');
        form.setAttribute('autocomplete', 'off');

        const icon = document.createElement('span');
        icon.className = 'exhibit-search-icon';
        icon.setAttribute('aria-hidden', 'true');
        icon.textContent = '🔍';

        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'exhibit-search-input';
        input.id = 'exhibit-search-input';
        input.placeholder = 'Αναζήτηση στο Ψηφιακό Αρχείο (π.χ. Στέμμα Λουδοβίκου, Regent)...';
        input.setAttribute('aria-label', 'Αναζήτηση πληροφοριών εκθεμάτων');

        const clearBtn = document.createElement('button');
        clearBtn.type = 'button';
        clearBtn.className = 'exhibit-search-clear-btn';
        clearBtn.title = 'Καθαρισμός';
        clearBtn.setAttribute('aria-label', 'Καθαρισμός αναζήτησης');
        clearBtn.textContent = '×';

        const submitBtn = document.createElement('button');
        submitBtn.type = 'submit';
        submitBtn.className = 'exhibit-search-submit-btn';
        submitBtn.textContent = 'Αναζήτηση';

        form.append(icon, input, clearBtn, submitBtn);

        // Dropdown panel αποτελεσμάτων
        const dropdown = document.createElement('div');
        dropdown.className = 'exhibit-search-dropdown';
        dropdown.setAttribute('role', 'region');
        dropdown.setAttribute('aria-live', 'polite');

        const dropdownHeader = document.createElement('div');
        dropdownHeader.className = 'exhibit-search-dropdown-header';

        const dropdownTitle = document.createElement('span');
        dropdownTitle.className = 'exhibit-search-dropdown-title';
        dropdownTitle.textContent = 'Αποτελέσματα Ψηφιακού Αρχείου';

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'exhibit-search-close-btn';
        closeBtn.title = 'Κλείσιμο (Esc)';
        closeBtn.setAttribute('aria-label', 'Κλείσιμο αποτελεσμάτων');
        closeBtn.textContent = '✕';

        dropdownHeader.append(dropdownTitle, closeBtn);

        const body = document.createElement('div');
        body.className = 'exhibit-search-body';

        dropdown.append(dropdownHeader, body);
        root.append(form, dropdown);
        slot.appendChild(root);

        // 2. Διαχείριση κατάστασης
        let activeRequestId = 0;

        function openDropdown() {
            dropdown.classList.add('is-open');
        }

        function closeDropdown() {
            dropdown.classList.remove('is-open');
        }

        function updateClearVisibility() {
            if (input.value.length > 0) {
                clearBtn.classList.add('is-visible');
            } else {
                clearBtn.classList.remove('is-visible');
            }
        }

        function renderStatus(message, options = {}) {
            body.innerHTML = '';
            const status = document.createElement('div');
            status.className = 'exhibit-search-status';
            if (options.isError) {
                status.classList.add('exhibit-search-status--error');
            }

            if (options.loading) {
                const spinner = document.createElement('span');
                spinner.className = 'exhibit-search-spinner';
                status.appendChild(spinner);
            }

            const text = document.createElement('span');
            text.textContent = message;
            status.appendChild(text);

            body.appendChild(status);
        }

        function renderResults(results, query) {
            body.innerHTML = '';

            if (!Array.isArray(results) || results.length === 0) {
                dropdownTitle.textContent = 'Αποτελέσματα Ψηφιακού Αρχείου';
                renderStatus(`Δεν βρέθηκαν αποτελέσματα για «${query}».`);
                return;
            }

            dropdownTitle.textContent = `Αποτελέσματα για «${query}» (${results.length})`;

            const list = document.createElement('ul');
            list.className = 'exhibit-search-list';

            results.forEach((item) => {
                const card = document.createElement('li');
                card.className = 'exhibit-search-card';

                if (item.imageUrl) {
                    const img = document.createElement('img');
                    img.className = 'exhibit-search-thumb';
                    img.src = item.imageUrl;
                    img.alt = item.title || 'Εικόνα εκθέματος';
                    img.loading = 'lazy';
                    img.onerror = () => img.remove();
                    card.appendChild(img);
                }

                const content = document.createElement('div');
                content.className = 'exhibit-search-card-content';

                const title = document.createElement('h4');
                title.className = 'exhibit-search-card-title';
                title.textContent = item.title || 'Άγνωστο Έκθεμα';
                content.appendChild(title);

                if (item.summary) {
                    const summary = document.createElement('p');
                    summary.className = 'exhibit-search-card-summary';
                    summary.textContent = item.summary;
                    content.appendChild(summary);
                }

                const meta = document.createElement('div');
                meta.className = 'exhibit-search-card-meta';

                if (item.date) {
                    const dateBadge = document.createElement('span');
                    dateBadge.className = 'exhibit-search-badge exhibit-search-badge--highlight';
                    dateBadge.textContent = `Χρονολογία: ${item.date}`;
                    meta.appendChild(dateBadge);
                }

                if (item.artist) {
                    const artistBadge = document.createElement('span');
                    artistBadge.className = 'exhibit-search-badge';
                    artistBadge.textContent = `Δημιουργός: ${item.artist}`;
                    meta.appendChild(artistBadge);
                }

                if (item.source) {
                    const sourceBadge = document.createElement('span');
                    sourceBadge.className = 'exhibit-search-badge';
                    sourceBadge.textContent = item.source;
                    meta.appendChild(sourceBadge);
                }

                if (meta.childElementCount > 0) {
                    content.appendChild(meta);
                }

                card.appendChild(content);
                list.appendChild(card);
            });

            body.appendChild(list);
        }

        async function executeSearch(rawQuery) {
            const query = rawQuery.trim();
            if (query.length < MIN_QUERY_LENGTH) {
                dropdownTitle.textContent = 'Ψηφιακό Αρχείο';
                renderStatus(`Πληκτρολογήστε τουλάχιστον ${MIN_QUERY_LENGTH} χαρακτήρες για αναζήτηση.`);
                openDropdown();
                return;
            }

            const currentRequestId = ++activeRequestId;
            submitBtn.disabled = true;
            dropdownTitle.textContent = `Αναζήτηση: «${query}»`;
            renderStatus('Αναζήτηση στο Ψηφιακό Αρχείο...', { loading: true });
            openDropdown();

            try {
                const headers = { Accept: 'application/json' };
                const token = getAuthToken();
                if (token) {
                    headers.Authorization = `Bearer ${token}`;
                }

                const response = await fetch(`${API_ENDPOINT}?q=${encodeURIComponent(query)}`, {
                    method: 'GET',
                    headers
                });

                if (currentRequestId !== activeRequestId) {
                    return;
                }

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }

                const data = await response.json();
                renderResults(data, query);
            } catch (error) {
                if (currentRequestId !== activeRequestId) {
                    return;
                }
                renderStatus('Παρουσιάστηκε πρόβλημα κατά την αναζήτηση. Δοκιμάστε ξανά.', {
                    isError: true
                });
            } finally {
                if (currentRequestId === activeRequestId) {
                    submitBtn.disabled = false;
                }
            }
        }

        // 3. Events φόρμας & κουμπιών
        form.addEventListener('submit', (event) => {
            event.preventDefault();
            executeSearch(input.value);
        });

        input.addEventListener('input', updateClearVisibility);

        clearBtn.addEventListener('click', () => {
            input.value = '';
            updateClearVisibility();
            closeDropdown();
            input.focus();
        });

        closeBtn.addEventListener('click', () => {
            closeDropdown();
            input.blur();
        });

        // 4. Αποτροπή σύγκρουσης πλήκτρων με κίνηση (WASD / βελάκια) & αλληλεπιδράσεις (E / I)
        const blockGameHotkeys = (event) => {
            if (event.key === 'Escape') {
                closeDropdown();
                input.blur();
            }
            event.stopPropagation();
        };

        input.addEventListener('keydown', blockGameHotkeys);
        input.addEventListener('keyup', blockGameHotkeys);
        input.addEventListener('keypress', blockGameHotkeys);

        // 5. Κλείσιμο dropdown με Escape ή κλικ έξω από τη μπάρα
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && dropdown.classList.contains('is-open')) {
                closeDropdown();
            }
        });

        document.addEventListener('click', (event) => {
            if (dropdown.classList.contains('is-open') && !root.contains(event.target)) {
                closeDropdown();
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initExhibitSearchBar);
    } else {
        initExhibitSearchBar();
    }
})();