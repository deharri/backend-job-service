package com.deharri.jlds.dev;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <strong>Dev-only.</strong> Wipes every job-service table (listings, bids, reviews,
 * saved jobs, disputes, the worker-profile cache). For the seed-data tooling.
 */
@RestController
@RequestMapping("/api/v1/dev")
@Slf4j
public class DevWipeController {

    @PersistenceContext
    private EntityManager em;

    @DeleteMapping("/wipe")
    @Transactional
    public ResponseEntity<Map<String, Object>> wipe() {
        log.warn("DEV WIPE: clearing all job-service tables");
        em.createNativeQuery(
                "DO $$ DECLARE r RECORD; " +
                "BEGIN FOR r IN " +
                "  SELECT tablename FROM pg_tables " +
                "  WHERE schemaname = 'public' " +
                "LOOP " +
                "  EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE'; " +
                "END LOOP; END $$;"
        ).executeUpdate();
        return ResponseEntity.ok(Map.of("service", "job-service", "wiped", true));
    }
}
