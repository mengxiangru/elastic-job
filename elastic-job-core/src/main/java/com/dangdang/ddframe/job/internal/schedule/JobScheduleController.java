/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.internal.schedule;

import com.dangdang.ddframe.job.exception.JobException;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * 作业调度控制器.
 * 
 * @author zhangliang
 */
@RequiredArgsConstructor
public class JobScheduleController {
    protected static Logger logger = LoggerFactory.getLogger(JobScheduleController.class);

    private final Scheduler scheduler;
    
    private final JobDetail jobDetail;
    
    private final SchedulerFacade schedulerFacade;
    
    private final String triggerIdentity;
    
    /**
     * 调度作业.
     * 
     * @param cronExpression CRON表达式
     */
    public void scheduleJob(final String cronExpression) {
        try {
            if (!scheduler.checkExists(jobDetail.getKey())) {
                scheduler.scheduleJob(jobDetail, createTrigger(cronExpression));
            }
            scheduler.start();
        } catch (final SchedulerException ex) {
            throw new JobException(ex);
        }
    }
    
    /**
     * 重新调度作业.
     * 
     * @param cronExpression CRON表达式
     */
    public void rescheduleJob(final String cronExpression) {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.rescheduleJob(TriggerKey.triggerKey(triggerIdentity), createTrigger(cronExpression));
            }
        } catch (final SchedulerException ex) {
            throw new JobException(ex);
        }
    }
    
    private CronTrigger createTrigger(final String cronExpression) {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
        if (schedulerFacade.isMisfire()) {
            cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
        } else {
            cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
        }
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerIdentity)
                .withSchedule(cronScheduleBuilder).build();
    }
    
    /**
     * 获取下次作业触发时间.
     * 
     * @return 下次作业触发时间
     */
    public Date getNextFireTime() {
        List<? extends Trigger> triggers;
        try {
            triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
        } catch (final SchedulerException ex) {
            return null;
        }
        Date result = null;
        for (Trigger each : triggers) {
            Date nextFireTime = each.getNextFireTime();
            if (null == nextFireTime) {
                continue;
            }
            if (null == result) {
                result = nextFireTime;
            } else if (nextFireTime.getTime() < result.getTime()) {
                result = nextFireTime;
            }
        }
        return result;
    }
    
    /**
     * 暂停作业.
     */
    public void pauseJob() {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.pauseAll();
            }
        } catch (final SchedulerException ex) {
            throw new JobException(ex);
        }
    }
    
    /**
     * 恢复作业.
     */
    public void resumeJob() {
        try {
            if (!scheduler.isShutdown()) {
                scheduler.resumeAll();
            }
        } catch (final SchedulerException ex) {
            throw new JobException(ex);
        }
    }
    
    /**
     * 立刻启动作业.
     */
    public void triggerJob() {
        try {
            if (!scheduler.isShutdown()) {
                logger.info(jobDetail.getKey() + " : 开始");
                scheduler.triggerJob(jobDetail.getKey());
                logger.info(jobDetail.getKey() + " : 结束");
            } else {
                logger.error(jobDetail.getKey() + " : 已关闭");
            }
        } catch (final SchedulerException ex) {
            logger.error(jobDetail.getKey() + " : " +ex.getMessage(), ex);
            throw new JobException(ex);
        }
    }
    
    /**
     * 关闭调度器.
     */
    public void shutdown() {
        schedulerFacade.releaseJobResource();
        try {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        } catch (final SchedulerException ex) {
            throw new JobException(ex);
        }
    }
}
