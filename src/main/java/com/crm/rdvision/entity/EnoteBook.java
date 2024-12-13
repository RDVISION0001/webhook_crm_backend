package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EnoteBook {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long noteId;
    private String title;
    @Column(columnDefinition = "LONGTEXT")
    private String noteContent;
    private LocalDateTime date;
    @ManyToOne
    private User user;
}
