package redcoder.quartzextendcore.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import redcoder.quartzextendcommon.utils.HttpTemplate;
import redcoder.quartzextendcommon.utils.IpUtils;
import redcoder.quartzextendcommon.utils.JsonUtils;
import redcoder.quartzextendcore.core.dto.QuartzApiResult;
import redcoder.quartzextendcore.core.dto.QuartzJobTriggerInfo;
import redcoder.quartzextendcore.core.dto.QuartzSchedulerInstance;
import org.quartz.*;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author redcoder54
 * @since 1.0.0
 */
public class QuartzService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(QuartzService.class);

    private Scheduler scheduler;
    private Environment environment;
    private QuartzJobSchedulerProperties properties;

    public QuartzService(Scheduler scheduler, Environment environment, QuartzJobSchedulerProperties properties) {
        this.scheduler = scheduler;
        this.environment = environment;
        this.properties = properties;
    }

    /**
     * 获取当前quartz scheduler中的job和trigger信息
     *
     * @return 包含job和trigger信息 {@link QuartzJobTriggerInfo} 的集合
     */
    List<QuartzJobTriggerInfo> getQuartzJobTriggerInfoList() throws SchedulerException {
        List<QuartzJobTriggerInfo> quartzJobTriggerInfos = new ArrayList<>();

        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup());
        for (TriggerKey triggerKey : triggerKeys) {
            try {
                QuartzJobTriggerInfo quartzJobTriggerInfo = createQuartzJobTrigger(triggerKey);
                quartzJobTriggerInfos.add(quartzJobTriggerInfo);
            } catch (SchedulerException e) {
                log.warn("createQuartBean error", e);
            }
        }

        return quartzJobTriggerInfos;
    }

    /**
     * 根据trigger查询对应的 {@link QuartzJobTriggerInfo}
     *
     * @param triggerName  trigger's name
     * @param triggerGroup trigger's group
     * @return trigger对应的 {@link QuartzJobTriggerInfo}
     * @throws SchedulerException scheduler异常
     */
    QuartzJobTriggerInfo getQuartzJobTriggerInfo(String triggerName, String triggerGroup) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);
        QuartzJobTriggerInfo quartBean = createQuartzJobTrigger(triggerKey);
        Assert.notNull(quartBean, "QuartzJobTriggerInfo not exist");
        return quartBean;
    }

    /**
     * 执行job
     *
     * @param jobName  job名称
     * @param jobGroup job所在组
     */
    void triggerJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.triggerJob(jobKey);
    }

    /**
     * 暂停job
     *
     * @param jobName  job名称
     * @param jobGroup job所在组
     */
    void pauseJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.pauseJob(jobKey);
    }

    /**
     * 恢复（取消暂停）job
     *
     * @param jobName  job名称
     * @param jobGroup job所在组
     */
    void resumeJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.resumeJob(jobKey);
    }

    /**
     * 删除job
     *
     * @param jobName  job名称
     * @param jobGroup job所在组
     */
    void deleteJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.deleteJob(jobKey);
    }

    private QuartzJobTriggerInfo createQuartzJobTrigger(TriggerKey triggerKey) throws SchedulerException {
        QuartzJobTriggerInfo quartzJobTriggerInfo = new QuartzJobTriggerInfo();

        String schedName = scheduler.getSchedulerName();
        quartzJobTriggerInfo.setSchedName(schedName);

        Trigger trigger = scheduler.getTrigger(triggerKey);
        TriggerState triggerState = scheduler.getTriggerState(triggerKey);
        // 设置触发器相关属性
        quartzJobTriggerInfo.setTriggerName(triggerKey.getName());
        quartzJobTriggerInfo.setTriggerGroup(triggerKey.getGroup());
        quartzJobTriggerInfo.setTriggerDesc(trigger.getDescription());
        Optional.ofNullable(trigger.getPreviousFireTime()).ifPresent(t -> quartzJobTriggerInfo.setPrevFireTime(t.getTime()));
        Optional.ofNullable(trigger.getNextFireTime()).ifPresent(t -> quartzJobTriggerInfo.setNextFireTime(t.getTime()));
        quartzJobTriggerInfo.setTriggerState(triggerState.name());

        JobKey jobKey = trigger.getJobKey();
        if (jobKey != null) {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            // 设置job相关属性
            quartzJobTriggerInfo.setJobName(jobDetail.getKey().getName());
            quartzJobTriggerInfo.setJobGroup(jobDetail.getKey().getGroup());
            quartzJobTriggerInfo.setJobDesc(jobDetail.getDescription());
        }
        return quartzJobTriggerInfo;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 异步注册
        new Thread(() -> {
            String registerUrl = properties.getRegisterUrl();
            if (isEmpty(registerUrl)) {
                return;
            }

            try {
                String schedName = scheduler.getSchedulerName();
                String host = IpUtils.getLocalIp();
                String port = environment.getProperty("server.port");
                if (isEmpty(port)) {
                    log.warn("注册实例信息失败：未配置\"server.port\"");
                    return;
                }
                QuartzSchedulerInstance instance = new QuartzSchedulerInstance(schedName, host, Integer.valueOf(port));
                QuartzApiResult<Boolean> apiResult = HttpTemplate.doPost(registerUrl, JsonUtils.beanToJsonString(instance),
                        new TypeReference<QuartzApiResult<Boolean>>() {
                        });
                if (apiResult.getStatus() == 0 && Boolean.TRUE.equals(apiResult.getData())) {
                    log.info("注册实例信息成功");
                } else {
                    log.warn("注册实例信息失败：" + apiResult.getMessage());
                }
            } catch (Exception e) {
                log.warn("注册实例信息失败", e);
            }
        }, "Quartz-Register-Thread").start();
    }

    @Override
    public void destroy() throws Exception {
        String unregisterUrl = properties.getUnregisterUrl();
        if (isEmpty(unregisterUrl)) {
            return;
        }
        try {
            String schedName = scheduler.getSchedulerName();
            String host = IpUtils.getLocalIp();
            String port = environment.getProperty("server.port");
            if (isEmpty(port)) {
                log.warn("解除注册的实例信息失败：未配置\"server.port\"");
                return;
            }
            QuartzSchedulerInstance instance = new QuartzSchedulerInstance(schedName, host, Integer.valueOf(port));
            QuartzApiResult<Boolean> apiResult = HttpTemplate.doPost(unregisterUrl, JsonUtils.beanToJsonString(instance),
                    new TypeReference<QuartzApiResult<Boolean>>() {
                    });
            if (apiResult.getStatus() == 0 && Boolean.TRUE.equals(apiResult.getData())) {
                log.info("解除注册的实例信息成功");
            } else {
                log.warn("解除注册的实例信息失败：" + apiResult.getMessage());
            }
        } catch (Exception e) {
            log.warn("解除注册的注册实例信息失败", e);
        }
    }
}
