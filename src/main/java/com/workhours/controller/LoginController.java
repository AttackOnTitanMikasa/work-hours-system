package com.workhours.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.dto.WorkLogQuery;
import com.workhours.entity.Department;
import com.workhours.entity.Employee;
import com.workhours.entity.EmployeeProject;
import com.workhours.entity.Project;
import com.workhours.entity.WorkLog;
import com.workhours.mapper.DepartmentMapper;
import com.workhours.mapper.EmployeeMapper;
import com.workhours.mapper.ProjectMapper;
import com.workhours.mapper.WorkLogMapper;
import com.workhours.service.EmployeeProjectService;
import com.workhours.service.EmployeeService;
import com.workhours.service.WorkLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class LoginController {

    @Resource
    private EmployeeService employeeService;
    @Resource
    private WorkLogService workLogService;
    @Resource
    private EmployeeProjectService employeeProjectService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private WorkLogMapper workLogMapper;
    @Resource
    private ProjectMapper projectMapper;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * 生成算式验证码，答案存 session，返回算式字符串
     */
    @GetMapping("/captcha")
    @ResponseBody
    public String captcha(HttpSession session) {
        Random rand = new Random();
        int a = rand.nextInt(20) + 1;   // 1~20
        int b = rand.nextInt(20) + 1;   // 1~20
        int answer = a + b;
        session.setAttribute("captcha_answer", answer);
        session.setAttribute("captcha_question", a + " + " + b + " = ?");
        return a + " + " + b + " = ?";
    }

    @GetMapping("/")
    public String index(Principal principal, Model model) {
        return buildDashboard(principal, model);
    }

    @GetMapping("/index")
    public String home(Principal principal, Model model) {
        return buildDashboard(principal, model);
    }

    private String buildDashboard(Principal principal, Model model) {
        Employee emp = employeeService.getByPhone(principal.getName());
        model.addAttribute("user", emp);

        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        // ============ 管理员仪表盘 ============
        if ("ADMIN".equals(emp.getRole())) {
            // 员工总数
            long empCount = employeeMapper.selectCount(
                    new LambdaQueryWrapper<Employee>().eq(Employee::getIsDeleted, 0));
            model.addAttribute("empCount", empCount);

            // 部门总数
            long deptCount = departmentMapper.selectCount(
                    new LambdaQueryWrapper<Department>().eq(Department::getIsDeleted, 0));
            model.addAttribute("deptCount", deptCount);

            // 项目总数
            long projectTotal = projectMapper.selectCount(
                    new LambdaQueryWrapper<Project>().eq(Project::getIsDeleted, 0));
            model.addAttribute("projectCount", projectTotal);

            // 本月全局已审批工时
            Map<String, Object> globalStats = workLogMapper.statGlobal(year, month);
            double totalHours = globalStats.get("total_hours") != null ?
                    ((Number) globalStats.get("total_hours")).doubleValue() : 0;
            model.addAttribute("monthHours", String.format("%.1f", totalHours));

            // 全局待审批数
            model.addAttribute("pendingCount", workLogMapper.countPending());

            // 最近 5 条全局工时记录
            WorkLogQuery recentQuery = new WorkLogQuery();
            recentQuery.setSize(5);
            Page<WorkLog> recentPage = workLogMapper.selectPageWithDetail(
                    new Page<>(1, 5), null, null, null, null, null, null, "w.create_time");
            model.addAttribute("recentLogs", recentPage.getRecords());

        } else if ("MANAGER".equals(emp.getRole())) {
            // ============ 经理仪表盘 ============
            // 部门员工数
            long deptEmpCount = employeeMapper.selectCount(
                    new LambdaQueryWrapper<Employee>()
                            .eq(Employee::getDeptId, emp.getDeptId())
                            .eq(Employee::getIsDeleted, 0));
            model.addAttribute("deptEmpCount", deptEmpCount);

            // 部门项目数
            long deptProjectCount = projectMapper.selectCount(
                    new LambdaQueryWrapper<Project>()
                            .eq(Project::getDeptId, emp.getDeptId())
                            .eq(Project::getIsDeleted, 0));
            model.addAttribute("deptProjectCount", deptProjectCount);

            // 部门待审批数
            Page<WorkLog> pendingPage = workLogService.pagePendingByDept(emp.getDeptId(), 1, 1000, "createTime");
            model.addAttribute("pendingCount", pendingPage.getTotal());

            // 部门本月总工时
            Map<String, Object> deptStats = workLogMapper.statDeptSummary(emp.getDeptId(), year, month);
            double deptHours = deptStats.get("total_hours") != null ?
                    ((Number) deptStats.get("total_hours")).doubleValue() : 0;
            model.addAttribute("deptMonthHours", String.format("%.1f", deptHours));

            // 最近 5 条个人工时
            WorkLogQuery recentQuery = new WorkLogQuery();
            recentQuery.setSize(5);
            Page<WorkLog> recentPage = workLogService.pageByEmp(emp.getId(), recentQuery);
            model.addAttribute("recentLogs", recentPage.getRecords());

        } else {
            // ============ 员工仪表盘 ============
            // 本月工时
            List<Map<String, Object>> monthStats = workLogService.statPersonal(
                    emp.getId(), year, month);
            double monthHours = monthStats.stream()
                    .mapToDouble(m -> ((Number) m.get("total_hours")).doubleValue()).sum();
            model.addAttribute("monthHours", String.format("%.1f", monthHours));

            // 参与项目数
            List<EmployeeProject> authProjects = employeeProjectService.listByEmpId(emp.getId());
            model.addAttribute("projectCount", authProjects.size());

            // 今日工时
            WorkLogQuery todayQuery = new WorkLogQuery();
            todayQuery.setStartDate(today);
            todayQuery.setEndDate(today);
            todayQuery.setSize(100);
            Page<WorkLog> todayPage = workLogService.pageByEmp(emp.getId(), todayQuery);
            double todayHours = todayPage.getRecords().stream()
                    .mapToDouble(w -> w.getHours().doubleValue()).sum();
            model.addAttribute("todayHours", String.format("%.1f", todayHours));

            // 最近 5 条工时记录
            WorkLogQuery recentQuery = new WorkLogQuery();
            recentQuery.setSize(5);
            Page<WorkLog> recentPage = workLogService.pageByEmp(emp.getId(), recentQuery);
            model.addAttribute("recentLogs", recentPage.getRecords());
        }

        return "index";
    }
}