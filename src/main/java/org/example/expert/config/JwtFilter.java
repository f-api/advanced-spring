package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        String url = request.getRequestURI();

        if (url.startsWith("/auth")) {
            chain.doFilter(request, response);
            return;
        }

        String bearerJwt = request.getHeader("Authorization");

        if (bearerJwt == null) {
            log.warn("인증 헤더 누락: URI={}", url);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            return;
        }

        String jwt = jwtUtil.substringToken(bearerJwt);

        try {
            // JWT 유효성 검사와 claims 추출
            Claims claims = jwtUtil.extractClaims(jwt);
            if (claims == null) {
                log.warn("Claims 추출 실패: URI={}", url);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
                return;
            }

            UserRole userRole = UserRole.valueOf(claims.get("userRole", String.class));

            request.setAttribute("userId", Long.parseLong(claims.getSubject()));
            request.setAttribute("email", claims.get("email"));
            request.setAttribute("userRole", claims.get("userRole"));

            if (url.startsWith("/admin") && !UserRole.ADMIN.equals(userRole)) {
                log.warn("권한 부족: userId={}, role={}, URI={}", claims.getSubject(), userRole, url);
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
                return;
            }

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료: userId={}, URI={}", e.getClaims().getSubject(), url);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            log.error("JWT 검증 실패 [{}]: URI={}", e.getClass().getSimpleName(), url, e);
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "인증이 필요합니다.");
        } catch (Exception e) {
            log.error("예상치 못한 오류: URI={}", url, e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다.");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.name());
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
