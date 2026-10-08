package com.workhours.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("<script>" +
            "SELECT p.*, d.name AS dept_name FROM project p " +
            "LEFT JOIN department d ON p.dept_id = d.id " +
            "WHERE p.is_deleted = 0 " +
            "<if test='deptId != null'> AND p.dept_id = #{deptId} </if>" +
            "<if test='status != null'> AND p.status = #{status} </if>" +
            "ORDER BY p.id DESC" +
            "</script>")
    Page<Project> selectPageWithDept(Page<Project> page,
                                     @Param("deptId") Integer deptId,
                                     @Param("status") Integer status);
}
