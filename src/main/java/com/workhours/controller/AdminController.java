package com.workhours.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.dto.WorkLogQuery;
import com.workhours.entity.Department;
import com.workhours.entity.Employee;
import com.workhours.entity.OperationLog;
import com.workhours.entity.Project;
import com.workhours.entity.WorkLog;
import com.workhours.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private EmployeeService employeeService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private ProjectService projectService;
    @Resource
    private WorkLogService workLogService;
    @Resource
    private OperationLogService operationLogService;

    // ========== 员工管理 ==========
    @GetMapping("/employees")
    public String listEmployees(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(required = false) Integer deptId,
                                @RequestParam(required = false) String role,
                                Model model) {
        Page<Employee> empPage = employeeService.page(page, 10, deptId, role);
        model.addAttribute("page", empPage);
        model.addAttribute("deptList", departmentService.page(1, 100).getRecords());
        model.addAttribute("deptId", deptId);
        model.addAttribute("role", role);
        return "admin/emp-list";
    }

    @PostMapping("/employees")
    public String addEmployee(Principal principal, Employee employee, RedirectAttributes ra, HttpServletRequest request) {
        try {
            employeeService.add(employee);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "员工管理", "新增", "新增员工：" + employee.getName() + "（" + employee.getPhone() + "）", request);
            ra.addFlashAttribute("msg", "添加成功");
        } catch (com.workhours.common.BizException e) {
            ra.addFlashAttribute("err", e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("UQ__employee")) {
                msg = "手机号已存在，请使用其他手机号";
            }
            ra.addFlashAttribute("err", msg);
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/{id}/edit")
    public String updateEmployee(Principal principal, @PathVariable Integer id, Employee employee,
                                 RedirectAttributes ra, HttpServletRequest request) {
        try {
            employee.setId(id);
            employeeService.update(employee);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "员工管理", "修改", "修改员工ID：" + id + "（" + employee.getName() + "）", request);
            ra.addFlashAttribute("msg", "修改成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @GetMapping("/employees/{id}/delete")
    public String deleteEmployee(Principal principal, @PathVariable Integer id,
                                 RedirectAttributes ra, HttpServletRequest request) {
        try {
            Employee emp = employeeService.getById(id);
            employeeService.delete(id);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "员工管理", "删除", "删除员工：" + emp.getName() + "（ID=" + id + "）", request);
            ra.addFlashAttribute("msg", "删除成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    // ========== 部门管理 ==========
    @GetMapping("/departments")
    public String listDepartments(@RequestParam(defaultValue = "1") int page, Model model) {
        Page<Department> deptPage = departmentService.page(page, 10);
        model.addAttribute("page", deptPage);
        return "admin/dept-list";
    }

    @PostMapping("/departments")
    public String addDepartment(Principal principal, Department department,
                                RedirectAttributes ra, HttpServletRequest request) {
        try {
            departmentService.add(department);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "部门管理", "新增", "新增部门：" + department.getName() + "（" + department.getCode() + "）", request);
            ra.addFlashAttribute("msg", "添加成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/edit")
    public String updateDepartment(Principal principal, @PathVariable Integer id,
                                   Department department, RedirectAttributes ra, HttpServletRequest request) {
        try {
            department.setId(id);
            departmentService.update(department);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "部门管理", "修改", "修改部门ID：" + id + "（" + department.getName() + "）", request);
            ra.addFlashAttribute("msg", "修改成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @GetMapping("/departments/{id}/delete")
    public String deleteDepartment(Principal principal, @PathVariable Integer id,
                                   RedirectAttributes ra, HttpServletRequest request) {
        try {
            Department dept = departmentService.getById(id);
            departmentService.delete(id);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "部门管理", "删除", "删除部门：" + dept.getName() + "（ID=" + id + "）", request);
            ra.addFlashAttribute("msg", "删除成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    // ========== 项目管理 ==========
    @GetMapping("/projects")
    public String listProjects(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(required = false) Integer deptId,
                               @RequestParam(required = false) Integer status,
                               Model model) {
        Page<Project> projPage = projectService.page(page, 10, deptId, status);
        model.addAttribute("page", projPage);
        model.addAttribute("deptList", departmentService.page(1, 100).getRecords());
        model.addAttribute("deptId", deptId);
        model.addAttribute("status", status);
        return "admin/project-list";
    }

    @PostMapping("/projects")
    public String addProject(Principal principal, Project project,
                             RedirectAttributes ra, HttpServletRequest request) {
        try {
            projectService.add(project);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "项目管理", "新增", "新增项目：" + project.getName(), request);
            ra.addFlashAttribute("msg", "添加成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/projects";
    }

    @PostMapping("/projects/{id}/edit")
    public String updateProject(Principal principal, @PathVariable Integer id,
                                Project project, RedirectAttributes ra, HttpServletRequest request) {
        try {
            project.setId(id);
            projectService.update(project);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "项目管理", "修改", "修改项目ID：" + id + "（" + project.getName() + "）", request);
            ra.addFlashAttribute("msg", "修改成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/projects";
    }

    @GetMapping("/projects/{id}/delete")
    public String deleteProject(Principal principal, @PathVariable Integer id,
                                RedirectAttributes ra, HttpServletRequest request) {
        try {
            Project proj = projectService.getById(id);
            projectService.delete(id);
            Employee admin = employeeService.getByPhone(principal.getName());
            operationLogService.log(admin.getId(), admin.getName(), admin.getRole(),
                    "项目管理", "删除", "删除项目：" + proj.getName() + "（ID=" + id + "）", request);
            ra.addFlashAttribute("msg", "删除成功");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/admin/projects";
    }

    // ========== 员工工时查看 ==========
    @GetMapping("/worklog")
    public String viewWorkLogs(Principal principal, Model model, WorkLogQuery query,
                               @RequestParam(required = false) Integer deptId) {
        Page<WorkLog> page = workLogService.pageAllByDept(query, deptId);
        model.addAttribute("page", page);
        model.addAttribute("query", query);
        model.addAttribute("deptId", deptId);
        model.addAttribute("deptList", departmentService.page(1, 100).getRecords());
        return "admin/worklog-list";
    }

    // ========== 操作日志 ==========
    @GetMapping("/logs")
    public String viewLogs(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(required = false) String module,
                           @RequestParam(required = false) String action,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           Model model) {
        Page<OperationLog> logPage = operationLogService.page(page, 20, module, action, startDate, endDate);
        model.addAttribute("page", logPage);
        model.addAttribute("module", module);
        model.addAttribute("action", action);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/operation-log";
    }
}