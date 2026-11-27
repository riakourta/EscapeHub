package gr.ihu.escapehub.inventory.service;

import gr.ihu.escapehub.inventory.dto.InventoryItemDto;
import gr.ihu.escapehub.journal.dto.JournalEntry;
import gr.ihu.escapehub.journal.service.JournalService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {

    private final Map<Long, Map<String, InventoryItemDto>> playerInventories = new ConcurrentHashMap<>();
    private final JournalService journalService;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryService(JournalService journalService, ApplicationEventPublisher eventPublisher) {
        this.journalService = journalService;
        this.eventPublisher = eventPublisher;
    }

    public List<InventoryItemDto> getInventory(Long userId) {
        Map<String, InventoryItemDto> items = playerInventories.get(userId);
        if (items == null) {
            return List.of();
        }
        synchronized (items) {
            return List.copyOf(items.values());
        }
    }

    public List<InventoryItemDto> addItem(Long userId, InventoryItemDto item) {
        validateItem(item);

        String normalizedId = item.id().trim();
        InventoryItemDto sanitized = new InventoryItemDto(
                normalizedId,
                item.title().trim(),
                item.description() != null ? item.description().trim() : "",
                item.sourceExhibit() != null ? item.sourceExhibit().trim() : "",
                false
        );

        Map<String, InventoryItemDto> items = playerInventories.computeIfAbsent(
                userId,
                key -> new LinkedHashMap<>()
        );

        synchronized (items) {
            items.putIfAbsent(normalizedId, sanitized);
            return List.copyOf(items.values());
        }
    }

    public ShareResult shareItem(Long userId, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Το itemId είναι υποχρεωτικό.");
        }

        String normalizedId = itemId.trim();
        Map<String, InventoryItemDto> items = playerInventories.get(userId);
        if (items == null) {
            throw new IllegalArgumentException("Δεν βρέθηκε το στοιχείο στο inventory: " + normalizedId);
        }

        InventoryItemDto sharedItem;
        List<InventoryItemDto> updatedInventory;

        synchronized (items) {
            InventoryItemDto existing = items.get(normalizedId);
            if (existing == null) {
                throw new IllegalArgumentException("Δεν βρέθηκε το στοιχείο στο inventory: " + normalizedId);
            }
            sharedItem = existing.withShared(true);
            items.put(normalizedId, sharedItem);
            updatedInventory = List.copyOf(items.values());
        }

        List<JournalEntry> journalEntries = journalService.recordClue(
                sharedItem.id(),
                sharedItem.title(),
                sharedItem.description(),
                sharedItem.sourceExhibit()
        );

        publishJournalUpdate(journalEntries);
        return new ShareResult(updatedInventory, journalEntries);
    }

    public List<JournalEntry> addDirectlyToJournal(InventoryItemDto item) {
        validateItem(item);

        List<JournalEntry> journalEntries = journalService.recordClue(
                item.id().trim(),
                item.title().trim(),
                item.description() != null ? item.description().trim() : "",
                item.sourceExhibit() != null ? item.sourceExhibit().trim() : ""
        );

        publishJournalUpdate(journalEntries);
        return journalEntries;
    }

    private void validateItem(InventoryItemDto item) {
        if (item == null || item.id() == null || item.id().isBlank()) {
            throw new IllegalArgumentException("Το στοιχείο πρέπει να έχει έγκυρο id.");
        }
        if (item.title() == null || item.title().isBlank()) {
            throw new IllegalArgumentException("Το στοιχείο πρέπει να έχει τίτλο.");
        }
    }

    private void publishJournalUpdate(List<JournalEntry> entries) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new JournalUpdatedEvent(entries));
        }
    }

    public record ShareResult(List<InventoryItemDto> inventory, List<JournalEntry> entries) {}

    public record JournalUpdatedEvent(List<JournalEntry> entries) {}
}