package org.example.expert.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class AdminApiLogAspect {

    @Pointcut("@annotation(org.example.expert.domain.common.annotation.AdminAspect)")
    public void adminApiLog() {}

    @Around("adminApiLog()")
    public Object logAdminApi(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        Long userId = (Long) request.getAttribute("userId");
        log.info("AOP - Admin API Access: UserId={}, Timestamp={}, URL={}", 
                userId, System.currentTimeMillis(), request.getRequestURI());

        return joinPoint.proceed();
    }
}
