package gr.ihu.escapehub.mission.dto;

public class MissionDto {
    private String id;
    private int order;
    private String title;
    private String description;
    private boolean completed;

    public MissionDto() {}

    public MissionDto(String id, int order, String title, String description, boolean completed) {
        this.id = id;
        this.order = order;
        this.title = title;
        this.description = description;
        this.completed = completed;
    }

    public MissionDto copy() {
        return new MissionDto(id, order, title, description, completed);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
