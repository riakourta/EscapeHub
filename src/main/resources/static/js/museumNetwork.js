import {connectGame} from './gameSocket.js';
const museum = window.museumGame;
const status = document.getElementById('network-status');
const enter = document.getElementById('enter-museum');
enter.disabled = true;
const game = connectGame({
    onWelcome: id => museum.identify(id),
    onSnapshot: (players, state) => {
        museum.snapshot(players, state.alarm);
        status.textContent = `${players.length}/4 παίκτες στην αίθουσα`;
        enter.disabled = false;
    },
    onMove: player => museum.move(player),
    onStatus: text => {
        museum.connection(false); enter.disabled = true;
        status.textContent = text;
        document.getElementById('hud-text').textContent = text;
    }
});
museum.sendMove = player => game.move(player.x,player.y,player.direction);
window.addEventListener('pagehide', () => game.close());
