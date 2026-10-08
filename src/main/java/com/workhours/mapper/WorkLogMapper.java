package com.workhours.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.entity.WorkLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkLogMapper extends BaseMapper<WorkLog> {

    @Select("<script>" +
            "SELECT w.*, e.name AS emp_name, p.name AS project_name, d.name AS dept_name " +
            "FROM work_log w " +
            "LEFT JOIN employee e ON w.emp_id = e.id " +
            "LEFT JOIN project p ON w.project_id = p.id " +
            "LEFT JOIN department d ON e.dept_id = d.id " +
            "WHERE w.is_deleted = 0 " +
            "<if test='empId != null'> AND w.emp_id = #{empId} </if>" +
            "<if test='projectId != null'> AND w.project_id = #{projectId} </if>" +
            "<if test='status != null and status != \"\"'> AND w.status = #{status} </if>" +
            "<if test='deptId != null'> AND e.dept_id = #{deptId} </if>" +
            "<if test='startDate != null'> AND w.work_date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND w.work_date &lt;= #{endDate} </if>" +
            "ORDER BY ${orderBy} DESC" +
            "</script>")
    Page<WorkLog> selectPageWithDetail(Page<WorkLog> page,
                                       @Param("empId") Integer empId,
                                       @Param("projectId") Integer projectId,
                                       @Param("status") String status,
                                       @Param("deptId") Integer deptId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("orderBy") String orderBy);

    // 个人工时统计：当月按项目分布
    @Select("SELECT p.name AS project_name, SUM(w.hours) AS total_hours " +
            "FROM work_log w " +
            "LEFT JOIN project p ON w.project_id = p.id " +
            "WHERE w.emp_id = #{empId} AND w.status = 'APPROVED' AND w.is_deleted = 0 " +
            "AND YEAR(w.work_date) = #{year} AND MONTH(w.work_date) = #{month} " +
            "GROUP BY p.name")
    List<Map<String, Object>> statByEmpMonth(@Param("empId") Integer empId,
                                             @Param("year") int year,
                                             @Param("month") int month);

    // 部门工时统计：按员工
    @Select("SELECT e.name AS emp_name, SUM(w.hours) AS total_hours " +
            "FROM work_log w " +
            "LEFT JOIN employee e ON w.emp_id = e.id " +
            "WHERE e.dept_id = #{deptId} AND w.status = 'APPROVED' AND w.is_deleted = 0 " +
            "AND YEAR(w.work_date) = #{year} AND MONTH(w.work_date) = #{month} " +
            "GROUP BY e.name")
    List<Map<String, Object>> statByDeptEmp(@Param("deptId") Integer deptId,
                                            @Param("year") int year,
                                            @Param("month") int month);

    // 部门工时统计：按项目
    @Select("SELECT p.name AS project_name, SUM(w.hours) AS total_hours " +
            "FROM work_log w " +
            "LEFT JOIN project p ON w.project_id = p.id " +
            "LEFT JOIN employee e ON w.emp_id = e.id " +
            "WHERE e.dept_id = #{deptId} AND w.status = 'APPROVED' AND w.is_deleted = 0 " +
            "AND YEAR(w.work_date) = #{year} AND MONTH(w.work_date) = #{month} " +
            "GROUP BY p.name")
    List<Map<String, Object>> statByDeptProject(@Param("deptId") Integer deptId,
                                                @Param("year") int year,
                                                @Param("month") int month);

    // 部门总工时
    @Select("SELECT ISNULL(SUM(w.hours), 0) AS total_hours, COUNT(DISTINCT w.emp_id) AS emp_count " +
            "FROM work_log w " +
            "LEFT JOIN employee e ON w.emp_id = e.id " +
            "WHERE e.dept_id = #{deptId} AND w.status = 'APPROVED' AND w.is_deleted = 0 " +
            "AND YEAR(w.work_date) = #{year} AND MONTH(w.work_date) = #{month}")
    Map<String, Object> statDeptSummary(@Param("deptId") Integer deptId,
                                        @Param("year") int year,
                                        @Param("month") int month);

    // 全局统计（管理员仪表盘）
    @Select("SELECT ISNULL(SUM(hours), 0) AS total_hours, COUNT(*) AS total_records " +
            "FROM work_log WHERE status = 'APPROVED' AND is_deleted = 0 " +
            "AND YEAR(work_date) = #{year} AND MONTH(work_date) = #{month}")
    Map<String, Object> statGlobal(@Param("year") int year, @Param("month") int month);

    // 待审批总数
    @Select("SELECT COUNT(*) FROM work_log WHERE status = 'SUBMITTED' AND is_deleted = 0")
    long countPending();

    // 个人当月实际出勤天数
    @Select("SELECT COUNT(DISTINCT work_date) FROM work_log " +
            "WHERE emp_id = #{empId} AND status = 'APPROVED' AND is_deleted = 0 " +
            "AND YEAR(work_date) = #{year} AND MONTH(work_date) = #{month}")
    int countWorkDays(@Param("empId") Integer empId, @Param("year") int year, @Param("month") int month);
}
