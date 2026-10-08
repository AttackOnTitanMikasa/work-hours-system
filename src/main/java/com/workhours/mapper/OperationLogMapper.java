package com.workhours.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("<script>" +
            "SELECT * FROM operation_log WHERE 1=1 " +
            "<if test='module != null and module != \"\"'> AND module = #{module} </if>" +
            "<if test='action != null and action != \"\"'> AND action = #{action} </if>" +
            "<if test='startDate != null'> AND CAST(create_time AS DATE) &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND CAST(create_time AS DATE) &lt;= #{endDate} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    Page<OperationLog> selectPageWithFilter(Page<OperationLog> page,
                                            @Param("module") String module,
                                            @Param("action") String action,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate);
}
