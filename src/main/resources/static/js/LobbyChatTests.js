// LobbyChatTests.js
// Τρέχει είτε με Node.js (node src/main/resources/static/js/LobbyChatTests.js)
// είτε με copy-paste στο Console του Chrome (F12 -> Console).

function assertEqual(actual, expected) {
    if (actual !== expected) {
        throw new Error(`Αναμενόταν "${expected}", αλλά πήρε "${actual}"`);
    }
}

function assertDeepEqual(actual, expected) {
    const a = JSON.stringify(actual);
    const b = JSON.stringify(expected);
    if (a !== b) {
        throw new Error(`Αναμενόταν ${b}, αλλά πήρε ${a}`);
    }
}

function parsePlayerEntry(rawEntry) {
    const [rawColor, ...nameParts] = rawEntry.includes('|')
        ? rawEntry.split('|')
        : ['#38bdf8', rawEntry];
    return {
        color: rawColor.trim() || '#38bdf8',
        nickname: nameParts.join('|')
    };
}

function formatLobbyError(template, count, required, max) {
    return template
        .replaceAll('{count}', count)
        .replaceAll('{required}', required)
        .replaceAll('{max}', max);
}

function buildChatPayload(text, sender, color) {
    const cleanText = (text || '').trim();
    if (!cleanText) return null;
    return {
        type: 'CHAT_MESSAGE',
        sender: sender,
        color: (color && color.trim()) || '#38bdf8',
        message: cleanText
    };
}

function buildLeaveLobbyPayload(color, nickname) {
    return {
        type: 'LEAVE_LOBBY',
        username: `${color}|${nickname}`
    };
}

function buildLeaveGamePayload(nickname) {
    return {
        type: 'LEAVE_GAME',
        sender: nickname
    };
}

function getAbortReason(data) {
    const defaultMsg = 'Έμειναν λιγότεροι από 3 παίκτες στο δωμάτιο. Η ληστεία δεν μπορεί να συνεχιστεί.';
    return (data && data.reason && data.reason.trim()) ? data.reason : defaultMsg;
}

let passed = 0;

function runTest(name, fn) {
    try {
        fn();
        console.log(`✅ PASS: ${name}`);
        passed++;
    } catch (err) {
        console.error(`❌ FAIL: ${name} -> ${err.message}`);
        if (typeof process !== 'undefined') process.exitCode = 1;
    }
}

runTest('Lobby: Σωστός διαχωρισμός color|nickname', () => {
    const res = parsePlayerEntry('#ef4444|Eleftheria');
    assertEqual(res.color, '#ef4444');
    assertEqual(res.nickname, 'Eleftheria');
});

runTest('Lobby: Fallback σε default χρώμα αν λείπει το |', () => {
    const res = parsePlayerEntry('Christos');
    assertEqual(res.color, '#38bdf8');
    assertEqual(res.nickname, 'Christos');
});

runTest('Lobby: Μορφοποίηση μηνύματος σφάλματος (count/required/max)', () => {
    const template = 'Απαιτούνται τουλάχιστον {required} ληστές! (Συνδεδεμένοι: {count}/{max})';
    const msg = formatLobbyError(template, 2, 3, 6);
    assertEqual(msg, 'Απαιτούνται τουλάχιστον 3 ληστές! (Συνδεδεμένοι: 2/6)');
});

runTest('Lobby Exit: Δημιουργία σωστού LEAVE_LOBBY payload', () => {
    const payload = buildLeaveLobbyPayload('#2a9d8f', 'Eleftheria');
    assertDeepEqual(payload, { type: 'LEAVE_LOBBY', username: '#2a9d8f|Eleftheria' });
});

runTest('Live Chat: Απόρριψη κενού ή whitespace-only μηνύματος', () => {
    assertEqual(buildChatPayload('', 'Eleftheria', '#ef4444'), null);
    assertEqual(buildChatPayload('    ', 'Eleftheria', '#ef4444'), null);
});

runTest('Live Chat: Σωστό CHAT_MESSAGE payload με trim και χρώμα προφίλ', () => {
    const payload = buildChatPayload('  Βρήκα το κλειδί!  ', 'Eleftheria', '#ef4444');
    assertDeepEqual(payload, {
        type: 'CHAT_MESSAGE',
        sender: 'Eleftheria',
        color: '#ef4444',
        message: 'Βρήκα το κλειδί!'
    });
});

runTest('Game Exit: Δημιουργία σωστού LEAVE_GAME payload', () => {
    const payload = buildLeaveGamePayload('Eleftheria');
    assertDeepEqual(payload, { type: 'LEAVE_GAME', sender: 'Eleftheria' });
});

runTest('Game Abort: Εμφάνιση reason από server ή default μηνύματος', () => {
    assertEqual(
        getAbortReason({ type: 'GAME_ABORTED', reason: 'Έμειναν 2 παίκτες.' }),
        'Έμειναν 2 παίκτες.'
    );
    assertEqual(
        getAbortReason({ type: 'GAME_ABORTED' }),
        'Έμειναν λιγότεροι από 3 παίκτες στο δωμάτιο. Η ληστεία δεν μπορεί να συνεχιστεί.'
    );
});

console.log(`Ολοκληρώθηκαν ${passed}/8 tests.`);