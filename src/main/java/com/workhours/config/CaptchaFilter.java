package com.workhours.config;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 验证码校验过滤器 — 在 Spring Security 认证前拦截 POST /login
 */
public class CaptchaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 只拦截登录请求
        if ("/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            HttpSession session = request.getSession(false);
            if (session == null) {
                response.sendRedirect("/login?captchaError");
                return;
            }

            Integer expectedAnswer = (Integer) session.getAttribute("captcha_answer");
            String userInput = request.getParameter("captcha");

            if (expectedAnswer == null) {
                response.sendRedirect("/login?captchaError");
                return;
            }

            // 校验后立即清除，防止重用
            session.removeAttribute("captcha_answer");
            session.removeAttribute("captcha_question");

            if (userInput == null || userInput.trim().isEmpty()) {
                response.sendRedirect("/login?captchaError");
                return;
            }

            try {
                int answer = Integer.parseInt(userInput.trim());
                if (answer != expectedAnswer) {
                    response.sendRedirect("/login?captchaError");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendRedirect("/login?captchaError");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}