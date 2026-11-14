package gr.ihu.escapehub.state.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Lobby admission survives the browser navigation into the museum. One room per server. */
@Component
public class MuseumRoom {
    public record Member(String id, String nickname, String color) { }
    private final Map<String, Member> members = new LinkedHashMap<>();
    private boolean alarm;
    public synchronized void start(Collection<Member> roster) {
        if (!members.isEmpty()) { return; }
        for (Member member : roster) { members.put(member.id(), member); }
    }
    public synchronized boolean started() { return !members.isEmpty(); }
    public synchronized boolean contains(String id) { return members.containsKey(id); }
    public synchronized Member member(String id) { return members.get(id); }
    public synchronized int slot(String id) { return new java.util.ArrayList<>(members.keySet()).indexOf(id); }
    public synchronized boolean alarm() { return alarm; }
    public synchronized void triggerAlarm() { alarm = true; }
    public synchronized void reset() { members.clear(); alarm = false; }
}
