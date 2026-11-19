const roomConfig = {
    width: 800,
    height: 600,
    wallThickness: 20,
    alarmTriggered: typeof window === 'undefined',
    obstacles: [
        // Τα 2 πλαϊνά εκθέματα είναι κρυφά πριν χτυπήσει ο συναγερμός (hiddenBeforeAlarm: true)
        { id: 'exhibit-1', x: 180, y: 150, width: 70, height: 70, label: 'Διαμάντι Regent', hiddenBeforeAlarm: true },
        { id: 'exhibit-2', x: 520, y: 150, width: 70, height: 70, label: 'Στέμμα Λουδοβίκου', hiddenBeforeAlarm: true },
        // Το μπροστινό έκθεμα-στόχος που ενεργοποιεί τον συναγερμό μόλις το ακουμπήσεις (isTarget: true)
        { id: 'exhibit-3', x: 350, y: 360, width: 90, height: 70, label: 'Ξίφος Καρλομάγνου', isTarget: true }
    ],
    doors: [
        { id: 'main-door', x: 340, y: 580, width: 120, height: 20, locked: true, label: 'Κεντρική Πύλη Πυραμίδας (Κλειδωμένη)' },
        { id: 'room-b-door', x: 780, y: 240, width: 20, height: 120, locked: false, label: 'Προς Πτέρυγα Sully' }
        ],
    coOpPuzzle: [

        { id: 'twin-security-plates',
        label: 'Διπλός Μηχανισμός Ασφαλείας (2 Παίκτες)',
        plates: [
        { id: 'plate-1', x: 250, y: 455, width: 38, height: 38, active: false },
        { id: 'plate-2', x: 510, y: 455, width: 38, height: 38, active: false }
    ],
        solved: false
},
    ]
};

const playerState = {
    x: 380,
    y: 500,
    size: 30,
    speed: 10,
    direction: 'up'
};
const teammateState = {
    x: 425,
    y: 500,
    size: 30,
    direction: 'up',
    visible: true // Θα συγχρονίζεται live από το gameSocket.js του Άγγελου
};

function getNearbyExhibit(state = playerState, config = roomConfig, reach = 25) {
    const reachBox = {
        x: state.x - reach,
        y: state.y - reach,
        width: state.size + reach * 2,
        height: state.size + reach * 2
    };
    return config.obstacles.find(obs => {
        if (obs.collected) return false;
        if (!config.alarmTriggered && obs.hiddenBeforeAlarm) return false;
        return isRectColliding(reachBox, obs);
    }) || null;
}

function collectNearbyExhibit(state = playerState, config = roomConfig) {
    const exhibit = getNearbyExhibit(state, config);
    if (!exhibit) return null;
    exhibit.collected = true;
    return exhibit;
}

function checkCoOpPuzzle(p1 = playerState, p2 = teammateState, config = roomConfig) {
    const puzzle = config.coOpPuzzle;
    if (!puzzle || puzzle.solved) return false;

    const box1 = { x: p1.x, y: p1.y, width: p1.size, height: p1.size };
    const box2 = { x: p2.x, y: p2.y, width: p2.size, height: p2.size };

    puzzle.plates.forEach(plate => {
        plate.active = isRectColliding(box1, plate) || (p2.visible && isRectColliding(box2, plate));
    });

    if (puzzle.plates.every(plate => plate.active)) {
        puzzle.solved = true;
        return true;
    }
    return false;
}

/**
 * Βήμα 5: Έλεγχος αν δύο ορθογώνια επικαλύπτονται (AABB Collision)
 */
function isRectColliding(rect1, rect2) {
    return (
        rect1.x < rect2.x + rect2.width &&
        rect1.x + rect1.width > rect2.x &&
        rect1.y < rect2.y + rect2.height &&
        rect1.y + rect1.height > rect2.y
    );
}

/**
 * Ελέγχει αν η νέα θέση (newX, newY) χτυπάει σε τοίχο ή ορατό έκθεμα
 */
