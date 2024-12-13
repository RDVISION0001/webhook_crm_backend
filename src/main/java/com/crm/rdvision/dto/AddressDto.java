package com.crm.rdvision.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddressDto {
    private Integer addressId;
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
