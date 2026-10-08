package com.workhours.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // SQL Server 2008 R2 分页使用 ROW_NUMBER() 而非 OFFSET/FETCH
        PaginationInnerInterceptor pageInterceptor = new PaginationInnerInterceptor(DbType.SQL_SERVER2005);
        pageInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(pageInterceptor);
        return interceptor;
    }
}
