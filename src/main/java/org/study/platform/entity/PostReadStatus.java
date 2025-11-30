package org.study.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_read_status")
public class PostReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "read_id")
    private Long readId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        readAt = LocalDateTime.now();
    }

    public PostReadStatus() {}

    public PostReadStatus(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }

    public Long getReadId() { return readId; }
    public void setReadId(Long readId) { this.readId = readId; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
