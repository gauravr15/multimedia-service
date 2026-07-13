package com.odin.multimedia.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.enums.StatusReconciliationState;

@Repository
public interface StatusCatalogRepository extends JpaRepository<StatusCatalogRecord, String> {
    Optional<StatusCatalogRecord> findByUploaderIdAndIdempotencyKey(String uploaderId, String idempotencyKey);
    Optional<StatusCatalogRecord> findByLegacyStatusKey(String legacyStatusKey);
    boolean existsByLegacyStatusKey(String legacyStatusKey);
    List<StatusCatalogRecord> findByLifecycleStateAndUpdatedTimestampBefore(
            StatusLifecycleState state, Instant updatedBefore);
    List<StatusCatalogRecord> findByReconciliationStateAndUpdatedTimestampBefore(
            StatusReconciliationState state, Instant updatedBefore);
    List<StatusCatalogRecord> findByLifecycleStateAndReconciliationState(
            StatusLifecycleState state, StatusReconciliationState reconciliationState, Pageable pageable);
    List<StatusCatalogRecord> findByLifecycleStateAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            StatusLifecycleState state, Instant expiresAt, Pageable pageable);
    List<StatusCatalogRecord> findByLifecycleStateAndReconciliationStateOrderByUpdatedTimestampAsc(
            StatusLifecycleState state, StatusReconciliationState reconciliationState, Pageable pageable);

    @Query("select s from StatusCatalogRecord s where s.lifecycleState = :state and s.expiresAt > :now " +
            "and (:cursorTime is null or s.createdAt < :cursorTime or " +
            "(s.createdAt = :cursorTime and s.statusId < :cursorId)) " +
            "order by s.createdAt desc, s.statusId desc")
    List<StatusCatalogRecord> findFeedCandidates(@Param("state") StatusLifecycleState state,
            @Param("now") Instant now, @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") String cursorId, Pageable pageable);
}
