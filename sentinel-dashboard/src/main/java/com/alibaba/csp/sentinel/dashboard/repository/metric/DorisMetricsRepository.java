package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.service.DorisScheduledStoreService;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 * CREATE TABLE IF NOT EXISTS sentinel_metric (
 * app            VARCHAR(64) NOT NULL,
 * resource       VARCHAR(64) NOT NULL,
 * `timestamp`   DATETIMEV2 (3) ,
 * id             BIGINT ,
 * gmt_create     DATETIMEV2 (3) ,
 * gmt_modified   DATETIMEV2 (3) ,
 * pass_qps       BIGINT ,
 * success_qps    BIGINT ,
 * block_qps      BIGINT ,
 * exception_qps  BIGINT ,
 * rt             DOUBLE ,
 * count          BIGINT ,
 * resource_code  INT
 * )
 * ENGINE=OLAP
 * DUPLICATE KEY(app, resource, `timestamp`)
 * PARTITION BY RANGE(`timestamp`)()
 * DISTRIBUTED BY HASH(app, resource) BUCKETS 8
 * PROPERTIES (
 * "replication_num" = "1",
 * "compression"="zstd",
 * "dynamic_partition.enable" = "true",
 * "dynamic_partition.prefix"="p_",
 * "dynamic_partition.time_unit"="DAY",
 * "dynamic_partition.start"="-7",
 * "dynamic_partition.end"="1",
 * "dynamic_partition.create_history_partition"="true",
 * "dynamic_partition.buckets"="8"
 * );
 */
@Component("dorisDorisMetricsRepository")
public class DorisMetricsRepository implements MetricsRepository<MetricEntity> {

    private final JdbcTemplate jdbcTemplate;

    private final DorisScheduledStoreService dorisScheduledStoreService;

    public DorisMetricsRepository(JdbcTemplate jdbcTemplate, DorisScheduledStoreService dorisScheduledStoreService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dorisScheduledStoreService = dorisScheduledStoreService;
    }

    @Override
    public void save(MetricEntity metric) {
        dorisScheduledStoreService.offer(metric);
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        for (MetricEntity metric : metrics) {
            dorisScheduledStoreService.offer(metric);
        }
    }


    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource,
                                                           long startTime, long endTime) {
        if (StringUtil.isBlank(app) || StringUtil.isBlank(resource)) {
            return Collections.emptyList();
        }

        String sql = "SELECT *"+
                "FROM sentinel_metric " +
                "WHERE app = ? AND resource = ? AND `timestamp` BETWEEN ? AND ? " +
                "ORDER BY `timestamp` ASC";

        java.sql.Timestamp start = new java.sql.Timestamp(startTime);
        java.sql.Timestamp end = new java.sql.Timestamp(endTime);

        return jdbcTemplate.query(sql, new Object[]{app, resource, start, end}, (rs, rowNum) -> {
            MetricEntity entity = new MetricEntity();
            entity.setApp(rs.getString("app"));
            entity.setResource(rs.getString("resource"));
            entity.setTimestamp(rs.getTimestamp("timestamp"));
            entity.setPassQps(rs.getLong("pass_qps"));
            entity.setBlockQps(rs.getLong("block_qps"));
            entity.setSuccessQps(rs.getLong("success_qps"));
            entity.setExceptionQps(rs.getLong("exception_qps"));
            entity.setRt(rs.getDouble("rt"));
            entity.setGmtCreate(rs.getTimestamp("gmt_create"));
            entity.setGmtModified(rs.getTimestamp("gmt_modified"));
            return entity;
        });
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        if (StringUtil.isBlank(app)) {
            return Collections.emptyList();
        }

        long minTimeMs = System.currentTimeMillis() - 1000 * 60;
        Timestamp minTime = new Timestamp(minTimeMs);

        String sql = "SELECT resource, " +
                "SUM(pass_qps) AS pass_qps, " +
                "SUM(block_qps) AS block_qps, " +
                "SUM(success_qps) AS success_qps, " +
                "SUM(exception_qps) AS exception_qps, " +
                "SUM(rt) AS rt, " +
                "COUNT(*) AS count " +
                "FROM sentinel_metric " +
                "WHERE app = ? AND `timestamp` >= ? " +
                "GROUP BY resource " +
                "ORDER BY block_qps DESC, pass_qps DESC";

        return jdbcTemplate.query(sql, new Object[]{app, minTime}, (rs, rowNum) -> rs.getString("resource"));
    }

}
