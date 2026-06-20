package com.pmfml.cognitive_vault.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps semantic relationships calculated dynamically between notes based on their embedding vectors.
 */
@Entity
@Table(name = "relationships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source_note_id", "target_note_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_note_id", nullable = false)
    private Note sourceNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_note_id", nullable = false)
    private Note targetNote;

    @Column(name = "similarity_score", nullable = false)
    private Double similarityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
