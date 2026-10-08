package com.workhours.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workhours.common.BizException;
import com.workhours.entity.Project;
import com.workhours.mapper.EmployeeProjectMapper;
import com.workhours.mapper.ProjectMapper;
import com.workhours.mapper.WorkLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private EmployeeProjectMapper employeeProjectMapper;
    @Resource
    private WorkLogMapper workLogMapper;

    public Page<Project> page(int pageNum, int pageSize, Integer deptId, Integer status) {
        return projectMapper.selectPageWithDept(new Page<>(pageNum, pageSize), deptId, status);
    }

    public Project getById(Integer id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException("项目不存在");
        }
        return project;
    }

    public void add(Project project) {
        projectMapper.insert(project);
    }

    public void update(Project project) {
        getById(project.getId());
        projectMapper.updateById(project);
    }

    @Transactional
    public void delete(Integer id) {
        getById(id);
        // BR-10: 有工时记录时禁止删除
        Long workLogCount = workLogMapper.selectCount(
                new LambdaQueryWrapper<com.workhours.entity.WorkLog>()
                        .eq(com.workhours.entity.WorkLog::getProjectId, id));
        if (workLogCount > 0) {
            throw new BizException("该项目存在工时记录，无法删除");
        }
        // 删除项目授权
        employeeProjectMapper.deleteByProjectId(id);
        // 删除项目
        projectMapper.deleteById(id);
    }
}