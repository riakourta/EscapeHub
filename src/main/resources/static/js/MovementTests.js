/* eslint-env jest, node */
/* global require, describe, beforeEach, test, expect */
// noinspection JSUnresolvedReference

const {
    roomConfig,
    checkCollision,
    movePlayer
} = require('./playerMovement');

describe('Κεντρική Αίθουσα Richelieu - Έλεγχοι Κίνησης & Σύγκρουσης (MovementTests)', () => {
    let testPlayer;

    beforeEach(() => {
        testPlayer = {
            x: 300,
            y: 300,
            size: 30,
            speed: 10
        };
    });

    test('Επιτρέπει την κίνηση σε ελεύθερο χώρο με τα βελάκια', () => {
        const resultUp = movePlayer('ArrowUp', testPlayer, roomConfig);
        expect(resultUp).toBe(true);
        expect(testPlayer.y).toBe(290);

        const resultRight = movePlayer('ArrowRight', testPlayer, roomConfig);
        expect(resultRight).toBe(true);
        expect(testPlayer.x).toBe(310);
    });

    test('Ακυρώνει την κίνηση (επιστρέφει false) όταν ο παίκτης πέφτει πάνω σε τοίχο', () => {
        testPlayer.x = 20;
        testPlayer.y = 300;

        const moved = movePlayer('ArrowLeft', testPlayer, roomConfig);

        expect(moved).toBe(false);
        expect(testPlayer.x).toBe(20);
    });

    test('Ακυρώνει την κίνηση (επιστρέφει false) όταν ο παίκτης πέφτει πάνω σε έκθεμα', () => {
        testPlayer.x = 150;
        testPlayer.y = 160;

        const moved = movePlayer('ArrowRight', testPlayer, roomConfig);

        expect(moved).toBe(false);
        expect(testPlayer.x).toBe(150);
    });

    test('Η checkCollision επιστρέφει true εκτός ορίων δωματίου', () => {
        expect(checkCollision(5, 300, 30, roomConfig)).toBe(true);
        expect(checkCollision(300, 5, 30, roomConfig)).toBe(true);
        expect(checkCollision(760, 300, 30, roomConfig)).toBe(true);
        expect(checkCollision(300, 560, 30, roomConfig)).toBe(true);
    });
});