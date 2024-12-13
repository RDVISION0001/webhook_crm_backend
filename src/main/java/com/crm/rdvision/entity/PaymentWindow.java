package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class PaymentWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int windowId;

    @Column(unique = true,nullable = false)
    private String paymentWindowName;
}
