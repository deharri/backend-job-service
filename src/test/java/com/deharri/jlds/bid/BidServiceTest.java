package com.deharri.jlds.bid;

import com.deharri.jlds.bid.dto.request.CreateBidRequest;
import com.deharri.jlds.bid.entity.JobBid;
import com.deharri.jlds.bid.mapper.BidMapper;
import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingRepository;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.listing.entity.JobListing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidService — agency-bid validation")
class BidServiceTest {

    @Mock BidRepository bidRepository;
    @Mock BidMapper bidMapper;
    @Mock JobListingService jobListingService;
    @Mock JobListingRepository jobListingRepository;

    @InjectMocks BidService bidService;

    private UUID jobId;
    private UUID agencyId;
    private UUID agencyOwnerId;
    private UUID strangerId;
    private UUID consumerId;
    private JobListing listing;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        agencyId = UUID.randomUUID();
        agencyOwnerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();
        consumerId = UUID.randomUUID();

        listing = new JobListing();
        listing.setJobId(jobId);
        listing.setConsumerId(consumerId);
        listing.setStatus(JobStatus.OPEN);

        when(jobListingService.findListingOrThrow(jobId)).thenReturn(listing);
    }

    @Test
    @DisplayName("Agency bid by non-owner throws UnauthorizedAccessException")
    void agencyBid_byNonOwner_throws() {
        when(jobListingService.umsFetchAgencyInfo(agencyId))
                .thenReturn(new JobListingService.UmsAgencyInfo(agencyId, agencyOwnerId, true));

        CreateBidRequest req = agencyBidRequest();

        assertThatThrownBy(() -> bidService.placeBid(jobId, req, strangerId, "stranger"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("Only the agency owner");
    }

    @Test
    @DisplayName("Agency bid when subscription inactive throws InvalidOperationException")
    void agencyBid_whenSubscriptionInactive_throws() {
        when(jobListingService.umsFetchAgencyInfo(agencyId))
                .thenReturn(new JobListingService.UmsAgencyInfo(agencyId, agencyOwnerId, false));

        CreateBidRequest req = agencyBidRequest();

        assertThatThrownBy(() -> bidService.placeBid(jobId, req, agencyOwnerId, "owner"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("subscription is inactive");
    }

    @Test
    @DisplayName("Agency bid for unknown agency throws InvalidOperationException")
    void agencyBid_unknownAgency_throws() {
        when(jobListingService.umsFetchAgencyInfo(agencyId)).thenReturn(null);

        CreateBidRequest req = agencyBidRequest();

        assertThatThrownBy(() -> bidService.placeBid(jobId, req, agencyOwnerId, "owner"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Agency not found");
    }

    private CreateBidRequest agencyBidRequest() {
        CreateBidRequest req = new CreateBidRequest();
        req.setAgencyId(agencyId);
        req.setAgencyName("Acme");
        req.setProposedAmount(new BigDecimal("5000"));
        req.setProposedRateType(BudgetType.FIXED);
        req.setCoverMessage("we'll handle it");
        req.setEstimatedDays(2);
        return req;
    }
}
