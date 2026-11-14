package gr.ihu.escapehub.profile.entity;

import jakarta.persistence.*;

@Entity
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String ownerUsername;

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String value) { ownerUsername = value; }

    private String nickname;
    private String avatarColor;

    /*
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
     */

    public Long getId() {return id;}
    public void setId(long id) {this.id = id;}

    public String getNickname() {return nickname;}
    public void setNickname(String nickname) {this.nickname = nickname;}

    public String getAvatarColor() {return avatarColor;}
    public void setAvatarColor(String avatarColor) {this.avatarColor = avatarColor;}

    /*
    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
     */
}
