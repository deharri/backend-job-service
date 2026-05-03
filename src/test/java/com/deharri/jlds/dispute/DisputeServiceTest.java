package com.deharri.jlds.dispute;

import com.deharri.jlds.dispute.dto.request.SubmitDisputeRequest;
import com.deharri.jlds.dispute.entity.Dispute;
import com.deharri.jlds.dispute.entity.Dispute.DisputeCategory;
import com.deharri.jlds.dispute.entity.Dispute.RaisedByRole;
import com.deharri.jlds.dispute.mapper.DisputeMapper;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingRepository;
import com.deharri.jlds.listing.entity.JobListing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private JobListingRepository jobListingRepository;
    @Mock private DisputeMapper mapper;

    @InjectMocks private DisputeService disputeService;

    private UUID jobId;
    private UUID consumerId;
    private UUID assignedWorkerId;
    private UUID dispatchedWorkerId;
    private UUID strangerId;
    private JobListing listing;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        consumerId = UUID.randomUUID();
        assignedWorkerId = UUID.randomUUID();
        dispatchedWorkerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();

        listing = new JobListing();
        listing.setJobId(jobId);
        listing.setConsumerId(consumerId);
        listing.setAssignedWorkerId(assignedWorkerId);
        listing.setDispatchedWorkerId(dispatchedWorkerId);
    }

    @Test
    void submitDispute_asConsumer_marksRoleConsumer() {
        SubmitDisputeRequest req = newReq();
        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(listing));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        disputeService.submitDispute(req, consumerId);

        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        Mockito.verify(disputeRepository).save(captor.capture());
        assertThat(captor.getValue().getRaisedByRole()).isEqualTo(RaisedByRole.CONSUMER);
        assertThat(captor.getValue().getRaisedBy()).isEqualTo(consumerId);
    }

    @Test
    void submitDispute_asAssignedWorker_marksRoleWorker() {
        SubmitDisputeRequest req = newReq();
        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(listing));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        disputeService.submitDispute(req, assignedWorkerId);

        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        Mockito.verify(disputeRepository).save(captor.capture());
        assertThat(captor.getValue().getRaisedByRole()).isEqualTo(RaisedByRole.WORKER);
    }

    @Test
    void submitDispute_asDispatchedWorker_marksRoleWorker() {
        SubmitDisputeRequest req = newReq();
        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(listing));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        disputeService.submitDispute(req, dispatchedWorkerId);

        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        Mockito.verify(disputeRepository).save(captor.capture());
        assertThat(captor.getValue().getRaisedByRole()).isEqualTo(RaisedByRole.WORKER);
    }

    @Test
    void submitDispute_byNonParticipant_throwsUnauthorized() {
        SubmitDisputeRequest req = newReq();
        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> disputeService.submitDispute(req, strangerId))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void submitDispute_jobNotFound_throws() {
        SubmitDisputeRequest req = newReq();
        when(jobListingRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disputeService.submitDispute(req, consumerId))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void getDispute_byNonRaiser_throwsUnauthorized() {
        UUID disputeId = UUID.randomUUID();
        Dispute d = Dispute.builder()
                .disputeId(disputeId).jobId(jobId)
                .raisedBy(consumerId).raisedByRole(RaisedByRole.CONSUMER)
                .category(DisputeCategory.PAYMENT)
                .subject("x").description("y")
                .status(Dispute.DisputeStatus.OPEN)
                .build();
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> disputeService.getDispute(disputeId, strangerId))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    private SubmitDisputeRequest newReq() {
        SubmitDisputeRequest req = new SubmitDisputeRequest();
        req.setJobId(jobId);
        req.setCategory(DisputeCategory.PAYMENT);
        req.setSubject("Worker did not arrive");
        req.setDescription("Booked for 9am, never showed.");
        return req;
    }
}
