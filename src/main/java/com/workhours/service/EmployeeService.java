package com.workhours.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.common.BizException;
import com.workhours.entity.Employee;
import com.workhours.mapper.EmployeeMapper;
import com.workhours.mapper.EmployeeProjectMapper;
import com.workhours.mapper.WorkLogMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

@Service
public class EmployeeService {

    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeProjectMapper employeeProjectMapper;
    @Resource
    private WorkLogMapper workLogMapper;
    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    public Page<Employee> page(int pageNum, int pageSize, Integer deptId, String role) {
        return employeeMapper.selectPageWithDept(new Page<>(pageNum, pageSize), deptId, role);
    }

    public Employee getById(Integer id) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw new BizException("员工不存在");
        }
        return emp;
    }

    public Employee getByPhone(String phone) {
        return employeeMapper.selectByPhone(phone);
    }

    public void add(Employee employee) {
        // 手机号唯一校验
        Long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getPhone, employee.getPhone()));
        if (count > 0) {
            throw new BizException("手机号已存在");
        }
        // 密码为空时使用默认密码 123456
        String pwd = employee.getPassword();
        if (pwd == null || pwd.trim().isEmpty()) {
            pwd = "123456";
        }
        employee.setPassword(passwordEncoder.encode(pwd));
        employeeMapper.insert(employee);
    }

    public void update(Employee employee) {
        Employee exist = getById(employee.getId());
        // 手机号唯一校验
        Long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getPhone, employee.getPhone())
                        .ne(Employee::getId, employee.getId()));
        if (count > 0) {
            throw new BizException("手机号已存在");
        }
        // 如果密码为空，保留原密码
        if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
            employee.setPassword(exist.getPassword());
        } else {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        }
        employeeMapper.updateById(employee);
    }

    @Transactional
    public void delete(Integer id) {
        getById(id);
        // BR-09: 有工时记录时禁止删除
        Long workLogCount = workLogMapper.selectCount(
                new LambdaQueryWrapper<com.workhours.entity.WorkLog>()
                        .eq(com.workhours.entity.WorkLog::getEmpId, id));
        if (workLogCount > 0) {
            throw new BizException("该员工存在工时记录，无法删除");
        }
        // 删除项目授权
        employeeProjectMapper.deleteByEmpId(id);
        // 删除员工
        employeeMapper.deleteById(id);
    }
}