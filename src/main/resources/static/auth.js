// auth.js

function validateAuthInput(username, password, minPasswordLength = 1) {
    if (!username || username.trim() === '') {
        return { valid: false, error: 'Username is required.' };
    }
    if (!password || password.trim() === '') {
        return { valid: false, error: 'Password is required.' };
    }
    if (password.length < minPasswordLength) {
        return { valid: false, error: `Password must be at least ${minPasswordLength} characters long.` };
    }
    return { valid: true, error: null };
}

function validateProfileInput(nickname, avatarColor) {
    if (!nickname || nickname.trim() === '') {
        return { valid: false, error: 'Nickname is required.' };
    }
    if (!avatarColor || avatarColor.trim() === '') {
        return { valid: false, error: 'Avatar color selection is required.' };
    }
    return { valid: true, error: null };
}

function extractAndSaveJwtToken(authResponse, storageKey) {
    if (!storageKey) {
        throw new Error('Storage key must be provided.');
    }
    const token = authResponse && (authResponse.token || authResponse.jwt || authResponse.accessToken);
    if (!token || typeof token !== 'string' || token.trim() === '') {
        throw new Error('No valid JWT token returned from server.');
    }
    localStorage.setItem(storageKey, token);
    return token;
}

function renderStatusMessage(element, text, isError = true) {
    if (!element) return;
    element.textContent = text;
    element.className = isError ? 'msg-error' : 'msg-success';
}

function initLoginPage() {
    const loginForm = document.getElementById('loginForm');
    const registerBtn = document.getElementById('registerBtn');
    const messageBox = document.getElementById('authMessage');

    if (!loginForm) return;

    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const minPassLength = Number(passwordInput.getAttribute('minlength')) || 1;
    const nextRoute = loginForm.dataset.nextPage;

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = usernameInput.value;
        const password = passwordInput.value;

        const check = validateAuthInput(username, password, minPassLength);
        if (!check.valid) {
            renderStatusMessage(messageBox, check.error, true);
            return;
        }

        try {
            const responseData = await apiClient.login(username.trim(), password);
            extractAndSaveJwtToken(responseData, API_CONFIG.STORAGE_KEYS.TOKEN);
            localStorage.setItem(API_CONFIG.STORAGE_KEYS.USERNAME, username.trim());
            if (nextRoute) {
                window.location.href = nextRoute;
            }
        } catch (err) {
            renderStatusMessage(messageBox, err.message, true);
        }
    });

    if (registerBtn) {
        registerBtn.addEventListener('click', async () => {
            const username = usernameInput.value;
            const password = passwordInput.value;

            const check = validateAuthInput(username, password, minPassLength);
            if (!check.valid) {
                renderStatusMessage(messageBox, check.error, true);
                return;
            }

            try {
                const res = await apiClient.register(username.trim(), password);
                renderStatusMessage(messageBox, res.message || 'Registration successful! You can now log in.', false);
            } catch (err) {
                renderStatusMessage(messageBox, err.message, true);
            }
        });
    }
}

async function initProfilePage() {
    const profileForm = document.getElementById('profileForm');
    if (!profileForm) return;

    const loginRedirect = profileForm.dataset.authPage;
    const nextRoute = profileForm.dataset.nextPage;
    const token = localStorage.getItem(API_CONFIG.STORAGE_KEYS.TOKEN);

    if (!token) {
        if (loginRedirect) window.location.href = loginRedirect;
        return;
    }

    const nicknameInput = document.getElementById('nickname');
    const avatarColorSelect = document.getElementById('avatarColor');
    const messageBox = document.getElementById('profileMessage');

    try {
        const existingProfile = await apiClient.getProfile();
        if (existingProfile && existingProfile.nickname) {
            nicknameInput.value = existingProfile.nickname;
        }
        if (existingProfile && Array.isArray(existingProfile.availableColors) && existingProfile.availableColors.length > 0) {
            avatarColorSelect.innerHTML = '';
            existingProfile.availableColors.forEach(color => {
                const opt = document.createElement('option');
                opt.value = color;
                opt.textContent = color;
                avatarColorSelect.appendChild(opt);
            });
        }
        if (existingProfile && existingProfile.avatarColor) {
            avatarColorSelect.value = existingProfile.avatarColor;
        }
    } catch (err) {
        // Profile not initialized on server yet
    }

    profileForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const nickname = nicknameInput.value;
        const avatarColor = avatarColorSelect.value;

        const check = validateProfileInput(nickname, avatarColor);
        if (!check.valid) {
            renderStatusMessage(messageBox, check.error, true);
            return;
        }

        try {
            const updated = await apiClient.updateProfile(nickname.trim(), avatarColor);
            localStorage.setItem(API_CONFIG.STORAGE_KEYS.NICKNAME, (updated && updated.nickname) || nickname.trim());
            localStorage.setItem(API_CONFIG.STORAGE_KEYS.AVATAR_COLOR, (updated && updated.avatarColor) || avatarColor);
            if (nextRoute) {
                window.location.href = nextRoute;
            }
        } catch (err) {
            renderStatusMessage(messageBox, err.message, true);
        }
    });
}

if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', () => {
        initLoginPage();
        initProfilePage();
    });
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        validateAuthInput,
        validateProfileInput,
        extractAndSaveJwtToken
    };
}