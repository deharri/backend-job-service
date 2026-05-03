package com.deharri.jlds.notifications;

import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.notifications.entity.AgencyNotification.Kind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/notifications")
@RequiredArgsConstructor
@Tag(name = "Internal — notifications", description = "Service-to-service notification callbacks")
public class InternalNotificationController {

    private final AgencyNotificationService service;

    @Value("${jlds.internal.shared-secret:internal-secret-change-me}")
    private String sharedSecret;

    @PostMapping("/payment-event")
    @Operation(summary = "Internal: payment-service publishes a payment lifecycle event for the agency")
    public ResponseEntity<Void> paymentEvent(
            @RequestBody Map<String, Object> body,
            HttpServletRequest req) {
        String s = req.getHeader("X-Internal-Secret");
        if (s == null || !s.equals(sharedSecret)) {
            throw new UnauthorizedAccessException("Bad internal secret");
        }
        UUID agencyId = UUID.fromString(body.get("agencyId").toString());
        UUID jobId = body.get("jobId") == null ? null : UUID.fromString(body.get("jobId").toString());
        Kind kind = Kind.valueOf(body.get("kind").toString());
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(body.get("amount").toString());
        String currency = body.get("currency") == null ? "PKR" : body.get("currency").toString();

        String title = kind == Kind.PAYMENT_RELEASED
                ? "Payment released to your agency"
                : "Payment refunded";
        String summary = amount == null
                ? null
                : currency + " " + amount.toPlainString();
        service.publish(agencyId, kind, jobId, title, summary);
        return ResponseEntity.noContent().build();
    }
}
