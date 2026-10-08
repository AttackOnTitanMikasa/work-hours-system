package com.workhours.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.common.BizException;
import com.workhours.entity.Department;
import com.workhours.entity.Employee;
import com.workhours.entity.Project;
import com.workhours.mapper.DepartmentMapper;
import com.workhours.mapper.EmployeeMapper;
import com.workhours.mapper.ProjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class DepartmentService {

    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    public Page<Department> page(int pageNum, int pageSize) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Department::getId);
        return departmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public Department getById(Integer id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BizException("部门不存在");
        }
        return dept;
    }

    public void add(Department department) {
        // 编码唯一校验（包含已删除记录，因为数据库唯一约束包含所有记录）
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM department WHERE code = ?",
                Integer.class, department.getCode());
        if (count != null && count > 0) {
            throw new BizException("部门编码已存在");
        }
        departmentMapper.insert(department);
    }

    public void update(Department department) {
        Department exist = getById(department.getId());
        // 编码唯一校验（排除自身）
        Long count = departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getCode, department.getCode())
                        .ne(Department::getId, department.getId()));
        if (count > 0) {
            throw new BizException("部门编码已存在");
        }
        departmentMapper.updateById(department);
    }

    public void delete(Integer id) {
        getById(id);
        // 检查部门下是否有员工
        Long empCount = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getDeptId, id)
                        .eq(Employee::getIsDeleted, 0));
        if (empCount > 0) {
            throw new BizException("该部门下有员工，无法删除");
        }
        // 检查部门下是否有项目
        Long projCount = projectMapper.selectCount(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getDeptId, id)
                        .eq(Project::getIsDeleted, 0));
        if (projCount > 0) {
            throw new BizException("该部门下有项目，无法删除");
        }
        departmentMapper.deleteById(id);  // MyBatis-Plus 逻辑删除
    }
}