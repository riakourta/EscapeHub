// Renderer contract: onSnapshot(players), onMove({id,x,y}), onStart(), onStatus(text).
export function connectGame(callbacks = {}) {
    const socket = new WebSocket(`${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/game`);
    socket.onmessage = ({data}) => {
        const event = JSON.parse(data);
        if (event.type === 'welcome') callbacks.onWelcome?.(event.id);
        if (event.type === 'snapshot') {
            callbacks.onSnapshot?.(event.players);
            if (event.started) callbacks.onStart?.();
        }
        if (event.type === 'move') callbacks.onMove?.(event);
        if (event.type === 'error') callbacks.onStatus?.(event.message);
    };
    socket.onclose = e => callbacks.onStatus?.(`Disconnected (${e.code}). ${e.reason} Reload to reconnect.`);
    socket.onerror = () => callbacks.onStatus?.('Connection failed. Check login and server.');
    const send = event => {
        if (socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify(event));
    };
    return {ready: () => send({type:'ready'}), move: (x,y) => send({type:'move',x,y}), close: () => socket.close()};
}
