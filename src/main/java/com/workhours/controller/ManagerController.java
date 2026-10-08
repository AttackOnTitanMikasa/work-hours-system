package com.workhours.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.dto.ApprovalForm;
import com.workhours.dto.WorkLogQuery;
import com.workhours.entity.Employee;
import com.workhours.entity.EmployeeProject;
import com.workhours.entity.Project;
import com.workhours.entity.WorkLog;
import com.workhours.mapper.ProjectMapper;
import com.workhours.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    @Resource
    private EmployeeService employeeService;
    @Resource
    private EmployeeProjectService employeeProjectService;
    @Resource
    private ApprovalService approvalService;
    @Resource
    private WorkLogService workLogService;
    @Resource
    private ProjectMapper projectMapper;

    private Employee getManager(Principal principal) {
        return employeeService.getByPhone(principal.getName());
    }

    // ========== 员工项目授权 (F-03, F-04, F-05) ==========
    @GetMapping("/auth")
    public String authPage(Principal principal, Model model,
                           @RequestParam(required = false) Integer empId) {
        Employee manager = getManager(principal);
        List<Employee> employees = employeeProjectService.listDeptEmployees(manager.getId());
        model.addAttribute("employees", employees);
        model.addAttribute("deptProjects",
                projectMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Project>()
                        .eq(Project::getDeptId, manager.getDeptId())
                        .eq(Project::getStatus, 1)));

        if (empId != null) {
            List<EmployeeProject> authProjects = employeeProjectService.listByEmpId(empId);
            model.addAttribute("authProjects", authProjects);
            model.addAttribute("selectedEmpId", empId);
        }
        return "manager/emp-auth";
    }

    @PostMapping("/auth")
    public String authorize(Principal principal,
                            @RequestParam Integer empId,
                            @RequestParam(required = false) List<Integer> projectIds,
                            RedirectAttributes ra) {
        Employee manager = getManager(principal);
        try {
            if (projectIds == null) projectIds = java.util.Collections.emptyList();
            employeeProjectService.authorizeProjects(manager.getId(), empId, projectIds);
            ra.addFlashAttribute("msg", "授权成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/manager/auth?empId=" + empId;
    }

    // ========== 待审批列表 (F-11) ==========
    @GetMapping("/approval")
    public String approvalPage(Principal principal, Model model,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "createTime") String sort) {
        Employee manager = getManager(principal);
        Page<WorkLog> workLogPage = workLogService.pagePendingByDept(manager.getDeptId(), page, 10, sort);
        model.addAttribute("page", workLogPage);
        model.addAttribute("sort", sort);
        return "manager/approval-list";
    }

    // ========== 单条审批 (F-12) ==========
    @PostMapping("/approval/single")
    public String singleApprove(Principal principal,
                                @RequestParam Integer workLogId,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes ra) {
        Employee manager = getManager(principal);
        try {
            approvalService.approve(manager.getId(), workLogId, approved, reason);
            ra.addFlashAttribute("msg", approved ? "已通过" : "已驳回");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/manager/approval";
    }

    // ========== 批量审批 (F-13) ==========
    @PostMapping("/approval/batch")
    public String batchApprove(Principal principal,
                               ApprovalForm form,
                               RedirectAttributes ra) {
        Employee manager = getManager(principal);
        try {
            String error = approvalService.batchApprove(manager.getId(), form.getWorkLogIds(),
                    form.getApproved(), form.getReason());
            if (error != null) {
                ra.addFlashAttribute("err", error);
            } else {
                ra.addFlashAttribute("msg", "批量审批完成");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/manager/approval";
    }

    // ========== 部门工时查看 (F-14) ==========
    @GetMapping("/worklog")
    public String deptWorkLog(Principal principal, Model model, WorkLogQuery query) {
        Employee manager = getManager(principal);
        Page<WorkLog> page = workLogService.pageApprovedByDept(manager.getDeptId(), query);
        model.addAttribute("page", page);
        model.addAttribute("query", query);
        return "manager/dept-worklog";
    }

    // ========== 部门工时统计 (F-16) ==========
    @GetMapping("/stats")
    public String stats(Principal principal, Model model,
                        @RequestParam(defaultValue = "0") int year,
                        @RequestParam(defaultValue = "0") int month) {
        Employee manager = getManager(principal);
        LocalDate now = LocalDate.now();
        if (year == 0) year = now.getYear();
        if (month == 0) month = now.getMonthValue();

        Map<String, Object> stats = workLogService.statDept(manager.getDeptId(), year, month);
        model.addAttribute("stats", stats);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        return "manager/dept-stats";
    }
}