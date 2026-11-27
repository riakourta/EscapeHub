package gr.ihu.escapehub.inventory;

import gr.ihu.escapehub.inventory.dto.InventoryItemDto;
import gr.ihu.escapehub.inventory.service.InventoryService;
import gr.ihu.escapehub.journal.dto.JournalEntry;
import gr.ihu.escapehub.journal.service.JournalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceTest {

    private JournalService journalService;
    private InventoryService inventoryService;
    private List<Object> publishedEvents;

    @BeforeEach
    void setUp() {
        journalService = new JournalService();
        publishedEvents = new ArrayList<>();
        inventoryService = new InventoryService(journalService, publishedEvents::add);
    }

    @Test
    void addItem_isolatesInventoryPerPlayer() {
        InventoryItemDto clueA = new InventoryItemDto("clue-1722", "Έτος Στέψης", "1722", "Στέμμα Λουδοβίκου ΙΕ'", false);
        InventoryItemDto clueB = new InventoryItemDto("clue-140", "Βάρος σε καράτια", "140.64", "Διαμάντι Regent", false);

        inventoryService.addItem(1L, clueA);
        inventoryService.addItem(2L, clueB);

        List<InventoryItemDto> player1Items = inventoryService.getInventory(1L);
        List<InventoryItemDto> player2Items = inventoryService.getInventory(2L);

        assertEquals(1, player1Items.size());
        assertEquals("clue-1722", player1Items.get(0).id());

        assertEquals(1, player2Items.size());
        assertEquals("clue-140", player2Items.get(0).id());
    }

    @Test
    void addItem_preventsDuplicateItemId() {
        InventoryItemDto clue = new InventoryItemDto("clue-1722", "Έτος Στέψης", "1722", "Στέμμα Λουδοβίκου ΙΕ'", false);

        inventoryService.addItem(1L, clue);
        List<InventoryItemDto> afterSecondAdd = inventoryService.addItem(1L, clue);

        assertEquals(1, afterSecondAdd.size());
    }

    @Test
    void shareItem_transfersClueToJournalAndMarksShared() {
        InventoryItemDto clue = new InventoryItemDto("clue-1722", "Έτος Στέψης", "1722", "Στέμμα Λουδοβίκου ΙΕ'", false);
        inventoryService.addItem(1L, clue);

        InventoryService.ShareResult result = inventoryService.shareItem(1L, "clue-1722");

        assertEquals(1, result.inventory().size());
        assertTrue(result.inventory().get(0).shared());
        assertEquals(1, result.entries().size());
        assertEquals("CLUE_SHARED", result.entries().get(0).type());
        assertEquals("clue-1722", result.entries().get(0).targetId());
        assertEquals(1, publishedEvents.size());
    }

    @Test
    void shareItem_throwsExceptionWhenItemDoesNotExist() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.shareItem(1L, "unknown-id"));
    }

    @Test
    void addDirectlyToJournal_recordsClueWithoutDuplicates() {
        InventoryItemDto clue = new InventoryItemDto("clue-140", "Βάρος σε καράτια", "140.64", "Διαμάντι Regent", false);

        List<JournalEntry> first = inventoryService.addDirectlyToJournal(clue);
        List<JournalEntry> second = inventoryService.addDirectlyToJournal(clue);

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals("clue-140", second.get(0).targetId());
    }
}