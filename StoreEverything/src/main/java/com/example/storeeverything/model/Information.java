package com.example.storeeverything.model;

// ... existing imports ...
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID; // NEW IMPORT for UUID


@Entity
public class Information {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Content cannot exceed 2000 characters")
    private String content;

    private String link; // URL link

    private LocalDate dateAdded;

    // Many-to-One relationship with User (owner of the information)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Many-to-Many relationship for sharing
    @ManyToMany
    @JoinTable(
            name = "information_shared_with_users",
            joinColumns = @JoinColumn(name = "information_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> sharedWithUsers = new HashSet<>();

    // NEW FIELD for shareable link
    private String shareableToken; // Unique token for direct access via link

    // --- Constructors ---
    public Information() {
        this.dateAdded = LocalDate.now(); // Default to current date
    }

    // You might have other constructors, ensure they also handle new fields if needed.
    // Example:
    public Information(String title, String content, String link, Category category, User user) {
        this.title = title;
        this.content = content;
        this.link = link;
        this.dateAdded = LocalDate.now();
        this.category = category;
        this.user = user;
        // Do not generate token here, generate it when sharing is initiated or updated
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    // Category association (assuming you have a Category entity)
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<User> getSharedWithUsers() {
        return sharedWithUsers;
    }

    public void setSharedWithUsers(Set<User> sharedWithUsers) {
        this.sharedWithUsers = sharedWithUsers;
    }

    // Helper methods for sharing
    public void addSharedUser(User user) {
        this.sharedWithUsers.add(user);
    }

    public void removeSharedUser(User user) {
        this.sharedWithUsers.remove(user);
    }

    // NEW METHODS for shareable token
    public String getShareableToken() {
        return shareableToken;
    }

    public void setShareableToken(String shareableToken) {
        this.shareableToken = shareableToken;
    }

    /**
     * Generates a unique shareable token for this information item.
     * This should be called when the item is created or when sharing is initiated.
     */
    public void generateShareableToken() {
        if (this.shareableToken == null || this.shareableToken.isEmpty()) {
            this.shareableToken = UUID.randomUUID().toString();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Information that = (Information) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}