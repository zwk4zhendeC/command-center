DROP TABLE IF EXISTS qrtz_fired_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_paused_trigger_grps CASCADE;
DROP TABLE IF EXISTS qrtz_scheduler_state CASCADE;
DROP TABLE IF EXISTS qrtz_locks CASCADE;
DROP TABLE IF EXISTS qrtz_simple_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_simprop_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_cron_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_blob_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_job_details CASCADE;
DROP TABLE IF EXISTS qrtz_calendars CASCADE;

-- ============================================================
-- 1. 存储每一个已配置的 JobDetail 的详细信息
-- ============================================================
CREATE TABLE qrtz_job_details (
                                  sched_name        VARCHAR(120) NOT NULL,
                                  job_name          VARCHAR(200) NOT NULL,
                                  job_group         VARCHAR(200) NOT NULL,
                                  description       VARCHAR(250),
                                  job_class_name    VARCHAR(250) NOT NULL,
                                  is_durable        VARCHAR(1)   NOT NULL,
                                  is_nonconcurrent  VARCHAR(1)   NOT NULL,
                                  is_update_data    VARCHAR(1)   NOT NULL,
                                  requests_recovery VARCHAR(1)   NOT NULL,
                                  job_data          BYTEA,
                                  PRIMARY KEY (sched_name, job_name, job_group)
);

COMMENT ON TABLE qrtz_job_details IS '任务详细信息表';
COMMENT ON COLUMN qrtz_job_details.sched_name IS '调度名称';
COMMENT ON COLUMN qrtz_job_details.job_name IS '任务名称';
COMMENT ON COLUMN qrtz_job_details.job_group IS '任务组名';
COMMENT ON COLUMN qrtz_job_details.description IS '相关介绍';
COMMENT ON COLUMN qrtz_job_details.job_class_name IS '执行任务类名称';
COMMENT ON COLUMN qrtz_job_details.is_durable IS '是否持久化';
COMMENT ON COLUMN qrtz_job_details.is_nonconcurrent IS '是否并发';
COMMENT ON COLUMN qrtz_job_details.is_update_data IS '是否更新数据';
COMMENT ON COLUMN qrtz_job_details.requests_recovery IS '是否接受恢复执行';
COMMENT ON COLUMN qrtz_job_details.job_data IS '存放持久化job对象';

-- ============================================================
-- 2. 存储已配置的 Trigger 的信息
-- ============================================================
CREATE TABLE qrtz_triggers (
                               sched_name      VARCHAR(120) NOT NULL,
                               trigger_name    VARCHAR(200) NOT NULL,
                               trigger_group   VARCHAR(200) NOT NULL,
                               job_name        VARCHAR(200) NOT NULL,
                               job_group       VARCHAR(200) NOT NULL,
                               description     VARCHAR(250),
                               next_fire_time  BIGINT,
                               prev_fire_time  BIGINT,
                               priority        INTEGER,
                               trigger_state   VARCHAR(16) NOT NULL,
                               trigger_type    VARCHAR(8)  NOT NULL,
                               start_time      BIGINT      NOT NULL,
                               end_time        BIGINT,
                               calendar_name   VARCHAR(200),
                               misfire_instr   SMALLINT,
                               job_data        BYTEA,
                               PRIMARY KEY (sched_name, trigger_name, trigger_group),
                               CONSTRAINT fk_qrtz_triggers_job_details
                                   FOREIGN KEY (sched_name, job_name, job_group)
                                       REFERENCES qrtz_job_details (sched_name, job_name, job_group)
);

COMMENT ON TABLE qrtz_triggers IS '触发器详细信息表';

-- ============================================================
-- 3. 简单触发器
-- ============================================================
CREATE TABLE qrtz_simple_triggers (
                                      sched_name      VARCHAR(120) NOT NULL,
                                      trigger_name    VARCHAR(200) NOT NULL,
                                      trigger_group   VARCHAR(200) NOT NULL,
                                      repeat_count    BIGINT       NOT NULL,
                                      repeat_interval BIGINT       NOT NULL,
                                      times_triggered BIGINT       NOT NULL,
                                      PRIMARY KEY (sched_name, trigger_name, trigger_group),
                                      CONSTRAINT fk_qrtz_simple_triggers
                                          FOREIGN KEY (sched_name, trigger_name, trigger_group)
                                              REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
);

