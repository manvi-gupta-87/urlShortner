package com.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;  // Store ID, not entity - no cross-service relationships

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private String referrer;

    @Column
    private String country;

    @Column
    private String city;

    @Column
    private String deviceType;

    @Column
    private String browser;
} 