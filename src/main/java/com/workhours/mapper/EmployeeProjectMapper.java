package com.workhours.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workhours.entity.EmployeeProject;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmployeeProjectMapper extends BaseMapper<EmployeeProject> {

    @Select("SELECT ep.*, p.name AS project_name FROM employee_project ep " +
            "LEFT JOIN project p ON ep.project_id = p.id " +
            "WHERE ep.emp_id = #{empId}")
    List<EmployeeProject> selectByEmpId(@Param("empId") Integer empId);

    @Delete("DELETE FROM employee_project WHERE emp_id = #{empId}")
    int deleteByEmpId(@Param("empId") Integer empId);

    @Delete("DELETE FROM employee_project WHERE project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") Integer projectId);
}
