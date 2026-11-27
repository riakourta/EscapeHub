(() => {
    let missions = [];
    let modalOpenedOnce = false;
    let hudVisible = false;

    function getAuthHeaders() {
        const token = localStorage.getItem('token') || localStorage.getItem('jwt') || '';
        return token ? { Authorization: `Bearer ${token}` } : {};
    }

    async function fetchMissions() {
        try {
            const res = await fetch('/api/missions', { headers: getAuthHeaders() });
            if (!res.ok) return;
            const data = await res.json();
            if (Array.isArray(data)) {
                missions = data;
                if (hudVisible) renderHud();
                updateOpenModalIfAny();
            }
        } catch (err) {
            console.error('Αποτυχία φόρτωσης αποστολών:', err);
        }
    }

    function renderHud() {
        const slot = document.getElementById('missions-hud-slot');
        if (!slot) return;

        hudVisible = true;
        slot.innerHTML = '';

        const panel = document.createElement('section');
        panel.className = 'missions-hud-panel';

        const header = document.createElement('div');
        header.className = 'missions-hud-header';
        header.innerHTML = `
            <h2>📋 Αποστολές</h2>
            <button type="button" class="missions-hud-reopen" title="Προβολή αναλυτικά">?</button>
        `;
        header.querySelector('.missions-hud-reopen').addEventListener('click', () => openMissionsModal(true));

        const list = document.createElement('ul');
        list.className = 'missions-hud-list';

        missions.forEach((m, idx) => {
            const li = document.createElement('li');
            li.className = `missions-hud-item${m.completed ? ' completed' : ''}`;
            const icon = m.completed ? '✔' : '✖';
            li.textContent = `${icon} ${idx + 1}. ${m.title}`;
            list.appendChild(li);
        });

        panel.appendChild(header);
        panel.appendChild(list);
        slot.appendChild(panel);
    }

    function buildModalContent(backdrop) {
        const box = document.createElement('div');
        box.className = 'missions-modal-box';

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'missions-modal-close';
        closeBtn.setAttribute('aria-label', 'Κλείσιμο');
        closeBtn.textContent = '✕';
        closeBtn.addEventListener('click', () => {
            backdrop.remove();
            renderHud();
        });

        const title = document.createElement('h1');
        title.className = 'missions-modal-title';
        title.textContent = '🚨 ΠΡΩΤΟΚΟΛΛΟ ΚΛΕΙΔΩΜΑΤΟΣ — RICHELIEU WING';

        const subtitle = document.createElement('p');
        subtitle.className = 'missions-modal-subtitle';
        subtitle.textContent = 'Η Κεντρική Πύλη της Πυραμίδας σφραγίστηκε. Για να ανοίξει η έξοδος «Προς Πτέρυγα Sully», αποκρυπτογραφήστε το πρωτόκολλο των επιμελητών:';

        const list = document.createElement('ul');
        list.className = 'missions-modal-list';

        missions.forEach((m, idx) => {
            const li = document.createElement('li');
            li.className = `missions-modal-card${m.completed ? ' completed' : ''}`;
            const icon = m.completed ? '✔' : '•';
            li.innerHTML = `
                <h3>${icon} Αποστολή ${idx + 1} — ${m.title}</h3>
                <p>${m.description}</p>
            `;
            list.appendChild(li);
        });

        box.appendChild(closeBtn);
        box.appendChild(title);
        box.appendChild(subtitle);
        box.appendChild(list);
        return box;
    }

    async function openMissionsModal(forceReopen = false) {
        if (modalOpenedOnce && !forceReopen) return;
        modalOpenedOnce = true;

        if (missions.length === 0) {
            await fetchMissions();
        }

        const root = document.getElementById('modals-root');
        if (!root) return;

        const existing = document.getElementById('missions-modal-backdrop');
        if (existing) existing.remove();

        const backdrop = document.createElement('div');
        backdrop.id = 'missions-modal-backdrop';
        backdrop.className = 'missions-modal-backdrop';
        backdrop.appendChild(buildModalContent(backdrop));
        root.appendChild(backdrop);
    }

    function updateOpenModalIfAny() {
        const backdrop = document.getElementById('missions-modal-backdrop');
        if (!backdrop) return;
        backdrop.innerHTML = '';
        backdrop.appendChild(buildModalContent(backdrop));
    }

    function applyMissionUpdate(payload) {
        if (!payload) return;
        if (Array.isArray(payload.missions)) {
            missions = payload.missions;
        } else if (payload.mission && payload.mission.id) {
            const target = missions.find(m => m.id === payload.mission.id);
            if (target) target.completed = Boolean(payload.mission.completed);
        } else if (payload.missionId) {
            const target = missions.find(m => m.id === payload.missionId);
            if (target) target.completed = true;
        }
        if (hudVisible) renderHud();
        updateOpenModalIfAny();
    }

    // 1. Άνοιγμα όταν χτυπήσει ο συναγερμός (Άτομο 6)
    window.addEventListener('alarm:triggered', () => {
        openMissionsModal(false);
    });

    // Fallback για τοπικό testing πριν γίνει merge το branch του Ατόμου 6
    document.addEventListener('DOMContentLoaded', () => {
        fetchMissions();
        const startBtn = document.getElementById('start-btn');
        if (startBtn) {
            startBtn.addEventListener('click', () => openMissionsModal(false));
        }
    });

    // 2. Ζωντανή ενημέρωση από WebSocket Event Bus
    window.addEventListener('ws:message', (e) => {
        const msg = e.detail;
        if (!msg) return;
        if (msg.type === 'MISSION_UPDATED') {
            applyMissionUpdate(msg);
        }
    });

    // 3. Ζωντανή ενημέρωση από puzzle:solved (Άτομο 4)
    window.addEventListener('puzzle:solved', (e) => {
        if (e.detail && e.detail.missionId) {
            applyMissionUpdate({ missionId: e.detail.missionId });
        } else {
            fetchMissions();
        }
    });
})();