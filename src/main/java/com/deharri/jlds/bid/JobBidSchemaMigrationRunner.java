package com.deharri.jlds.bid;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobBidSchemaMigrationRunner implements ApplicationRunner {

    private final EntityManager entityManager;

    @Value("${jobbid.relax-worker-not-null:false}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        log.warn("JOB_BID_RELAX_WORKER_NOT_NULL enabled — dropping NOT NULL on worker_id and worker_username");
        entityManager.createNativeQuery("ALTER TABLE job_bids ALTER COLUMN worker_id DROP NOT NULL").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE job_bids ALTER COLUMN worker_username DROP NOT NULL").executeUpdate();
        log.warn("worker_id/worker_username NOT NULL dropped. Disable JOB_BID_RELAX_WORKER_NOT_NULL for subsequent boots.");
    }
}