COMMENT ON TABLE qrtz_simple_triggers IS '简单触发器的信息表';

-- ============================================================
-- 4. Cron Trigger
-- ============================================================
CREATE TABLE qrtz_cron_triggers (
                                    sched_name      VARCHAR(120) NOT NULL,
                                    trigger_name    VARCHAR(200) NOT NULL,
                                    trigger_group   VARCHAR(200) NOT NULL,
                                    cron_expression VARCHAR(200) NOT NULL,
                                    time_zone_id    VARCHAR(80),
                                    PRIMARY KEY (sched_name, trigger_name, trigger_group),
                                    CONSTRAINT fk_qrtz_cron_triggers
                                        FOREIGN KEY (sched_name, trigger_name, trigger_group)
                                            REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
);

COMMENT ON TABLE qrtz_cron_triggers IS 'Cron类型的触发器表';

-- ============================================================
-- 5. Blob Trigger
-- ============================================================
CREATE TABLE qrtz_blob_triggers (
                                    sched_name    VARCHAR(120) NOT NULL,
                                    trigger_name  VARCHAR(200) NOT NULL,
                                    trigger_group VARCHAR(200) NOT NULL,
                                    blob_data     BYTEA,
                                    PRIMARY KEY (sched_name, trigger_name, trigger_group),
                                    CONSTRAINT fk_qrtz_blob_triggers
                                        FOREIGN KEY (sched_name, trigger_name, trigger_group)
                                            REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
);

COMMENT ON TABLE qrtz_blob_triggers IS 'Blob类型的触发器表';

-- ============================================================
-- 6. 日历信息
-- ============================================================
CREATE TABLE qrtz_calendars (
                                sched_name    VARCHAR(120) NOT NULL,
                                calendar_name VARCHAR(200) NOT NULL,
                                calendar      BYTEA        NOT NULL,
                                PRIMARY KEY (sched_name, calendar_name)
);

COMMENT ON TABLE qrtz_calendars IS '日历信息表';

-- ============================================================
-- 7. 暂停的 Trigger 组
-- ============================================================
CREATE TABLE qrtz_paused_trigger_grps (
                                          sched_name   VARCHAR(120) NOT NULL,
                                          trigger_group VARCHAR(200) NOT NULL,
                                          PRIMARY KEY (sched_name, trigger_group)
);

COMMENT ON TABLE qrtz_paused_trigger_grps IS '暂停的触发器表';

-- ============================================================
-- 8. 已触发 Trigger 信息
-- ============================================================
CREATE TABLE qrtz_fired_triggers (
                                     sched_name         VARCHAR(120) NOT NULL,
                                     entry_id           VARCHAR(95)  NOT NULL,
                                     trigger_name       VARCHAR(200) NOT NULL,
                                     trigger_group      VARCHAR(200) NOT NULL,
                                     instance_name      VARCHAR(200) NOT NULL,
                                     fired_time         BIGINT       NOT NULL,
                                     sched_time         BIGINT       NOT NULL,
                                     priority           INTEGER      NOT NULL,
                                     state              VARCHAR(16)  NOT NULL,
                                     job_name           VARCHAR(200),
                                     job_group          VARCHAR(200),
                                     is_nonconcurrent   VARCHAR(1),
                                     requests_recovery  VARCHAR(1),
                                     PRIMARY KEY (sched_name, entry_id)
);

COMMENT ON TABLE qrtz_fired_triggers IS '已触发的触发器表';

-- ============================================================
-- 9. Scheduler 状态
-- ============================================================
CREATE TABLE qrtz_scheduler_state (
                                      sched_name        VARCHAR(120) NOT NULL,
                                      instance_name     VARCHAR(200) NOT NULL,
                                      last_checkin_time BIGINT       NOT NULL,
                                      checkin_interval  BIGINT       NOT NULL,
                                      PRIMARY KEY (sched_name, instance_name)
);

COMMENT ON TABLE qrtz_scheduler_state IS '调度器状态表';

