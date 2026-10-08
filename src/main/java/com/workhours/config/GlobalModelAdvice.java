package com.workhours.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Thymeleaf 3.1 默认禁用了 #request / #session 等表达式对象，
 * 因此通过 @ModelAttribute 将当前请求路径注入 Model，供导航高亮使用。
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}