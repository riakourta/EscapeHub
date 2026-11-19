const canvas = document.getElementById('museumCanvas');
const ctx = canvas ? canvas.getContext('2d') : null;

const startBtn = document.getElementById('start-btn');
const introModal = document.getElementById('intro-modal');
const darknessOverlay = document.getElementById('darkness');
const gameWrapper = document.getElementById('game-wrapper');
const hudText = document.getElementById('hud-text');

let isPanicSequenceRunning = false;

// Κλείσιμο του Intro Modal όταν πατηθεί το κουμπί "Συνέχεια"
if (startBtn && introModal) {
    startBtn.addEventListener('click', () => {
        introModal.style.display = 'none';
        renderGame();
    });
}

/**
 * Ελέγχει αν ο παίκτης έφτασε στο μπροστινό έκθεμα για να το κλέψει
 */
function checkHeistTrigger() {
    if (roomConfig.alarmTriggered || isPanicSequenceRunning) return;

    const target = roomConfig.obstacles.find(o => o.isTarget);
    if (!target) return;

    const touchBox = {
        x: playerState.x - 10,
        y: playerState.y - 10,
        width: playerState.size + 20,
        height: playerState.size + 20
    };

    if (isRectColliding(touchBox, target)) {
        triggerAlarmSequence(target);
    }
}

/**
 * Αγχωτικό εφέ κλοπής: Τρέμουλο οθόνης, κόκκινα flashes και εμφάνιση του Modal συναγερμού
 */
function triggerAlarmSequence(targetExhibit) {
    isPanicSequenceRunning = true;
    targetExhibit.label = 'Ξίφος (ΚΛΑΠΗΚΕ!)';

    if (gameWrapper) gameWrapper.classList.add('panic-shake');
    if (darknessOverlay) darknessOverlay.classList.add('alarm-active');
    if (hudText) {
        hudText.innerHTML = '🚨 <b style="color:#ff3333">ΣΥΝΑΓΕΡΜΟΣ!</b> | Κεντρική Αίθουσα Richelieu Wing | Δραπετεύστε από την Πτέρυγα Sully!';
    }

    let flashes = 0;
    const panicInterval = setInterval(() => {
        renderGame();
        if (flashes % 2 === 0 && ctx) {
            ctx.fillStyle = 'rgba(255, 0, 0, 0.35)';
            ctx.fillRect(0, 0, roomConfig.width, roomConfig.height);
        }
        flashes++;

        if (flashes >= 6) {
            clearInterval(panicInterval);
            if (gameWrapper) gameWrapper.classList.remove('panic-shake');
            roomConfig.alarmTriggered = true;
            isPanicSequenceRunning = false;
            if (introModal) introModal.style.display = 'flex';
            renderGame();
        }
    }, 140);
}

/**
 * Υπολογίζει τη γωνία του φακού ανάλογα με την κατεύθυνση του παίκτη
 */
function getDirectionAngle(direction) {
    if (direction === 'down') return Math.PI / 2;
    if (direction === 'left') return Math.PI;
    if (direction === 'right') return 0;
    return -Math.PI / 2; // Προεπιλογή: Πάνω
}

/**
 * Ζωγραφίζει τα αντικείμενα του δωματίου (χρησιμοποιείται σκοτεινά στο βάθος και φωτεινά μέσα στον φακό)
 */
