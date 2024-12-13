package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "agent_table")
public class HoduCall {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(unique = true,nullable = false)
    private int userId;
    private String agentUserName;
    private String agentPassword;
    private String hoduToken;
    private String hoduTanentId;
}
