package com.urlshortener.repository;

import com.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
} 