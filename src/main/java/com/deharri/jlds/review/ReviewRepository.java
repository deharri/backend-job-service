package com.deharri.jlds.review;

import com.deharri.jlds.enums.ReviewType;
import com.deharri.jlds.review.entity.JobReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<JobReview, UUID> {

    List<JobReview> findByJobId(UUID jobId);

    Optional<JobReview> findByJobIdAndReviewType(UUID jobId, ReviewType reviewType);

    // Reviews where the reviewee is a specific worker (consumer→worker reviews)
    Page<JobReview> findByRevieweeIdAndReviewTypeOrderByCreatedAtDesc(
            UUID revieweeId, ReviewType reviewType, Pageable pageable);

    // Reviews where the reviewee is a specific consumer (worker→consumer reviews)
    Page<JobReview> findByRevieweeIdOrderByCreatedAtDesc(UUID revieweeId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM JobReview r WHERE r.revieweeId = :revieweeId AND r.reviewType = :reviewType")
    Double getAverageRating(@Param("revieweeId") UUID revieweeId, @Param("reviewType") ReviewType reviewType);

    @Query("SELECT COUNT(r) FROM JobReview r WHERE r.revieweeId = :revieweeId AND r.reviewType = :reviewType")
    Long countReviews(@Param("revieweeId") UUID revieweeId, @Param("reviewType") ReviewType reviewType);

    @Query("SELECT AVG(r.qualityRating) FROM JobReview r WHERE r.revieweeId = :id AND r.reviewType = 'CONSUMER_TO_WORKER' AND r.qualityRating IS NOT NULL")
    Double getAvgQualityRating(@Param("id") UUID revieweeId);

    @Query("SELECT AVG(r.punctualityRating) FROM JobReview r WHERE r.revieweeId = :id AND r.reviewType = 'CONSUMER_TO_WORKER' AND r.punctualityRating IS NOT NULL")
    Double getAvgPunctualityRating(@Param("id") UUID revieweeId);

    @Query("SELECT AVG(r.communicationRating) FROM JobReview r WHERE r.revieweeId = :id AND r.communicationRating IS NOT NULL")
    Double getAvgCommunicationRating(@Param("id") UUID revieweeId);

    @Query("SELECT AVG(r.valueRating) FROM JobReview r WHERE r.revieweeId = :id AND r.reviewType = 'CONSUMER_TO_WORKER' AND r.valueRating IS NOT NULL")
    Double getAvgValueRating(@Param("id") UUID revieweeId);

    @Query("SELECT AVG(r.reliabilityRating) FROM JobReview r WHERE r.revieweeId = :id AND r.reviewType = 'WORKER_TO_CONSUMER' AND r.reliabilityRating IS NOT NULL")
    Double getAvgReliabilityRating(@Param("id") UUID revieweeId);
}
