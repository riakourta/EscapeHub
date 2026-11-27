(() => {
    const WS_ENDPOINT = '/ws/lobby';
    const EXIT_PAGE = 'profile.html';

    // Διαβάζει nickname και χρώμα από το profile.html
    const nickKey = (typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS?.NICKNAME) || 'nickname';
    const colorKey = (typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS?.AVATAR_COLOR) || 'avatarColor';

    const nickname = localStorage.getItem(nickKey) || localStorage.getItem('nickname') || localStorage.getItem('username') || 'Ληστής';
    const playerColor = localStorage.getItem(colorKey) || localStorage.getItem('avatarColor') || '#38bdf8';

    // 1. CSS για το Chat, το Exit και το Abort Modal
    const style = document.createElement('style');
    style.textContent = `
        #game-exit-btn {
            position: fixed;
            top: 14px;
            right: 16px;
            z-index: 900;
            background-color: #0d0f14;
            color: #f87171;
            border: 1px solid #7f1d1d;
            padding: 7px 16px;
            font-size: 0.82rem;
            font-weight: 700;
            letter-spacing: 0.6px;
            text-transform: uppercase;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        #game-exit-btn:hover {
            background-color: #991b1b;
            color: #ffffff;
        }

        #live-chat-widget {
            position: fixed;
            bottom: 16px;
            right: 16px;
            width: 300px;
            height: 280px;
            z-index: 900;
            background-color: #0d0f14;
            border: 1px solid #262934;
            border-radius: 6px;
            padding: 12px;
            display: flex;
            flex-direction: column;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.75);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        #live-chat-widget.is-collapsed {
            height: auto;
            padding: 10px 12px;
        }
        #live-chat-widget.is-collapsed .chat-title {
            border-bottom: none;
            padding-bottom: 0;
            margin-bottom: 0;
        }
        #live-chat-widget.is-collapsed #chat-messages,
        #live-chat-widget.is-collapsed .chat-form {
            display: none;
        }

        .chat-title {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            font-size: 0.85rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.7px;
            color: #eab308;
            border-bottom: 1px solid #262934;
            padding-bottom: 6px;
            margin-bottom: 8px;
        }
        .chat-title-left {
            display: flex;
            align-items: center;
            gap: 8px;
            min-width: 0;
        }
        .chat-unread-badge {
            display: none;
            align-items: center;
            justify-content: center;
            min-width: 20px;
            height: 20px;
            padding: 0 6px;
            border-radius: 999px;
            background-color: #dc2626;
            color: #ffffff;
            font-size: 0.72rem;
            font-weight: 700;
            line-height: 1;
            letter-spacing: 0;
            text-transform: none;
            box-sizing: border-box;
            font-variant-numeric: tabular-nums;
            box-shadow: 0 0 8px rgba(220, 38, 38, 0.55);
        }
        .chat-unread-badge.is-visible {
            display: inline-flex;
        }
        #chat-toggle-btn {
            background: transparent;
            border: 1px solid #374151;
            color: #e5e7eb;
            width: 24px;
            height: 24px;
            border-radius: 4px;
            font-size: 0.7rem;
            line-height: 1;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
        }
        #chat-toggle-btn:hover {
            background-color: #1f2430;
            border-color: #eab308;
            color: #eab308;
        }

        #chat-messages {
            flex: 1;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 6px;
            margin-bottom: 8px;
            padding-right: 4px;
            font-size: 0.85rem;
            color: #e5e7eb;
        }
        .chat-msg {
            background-color: #161922;
            padding: 6px 8px;
            border-radius: 4px;
            line-height: 1.35;
            word-wrap: break-word;
        }
        .chat-sender {
            font-weight: 700;
            margin-right: 5px;
        }
        .chat-form {
            display: flex;
            gap: 6px;
        }
        #chat-input {
            flex: 1;
            background-color: #161922;
            border: 1px solid #374151;
            border-radius: 4px;
            color: #f3f4f6;
            padding: 7px 9px;
            font-size: 0.84rem;
            outline: none;
        }
        #chat-input:focus { border-color: #eab308; }
        #chat-send-btn {
            background-color: #990000;
            color: #fff;
            border: none;
            border-radius: 4px;
            padding: 0 12px;
            font-size: 0.82rem;
            font-weight: 700;
            cursor: pointer;
        }
        #chat-send-btn:hover { background-color: #b91c1c; }

        #abort-modal {
            position: fixed;
            inset: 0;
            background-color: rgba(0, 0, 0, 0.94);
            display: none;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            z-index: 9999;
            text-align: center;
            padding: 2rem;
        }
        #abort-back-btn {
            padding: 12px 28px;
            font-size: 1rem;
            font-weight: bold;
            background-color: #b30000;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        #abort-back-btn:hover { background-color: #ff1a1a; }
    `;
    document.head.appendChild(style);

    // 2. Δημιουργία HTML στοιχείων
    const exitBtn = document.createElement('button');
    exitBtn.id = 'game-exit-btn';
    exitBtn.type = 'button';
    exitBtn.textContent = 'EXIT';

    const chatWidget = document.createElement('section');
    chatWidget.id = 'live-chat-widget';
    chatWidget.innerHTML = `
        <div class="chat-title">
            <div class="chat-title-left">
                <span>💬 Live Chat</span>
                <span id="chat-unread-badge" class="chat-unread-badge" aria-live="polite">0</span>
            </div>
            <button type="button" id="chat-toggle-btn" title="Κλείσιμο chat" aria-label="Εναλλαγή παραθύρου chat" aria-expanded="true">▼</button>
        </div>
        <div id="chat-messages"></div>
        <form class="chat-form" id="chat-form">
            <input type="text" id="chat-input" placeholder="Type a message..." autocomplete="off" maxlength="200" />
            <button type="submit" id="chat-send-btn">Send</button>
        </form>
    `;

    const abortModal = document.createElement('div');
    abortModal.id = 'abort-modal';
    abortModal.innerHTML = `
        <div class="alarm-icon">⚠️</div>
        <div class="modal-box">
            <h1>Η ΑΠΟΣΤΟΛΗ ΔΙΑΚΟΠΗΚΕ</h1>
            <p id="abort-reason">Έμειναν λιγότεροι από 3 παίκτες στο δωμάτιο. Η ληστεία δεν μπορεί να συνεχιστεί.</p>
            <button id="abort-back-btn" type="button">Επιστροφή στο Προφίλ</button>
        </div>
    `;

    document.body.append(exitBtn, chatWidget, abortModal);

    // 3. WebSocket για Chat & Exit
    const chatMessagesEl = document.getElementById('chat-messages');
    const chatForm = document.getElementById('chat-form');
    const chatInput = document.getElementById('chat-input');
    const chatToggleBtn = document.getElementById('chat-toggle-btn');
    const chatUnreadBadge = document.getElementById('chat-unread-badge');
    const abortReasonEl = document.getElementById('abort-reason');
    const abortBackBtn = document.getElementById('abort-back-btn');

    let isChatCollapsed = false;
    let unreadCount = 0;

    function updateUnreadBadge() {
        if (isChatCollapsed && unreadCount > 0) {
            chatUnreadBadge.textContent = String(unreadCount);
            chatUnreadBadge.classList.add('is-visible');
        } else {
            chatUnreadBadge.textContent = '0';
            chatUnreadBadge.classList.remove('is-visible');
        }
    }

    function setChatCollapsed(collapsed) {
        isChatCollapsed = collapsed;
        chatWidget.classList.toggle('is-collapsed', collapsed);
        chatToggleBtn.textContent = collapsed ? '▲' : '▼';
        chatToggleBtn.title = collapsed ? 'Άνοιγμα chat' : 'Κλείσιμο chat';
        chatToggleBtn.setAttribute('aria-expanded', String(!collapsed));

        if (!collapsed) {
            unreadCount = 0;
            chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight;
        }

        updateUnreadBadge();
    }

    chatToggleBtn.addEventListener('click', () => {
        setChatCollapsed(!isChatCollapsed);
        chatToggleBtn.blur();
    });

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(protocol + '//' + window.location.host + WS_ENDPOINT);
    const tokenKey = (typeof API_CONFIG !== 'undefined' && API_CONFIG.STORAGE_KEYS?.TOKEN) || 'jwtToken';
    const token = localStorage.getItem(tokenKey) || localStorage.getItem('jwtToken') || '';

    socket.addEventListener('open', () => {
        socket.send(JSON.stringify({
            type: 'JOIN_LOBBY',
            username: `${playerColor}|${nickname}`,
            token: token
        }));
    });
    socket.addEventListener('message', (event) => {
        const data = JSON.parse(event.data);

        if (data.type === 'CHAT_MESSAGE') {
            appendChatMessage(data.sender, data.color, data.message);
        } else if (data.type === 'GAME_ABORTED') {
            if (data.reason) abortReasonEl.textContent = data.reason;
            abortModal.style.display = 'flex';
        }
    });
    window.addEventListener('ws:message', (event) => {
        const data = event.detail;
        if (data && data.type === 'GAME_ABORTED') {
            if (data.reason) abortReasonEl.textContent = data.reason;
            abortModal.style.display = 'flex';
        }
    });

    chatForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const text = chatInput.value.trim();
        if (!text) return;

        if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: 'CHAT_MESSAGE',
                sender: nickname,
                color: playerColor,
                message: text
            }));
        } else {
            appendChatMessage(nickname, playerColor, text);
        }

        chatInput.value = '';
    });

    chatInput.addEventListener('keydown', (e) => {
        e.stopPropagation();
    });

    exitBtn.addEventListener('click', () => {
        if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: 'LEAVE_GAME',
                username: `${playerColor}|${nickname}`
            }));
            socket.close();
        }
        window.location.href = EXIT_PAGE;
    });

    abortBackBtn.addEventListener('click', () => {
        window.location.href = EXIT_PAGE;
    });

    function appendChatMessage(sender, color, message) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'chat-msg';

        const senderSpan = document.createElement('span');
        senderSpan.className = 'chat-sender';
        senderSpan.style.color = (color && color.trim()) || '#38bdf8';
        senderSpan.textContent = `${sender}:`;

        const textNode = document.createTextNode(` ${message}`);

        msgDiv.append(senderSpan, textNode);
        chatMessagesEl.appendChild(msgDiv);
        if (isChatCollapsed) {
            unreadCount += 1;
            updateUnreadBadge();
        } else {
            chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight;
        }
    }
})();