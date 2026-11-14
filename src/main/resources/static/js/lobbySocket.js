(() => {
    const container = document.getElementById('lobbyContainer');
    const currentCountEl = document.getElementById('currentCount');
    const requiredCountEl = document.getElementById('requiredCount');
    const playerListEl = document.getElementById('playerList');
    const startBtn = document.getElementById('startBtn');
    const statusMsgEl = document.getElementById('statusMsg');
    const avatarButtons = document.querySelectorAll('.avatar-option');

    const wsEndpoint = container.dataset.wsEndpoint;
    const nextPage = container.dataset.nextPage;
    const emptySlotText = container.dataset.emptySlotText;
    const errorTemplate = container.dataset.errorTemplate;
    const startingText = container.dataset.startingText;

    const MessageType = Object.freeze({
        JOIN_LOBBY: 'JOIN_LOBBY',
        START_HEIST: 'START_HEIST',
        LOBBY_UPDATE: 'LOBBY_UPDATE',
        GAME_STARTING: 'GAME_STARTING',
        ERROR: 'ERROR'
    });

    // Καταγραφή των 4 χρωμάτων από το HTML
    const avatarCatalog = {};
    avatarButtons.forEach(btn => {
        const key = btn.dataset.avatar;
        const circle = btn.querySelector('.avatar-icon-circle');
        avatarCatalog[key] = {
            buttonEl: btn,
            nameLabelEl: btn.querySelector('.avatar-owner-name'),
            themeClass: Array.from(circle.classList).find(c => c.startsWith('theme-')) || 'theme-blue',
            svgHtml: circle.innerHTML
        };
    });

    const urlParams = new URLSearchParams(window.location.search);
    const baseUsername = urlParams.get('user') || localStorage.getItem('username') || `Παίκτης_${crypto.randomUUID().slice(0, 3)}`;
    const token = localStorage.getItem('jwtToken') || '';
    let selectedAvatar = localStorage.getItem('playerColor') || 'blue';

    if (!avatarCatalog[selectedAvatar]) {
        selectedAvatar = 'blue';
    }

    // Ενημέρωση όταν ο παίκτης κάνει κλικ σε ένα από τα 4 ανθρωπάκια
    avatarButtons.forEach(btn => {
        btn.classList.toggle('selected', btn.dataset.avatar === selectedAvatar);
        btn.addEventListener('click', () => {
            selectedAvatar = btn.dataset.avatar;
            localStorage.setItem('playerColor', selectedAvatar);
            avatarButtons.forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');
            sendJoinOrUpdate();
        });
    });

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocol}//${window.location.host}${wsEndpoint}`);

    function sendJoinOrUpdate() {
        if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: MessageType.JOIN_LOBBY,
                username: `${selectedAvatar}|${baseUsername}`,
                token: token
            }));
        }
    }

    socket.addEventListener('open', () => {
        sendJoinOrUpdate();
    });

    socket.addEventListener('message', (event) => {
        const data = JSON.parse(event.data);

        switch (data.type) {
            case MessageType.LOBBY_UPDATE:
                renderLobbyState(data);
                clearStatus();
                break;

            case MessageType.ERROR:
                renderLobbyState(data);
                showError(data.count, data.required);
                break;

            case MessageType.GAME_STARTING:
                showStarting();
                window.location.href = nextPage;
                break;
        }
    });

    startBtn.addEventListener('click', () => {
        if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: MessageType.START_HEIST
            }));
        }
    });

    function renderLobbyState(data) {
        if (typeof data.count === 'number') {
            currentCountEl.textContent = data.count;
        }
        if (typeof data.required === 'number') {
            requiredCountEl.textContent = data.required;
        }

        if (Array.isArray(data.players)) {
            // 1. Μηδενίζουμε αρχικά τα ονόματα κάτω από τα 4 ανθρωπάκια σε "Διαθέσιμο"
            Object.values(avatarCatalog).forEach(item => {
                item.nameLabelEl.textContent = emptySlotText;
                item.buttonEl.classList.remove('taken');
            });

            // 2. Ενημερώνουμε και τα 4 ανθρωπάκια πάνω ΚΑΙ τη λίστα από κάτω
            playerListEl.replaceChildren(
                ...data.players.map(rawEntry => {
                    const [avatarKey, ...nameParts] = rawEntry.includes('|')
                        ? rawEntry.split('|')
                        : ['blue', rawEntry];
                    const cleanName = nameParts.join('|');
                    const avatarInfo = avatarCatalog[avatarKey] || avatarCatalog['blue'];

                    // Γράφουμε το όνομα του παίκτη ακριβώς κάτω από το ανθρωπάκι που διάλεξε!
                    avatarInfo.nameLabelEl.textContent = cleanName;
                    avatarInfo.buttonEl.classList.add('taken');

                    // Φτιάχνουμε τη σειρά στη λίστα με το χρωματιστό ανθρωπάκι και το όνομα
                    const li = document.createElement('li');

                    const badge = document.createElement('span');
                    badge.className = `player-badge ${avatarInfo.themeClass}`;
                    badge.innerHTML = avatarInfo.svgHtml;

                    const nameSpan = document.createElement('span');
                    nameSpan.textContent = cleanName;

                    li.append(badge, nameSpan);
                    return li;
                })
            );
        }
    }

    function showError(count, required) {
        statusMsgEl.className = 'status-msg status-error';
        statusMsgEl.textContent = errorTemplate
            .replaceAll('{count}', count)
            .replaceAll('{required}', required);
    }

    function showStarting() {
        statusMsgEl.className = 'status-msg status-success';
        statusMsgEl.textContent = startingText;
    }

    function clearStatus() {
        statusMsgEl.className = 'status-msg';
        statusMsgEl.textContent = '';
    }
})();