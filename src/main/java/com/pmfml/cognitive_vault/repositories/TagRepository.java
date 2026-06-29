package com.pmfml.cognitive_vault.repositories;

import com.pmfml.cognitive_vault.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Tag entities.
 */
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
}
