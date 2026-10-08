package com.workhours.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.entity.OperationLog;
import com.workhours.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class OperationLogService {

    @Resource
    private OperationLogMapper operationLogMapper;

    public void log(Integer operatorId, String operatorName, String operatorRole,
                    String module, String action, String detail, HttpServletRequest request) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorRole(operatorRole);
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setIp(getClientIp(request));
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public Page<OperationLog> page(int pageNum, int pageSize, String module,
                                   String action, String startDate, String endDate) {
        return operationLogMapper.selectPageWithFilter(
                new Page<>(pageNum, pageSize), module, action, startDate, endDate);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}