package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.IdempotencyProvenance;
import com.odin.multimedia.enums.StatusMediaType;
import com.odin.multimedia.repository.StatusCatalogRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:status-repository;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class StatusCatalogRepositoryTest {

    @Autowired
    private StatusCatalogRepository repository;

    @Test
    void savesUnlimitedMixedStatusesAndFindsCorrelations() {
        StatusCatalogRecord image = record("89", "image-key", StatusMediaType.IMAGE);
        image.attachLegacyStatusKey("legacy-image");
        StatusCatalogRecord video = record("89", "video-key", StatusMediaType.VIDEO);
        repository.saveAndFlush(image);
        repository.saveAndFlush(video);

        assertEquals(2, repository.count());
        assertEquals(image.getStatusId(), repository.findByUploaderIdAndIdempotencyKey("89", "image-key")
                .orElseThrow().getStatusId());
        assertEquals(image.getStatusId(), repository.findByLegacyStatusKey("legacy-image")
                .orElseThrow().getStatusId());
        assertEquals(StatusMediaType.VIDEO, repository.findById(video.getStatusId()).orElseThrow().getMediaType());
        assertNotNull(repository.findById(image.getStatusId()).orElseThrow().getCreatedTimestamp());
    }

    @Test
    void rejectsDuplicateUploaderIdempotencyPair() {
        repository.saveAndFlush(record("89", "same-key", StatusMediaType.IMAGE));
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(record("89", "same-key", StatusMediaType.VIDEO)));
    }

    private StatusCatalogRecord record(String uploader, String key, StatusMediaType type) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new StatusCatalogRecord(uploader, type, now, now.plusSeconds(86400), key,
                IdempotencyProvenance.CLIENT);
    }
}
