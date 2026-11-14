// UI_Tests.js
const assert = require('assert');
const { API_CONFIG } = require('./apiClient.js');
const { validateAuthInput, validateProfileInput, extractAndSaveJwtToken } = require('./auth.js');

global.localStorage = {
    store: {},
    getItem(key) { return this.store[key] || null; },
    setItem(key, val) { this.store[key] = String(val); },
    clear() { this.store = {}; }
};

console.log('--- Running UI & Authentication Unit Tests ---');

// Test 1: Reject submission when password is empty
const emptyPasswordResult = validateAuthInput('testUser', '');
assert.strictEqual(emptyPasswordResult.valid, false);
console.log('PASS - Test 1: Form validation rejects empty password.');

// Test 2: Reject submission when username is empty or whitespace
const emptyUsernameResult = validateAuthInput('   ', 'validPass123');
assert.strictEqual(emptyUsernameResult.valid, false);
console.log('PASS - Test 2: Form validation rejects empty username.');

// Test 3: Accept valid credentials meeting minimum length criteria
const validAuthResult = validateAuthInput('testUser', 'validPass123', 4);
assert.strictEqual(validAuthResult.valid, true);
console.log('PASS - Test 3: Form validation accepts valid credentials.');

// Test 4: Dynamically extract and store JWT token using configured storage key
localStorage.clear();
const dynamicToken = `header.${Date.now()}.signature`;
const mockAuthResponse = { token: dynamicToken };
extractAndSaveJwtToken(mockAuthResponse, API_CONFIG.STORAGE_KEYS.TOKEN);
assert.strictEqual(localStorage.getItem(API_CONFIG.STORAGE_KEYS.TOKEN), dynamicToken);
console.log('PASS - Test 4: JWT token is dynamically extracted and stored in localStorage.');

// Test 5: Throw error when server response does not contain a JWT token
assert.throws(() => {
    extractAndSaveJwtToken({}, API_CONFIG.STORAGE_KEYS.TOKEN);
}, /No valid JWT token returned from server/);
console.log('PASS - Test 5: Missing JWT token throws an error without hardcoded fallback.');

// Test 6: Reject profile update when nickname is missing
const invalidProfileResult = validateProfileInput('', 'BLUE');
assert.strictEqual(invalidProfileResult.valid, false);
console.log('PASS - Test 6: Profile validation rejects empty nickname.');

console.log('SUCCESS: All unit tests passed!');