export function connectGame(callbacks = {}) {
    const socket = new WebSocket(`${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/game`);
    socket.onmessage = ({data}) => {
        const event = JSON.parse(data);
        if (event.type === 'welcome') callbacks.onWelcome?.(event.id);
        if (event.type === 'snapshot') callbacks.onSnapshot?.(event.players, event);
        if (event.type === 'move') callbacks.onMove?.(event);
    };
    socket.onclose = event => callbacks.onStatus?.(`Disconnected (${event.code}): ${event.reason}. Return to lobby or reload.`);
    socket.onerror = () => callbacks.onStatus?.('Connection failed. Check login and server.');
    return {
        move(x, y, direction) {
            if (socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify({type:'move',x,y,direction}));
        },
        close: () => socket.close()
    };
}
