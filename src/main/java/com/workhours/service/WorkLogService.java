package com.workhours.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.common.BizException;
import com.workhours.dto.WorkLogForm;
import com.workhours.dto.WorkLogQuery;
import com.workhours.entity.Employee;
import com.workhours.entity.EmployeeProject;
import com.workhours.entity.WorkLog;
import com.workhours.mapper.EmployeeMapper;
import com.workhours.mapper.EmployeeProjectMapper;
import com.workhours.mapper.WorkLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class WorkLogService {

    @Resource
    private WorkLogMapper workLogMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeProjectMapper employeeProjectMapper;

    /**
     * 填报工时
     */
    @Transactional
    public WorkLog create(Integer empId, WorkLogForm form) {
        Employee employee = employeeMapper.selectById(empId);

        // BR-03: 0.5 ~ 24 小时
        BigDecimal hours = form.getHours();
        if (hours == null) {
            throw new BizException("请填写工时");
        }
        if (form.getWorkDate() == null) {
            throw new BizException("请选择工作日期");
        }
        if (form.getProjectId() == null) {
            throw new BizException("请选择项目");
        }
        if (hours.compareTo(new BigDecimal("0.5")) < 0 || hours.compareTo(new BigDecimal("24")) > 0) {
            throw new BizException("工时范围: 0.5 ~ 24 小时");
        }

        // 精度 0.5
        BigDecimal remainder = hours.remainder(new BigDecimal("0.5"));
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new BizException("工时精度为 0.5 小时");
        }

        // BR-07: 只能填报已授权项目
        List<EmployeeProject> authProjects = employeeProjectMapper.selectByEmpId(empId);
        boolean authorized = authProjects.stream()
                .anyMatch(ep -> ep.getProjectId().equals(form.getProjectId()));
        if (!authorized) {
            throw new BizException("该项目未授权，无法填报工时");
        }

        // BR-01: 同员工+同项目+同日期唯一
        Long existCount = workLogMapper.selectCount(
                new LambdaQueryWrapper<WorkLog>()
                        .eq(WorkLog::getEmpId, empId)
                        .eq(WorkLog::getProjectId, form.getProjectId())
                        .eq(WorkLog::getWorkDate, form.getWorkDate()));
        if (existCount > 0) {
            throw new BizException("该日期该项目已有工时记录");
        }

        WorkLog workLog = new WorkLog();
        workLog.setEmpId(empId);
        workLog.setProjectId(form.getProjectId());
        workLog.setWorkDate(form.getWorkDate());
        workLog.setHours(hours);
        workLog.setDescription(form.getDescription());
        workLog.setStatus("DRAFT");
        workLog.setCreateTime(LocalDateTime.now());
        workLog.setUpdateTime(LocalDateTime.now());

        workLogMapper.insert(workLog);
        return workLog;
    }

    /**
     * 修改工时（仅草稿/驳回）
     */
    public void update(Integer empId, Integer workLogId, WorkLogForm form) {
        WorkLog exist = workLogMapper.selectById(workLogId);
        if (exist == null || !exist.getEmpId().equals(empId)) {
            throw new BizException("工时记录不存在");
        }
        // BR-04: 仅允许修改草稿/驳回
        if (!"DRAFT".equals(exist.getStatus()) && !"REJECTED".equals(exist.getStatus())) {
            throw new BizException("只能修改草稿或被驳回的工时");
        }

        // BR-03
        BigDecimal hours = form.getHours();
        if (hours == null) {
            throw new BizException("请填写工时");
        }
        if (form.getWorkDate() == null) {
            throw new BizException("请选择工作日期");
        }
        if (form.getProjectId() == null) {
            throw new BizException("请选择项目");
        }
        if (hours.compareTo(new BigDecimal("0.5")) < 0 || hours.compareTo(new BigDecimal("24")) > 0) {
            throw new BizException("工时范围: 0.5 ~ 24 小时");
        }
        BigDecimal remainder = hours.remainder(new BigDecimal("0.5"));
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new BizException("工时精度为 0.5 小时");
        }

        // BR-07: 只能填报已授权项目（修改时同样校验）
        boolean authorized = employeeProjectMapper.selectByEmpId(empId).stream()
                .anyMatch(ep -> ep.getProjectId().equals(form.getProjectId()));
        if (!authorized) {
            throw new BizException("该项目未授权，无法填报工时");
        }

        // BR-01 排除自身
        Long existCount = workLogMapper.selectCount(
                new LambdaQueryWrapper<WorkLog>()
                        .eq(WorkLog::getEmpId, empId)
                        .eq(WorkLog::getProjectId, form.getProjectId())
                        .eq(WorkLog::getWorkDate, form.getWorkDate())
                        .ne(WorkLog::getId, workLogId));
        if (existCount > 0) {
            throw new BizException("该日期该项目已有工时记录");
        }

        exist.setProjectId(form.getProjectId());
        exist.setWorkDate(form.getWorkDate());
        exist.setHours(hours);
        exist.setDescription(form.getDescription());
        exist.setStatus("DRAFT"); // BR-04: 修改后重回草稿
        exist.setUpdateTime(LocalDateTime.now());

        workLogMapper.updateById(exist);
    }

    /**
     * 删除工时（仅草稿/驳回）
     */
    public void delete(Integer empId, Integer workLogId) {
        WorkLog exist = workLogMapper.selectById(workLogId);
        if (exist == null || !exist.getEmpId().equals(empId)) {
            throw new BizException("工时记录不存在");
        }
        if (!"DRAFT".equals(exist.getStatus()) && !"REJECTED".equals(exist.getStatus())) {
            throw new BizException("只能删除草稿或被驳回的工时");
        }
        workLogMapper.deleteById(workLogId);
    }

    /**
     * 提交审批
     */
    @Transactional
    public void submit(Integer empId, Integer workLogId) {
        WorkLog exist = workLogMapper.selectById(workLogId);
        if (exist == null || !exist.getEmpId().equals(empId)) {
            throw new BizException("工时记录不存在");
        }
        if (!"DRAFT".equals(exist.getStatus())) {
            throw new BizException("只能提交草稿状态的工时");
        }
        exist.setStatus("SUBMITTED");
        exist.setUpdateTime(LocalDateTime.now());
        workLogMapper.updateById(exist);
    }

    /**
     * 查询个人工时列表
     */
    public Page<WorkLog> pageByEmp(Integer empId, WorkLogQuery query) {
        return workLogMapper.selectPageWithDetail(
                new Page<>(query.getPage(), query.getSize()),
                empId, query.getProjectId(), query.getStatus(),
                null, query.getStartDate(), query.getEndDate(), "w.create_time");
    }

    /**
     * 查询部门待审批工时（经理端）
     */
    public Page<WorkLog> pagePendingByDept(Integer deptId, int pageNum, int pageSize, String sort) {
        String orderBy = "empName".equals(sort) ? "e.name" : "w.create_time";
        return workLogMapper.selectPageWithDetail(
                new Page<>(pageNum, pageSize),
                null, null, "SUBMITTED", deptId, null, null, orderBy);
    }

    /**
     * 查询部门已审批工时（经理端）
     */
    public Page<WorkLog> pageApprovedByDept(Integer deptId, WorkLogQuery query) {
        return workLogMapper.selectPageWithDetail(
                new Page<>(query.getPage(), query.getSize()),
                null, null, "APPROVED", deptId,
                query.getStartDate(), query.getEndDate(), "w.create_time");
    }

    /**
     * 管理员查看所有员工工时（可按部门筛选）
     */
    public Page<WorkLog> pageAllByDept(WorkLogQuery query, Integer deptId) {
        return workLogMapper.selectPageWithDetail(
                new Page<>(query.getPage(), query.getSize()),
                null, query.getProjectId(), query.getStatus(),
                deptId, query.getStartDate(), query.getEndDate(), "w.create_time");
    }

    /**
     * 个人工时统计
     */
    public List<Map<String, Object>> statPersonal(Integer empId, int year, int month) {
        return workLogMapper.statByEmpMonth(empId, year, month);
    }

    /**
     * 部门工时统计
     */
    public Map<String, Object> statDept(Integer deptId, int year, int month) {
        Map<String, Object> summary = workLogMapper.statDeptSummary(deptId, year, month);
        summary.put("byEmployee", workLogMapper.statByDeptEmp(deptId, year, month));
        summary.put("byProject", workLogMapper.statByDeptProject(deptId, year, month));
        return summary;
    }
}