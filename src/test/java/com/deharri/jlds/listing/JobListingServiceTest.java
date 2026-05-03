package com.deharri.jlds.listing;

import com.deharri.jlds.bid.BidRepository;
import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.dto.request.CreateJobFromOfferRequest;
import com.deharri.jlds.listing.dto.response.JobListingResponse;
import com.deharri.jlds.listing.entity.JobListing;
import com.deharri.jlds.listing.mapper.JobListingMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobListingService — createJobFromOffer agency variant")
class JobListingServiceTest {

    @Mock JobListingRepository jobListingRepository;
    @Mock BidRepository bidRepository;
    @Mock JobListingMapper jobListingMapper;
    @Mock RestTemplate restTemplate;
    @Mock com.deharri.jlds.events.JobEventPublisher jobEventPublisher;

    @InjectMocks JobListingService service;

    private CreateJobFromOfferRequest.CreateJobFromOfferRequestBuilder baseBuilder() {
        return CreateJobFromOfferRequest.builder()
                .title("Fix the sink")
                .description("Kitchen sink leaks")
                .workerType(com.deharri.jlds.enums.WorkerType.PLUMBER)
                .city(com.deharri.jlds.enums.PakistanCity.LAHORE)
                .budgetAmount(new BigDecimal("5000"))
                .consumerId(UUID.randomUUID())
                .consumerUsername("a_customer");
    }

