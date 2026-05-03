package com.deharri.jlds.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseEmitterRegistry {

    private static final long TIMEOUT_MS = 30L * 60_000L; // 30 minutes

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> byAgency = new ConcurrentHashMap<>();

    public SseEmitter register(UUID agencyId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        byAgency.computeIfAbsent(agencyId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> {
            CopyOnWriteArrayList<SseEmitter> list = byAgency.get(agencyId);
            if (list != null) list.remove(emitter);
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(t -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("hello").data("connected"));
        } catch (IOException e) {
            log.warn("SSE hello failed for agency {}: {}", agencyId, e.getMessage());
        }
        return emitter;
    }

    public void broadcast(UUID agencyId, String eventName, Object payload) {
        List<SseEmitter> list = byAgency.get(agencyId);
        if (list == null || list.isEmpty()) return;
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException ex) {
                e.completeWithError(ex);
            }
        }
    }
}
