package gr.ihu.escapehub.inventory.dto;

public record InventoryItemDto(
        String id,
        String title,
        String description,
        String sourceExhibit,
        boolean shared
) {
    public InventoryItemDto withShared(boolean shared) {
        return new InventoryItemDto(id, title, description, sourceExhibit, shared);
    }
}