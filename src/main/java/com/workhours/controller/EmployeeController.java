package com.workhours.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.common.Result;
import com.workhours.dto.WorkLogForm;
import com.workhours.dto.WorkLogQuery;
import com.workhours.entity.Employee;
import com.workhours.entity.EmployeeProject;
import com.workhours.entity.WorkLog;
import com.workhours.service.EmployeeProjectService;
import com.workhours.service.EmployeeService;
import com.workhours.mapper.WorkLogMapper;
import com.workhours.service.WorkLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    @Resource
    private WorkLogService workLogService;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private EmployeeProjectService employeeProjectService;
    @Resource
    private WorkLogMapper workLogMapper;

    private Employee getEmployee(Principal principal) {
        return employeeService.getByPhone(principal.getName());
    }

    // ========== 工时填报 (F-06, F-10) ==========
    @GetMapping("/worklog")
    public String workLogPage(Principal principal, Model model, WorkLogQuery query) {
        Employee emp = getEmployee(principal);
        Page<WorkLog> page = workLogService.pageByEmp(emp.getId(), query);
        model.addAttribute("page", page);
        model.addAttribute("query", query);

        // 获取该员工已授权项目列表
        List<EmployeeProject> authProjects = employeeProjectService.listByEmpId(emp.getId());
        model.addAttribute("authProjects", authProjects);
        return "employee/worklog-list";
    }

    @PostMapping("/worklog")
    public String createWorkLog(Principal principal, WorkLogForm form,
                                @RequestParam(defaultValue = "false") String directSubmit,
                                RedirectAttributes ra) {
        Employee emp = getEmployee(principal);
        if (!"EMPLOYEE".equals(emp.getRole())) {
            ra.addFlashAttribute("err", "只有员工可以填报工时");
            return "redirect:/employee/worklog";
        }
        try {
            WorkLog workLog = workLogService.create(emp.getId(), form);
            if ("true".equals(directSubmit)) {
                workLogService.submit(emp.getId(), workLog.getId());
                ra.addFlashAttribute("msg", "填报并提交成功，等待审批");
            } else {
                ra.addFlashAttribute("msg", "已保存为草稿");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/employee/worklog";
    }

    @PostMapping("/worklog/{id}/edit")
    public String updateWorkLog(Principal principal, @PathVariable Integer id,
                                WorkLogForm form, RedirectAttributes ra) {
        Employee emp = getEmployee(principal);
        if (!"EMPLOYEE".equals(emp.getRole())) {
            ra.addFlashAttribute("err", "只有员工可以修改工时");
            return "redirect:/employee/worklog";
        }
        try {
            workLogService.update(emp.getId(), id, form);
            ra.addFlashAttribute("msg", "修改成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/employee/worklog";
    }

    @GetMapping("/worklog/{id}/delete")
    public String deleteWorkLog(Principal principal, @PathVariable Integer id, RedirectAttributes ra) {
        Employee emp = getEmployee(principal);
        if (!"EMPLOYEE".equals(emp.getRole())) {
            ra.addFlashAttribute("err", "只有员工可以删除工时");
            return "redirect:/employee/worklog";
        }
        try {
            workLogService.delete(emp.getId(), id);
            ra.addFlashAttribute("msg", "删除成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/employee/worklog";
    }

    @GetMapping("/worklog/{id}/submit")
    public String submitWorkLog(Principal principal, @PathVariable Integer id, RedirectAttributes ra) {
        Employee emp = getEmployee(principal);
        if (!"EMPLOYEE".equals(emp.getRole())) {
            ra.addFlashAttribute("err", "只有员工可以提交工时");
            return "redirect:/employee/worklog";
        }
        try {
            workLogService.submit(emp.getId(), id);
            ra.addFlashAttribute("msg", "提交成功，等待审批");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/employee/worklog";
    }

    // ========== 个人工时统计 (F-15) ==========
    @GetMapping("/stats")
    public String stats(Principal principal, Model model,
                        @RequestParam(defaultValue = "0") int year,
                        @RequestParam(defaultValue = "0") int month) {
        Employee emp = getEmployee(principal);
        LocalDate now = LocalDate.now();
        if (year == 0) year = now.getYear();
        if (month == 0) month = now.getMonthValue();

        List<Map<String, Object>> stats = workLogService.statPersonal(emp.getId(), year, month);
        int workDays = workLogMapper.countWorkDays(emp.getId(), year, month);
        model.addAttribute("stats", stats);
        model.addAttribute("workDays", workDays);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        return "employee/worklog-stats";
    }
}