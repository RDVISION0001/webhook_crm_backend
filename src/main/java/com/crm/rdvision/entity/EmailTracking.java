package com.crm.rdvision.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EmailTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_type", nullable = false, length = 50)
    private String emailType;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "opened_time")
    private LocalDateTime openedTime;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "tracking_id", nullable = false, unique = true, length = 100)
    private String trackingId;

    @Column(name = "related_entity_id")
    private String relatedEntityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