-- ============================================================
-- 10. 锁表
-- ============================================================
CREATE TABLE qrtz_locks (
                            sched_name VARCHAR(120) NOT NULL,
                            lock_name  VARCHAR(40)  NOT NULL,
                            PRIMARY KEY (sched_name, lock_name)
);

COMMENT ON TABLE qrtz_locks IS '存储的悲观锁信息表';

-- ============================================================
-- 11. SimProp Trigger
-- ============================================================
CREATE TABLE qrtz_simprop_triggers (
                                       sched_name    VARCHAR(120) NOT NULL,
                                       trigger_name  VARCHAR(200) NOT NULL,
                                       trigger_group VARCHAR(200) NOT NULL,
                                       str_prop_1    VARCHAR(512),
                                       str_prop_2    VARCHAR(512),
                                       str_prop_3    VARCHAR(512),
                                       int_prop_1    INTEGER,
                                       int_prop_2    INTEGER,
                                       long_prop_1   BIGINT,
                                       long_prop_2   BIGINT,
                                       dec_prop_1    NUMERIC(13,4),
                                       dec_prop_2    NUMERIC(13,4),
                                       bool_prop_1   VARCHAR(1),
                                       bool_prop_2   VARCHAR(1),
                                       PRIMARY KEY (sched_name, trigger_name, trigger_group),
                                       CONSTRAINT fk_qrtz_simprop_triggers
                                           FOREIGN KEY (sched_name, trigger_name, trigger_group)
                                               REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
);

COMMENT ON TABLE qrtz_simprop_triggers IS '同步机制的行锁表';

-- ============================================================
-- 推荐索引（Quartz 官方 PostgreSQL 脚本中通常包含）
-- ============================================================
CREATE INDEX idx_qrtz_j_req_recovery
    ON qrtz_job_details (sched_name, requests_recovery);

CREATE INDEX idx_qrtz_t_j
    ON qrtz_triggers (sched_name, job_name, job_group);

CREATE INDEX idx_qrtz_t_jg
    ON qrtz_triggers (sched_name, job_group);

CREATE INDEX idx_qrtz_t_c
    ON qrtz_triggers (sched_name, calendar_name);

CREATE INDEX idx_qrtz_t_g
    ON qrtz_triggers (sched_name, trigger_group);

CREATE INDEX idx_qrtz_t_state
    ON qrtz_triggers (sched_name, trigger_state);

CREATE INDEX idx_qrtz_t_n_state
    ON qrtz_triggers (sched_name, trigger_name, trigger_group, trigger_state);

CREATE INDEX idx_qrtz_t_n_g_state
    ON qrtz_triggers (sched_name, trigger_group, trigger_state);

CREATE INDEX idx_qrtz_t_next_fire_time
    ON qrtz_triggers (sched_name, next_fire_time);

CREATE INDEX idx_qrtz_t_nft_st
    ON qrtz_triggers (sched_name, trigger_state, next_fire_time);

CREATE INDEX idx_qrtz_t_nft_misfire
    ON qrtz_triggers (sched_name, misfire_instr, next_fire_time);

CREATE INDEX idx_qrtz_t_nft_st_misfire
    ON qrtz_triggers (sched_name, trigger_state, misfire_instr, next_fire_time);

CREATE INDEX idx_qrtz_t_nft_st_misfire_grp
    ON qrtz_triggers (sched_name, trigger_group, trigger_state, misfire_instr, next_fire_time);

CREATE INDEX idx_qrtz_ft_trig_inst_name
    ON qrtz_fired_triggers (sched_name, instance_name);

CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry
    ON qrtz_fired_triggers (sched_name, instance_name, requests_recovery);

CREATE INDEX idx_qrtz_ft_j_g
    ON qrtz_fired_triggers (sched_name, job_name, job_group);

CREATE INDEX idx_qrtz_ft_jg
    ON qrtz_fired_triggers (sched_name, job_group);

CREATE INDEX idx_qrtz_ft_t_g
    ON qrtz_fired_triggers (sched_name, trigger_name, trigger_group);

CREATE INDEX idx_qrtz_ft_tg
    ON qrtz_fired_triggers (sched_name, trigger_group);
--
-- select * from sys_config
