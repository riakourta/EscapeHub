const gameSocket = (() => {
    let ws = null;

    // 1. Προσθήκη καταχώρησης στο Κοινό Ημερολόγιο (Sidebar)
    function logToJournal(message) {
        const list = document.getElementById('journal-list');
        if (!list) return;
        const li = document.createElement('li');
        li.textContent = message;
        list.appendChild(li);
        list.scrollTop = list.scrollHeight;
    }

    // 2. Αποστολή γεγονότων προς το Backend (θα τα διαβάζει ο Άγγελος)
    function emit(type, payload) {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type, ...payload }));
        }
    }

    function sendMove(state) {
        emit('PLAYER_MOVE', { x: state.x, y: state.y, direction: state.direction });
    }

    function sendExhibitCollected(exhibit) {
        emit('EXHIBIT_COLLECTED', { exhibitId: exhibit.id, label: exhibit.label });
        logToJournal(`💎 Συλλέχθηκε: ${exhibit.label}`);
        updateTeamCollection(); // <--- Αυτό αυξάνει το 0/3 σε 1/3, 2/3, 3/3
    }

    function sendPuzzleSolved(puzzle) {
        logToJournal(`🧩 Λύθηκε: ${puzzle.label} — Η έξοδος άνοιξε!`);
        emit('PUZZLE_SOLVED', { puzzleId: puzzle.id });
    }

    // 3. Υποδοχή μηνυμάτων από το Backend του Άγγελου
    function onServerMessage(data) {
        const msg = typeof data === 'string' ? JSON.parse(data) : data;

        if (msg.type === 'TEAMMATE_MOVE' && typeof teammateState !== 'undefined') {
            teammateState.x = msg.x;
            teammateState.y = msg.y;
            teammateState.direction = msg.direction || 'up';
            teammateState.visible = true;
            if (typeof checkCoOpPuzzle === 'function' && checkCoOpPuzzle()) {
                sendPuzzleSolved(roomConfig.coOpPuzzle);
            }
            if (typeof renderGame === 'function') renderGame();
        }

        if (msg.type === 'EXHIBIT_COLLECTED') {
            const obs = roomConfig.obstacles.find(o => o.id === msg.exhibitId);
            if (obs && !obs.collected) {
                obs.collected = true;
                logToJournal(`🏛️ Ο συμπαίκτης πήρε: ${obs.label}`);
                if (typeof renderGame === 'function') renderGame();
            }
        }

        if (msg.type === 'PUZZLE_SOLVED') {
            roomConfig.coOpPuzzle.solved = true;
            logToJournal(`🧩 Λύθηκε: ${roomConfig.coOpPuzzle.label}`);
            if (typeof renderGame === 'function') renderGame();
        }
    }

    function connect(socketUrl) {
        if (!socketUrl) return;
        ws = new WebSocket(socketUrl);

        ws.onopen = () => {
            const token = localStorage.getItem('token') || '';
            const nickname = localStorage.getItem('username') || 'Player';

            ws.send(JSON.stringify({
                type: 'join',
                token: token,
                nickname: nickname,
                color: 'blue'
            }));
        };

        ws.onmessage = (e) => onServerMessage(e.data);
    }

    return { connect, sendMove, sendExhibitCollected, sendPuzzleSolved, logToJournal, onServerMessage };
})();