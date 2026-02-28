package com.deharri.jlds.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String REQUEST_ID_KEY = "requestId";

    @Pointcut("execution(* com.deharri.jlds..listing..*(..)) || " +
              "execution(* com.deharri.jlds..bid..*(..)) || " +
              "execution(* com.deharri.jlds..review..*(..)) || " +
              "execution(* com.deharri.jlds..worker..*(..)) || " +
              "execution(* com.deharri.jlds..saved..*(..)) || " +
              "execution(* com.deharri.jlds..media..*(..))")
    public void applicationPackagePointcut() {
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {
    }

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void servicePointcut() {
    }

    @Around("controllerPointcut()")
    public Object logControllerCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_KEY, correlationId);
        MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString().substring(0, 8));

        String methodName = joinPoint.getSignature().toShortString();

        log.info("▶ REQUEST  | {} {} | Method: {} | Args: {}",
                request.getMethod(),
                request.getRequestURI(),
                methodName,
                maskSensitiveData(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("◀ RESPONSE | {} {} | Duration: {}ms | Status: SUCCESS",
                    request.getMethod(),
                    request.getRequestURI(),
                    duration);

            if (duration > 2000) {
                log.warn("⚠ SLOW REQUEST | {} {} | Duration: {}ms exceeded threshold",
                        request.getMethod(),
                        request.getRequestURI(),
                        duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✖ RESPONSE | {} {} | Duration: {}ms | Status: ERROR | Exception: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    duration,
                    e.getMessage());
            throw e;
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
            MDC.remove(REQUEST_ID_KEY);
        }
    }

    @Before("servicePointcut() && applicationPackagePointcut()")
    public void logServiceMethodEntry(JoinPoint joinPoint) {
        if (log.isDebugEnabled()) {
            log.debug("→ ENTERING | {} | Args: {}",
                    joinPoint.getSignature().toShortString(),
                    maskSensitiveData(joinPoint.getArgs()));
        }
    }

    @AfterReturning(pointcut = "servicePointcut() && applicationPackagePointcut()", returning = "result")
    public void logServiceMethodExit(JoinPoint joinPoint, Object result) {
        if (log.isDebugEnabled()) {
            log.debug("← EXITING  | {}",
                    joinPoint.getSignature().toShortString());
        }
    }

    @AfterThrowing(pointcut = "applicationPackagePointcut()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        log.error("✖ EXCEPTION | {} | Type: {} | Message: {}",
                joinPoint.getSignature().toShortString(),
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }

    private String maskSensitiveData(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return "null";
                    if (arg instanceof HttpServletRequest) return "[HttpServletRequest]";
                    return arg.toString();
                })
                .toList()
                .toString();
    }
}
