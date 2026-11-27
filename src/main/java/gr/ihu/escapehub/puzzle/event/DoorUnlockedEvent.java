package gr.ihu.escapehub.puzzle.event;

public record DoorUnlockedEvent(String roomId, String nextRoom) {}