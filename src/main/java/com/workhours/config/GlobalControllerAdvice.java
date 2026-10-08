package com.workhours.config;

import com.workhours.entity.Employee;
import com.workhours.service.EmployeeService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.annotation.Resource;
import java.security.Principal;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Resource
    private EmployeeService employeeService;

    @ModelAttribute
    public void addUserToModel(Principal principal, Model model) {
        if (principal != null) {
            Employee emp = employeeService.getByPhone(principal.getName());
            if (emp != null) {
                model.addAttribute("currentUserName", emp.getName());
                model.addAttribute("currentUserRole", emp.getRole());
            }
        }
    }
}