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
    const beamLength = 230;
    const spread = Math.PI / 4;

    ctx.clearRect(0, 0, width, height);

    // ΣΤΡΩΣΗ 1: Σχεδίαση ολόκληρου του δωματίου σε βαθύ σκοτάδι (σχεδόν αόρατο)
    drawRoomElements(false);

    // ΣΤΡΩΣΗ 2: Αποκάλυψη του δωματίου ΜΟΝΟ μέσα στον κώνο του φακού και γύρω από τον παίκτη
    ctx.save();
    ctx.beginPath();
    // Κώνος φακού μπροστά
    ctx.moveTo(centerX, centerY);
    ctx.arc(centerX, centerY, beamLength, angle - spread, angle + spread);
    ctx.closePath();
    // Μικρός κύκλος γύρω από τον παίκτη
    ctx.arc(centerX, centerY, 42, 0, Math.PI * 2);
    ctx.clip();

    // Ζωγραφίζουμε τα φωτισμένα εκθέματα/πάτωμα μόνο μέσα στην περιοχή του φακού
    drawRoomElements(true);

    // Προσθήκη απαλής κιτρινωπής λάμψης του φακού
    const coneGrad = ctx.createRadialGradient(centerX, centerY, 5, centerX, centerY, beamLength);
    coneGrad.addColorStop(0, 'rgba(255, 255, 200, 0.28)');
    coneGrad.addColorStop(0.7, 'rgba(255, 255, 180, 0.08)');
    coneGrad.addColorStop(1, 'rgba(0, 0, 0, 0.65)');
    ctx.fillStyle = coneGrad;
    ctx.fillRect(0, 0, width, height);
    ctx.restore();

    // ΣΤΡΩΣΗ 3: Σχεδίαση Παίκτη (Φωτεινός Κύκλος)
    ctx.save();
    ctx.shadowColor = '#00d2ff';
    ctx.shadowBlur = 15;

    ctx.fillStyle = '#00d2ff';
    ctx.beginPath();
    ctx.arc(centerX, centerY, 9, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();
}

// Αρχικοποίηση χειρισμού και πρώτου render
initPlayerControls(() => {
    if (isPanicSequenceRunning) return;
    renderGame();
    checkHeistTrigger();
});

renderGame();