    @Test
    @DisplayName("Both workerId and agencyId set → InvalidOperationException")
    void bothPerformersSet_throws() {
        CreateJobFromOfferRequest req = baseBuilder()
                .workerId(UUID.randomUUID()).workerUsername("w")
                .agencyId(UUID.randomUUID()).agencyName("a")
                .build();

        assertThatThrownBy(() -> service.createJobFromOffer(req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Exactly one of");
    }

    @Test
    @DisplayName("Neither performer set → InvalidOperationException")
    void noPerformerSet_throws() {
        CreateJobFromOfferRequest req = baseBuilder().build();

        assertThatThrownBy(() -> service.createJobFromOffer(req))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Exactly one of");
    }

    @Test
    @DisplayName("Agency variant: skips JobBid creation")
    void agencyVariant_skipsBid() {
        CreateJobFromOfferRequest req = baseBuilder()
                .agencyId(UUID.randomUUID()).agencyName("Acme")
                .build();
        when(jobListingRepository.save(any(JobListing.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jobListingMapper.toResponse(any())).thenReturn(new JobListingResponse());

        service.createJobFromOffer(req);

        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmCompletion publishes JobConfirmed event")
    void confirmCompletion_publishesConfirmedEvent() {
        UUID jobId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();

        JobListing listing = new JobListing();
        listing.setJobId(jobId);
        listing.setConsumerId(consumerId);
        listing.setStatus(com.deharri.jlds.enums.JobStatus.COMPLETED);

        when(jobListingRepository.findById(jobId)).thenReturn(java.util.Optional.of(listing));
        when(jobListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jobListingMapper.toResponse(any())).thenReturn(new JobListingResponse());

        service.confirmCompletion(jobId, consumerId);

        verify(jobEventPublisher).publishConfirmed(any(JobListing.class));
    }

    @Test
    @DisplayName("cancelListing publishes JobCancelled event regardless of payment state")
    void cancelListing_publishesCancelledEvent() {
        UUID jobId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();

        JobListing listing = new JobListing();
        listing.setJobId(jobId);
        listing.setConsumerId(consumerId);
        listing.setStatus(com.deharri.jlds.enums.JobStatus.ASSIGNED);

        when(jobListingRepository.findById(jobId)).thenReturn(java.util.Optional.of(listing));
        when(jobListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jobListingMapper.toResponse(any())).thenReturn(new JobListingResponse());

        service.cancelListing(jobId, consumerId, "changed mind");

        verify(jobEventPublisher).publishCancelled(any(JobListing.class));
    }

    @Test
    @DisplayName("cancelListing publishes event even with no prior payment (consumer no-op on payment side)")
    void cancelListing_publishesEventWithNoPriorPayment() {
        UUID jobId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();

        JobListing listing = new JobListing();
        listing.setJobId(jobId);
        listing.setConsumerId(consumerId);
        listing.setStatus(com.deharri.jlds.enums.JobStatus.OPEN);

        when(jobListingRepository.findById(jobId)).thenReturn(java.util.Optional.of(listing));
        when(jobListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jobListingMapper.toResponse(any())).thenReturn(new JobListingResponse());

        service.cancelListing(jobId, consumerId, "changed mind");

        verify(jobEventPublisher).publishCancelled(any(JobListing.class));
    }

    @Test
    @DisplayName("dispatchToWorker rejects when job is IN_PROGRESS")
    void dispatchToWorker_rejectsWhenInProgress() {
        UUID jobId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        JobListing listing = new JobListing();
        listing.setJobId(jobId);
        listing.setAssignedAgencyId(agencyId);
        listing.setStatus(JobStatus.IN_PROGRESS);

        when(jobListingRepository.findById(jobId)).thenReturn(java.util.Optional.of(listing));

        // Spy umsFetchAgencyOwnerUserId via real call — but we can't mock RestTemplate easily here.
        // Instead, directly exercise: status guard fires before any UMS call when status != ASSIGNED.
        // (umsFetchAgencyOwnerUserId is called BEFORE the status check, so we need to stub RestTemplate.)
        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> ownerResp =
                org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "ownerUserId", ownerId.toString(),
                        "subscriptionActive", true));
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.contains("/agencies/internal/" + agencyId),
                org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpMethod.GET),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn((org.springframework.http.ResponseEntity) ownerResp);

        assertThatThrownBy(() -> service.dispatchToWorker(jobId, workerId, ownerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("ASSIGNED");
    }

    @Test
    @DisplayName("dispatchToWorker rejects when a worker is already dispatched")
    void dispatchToWorker_rejectsWhenAlreadyDispatched() {
        UUID jobId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID alreadyDispatched = UUID.randomUUID();

        JobListing listing = new JobListing();
        listing.setJobId(jobId);
        listing.setAssignedAgencyId(agencyId);
        listing.setStatus(JobStatus.ASSIGNED);
        listing.setDispatchedWorkerId(alreadyDispatched);

        when(jobListingRepository.findById(jobId)).thenReturn(java.util.Optional.of(listing));

        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> ownerResp =
                org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "ownerUserId", ownerId.toString(),
                        "subscriptionActive", true));
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.contains("/agencies/internal/" + agencyId),
                org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpMethod.GET),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn((org.springframework.http.ResponseEntity) ownerResp);

        assertThatThrownBy(() -> service.dispatchToWorker(jobId, workerId, ownerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already dispatched");
    }

    @Test
    @DisplayName("confirmCompletion syncs BOTH agency stats and dispatched worker stats for agency jobs")
    void confirmCompletion_syncsAgencyAndDispatchedWorker() {
        UUID jobId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        UUID dispatchedWorkerId = UUID.randomUUID();

        JobListing listing = new JobListing();
        listing.setJobId(jobId);
        listing.setConsumerId(consumerId);
        listing.setStatus(JobStatus.COMPLETED);
        listing.setAssignedAgencyId(agencyId);
        listing.setDispatchedWorkerId(dispatchedWorkerId);

        when(jobListingRepository.findById(jobId)).thenReturn(java.util.Optional.of(listing));
        when(jobListingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jobListingMapper.toResponse(any())).thenReturn(new JobListingResponse());

        service.confirmCompletion(jobId, consumerId);

        // The single event the publisher receives carries both worker and agency IDs;
        // downstream UMS listener fans out the stat sync to both.
        org.mockito.ArgumentCaptor<JobListing> captor = org.mockito.ArgumentCaptor.forClass(JobListing.class);
        verify(jobEventPublisher).publishConfirmed(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getAssignedAgencyId()).isEqualTo(agencyId);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getDispatchedWorkerId()).isEqualTo(dispatchedWorkerId);
    }
}