function drawRoomElements(isLit) {
    const { width, height, wallThickness, obstacles, doors, alarmTriggered } = roomConfig;

    // 1. Δάπεδο
    ctx.fillStyle = isLit ? '#161622' : '#040407';
    ctx.fillRect(0, 0, width, height);

    // Πλακάκια δαπέδου
    ctx.strokeStyle = isLit ? '#232336' : '#07070c';
    ctx.lineWidth = 1;
    for (let x = wallThickness; x < width - wallThickness; x += 40) {
        ctx.beginPath();
        ctx.moveTo(x, wallThickness);
        ctx.lineTo(x, height - wallThickness);
        ctx.stroke();
    }
    for (let y = wallThickness; y < height - wallThickness; y += 40) {
        ctx.beginPath();
        ctx.moveTo(wallThickness, y);
        ctx.lineTo(width - wallThickness, y);
        ctx.stroke();
    }

    // 2. Τοίχοι
    ctx.fillStyle = isLit ? '#2d2d3d' : '#0a0a10';
    ctx.fillRect(0, 0, width, wallThickness);
    ctx.fillRect(0, height - wallThickness, width, wallThickness);
    ctx.fillRect(0, 0, wallThickness, height);
    ctx.fillRect(width - wallThickness, 0, wallThickness, height);

    // 3. Πόρτες (Μετά τον συναγερμό)
    if (alarmTriggered) {
        doors.forEach(door => {
            if (isLit) {
                ctx.fillStyle = door.locked ? '#a30000' : '#d49b0b';
            } else {
                ctx.fillStyle = door.locked ? '#240000' : '#261c02';
            }
            ctx.fillRect(door.x, door.y, door.width, door.height);

            // Τα γράμματα στις πόρτες φαίνονται καθαρά όταν πέφτει πάνω τους ο φακός
            ctx.fillStyle = isLit ? '#ffffff' : 'rgba(255, 255, 255, 0.12)';
            ctx.font = '12px sans-serif';
            ctx.textAlign = 'center';
            if (door.id === 'main-door') {
                ctx.fillText(door.label, door.x + door.width / 2, door.y - 8);
            } else {
                ctx.save();
                ctx.translate(door.x - 10, door.y + door.height / 2);
                ctx.rotate(-Math.PI / 2);
                ctx.fillText(door.label, 0, 0);
                ctx.restore();
            }
        });
    }

    // 4. Εκθέματα
    obstacles.forEach(obs => {
        if (!alarmTriggered && obs.hiddenBeforeAlarm) return;

        // Στο σκοτάδι είναι σχεδόν αόρατες σκιές, στον φακό φωτίζονται κανονικά
        ctx.fillStyle = isLit ? '#3a2e28' : '#090808';
        ctx.fillRect(obs.x, obs.y, obs.width, obs.height);

        if (isLit) {
            ctx.strokeStyle = alarmTriggered ? '#ff4444' : '#c5a059';
        } else {
            ctx.strokeStyle = alarmTriggered ? 'rgba(90, 15, 15, 0.35)' : 'rgba(60, 50, 30, 0.25)';
        }
        ctx.lineWidth = 2;
        ctx.strokeRect(obs.x, obs.y, obs.width, obs.height);

        // Το όνομα του εκθέματος αποκαλύπτεται μόνο με τον φακό!
        if (isLit) {
            ctx.fillStyle = '#ffffff';
            ctx.font = '12px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(obs.label, obs.x + obs.width / 2, obs.y + obs.height / 2 + 4);
        }
    });
}

/**
 * Βήμα 2 & 3: Σχεδίαση με ρεαλιστική μάσκα φακού (όλα σκοτεινά εκτός από τη δέσμη)
 */
