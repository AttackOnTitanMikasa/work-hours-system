package com.workhours.dto;

import java.util.List;

/**
 * 审批表单
 */
public class ApprovalForm {

    private List<Integer> workLogIds;   // 工时记录ID列表（支持批量）
    private Boolean approved;            // true:通过 false:驳回
    private String reason;               // 驳回原因（驳回时必填）

    public List<Integer> getWorkLogIds() { return workLogIds; }
    public void setWorkLogIds(List<Integer> workLogIds) { this.workLogIds = workLogIds; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
