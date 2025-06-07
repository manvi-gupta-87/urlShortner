package com.urlshortener.repository;

import com.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByUrlIdOrderByTimestampDesc(Long urlId);

    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.url.id = ?1")
    Long countByUrlId(Long urlId);

    @Query("SELECT c.country, COUNT(c) FROM ClickEvent c WHERE c.url.id = ?1 GROUP BY c.country")
    List<Object[]> getClicksByCountry(Long urlId);

    @Query("SELECT c.browser, COUNT(c) FROM ClickEvent c WHERE c.url.id = ?1 GROUP BY c.browser")
    List<Object[]> getClicksByBrowser(Long urlId);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c WHERE c.url.id = ?1 GROUP BY c.deviceType")
    List<Object[]> getClicksByDeviceType(Long urlId);

    @Query(value = "SELECT FORMATDATETIME(timestamp, 'yyyy-MM-dd') as date, COUNT(id) as count FROM click_events WHERE url_id = :urlId AND timestamp >= :startDate GROUP BY FORMATDATETIME(timestamp, 'yyyy-MM-dd') ORDER BY date DESC", nativeQuery = true)
    List<Object[]> getClicksByDate(@Param("urlId") Long urlId, @Param("startDate") LocalDateTime startDate);
} 