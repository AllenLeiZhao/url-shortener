package com.example.urlshortener.repository;

import com.example.urlshortener.model.ClickEvent;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByCode(String code);

    long countByCodeAndOccurredAtAfter(String code, Instant after);

    Optional<ClickEvent> findTopByCodeOrderByOccurredAtDesc(String code);
}
