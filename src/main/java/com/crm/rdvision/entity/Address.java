package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer addressId;
    @Column(unique = true)
    private String ticketId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String houseNumber;
    private String landmark;
    private String city;
    private String zipCode;
    private String state;
    private String country;
}
