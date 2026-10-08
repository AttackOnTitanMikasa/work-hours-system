package com.workhours.entity;

import com.baomidou.mybatisplus.annotation.*;

@TableName("employee_project")
public class EmployeeProject {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer empId;
    private Integer projectId;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String empName;
    @TableField(exist = false)
    private String projectName;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getEmpId() { return empId; }
    public void setEmpId(Integer empId) { this.empId = empId; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}
