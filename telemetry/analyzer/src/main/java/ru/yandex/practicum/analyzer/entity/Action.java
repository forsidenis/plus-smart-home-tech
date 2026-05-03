package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "actions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer value;
}