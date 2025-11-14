package com.urlshortener.repository;

import com.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortUrl(String shortUrl);
    boolean existsByShortUrl(String shortUrl);
    List<Url> findByUserIdOrderByCreatedAtDesc(Long userId);
}


