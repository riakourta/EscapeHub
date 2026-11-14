// apiClient.js
const DEFAULT_CONFIG = {
    BASE_URL: (typeof window !== 'undefined' && window.location.origin !== 'null' && !window.location.origin.startsWith('file:'))
        ? window.location.origin
        : '',
    ENDPOINTS: {
        REGISTER: '/api/auth/register',
        LOGIN: '/api/auth/login',
        PROFILE: '/api/profile'
    },
    STORAGE_KEYS: {
        TOKEN: 'jwt_token',
        USERNAME: 'username',
        NICKNAME: 'player_nickname',
        AVATAR_COLOR: 'player_avatarColor'
    }
};

const API_CONFIG = (typeof window !== 'undefined' && window.ESCAPE_HUB_CONFIG)
    ? Object.assign({}, DEFAULT_CONFIG, window.ESCAPE_HUB_CONFIG)
    : DEFAULT_CONFIG;

const apiClient = {
    getAuthHeaders() {
        const token = localStorage.getItem(API_CONFIG.STORAGE_KEYS.TOKEN);
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    },

    async request(endpoint, method = 'GET', body = null, includeAuth = false) {
        const options = {
            method,
            headers: includeAuth ? this.getAuthHeaders() : { 'Content-Type': 'application/json' }
        };
        if (body) {
            options.body = JSON.stringify(body);
        }

        // Cookie session authenticates browser HTTP + WebSocket requests.
        // Keep the team's JWT response/storage API for compatibility; bearer-only auth is not implemented here.
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
            const csrfResponse = await fetch(`${API_CONFIG.BASE_URL}/api/csrf`, {credentials: 'same-origin'});
            if (!csrfResponse.ok) throw new Error('Cannot obtain CSRF token');
            const csrf = await csrfResponse.json();
            options.headers[csrf.headerName] = csrf.token;
        }
        options.credentials = 'same-origin';
        const response = await fetch(`${API_CONFIG.BASE_URL}${endpoint}`, options);
        const responseData = await response.json().catch(() => null);

        if (!response.ok) {
            const errorMessage = (responseData && (responseData.message || responseData.error))
                || response.statusText
                || `HTTP Error ${response.status}`;
            throw new Error(errorMessage);
        }
        return responseData || {};
    },

    register(username, password) {
        return this.request(API_CONFIG.ENDPOINTS.REGISTER, 'POST', { username, password }, false);
    },

    login(username, password) {
        return this.request(API_CONFIG.ENDPOINTS.LOGIN, 'POST', { username, password }, false);
    },

    getProfile() {
        return this.request(API_CONFIG.ENDPOINTS.PROFILE, 'GET', null, true);
    },

    updateProfile(nickname, avatarColor) {
        return this.request(API_CONFIG.ENDPOINTS.PROFILE, 'PUT', { nickname, avatarColor }, true);
    }
};

if (typeof module !== 'undefined' && module.exports) {
    module.exports = { apiClient, API_CONFIG };
}