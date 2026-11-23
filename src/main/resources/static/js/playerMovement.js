const roomConfig = {
    width: 800,
    height: 600,
    wallThickness: 20,
    alarmTriggered: typeof window === 'undefined',

    obstacles: [
        {
            id: 'exhibit-1',
            x: 180,
            y: 150,
            width: 70,
            height: 70,
            label: 'Διαμάντι Regent',
            hiddenBeforeAlarm: true
        },
        {
            id: 'exhibit-2',
            x: 520,
            y: 150,
            width: 70,
            height: 70,
            label: 'Στέμμα Λουδοβίκου',
            hiddenBeforeAlarm: true
        },
        {
            id: 'exhibit-3',
            x: 350,
            y: 360,
            width: 90,
            height: 70,
            label: 'Ξίφος Καρλομάγνου',
            isTarget: true
        },
        {
            id: 'hammurabi-code',
            x: 90,
            y: 260,
            width: 130,
            height: 60,
            label: 'Κώδικας Χαμουραμπί',
            collected: false,
            hiddenBeforeAlarm: true
        },
        {
            id: 'winged-bulls',
            x: 580,
            y: 260,
            width: 130,
            height: 60,
            label: 'Φτερωτοί Ταύροι',
            collected: false,
            hiddenBeforeAlarm: true
        }
    ],

    doors: [
        {
            id: 'main-door',
            x: 340,
            y: 580,
            width: 120,
            height: 20,
            locked: true,
            label: 'Κεντρική Πύλη Πυραμίδας (Κλειδωμένη)'
        },
        {
            id: 'room-b-door',
            x: 780,
            y: 240,
            width: 20,
            height: 120,
            locked: false,
            label: 'Προς Πτέρυγα Sully'
        }
    ],

    coOpPuzzle: {
        id: 'twin-security-plates',
        label: 'Διπλός Μηχανισμός Ασφαλείας (2 Παίκτες)',
        plates: [
            {
                id: 'plate-1',
                x: 250,
                y: 455,
                width: 38,
                height: 38,
                active: false
            },
            {
                id: 'plate-2',
                x: 510,
                y: 455,
                width: 38,
                height: 38,
                active: false
            }
        ],
        solved: false
    }
};

const playerState = {
    x: 380,
    y: 500,
    size: 30,
    speed: 10,
    direction: 'up'
};
const networkPlayers = new Map();
let localPlayerId = null;

const teammateState = {
    x: 425,
    y: 500,
    size: 30,
    direction: 'up',
    visible: true
};

function isRectColliding(rect1, rect2) {
    return (
        rect1.x < rect2.x + rect2.width &&
        rect1.x + rect1.width > rect2.x &&
        rect1.y < rect2.y + rect2.height &&
        rect1.y + rect1.height > rect2.y
    );
}

function getNearbyExhibit(
    state = playerState,
    config = roomConfig,
    reach = 25
) {
    const reachBox = {
        x: state.x - reach,
        y: state.y - reach,
        width: state.size + reach * 2,
        height: state.size + reach * 2
    };

    return config.obstacles.find(obs => {
        if (obs.collected) return false;

        if (!config.alarmTriggered && obs.hiddenBeforeAlarm) {
            return false;
        }

        return isRectColliding(reachBox, obs);
    }) || null;
}

function collectNearbyExhibit(
    state = playerState,
    config = roomConfig
) {
    return getNearbyExhibit(state, config);
}

function checkCoOpPuzzle(
    p1 = playerState,
    p2 = teammateState,
    config = roomConfig
) {
    return Boolean(config.coOpPuzzle?.solved);
}

function checkCollision(
    newX,
    newY,
    playerSize = playerState.size,
    config = roomConfig
) {
    const wt = config.wallThickness;

    if (
        newX < wt ||
        newY < wt ||
        newX + playerSize > config.width - wt ||
        newY + playerSize > config.height - wt
    ) {
        return true;
    }

    const playerBox = {
        x: newX,
        y: newY,
        width: playerSize,
        height: playerSize
    };

    for (const obs of config.obstacles) {
        if (obs.collected) {
            continue;
        }

        if (!config.alarmTriggered && obs.hiddenBeforeAlarm) {
            continue;
        }

        if (isRectColliding(playerBox, obs)) {
            return true;
        }
    }

    return false;
}

function movePlayer(
    key,
    state = playerState,
    config = roomConfig
) {
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

    if (checkCollision(nextX, nextY, state.size, config)) {
        return false;
    }

    state.x = nextX;
    state.y = nextY;

    return true;
}

function initPlayerControls(onMoveCallback) {
    if (typeof window === 'undefined') return;

    function notify(moved, action, exhibit) {
        if (typeof onMoveCallback === 'function') {
            onMoveCallback(playerState, moved, action, exhibit);
        }
    }

    function controlsBlocked() {
        return (
            localPlayerId === null ||
            (typeof isPanicSequenceRunning !== 'undefined' &&
                isPanicSequenceRunning) ||
            (typeof isIntroAutoWalking !== 'undefined' &&
                isIntroAutoWalking)
        );
    }

    window.addEventListener('keydown', event => {
        const target = event.target;

        if (
            target &&
            (
                ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) ||
                target.isContentEditable
            )
        ) {
            return;
        }

        const isArrow = [
            'ArrowUp',
            'ArrowDown',
            'ArrowLeft',
            'ArrowRight'
        ].includes(event.key);

        const isCollect = event.key.toLowerCase() === 'e';

        if (!isArrow && !isCollect) return;

        event.preventDefault();

        if (controlsBlocked()) return;

        if (isCollect) {
            if (event.repeat) return;

            const taken = collectNearbyExhibit();

            if (taken) {
                notify(true, 'Collect', taken);
            }

            return;
        }

        const moved = movePlayer(event.key);
        notify(moved, event.key);
    });

    const canvasEl = document.getElementById('museumCanvas');

    if (!canvasEl) return;

    canvasEl.addEventListener('click', event => {
        if (controlsBlocked()) return;

        const rect = canvasEl.getBoundingClientRect();

        if (rect.width === 0 || rect.height === 0) return;

        const mx =
            (event.clientX - rect.left) *
            (canvasEl.width / rect.width);

        const my =
            (event.clientY - rect.top) *
            (canvasEl.height / rect.height);

        const nearby = getNearbyExhibit();

        if (
            nearby &&
            mx >= nearby.x - 15 &&
            mx <= nearby.x + nearby.width + 15 &&
            my >= nearby.y - 15 &&
            my <= nearby.y + nearby.height + 15
        ) {
            const taken = collectNearbyExhibit();

            if (taken) {
                notify(true, 'ClickCollect', taken);
            }
        }
    });
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        roomConfig,
        playerState,
        teammateState,
        checkCollision,
        movePlayer,
        getNearbyExhibit,
        collectNearbyExhibit,
        checkCoOpPuzzle,
        isRectColliding
    };
}