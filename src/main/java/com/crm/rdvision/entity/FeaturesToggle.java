package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "featuresToggle")
public class FeaturesToggle {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String featureName;
    private Boolean isEnabled;
    private int lastActionPerformedByUserId;

}
