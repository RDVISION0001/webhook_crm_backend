package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_intent_id", unique = true, nullable = false)
    private String paymentIntentId;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_id")
    private String customerId;

    @CreationTimestamp
    @Column(name = "payment_date", updatable = false)
    private LocalDateTime paymentDate;

    @Column(name = "invoiceId", updatable = false)
    private String invoiceId;
    private LocalDateTime lastUpdateDateTime;
    private String paymentWindow;
    private String productCode;
    private String ticketId;
}
