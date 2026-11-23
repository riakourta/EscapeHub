const gameSocket = (() => {
    let ws = null;

    function logToJournal(message) {
        const list = document.getElementById('journal-list');

        if (!list) return;

        const li = document.createElement('li');
        li.textContent = message;
        list.appendChild(li);
        list.scrollTop = list.scrollHeight;
    }

    function updateTeamCollection() {
        const countEl = document.getElementById('collection-count');
        const listEl = document.getElementById('collection-list');

        if (!countEl || !listEl) return;

        const collected = roomConfig.obstacles.filter(
            exhibit => exhibit.collected
        );

        countEl.textContent =
            `${collected.length}/${roomConfig.obstacles.length}`;

        listEl.replaceChildren();

        if (collected.length === 0) {
            const li = document.createElement('li');
            li.className = 'empty-msg';
            li.textContent = 'Κανένα έκθεμα ακόμα...';
            listEl.appendChild(li);
            return;
        }

        for (const exhibit of collected) {
            const li = document.createElement('li');
            li.textContent = `💎 ${exhibit.label}`;
            listEl.appendChild(li);
        }
    }

    function emit(type, payload) {
        if (!ws || ws.readyState !== WebSocket.OPEN) {
            return false;
        }

        ws.send(JSON.stringify({ type, ...payload }));
        return true;
    }

    function sendMove(state) {
        return emit('move', {
            x: state.x,
            y: state.y,
            direction: state.direction
        });
    }

    function sendExhibitCollected(exhibit) {
        if (!exhibit) return false;

        return emit('collect', {
            exhibitId: exhibit.id
        });
    }

    function applyPuzzleState(puzzle) {
        if (!puzzle || !Array.isArray(puzzle.plates)) return;

        roomConfig.coOpPuzzle = {
            ...puzzle,
            plates: puzzle.plates.map(plate => ({ ...plate }))
        };
    }

    function applyExhibitState(exhibit) {
        if (!exhibit) return;

        const local = roomConfig.obstacles.find(
            item => item.id === exhibit.id
        );

        if (!local) return;

        Object.assign(local, exhibit);

        if (
            local.isTarget &&
            local.collected &&
            !roomConfig.alarmTriggered
        ) {
            if (
                typeof triggerAlarmSequence === 'function' &&
                typeof isPanicSequenceRunning !== 'undefined' &&
                !isPanicSequenceRunning
            ) {
                triggerAlarmSequence(local);
            }
        }
    }

    function applyJournalState(entries) {
        if (!Array.isArray(entries)) return;

        const list = document.getElementById('journal-list');

        if (!list) return;

        const items = entries.map(entry => {
            const li = document.createElement('li');
            li.textContent = entry.message;
            return li;
        });

        list.replaceChildren(...items);
        list.scrollTop = list.scrollHeight;
    }

    function refreshGame() {
        if (typeof renderGame === 'function') {
            renderGame();
        }
    }

    function onServerMessage(data) {
        let msg;

        try {
            msg = typeof data === 'string' ? JSON.parse(data) : data;
        } catch (error) {
            console.error('Μη έγκυρο μήνυμα από τον server:', error);
            return;
        }

        if (!msg || typeof msg !== 'object') return;

        if (msg.type === 'welcome') {
            if (!msg.player) return;

            localPlayerId = String(msg.player.id);

            Object.assign(playerState, {
                id: localPlayerId,
                nickname: msg.player.nickname,
                color: msg.player.color,
                x: msg.player.x,
                y: msg.player.y,
                direction: msg.player.direction
            });

            networkPlayers.delete(localPlayerId);
            refreshGame();
            return;
        }

        if (msg.type === 'snapshot') {
            networkPlayers.clear();

            for (const player of msg.players || []) {
                const id = String(player.id);

                if (id !== localPlayerId) {
                    networkPlayers.set(id, {
                        ...player,
                        id,
                        size: 30
                    });
                }
            }

            for (const exhibit of msg.exhibits || []) {
                applyExhibitState(exhibit);
            }

            applyPuzzleState(msg.puzzle);
            applyJournalState(msg.journal);
            updateTeamCollection();
            refreshGame();
            return;
        }

        if (msg.type === 'move') {
            const player = msg.player;

            if (!player) return;

            const id = String(player.id);

            if (id !== localPlayerId) {
                networkPlayers.set(id, {
                    ...player,
                    id,
                    size: 30
                });
            }

            refreshGame();
            return;
        }

        if (msg.type === 'puzzle_state') {
            applyPuzzleState(msg.puzzle);
            refreshGame();
            return;
        }

        if (msg.type === 'exhibit_collected') {
            applyExhibitState(msg.exhibit);
            updateTeamCollection();
            refreshGame();
            return;
        }

        if (msg.type === 'journal_state') {
            applyJournalState(msg.entries);
            return;
        }

        if (msg.type === 'error') {
            console.error(msg.code, msg.message);
        }
    }

    function connect(socketUrl) {
        if (!socketUrl) return;

        if (
            ws &&
            (
                ws.readyState === WebSocket.OPEN ||
                ws.readyState === WebSocket.CONNECTING
            )
        ) {
            return;
        }

        const storageKeys =
            typeof API_CONFIG !== 'undefined'
                ? API_CONFIG.STORAGE_KEYS
                : {};

        const token = localStorage.getItem(
            storageKeys?.TOKEN || 'jwtToken'
        );

        if (!token) {
            console.error('Δεν υπάρχει token. Κάνε ξανά login.');
            return;
        }

        const nickname =
            localStorage.getItem(storageKeys?.NICKNAME || 'nickname') ||
            localStorage.getItem('username') ||
            'Player';
        const profileColor = localStorage.getItem(
            storageKeys?.AVATAR_COLOR || 'avatarColor'
        );

        const colorMap = {
            '#38bdf8': 'blue',
            '#f472b6': 'pink',
            '#a78bfa': 'purple',
            '#34d399': 'green',
            '#ef4444': 'red',
            '#facc15': 'yellow'
        };

        const savedColor = (profileColor || '').trim().toLowerCase();

        const color = colorMap[savedColor] ||
            (
                ['blue', 'pink', 'purple', 'green', 'red', 'yellow']
                    .includes(savedColor)
                    ? savedColor
                    : 'blue'
            );
        ws = new WebSocket(socketUrl);

        ws.onopen = () => {
            ws.send(JSON.stringify({
                type: 'join',
                token,
                nickname,
                color
            }));
        };

        ws.onmessage = event => {
            onServerMessage(event.data);
        };

        ws.onerror = event => {
            console.error('Σφάλμα σύνδεσης παιχνιδιού:', event);
        };

        ws.onclose = event => {
            console.info(
                'Η σύνδεση παιχνιδιού έκλεισε:',
                event.code,
                event.reason
            );
        };
    }

    return {
        connect,
        sendMove,
        sendExhibitCollected,
        logToJournal,
        updateTeamCollection,
        onServerMessage
    };
})();

if (typeof window !== 'undefined') {
    const gameWsProtocol =
        window.location.protocol === 'https:' ? 'wss:' : 'ws:';

    gameSocket.connect(
        `${gameWsProtocol}//${window.location.host}/ws/game`
    );
}