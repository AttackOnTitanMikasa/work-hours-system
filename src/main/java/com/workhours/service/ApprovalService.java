package com.workhours.service;

import com.workhours.common.BizException;
import com.workhours.entity.Employee;
import com.workhours.entity.Project;
import com.workhours.entity.WorkLog;
import com.workhours.mapper.EmployeeMapper;
import com.workhours.mapper.ProjectMapper;
import com.workhours.mapper.WorkLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApprovalService {

    @Resource
    private WorkLogMapper workLogMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private ProjectMapper projectMapper;

    /**
     * 单条审批
     */
    @Transactional
    public void approve(Integer reviewerId, Integer workLogId, boolean approved, String reason) {
        Employee reviewer = employeeMapper.selectById(reviewerId);
        if (reviewer == null || !"MANAGER".equals(reviewer.getRole())) {
            throw new BizException("无审批权限");
        }

        WorkLog workLog = workLogMapper.selectById(workLogId);
        if (workLog == null) {
            throw new BizException("工时记录不存在");
        }

        if (!"SUBMITTED".equals(workLog.getStatus())) {
            throw new BizException("仅可审批「已提交」状态的工时");
        }

        // BR-05: 经理只能审批本部门员工
        Employee submitter = employeeMapper.selectById(workLog.getEmpId());
        if (submitter == null || !submitter.getDeptId().equals(reviewer.getDeptId())) {
            throw new BizException("只能审批本部门员工的工时");
        }

        // 验证项目属于本部门
        Project project = projectMapper.selectById(workLog.getProjectId());
        if (project == null || !project.getDeptId().equals(reviewer.getDeptId())) {
            throw new BizException("工时对应项目不属于本部门");
        }

        executeApproval(workLog, reviewerId, approved, reason);
    }

    /**
     * 批量审批，返回错误信息（部分失败不影响已成功的）
     */
    @Transactional
    public String batchApprove(Integer reviewerId, List<Integer> workLogIds,
                               boolean approved, String reason) {
        Employee reviewer = employeeMapper.selectById(reviewerId);
        if (reviewer == null || !"MANAGER".equals(reviewer.getRole())) {
            throw new BizException("无审批权限");
        }

        List<String> errors = new ArrayList<>();
        for (Integer workLogId : workLogIds) {
            try {
                WorkLog workLog = workLogMapper.selectById(workLogId);
                if (workLog == null || !"SUBMITTED".equals(workLog.getStatus())) {
                    errors.add("工时(ID=" + workLogId + ")状态不正确，跳过");
                    continue;
                }

                Employee submitter = employeeMapper.selectById(workLog.getEmpId());
                Project project = projectMapper.selectById(workLog.getProjectId());

                if (submitter == null || !submitter.getDeptId().equals(reviewer.getDeptId())) {
                    errors.add("工时(ID=" + workLogId + ")不属于本部门，跳过");
                    continue;
                }
                if (project == null || !project.getDeptId().equals(reviewer.getDeptId())) {
                    errors.add("工时(ID=" + workLogId + ")项目不属于本部门，跳过");
                    continue;
                }

                executeApproval(workLog, reviewerId, approved, reason);
            } catch (Exception e) {
                errors.add("工时(ID=" + workLogId + "): " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            return "部分审批失败: " + String.join("; ", errors);
        }
        return null;
    }

    private void executeApproval(WorkLog workLog, Integer reviewerId,
                                 boolean approved, String reason) {
        if (approved) {
            workLog.setStatus("APPROVED");
            workLog.setRejectReason(null);
        } else {
            // BR-06: 驳回必填原因
            if (reason == null || reason.trim().isEmpty()) {
                throw new BizException("驳回时必须填写原因");
            }
            workLog.setStatus("REJECTED");
            workLog.setRejectReason(reason);
        }
        workLog.setReviewerId(reviewerId);
        workLog.setReviewTime(LocalDateTime.now());
        workLog.setUpdateTime(LocalDateTime.now());
        workLogMapper.updateById(workLog);
    }
}