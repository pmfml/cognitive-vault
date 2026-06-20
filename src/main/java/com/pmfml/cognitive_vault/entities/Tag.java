package com.pmfml.cognitive_vault.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a categorization tag that can be linked to multiple notes.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
}