function checkCollision(newX, newY, playerSize = playerState.size, config = roomConfig) {
    const wt = config.wallThickness;

    // 1. Έλεγχος ορίων δωματίου (Τοίχοι)
    if (
        newX < wt ||
        newY < wt ||
        newX + playerSize > config.width - wt ||
        newY + playerSize > config.height - wt
    ) {
        return true; // Σύγκρουση με τοίχο
    }

    // 2. Έλεγχος σύγκρουσης με τα εκθέματα (obstacles)
    const playerBox = { x: newX, y: newY, width: playerSize, height: playerSize };
    for (const obs of config.obstacles) {
        // Αγνοούμε τα πλαϊνά εκθέματα μόνο αν είμαστε στην 1η σκηνή (alarmTriggered === false)
        if (config.alarmTriggered === false && obs.hiddenBeforeAlarm) {
            continue;
        }
        if (isRectColliding(playerBox, obs)) {
            return true; // Σύγκρουση με έκθεμα
        }
    }

    return false; // Ελεύθερη κίνηση
}

/**
 * Βήμα 4: Υπολογισμός κίνησης με τα βελάκια και ενημέρωση κατεύθυνσης φακού.
 * Επιστρέφει true αν ο παίκτης μετακινήθηκε, ή false αν ακυρώθηκε λόγω εμποδίου.
 */
function movePlayer(key, state = playerState, config = roomConfig) {
    let nextX = state.x;
    let nextY = state.y;

    switch (key) {
        case 'ArrowUp':
            nextY -= state.speed;
            state.direction = 'up';
            break;
        case 'ArrowDown':
            nextY += state.speed;
            state.direction = 'down';
            break;
        case 'ArrowLeft':
            nextX -= state.speed;
            state.direction = 'left';
            break;
        case 'ArrowRight':
            nextX += state.speed;
            state.direction = 'right';
            break;
        default:
            return false;
    }

    // Αν υπάρχει σύγκρουση, ακυρώνουμε την κίνηση
    if (checkCollision(nextX, nextY, state.size, config)) {
        return false;
    }

    // Ενημέρωση τοπικών συντεταγμένων
    state.x = nextX;
    state.y = nextY;
    return true;
}

/**
 * Προσθήκη Event Listener στον Browser
 */
function initPlayerControls(onMoveCallback) {
    if (typeof window !== 'undefined') {
        window.addEventListener('keydown', (event) => {
            window.addEventListener('keydown', (event) => {
                if (event.key === 'e' || event.key === 'E') {
                    const taken = collectNearbyExhibit();
                    if (taken && typeof onMoveCallback === 'function') {
                        onMoveCallback(playerState, true, 'Collect', taken);
                    }
                }
            });

            const canvasEl = document.getElementById('museumCanvas');
            if (canvasEl) {
                canvasEl.addEventListener('click', (event) => {
                    const rect = canvasEl.getBoundingClientRect();
                    const mx = event.clientX - rect.left;
                    const my = event.clientY - rect.top;
                    const nearby = getNearbyExhibit();
                    if (nearby && mx >= nearby.x - 15 && mx <= nearby.x + nearby.width + 15 &&
                        my >= nearby.y - 15 && my <= nearby.y + nearby.height + 15) {
                        const taken = collectNearbyExhibit();
                        if (taken && typeof onMoveCallback === 'function') {
                            onMoveCallback(playerState, true, 'ClickCollect', taken);
                        }
                    }
                });
            }
            if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
                event.preventDefault();
                const moved = movePlayer(event.key);
                if (typeof onMoveCallback === 'function') {
                    onMoveCallback(playerState, moved, event.key);
                }
            }
        });
    }
}

// Export για το Jest (MovementTests.js) χωρίς να σπάει στον Browser
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        roomConfig,
        playerState,
        checkCollision,
        movePlayer,
        teammateState,
        getNearbyExhibit,
        collectNearbyExhibit,
        checkCoOpPuzzle,
        isRectColliding
    };
}