import {connectGame} from './gameSocket.js';
const canvas = document.querySelector('canvas');
const ctx = canvas.getContext('2d');
const status = document.querySelector('#status');
let players = new Map(), self, started = false, entered = false;
const game = connectGame({
    onWelcome: id => { self = id; },
    onSnapshot: list => {
        players = new Map(list.map(p => [p.id,p]));
        status.textContent = list.map(p => `${p.id}: ${p.ready ? 'ready' : 'waiting'}`).join(' | ');
    },
    onMove: p => { if (players.has(p.id)) Object.assign(players.get(p.id),p); },
    onStart: () => {
        if (started) return;
        started = true;
        document.querySelector('#intro').hidden = false;
        document.querySelector('#ready').hidden = true;
    },
    onStatus: text => { status.textContent = text; }
});
document.querySelector('#ready').onclick = () => game.ready();
document.querySelector('#enter').onclick = () => {
    entered = true; canvas.hidden = false; document.querySelector('#intro').hidden = true;
};
const keys = new Set();
window.addEventListener('keydown', e => {
    if (['ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(e.key)) { e.preventDefault(); keys.add(e.key); }
});
window.addEventListener('keyup', e => keys.delete(e.key));
window.addEventListener('blur', () => keys.clear());
window.addEventListener('pagehide', () => game.close());
let last = performance.now(), sent = 0;
function render(now) {
    const dt = Math.min((now-last)/1000,0.05); last = now;
    const me = players.get(self);
    if (started && entered && me) {
        const dx = Number(keys.has('ArrowRight')) - Number(keys.has('ArrowLeft'));
        const dy = Number(keys.has('ArrowDown')) - Number(keys.has('ArrowUp'));
        const scale = dx && dy ? Math.SQRT1_2 : 1;
        me.x = Math.max(12,Math.min(788,me.x + dx*180*dt*scale));
        me.y = Math.max(12,Math.min(488,me.y + dy*180*dt*scale));
        if (now-sent >= 50) { game.move(me.x,me.y); sent = now; }
    }
    ctx.fillStyle = '#172235'; ctx.fillRect(0,0,800,500);
    for (const p of players.values()) {
        ctx.fillStyle = p.id === self ? '#63e6be' : '#ffc078';
        ctx.beginPath(); ctx.arc(p.x,p.y,12,0,Math.PI*2); ctx.fill();
        ctx.fillStyle = 'white'; ctx.fillText(p.id,p.x-20,p.y-20);
    }
    requestAnimationFrame(render);
}
requestAnimationFrame(render);
