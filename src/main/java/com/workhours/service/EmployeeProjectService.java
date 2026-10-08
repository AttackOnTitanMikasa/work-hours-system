package com.workhours.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workhours.common.BizException;
import com.workhours.entity.Employee;
import com.workhours.entity.EmployeeProject;
import com.workhours.entity.Project;
import com.workhours.mapper.EmployeeMapper;
import com.workhours.mapper.EmployeeProjectMapper;
import com.workhours.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeProjectService {

    @Resource
    private EmployeeProjectMapper employeeProjectMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private ProjectMapper projectMapper;

    /**
     * 获取某员工已授权项目列表
     */
    public List<EmployeeProject> listByEmpId(Integer empId) {
        return employeeProjectMapper.selectByEmpId(empId);
    }

    /**
     * 经理为员工授权项目（多选覆盖）
     */
    @Transactional
    public void authorizeProjects(Integer managerId, Integer empId, List<Integer> projectIds) {
        Employee manager = employeeMapper.selectById(managerId);
        Employee employee = employeeMapper.selectById(empId);

        if (manager == null || employee == null) {
            throw new BizException("经理或员工不存在");
        }

        // BR-08: 经理只能为本部门员工分配本部门项目
        if (!manager.getDeptId().equals(employee.getDeptId())) {
            throw new BizException("只能为本部门员工授权项目");
        }

        // 验证所有项目都属于本部门
        for (Integer projectId : projectIds) {
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                throw new BizException("项目(ID=" + projectId + ")不存在");
            }
            if (!project.getDeptId().equals(manager.getDeptId())) {
                throw new BizException("项目「" + project.getName() + "」不属于本部门，无法授权");
            }
        }

        // 先删除旧的授权，再插入新的
        employeeProjectMapper.deleteByEmpId(empId);
        for (Integer projectId : projectIds) {
            EmployeeProject ep = new EmployeeProject();
            ep.setEmpId(empId);
            ep.setProjectId(projectId);
            employeeProjectMapper.insert(ep);
        }
    }

    /**
     * 获取本部门员工列表（经理端用）
     */
    public List<Employee> listDeptEmployees(Integer managerId) {
        Employee manager = employeeMapper.selectById(managerId);
        if (manager == null) {
            throw new BizException("经理不存在");
        }
        return employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getDeptId, manager.getDeptId())
                        .ne(Employee::getId, managerId)  // 排除自己
                        .orderByAsc(Employee::getName));
    }
}