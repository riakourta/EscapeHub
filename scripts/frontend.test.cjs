const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.join(__dirname,'../src/main/resources/static/js');
const {roomConfig,movePlayer,checkCollision}=require(path.join(root,'playerMovement.js'));

test('team movement: free space, walls, exhibits and room bounds',()=>{
    const p={x:300,y:300,size:30,speed:10};
    assert.equal(movePlayer('ArrowUp',p,roomConfig),true);assert.equal(p.y,290);
    assert.equal(movePlayer('ArrowRight',p,roomConfig),true);assert.equal(p.x,310);
    p.x=20;p.y=300;assert.equal(movePlayer('ArrowLeft',p,roomConfig),false);assert.equal(p.x,20);
    p.x=150;p.y=160;assert.equal(movePlayer('ArrowRight',p,roomConfig),false);assert.equal(p.x,150);
    for(const [x,y] of [[5,300],[300,5],[760,300],[300,560]])assert.equal(checkCollision(x,y,30,roomConfig),true);
});

test('museum adapter: server spawn, remote rendering, blocked controls and shared alarm',()=>{
    const arcs=[],events={},nodes={},intervals=[];
    const ctx=new Proxy({createRadialGradient:()=>({addColorStop(){}}),arc:(...args)=>arcs.push(args)},
        {get:(target,key)=>key in target?target[key]:()=>{}});
    for(const id of ['museumCanvas','start-btn','intro-modal','darkness','game-wrapper','hud-text','heist-intro','enter-museum']){
        nodes[id]={style:{display:'none'},hidden:false,classList:{add(){},remove(){}},
            addEventListener(type,callback){this[type]=callback;},getContext:()=>ctx};
    }
    const sandbox={document:{getElementById:id=>nodes[id]},window:{addEventListener:(type,cb)=>events[type]=cb},
        setInterval:cb=>{intervals.push(cb);return intervals.length;},clearInterval(){},console};
    vm.createContext(sandbox);
    for(const name of ['playerMovement.js','gameRenderer.js'])vm.runInContext(fs.readFileSync(path.join(root,name),'utf8'),sandbox);
    const game=sandbox.window.museumGame;
    game.identify('p1');
    game.snapshot([{id:'p1',x:280,y:500,direction:'up',color:'green'},
        {id:'p2',x:100,y:100,nickname:'Two',color:'pink'}],false);
    assert.equal(vm.runInContext('playerState.x',sandbox),280);
    assert(arcs.some(a=>a[0]===115&&a[1]===115));
    assert.equal(sandbox.window.canMoveInMuseum(),false);
    nodes['enter-museum'].onclick();assert.equal(sandbox.window.canMoveInMuseum(),true);
    let sent=0;game.sendMove=()=>sent++;
    events.keydown({key:'ArrowLeft',preventDefault(){}});
    assert.equal(vm.runInContext('playerState.x',sandbox),270);assert.equal(sent,1);
    game.connection(false);events.keydown({key:'ArrowLeft',preventDefault(){}});
    assert.equal(sent,1);
    game.snapshot([{id:'p1',x:270,y:500,direction:'up',color:'green'}],true);
    assert.equal(intervals.length,1);
    for(let i=0;i<6;i++)intervals[0]();
    assert.equal(vm.runInContext('roomConfig.alarmTriggered',sandbox),true);
    assert.equal(nodes['intro-modal'].style.display,'flex');
    assert.equal(sandbox.window.canMoveInMuseum(),false);
    nodes['start-btn'].click();assert.equal(sandbox.window.canMoveInMuseum(),true);
});

test('socket protocol forwards snapshots, movement and directions using same-origin URL',async()=>{
    const source=fs.readFileSync(path.join(root,'gameSocket.js'),'utf8');
    global.location={protocol:'https:',host:'example.test'};
    let socket;
    global.WebSocket=class{static OPEN=1;readyState=1;sent=[];
        constructor(url){this.url=url;socket=this;}send(data){this.sent.push(JSON.parse(data));}close(){}};
    const {connectGame}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'));
    let id,alarm,move;
    const game=connectGame({onWelcome:value=>id=value,onSnapshot:(players,state)=>alarm=state.alarm,onMove:value=>move=value});
    assert.equal(socket.url,'wss://example.test/ws/game');
    for(const event of [{type:'welcome',id:'p1'},{type:'snapshot',players:[],alarm:true},{type:'move',id:'p2',x:40,y:50}]){
        socket.onmessage({data:JSON.stringify(event)});
    }
    assert.equal(id,'p1');assert.equal(alarm,true);assert.equal(move.id,'p2');
    game.move(40,50,'left');assert.deepEqual(socket.sent,[{type:'move',x:40,y:50,direction:'left'}]);
    socket.readyState=3;game.move(50,60,'up');assert.equal(socket.sent.length,1);
});
