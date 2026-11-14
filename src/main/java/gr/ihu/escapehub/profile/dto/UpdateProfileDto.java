package gr.ihu.escapehub.profile.dto;

public class UpdateProfileDto {
    private String nickname;
    private String avatarColor;

    public String getNickname() { return nickname; }
    public String getAvatarColor() { return avatarColor; }

    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }
}
