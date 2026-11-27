package gr.ihu.escapehub.inventory.controller;

import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.auth.service.AuthService;
import gr.ihu.escapehub.inventory.dto.InventoryItemDto;
import gr.ihu.escapehub.inventory.service.InventoryService;
import gr.ihu.escapehub.journal.dto.JournalEntry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService inventoryService;
    private final AuthService authService;

    public InventoryController(InventoryService inventoryService, AuthService authService) {
        this.inventoryService = inventoryService;
        this.authService = authService;
    }

    private Long resolveUserId(User principalUser, String authHeader) {
        if (principalUser != null) {
            return principalUser.getId();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return authService.extractUserIdFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryItemDto>> getInventory(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = resolveUserId(user, authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(inventoryService.getInventory(userId));
    }

    @PostMapping("/inventory/add")
    public ResponseEntity<?> addToInventory(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody InventoryItemDto item) {
        Long userId = resolveUserId(user, authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(inventoryService.addItem(userId, item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/inventory/share/{itemId}")
    public ResponseEntity<?> shareToJournal(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String itemId) {
        Long userId = resolveUserId(user, authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            InventoryService.ShareResult result = inventoryService.shareItem(userId, itemId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/journal/add")
    public ResponseEntity<?> addDirectlyToJournal(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody InventoryItemDto item) {
        Long userId = resolveUserId(user, authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<JournalEntry> entries = inventoryService.addDirectlyToJournal(item);
            return ResponseEntity.ok(Map.of("entries", entries));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}