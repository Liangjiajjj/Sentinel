package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.dashboard.client.ApacheHttpStreamLoadClient;
import com.alibaba.csp.sentinel.dashboard.config.DashboardConfig;
import com.alibaba.csp.sentinel.dashboard.config.DorisProperties;
import com.alibaba.csp.sentinel.dashboard.domain.DorisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

@Service
public class DorisScheduledStoreService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DorisScheduledStoreService.class);

    private static final int DEFAULT_CORE_POOL_SIZE = 10;

    private static final int DEFAULT_MAX_POOL_SIZE = 20;

    private static final int DEFAULT_MAX_QUEUE_CAPACITY = 1000000;

    /**
     * 最迟延迟多少秒存储
     */
    private final int batchDelay;

    /**
     * 一次最多存多少数据
     */
    private final int batchSize;

    /**
     * 当前Repository存储队列(每个Repository一个)
     */
    private final static ConcurrentLinkedQueue<Object> insertQueue = new ConcurrentLinkedQueue<>();

    /**
     * 投递定时线程池
     */
    private final ScheduledThreadPoolExecutor entityDeliverExecutor;

    /**
     * 存储线程池
     */
    private final ThreadPoolExecutor storeEntityExecutor;

    private final ApacheHttpStreamLoadClient dorisLoadClient;

    private final DorisProperties dorisDataBaseConfig;

    public DorisScheduledStoreService(DorisProperties dorisDataBaseConfig) {
        this.dorisDataBaseConfig = dorisDataBaseConfig;
        this.batchSize = dorisDataBaseConfig.getBatchSize() != 0 ? dorisDataBaseConfig.getBatchSize() : 10000;
        this.batchDelay = dorisDataBaseConfig.getBatchDelay() != 0 ? dorisDataBaseConfig.getBatchDelay() : 10;
        this.entityDeliverExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("ENTITY_DELIVER_EXECUTOR"));
        this.entityDeliverExecutor.scheduleWithFixedDelay(this::flushAllQueueToDB, 0, batchDelay, TimeUnit.SECONDS);
        this.storeEntityExecutor = new ThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(DEFAULT_MAX_QUEUE_CAPACITY),
                new NamedThreadFactory("STORE_EXECUTOR"), (r, executor) -> {
            LOGGER.error("there are {} executing task，refuse current task. current task name: {}", DEFAULT_MAX_QUEUE_CAPACITY,
                    Thread.currentThread().getName());
            if (!executor.isShutdown()) {
                r.run();
            }
        });
        ObjectMapper newObjectMapper = new ObjectMapper()
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"))
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.dorisLoadClient = new ApacheHttpStreamLoadClient(dorisDataBaseConfig.getHttpUrl(), getAuthString(), dorisDataBaseConfig.getConnectTimeout(), 60,
                60, 200, 3, newObjectMapper);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            entityDeliverExecutor.submit(this::flushAllQueueToDB);
            entityDeliverExecutor.shutdown();
        }));
        LOGGER.info("[ScheduledStoreService] doris batchSize is {}, batchDelay {}", batchSize, batchDelay);
    }

    /**
     * @param data
     */
    public void offer(Object data) {
        /**
         * 放在同一个checkExecutor保证queue和size是线程安全的
         */
        entityDeliverExecutor.execute(() -> {
            try {
                insertQueue.offer(data);
                checkAndFlushQueueToDB();
            } catch (Throwable throwable) {
                LOGGER.error("[ScheduledStoreService] offer queue error", throwable);
            }
        });
    }

    private void checkAndFlushQueueToDB() {
        int size = insertQueue.size();
        if (size >= batchSize) {
            LOGGER.info("[ScheduledStoreService] current queue count greater than {} , batch insert to doris. size {}", batchSize, size);
            flushAllQueueToDB();
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[ScheduledStoreService] current queue count less than {} , offer queue. size {}", batchSize, size);
            }
        }
    }

    /**
     * 一段时间巡查一下队列，入库
     */
    private void flushAllQueueToDB() {
        int allOfCurrentQueueCount = insertQueue.size();
        LOGGER.debug("[ScheduledStoreService] flushAllQueueToDB.  allOfCurrentQueueCount {}", allOfCurrentQueueCount);
        storeEntityExecutor.execute(this::flushQueueToDB);
    }


    private void flushQueueToDB() {
        try {
            /**
             * queueSize of data base
             */
            int size = insertQueue.size();
            if (size == 0) {
                LOGGER.error("[ScheduledStoreService] flushQueueToDB queue is empty don't save.");
                return;
            }
            List<Object> datas = new ArrayList<>(insertQueue);
            insertQueue.clear();
            load(dorisDataBaseConfig.getDataBaseName(), dorisDataBaseConfig.getTableName(), datas);
        } catch (Throwable throwable) {
            LOGGER.error("[ScheduledStoreService] batchInsert error", throwable);
        }
    }


    /**
     * @param dataBase
     * @param tableName
     * @param datas
     */
    private void load(String dataBase, String tableName, List<Object> datas) {
        DorisResponse response = null;
        long start = System.currentTimeMillis();
        long collectCostTime = 0;
        String auth = getAuthString();
        try {
            response = dorisLoadClient.load(dataBase, auth, tableName, datas);
            collectCostTime = System.currentTimeMillis() - start;
        } catch (Exception e) {
            LOGGER.error("batch insert error", e);
        } finally {
            long loadCostTime = System.currentTimeMillis() - start;
            if (loadCostTime > 1000) {
                if (Objects.nonNull(response)) {
                    LOGGER.info("[ScheduledStoreService] batch insert collectCostTime {} ms, loadCostTime {} ms label {} dataBase {} " +
                                    "insertCount {} batchSize {} batchDelay {}",
                            collectCostTime, loadCostTime, response.getLabel(), dataBase, datas.size(), batchSize, batchDelay);
                } else {
                    LOGGER.info("[ScheduledStoreService] batch insert collectCostTime {} ms, loadCostTime {} ms dataBase {} " +
                                    "insertCount {} batchSize {} batchDelay {}",
                            collectCostTime, loadCostTime, dataBase, datas.size(), batchSize, batchDelay);
                }
            }
        }
    }

    private String getAuthString() {
        String username = dorisDataBaseConfig.getUsername();
        String password = dorisDataBaseConfig.getPassword();
        String str = username + ":" + password;
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }


}