function renderGame() {
    if (!ctx) return;

    const { width, height } = roomConfig;
    const centerX = playerState.x + playerState.size / 2;
    const centerY = playerState.y + playerState.size / 2;
    const angle = getDirectionAngle(playerState.direction);

    ctx.clearRect(0, 0, width, height);

    // ΣΤΡΩΣΗ 1: Δωμάτιο φωτεινό στο υπόβαθρο
    drawRoomElements(true);

    // ΣΤΡΩΣΗ 2: Σκοτεινό πέπλο & ομαλός φακός ΜΟΝΟ μπροστά από τον παίκτη
    let srcX = centerX;
    let srcY = centerY;
    const tipOffset = 12;
    if (playerState.direction === 'up') srcY -= tipOffset;
    else if (playerState.direction === 'down') srcY += tipOffset;
    else if (playerState.direction === 'left') srcX -= tipOffset;
    else if (playerState.direction === 'right') srcX += tipOffset;

    const smoothBeamLength = 250;
    const narrowSpread = Math.PI / 5;

    ctx.save();
    ctx.fillStyle = 'rgba(2, 2, 6, 0.97)';
    ctx.fillRect(0, 0, width, height);

    ctx.globalCompositeOperation = 'destination-out';
    const fadeGrad = ctx.createRadialGradient(srcX, srcY, 2, srcX, srcY, smoothBeamLength);
    fadeGrad.addColorStop(0, 'rgba(0, 0, 0, 1)');
    fadeGrad.addColorStop(0.55, 'rgba(0, 0, 0, 0.85)');
    fadeGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

    ctx.beginPath();
    ctx.moveTo(srcX, srcY);
    ctx.arc(srcX, srcY, smoothBeamLength, angle - narrowSpread, angle + narrowSpread);
    ctx.closePath();
    ctx.fillStyle = fadeGrad;
    ctx.fill();
    ctx.restore();

    // ΣΤΡΩΣΗ 3: Πλάκες 2-Player Γρίφου & Ένδειξη [E] πάνω από κοντινό έκθεμα
    if (roomConfig.coOpPuzzle) {
        roomConfig.coOpPuzzle.plates.forEach(plate => {
            const isLitPlate = plate.active || roomConfig.coOpPuzzle.solved;
            ctx.fillStyle = isLitPlate ? 'rgba(0, 255, 136, 0.35)' : 'rgba(255, 180, 0, 0.18)';
            ctx.fillRect(plate.x, plate.y, plate.width, plate.height);
            ctx.strokeStyle = isLitPlate ? '#00ff88' : '#ffb400';
            ctx.lineWidth = 1.5;
            ctx.strokeRect(plate.x, plate.y, plate.width, plate.height);
        });
    }

    const nearby = typeof getNearbyExhibit === 'function' ? getNearbyExhibit() : null;
    if (nearby && roomConfig.alarmTriggered) {
        ctx.fillStyle = '#00d2ff';
        ctx.font = 'bold 12px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('[E] Πάρε', nearby.x + nearby.width / 2, nearby.y - 8);
    }

    // ΣΤΡΩΣΗ 4: Συμπαίκτης (Πορτοκαλί) & Παίκτης (Γαλάζιος)
    if (typeof teammateState !== 'undefined' && teammateState.visible) {
        const tx = teammateState.x + teammateState.size / 2;
        const ty = teammateState.y + teammateState.size / 2;
        ctx.save();
        ctx.shadowColor = '#ff9900';
        ctx.shadowBlur = 10;
        ctx.fillStyle = '#ff9900';
        ctx.beginPath();
        ctx.arc(tx, ty, 8, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.restore();
    }

    ctx.save();
    ctx.shadowColor = '#00d2ff';
    ctx.shadowBlur = 12;
    ctx.fillStyle = '#00d2ff';
    ctx.beginPath();
    ctx.arc(centerX, centerY, 8, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();
}

let isIntroAutoWalking = true;

function runAutoHeistIntro() {
    const target = roomConfig.obstacles.find(o => o.isTarget);
    if (!target || roomConfig.alarmTriggered) {
        isIntroAutoWalking = false;
        renderGame();
        return;
    }

    const goalY = target.y + target.height + 8;

    const walkInterval = setInterval(() => {
        if (playerState.y > goalY) {
            playerState.y -= 4;
            playerState.direction = 'up';
            if (typeof teammateState !== 'undefined') {
                teammateState.y -= 4;
                teammateState.direction = 'up';
            }
            renderGame();
        } else {
            clearInterval(walkInterval);
            isIntroAutoWalking = false;
            if (typeof gameSocket !== 'undefined') {
                gameSocket.logToJournal('🗡️ Κλάπηκε το Ξίφος του Καρλομάγνου! Χτύπησε συναγερμός!');
            }
            triggerAlarmSequence(target);
        }
    }, 55);
}

// Αρχικοποίηση χειρισμού, intro & πρώτου render
initPlayerControls((state, moved, action, takenExhibit) => {
    if (isPanicSequenceRunning || isIntroAutoWalking) return;

    if ((action === 'Collect' || action === 'ClickCollect') && takenExhibit) {
        if (typeof gameSocket !== 'undefined') {
            gameSocket.sendExhibitCollected(takenExhibit);
        }
    } else if (moved) {
        if (typeof gameSocket !== 'undefined') {
            gameSocket.sendMove(state);
        }
        if (typeof checkCoOpPuzzle === 'function' && checkCoOpPuzzle()) {
            if (typeof gameSocket !== 'undefined') {
                gameSocket.sendPuzzleSolved(roomConfig.coOpPuzzle);
            }
        }
    }

    renderGame();
    checkHeistTrigger();
});

renderGame();
runAutoHeistIntro();