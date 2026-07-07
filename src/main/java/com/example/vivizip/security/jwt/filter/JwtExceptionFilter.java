package com.example.vivizip.security.jwt.filter;

import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.common.exception.Reason;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            Reason reason = e.getErrorReasonHttpStatus();
            log.warn("[JWT Filter] 인증 오류: [{}] {}", reason.getCode(), reason.getMessage());
            writeErrorResponse(response, reason.getHttpStatus().value(), reason.getCode(), reason.getMessage());
        } catch (RuntimeException e) {
            log.warn("[JWT Filter] 예상치 못한 오류: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 4001, e.getMessage());
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int httpStatus, Integer code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("isSuccess", false);
        body.put("code", code);
        body.put("message", message);
        body.put("result", null);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
