package com.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "urls", 
       indexes = {@Index(name = "idx_short_url", columnList = "shortUrl", unique = true)})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 10)
    private String shortUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Integer clickCount;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deactivated = false;

    @Column(name = "user_id", nullable = false)
    private Long userId;
} 