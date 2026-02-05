package com.upec.factoryscheduling.aps.repository.query.impl;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.repository.query.WorkCenterQuery;
import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class WorkCenterQueryImpl extends JdbcTemplatePagination implements WorkCenterQuery {


    @Override
    public Page<WorkCenter> queryWorkCenterPage(String name, String code, Integer pageNum, Integer pageSize) {
        String querySQL = "select * from aps_work_center " +
                "where 1=1 ";
        if (StringUtils.hasLength(name)) {
            querySQL += " and name like '%" + name + "%'";
        }
        if (StringUtils.hasLength(code)) {
            querySQL += " and work_center_code like '%" + code + "%'";
        }
        querySQL = querySQL + " order by work_center_code desc ";
        return super.queryForPage(querySQL, new BeanPropertyRowMapper<>(WorkCenter.class), pageNum, pageSize);
    }
}
