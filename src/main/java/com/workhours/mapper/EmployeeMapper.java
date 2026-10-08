package com.workhours.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    @Select("SELECT e.*, d.name AS dept_name FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE e.is_deleted = 0 AND e.phone = #{phone}")
    Employee selectByPhone(@Param("phone") String phone);

    @Select("<script>" +
            "SELECT e.*, d.name AS dept_name FROM employee e " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE e.is_deleted = 0 " +
            "<if test='deptId != null'> AND e.dept_id = #{deptId} </if>" +
            "<if test='role != null and role != \"\"'> AND e.role = #{role} </if>" +
            "ORDER BY e.id DESC" +
            "</script>")
    Page<Employee> selectPageWithDept(Page<Employee> page,
                                      @Param("deptId") Integer deptId,
                                      @Param("role") String role);
}
