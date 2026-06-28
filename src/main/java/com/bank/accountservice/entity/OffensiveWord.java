package com.bank.accountservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "offensive_words")
@Data
public class OffensiveWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word", unique = true, nullable = false)
    private String word;
}
