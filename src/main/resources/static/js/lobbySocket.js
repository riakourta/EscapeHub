(() => {
    const container = document.getElementById('lobbyContainer');
    const currentCountEl = document.getElementById('currentCount');
    const maxCountEl = document.getElementById('maxCount');
    const playerListEl = document.getElementById('playerList');
    const startBtn = document.getElementById('startBtn');
    const exitBtn = document.getElementById('exitBtn');
    const statusMsgEl = document.getElementById('statusMsg');

    const wsEndpoint = container.dataset.wsEndpoint;
    const nextPage = container.dataset.nextPage;
    const exitPage = container.dataset.exitPage || 'profile.html';
    const defaultMaxPlayers = Number(container.dataset.maxPlayers) || 6;
    const errorTemplate = container.dataset.errorTemplate;
    const startingText = container.dataset.startingText;

    const MessageType = Object.freeze({
        JOIN_LOBBY: 'JOIN_LOBBY',
        LEAVE_LOBBY: 'LEAVE_LOBBY',
        START_HEIST: 'START_HEIST',
        LOBBY_UPDATE: 'LOBBY_UPDATE',
        GAME_STARTING: 'GAME_STARTING',
        ERROR: 'ERROR'
    });

    const AVATAR_SVG = '<svg viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>';

    const nickKey = (typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS?.NICKNAME) || 'nickname';
    const colorKey = (typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS?.AVATAR_COLOR) || 'avatarColor';
    const tokenKey = (typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS?.TOKEN) || 'jwtToken';

    const urlParams = new URLSearchParams(window.location.search);
    const nickname =
        urlParams.get('user') ||
        localStorage.getItem(nickKey) ||
        localStorage.getItem('nickname') ||
        localStorage.getItem('username') ||
        `Παίκτης_${crypto.randomUUID().slice(0, 3)}`;

    const playerColor = window.PlayerColors.toHex(
        localStorage.getItem(colorKey) ||
        localStorage.getItem('avatarColor') ||
        'blue'
    );

    const token = localStorage.getItem(tokenKey) || localStorage.getItem('jwtToken') || '';

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocol}//${window.location.host}${wsEndpoint}`);

    socket.addEventListener('open', () => {
        socket.send(JSON.stringify({
            type: MessageType.JOIN_LOBBY,
            username: `${playerColor}|${nickname}`,
            token: token
        }));
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
                showError(data.count, data.required || 3, data.maxPlayers || defaultMaxPlayers);
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

    exitBtn.addEventListener('click', () => {
        if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: MessageType.LEAVE_LOBBY,
                username: `${playerColor}|${nickname}`
            }));
            socket.close();
        }
        window.location.href = exitPage;
    });

    function renderLobbyState(data) {
        if (typeof data.count === 'number') {
            currentCountEl.textContent = data.count;
        }
        const maxPlayers = typeof data.maxPlayers === 'number' ? data.maxPlayers : defaultMaxPlayers;
        maxCountEl.textContent = maxPlayers;

        if (Array.isArray(data.players)) {
            playerListEl.replaceChildren(
                ...data.players.map(rawEntry => {

                    const [rawColor, ...nameParts] = rawEntry.includes('|')
                        ? rawEntry.split('|')
                        : ['blue', rawEntry];

                    const cleanName = nameParts.join('|');
                    const colorName = window.PlayerColors.normalize(rawColor);

                    const li = document.createElement('li');

                    const badge = document.createElement('span');
                    badge.className = `player-badge theme-${colorName}`;
                    badge.innerHTML = AVATAR_SVG;

                    const nameSpan = document.createElement('span');
                    nameSpan.textContent = cleanName;

                    li.append(badge, nameSpan);
                    return li;
                })
            );
        }
    }

    function showError(count, required, max) {
        statusMsgEl.className = 'status-msg status-error';
        statusMsgEl.textContent = errorTemplate
            .replaceAll('{count}', count)
            .replaceAll('{required}', required)
            .replaceAll('{max}', max);
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