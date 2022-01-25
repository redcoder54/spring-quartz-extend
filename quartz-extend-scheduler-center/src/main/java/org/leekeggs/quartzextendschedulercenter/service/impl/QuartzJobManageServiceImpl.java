package org.leekeggs.quartzextendschedulercenter.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.leekeggs.quartzextendcommon.utils.HttpTemplate;
import org.leekeggs.quartzextendcommon.utils.MapUtils;
import org.leekeggs.quartzextendcore.core.dto.QuartzJobTriggerInfo;
import org.leekeggs.quartzextendschedulercenter.constant.QuartzApiConstants;
import org.leekeggs.quartzextendschedulercenter.exception.JobManageException;
import org.leekeggs.quartzextendschedulercenter.mapper.QuartzSchedulerInstanceMapper;
import org.leekeggs.quartzextendschedulercenter.mapper.QuartzSchedulerJobTriggerInfoMapper;
import org.leekeggs.quartzextendschedulercenter.model.dto.ApiResult;
import org.leekeggs.quartzextendschedulercenter.model.dto.job.JobManageDTO;
import org.leekeggs.quartzextendschedulercenter.model.dto.job.JobTriggerDTO;
import org.leekeggs.quartzextendschedulercenter.model.dto.job.RefreshJobTriggerDTO;
import org.leekeggs.quartzextendschedulercenter.model.dto.job.RemoveLocalJobTriggerDTO;
import org.leekeggs.quartzextendschedulercenter.model.entity.QuartzSchedulerInstance;
import org.leekeggs.quartzextendschedulercenter.model.entity.QuartzSchedulerJobTriggerInfo;
import org.leekeggs.quartzextendschedulercenter.service.QuartzJobManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.leekeggs.quartzextendschedulercenter.constant.QuartzApiConstants.*;

/**
 * @author redcoder54
 * @since 2022-01-09
 */
@Service
public class QuartzJobManageServiceImpl implements QuartzJobManageService {

    private static final String OK = "OK";

    @Resource
    private QuartzSchedulerInstanceMapper instanceMapper;
    @Resource
    private QuartzSchedulerJobTriggerInfoMapper infoMapper;

    @Override
    public List<String> getSchedNames() {
        Example example = Example.builder(QuartzSchedulerJobTriggerInfo.class)
                .select("schedName")
                .distinct()
                .orderByDesc("schedName")
                .build();
        List<QuartzSchedulerJobTriggerInfo> list = infoMapper.selectByExample(example);
        return list.stream().map(QuartzSchedulerJobTriggerInfo::getSchedName).collect(Collectors.toList());
    }

    @Override
    public List<JobTriggerDTO> getJobTriggerInfos(@Nullable String schedName) {
        Example.Builder builder = Example.builder(QuartzSchedulerJobTriggerInfo.class)
                .orderByDesc("schedName");
        if (StringUtils.hasText(schedName)) {
            builder.where(Sqls.custom().andEqualTo("schedName", schedName));
        }
        Example example = builder.build();

        List<QuartzSchedulerJobTriggerInfo> list = infoMapper.selectByExample(example);
        return list.stream().map(t -> {
            JobTriggerDTO dto = new JobTriggerDTO();
            // 属性赋值
            BeanUtils.copyProperties(t, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public JobTriggerDTO refreshJobTrigger(RefreshJobTriggerDTO dto) {
        // 刷新数据
        refreshJobTriggerInfoInternal(dto);
        // 获取刷新后的数据
        Example example = Example.builder(QuartzSchedulerJobTriggerInfo.class)
                .andWhere(Sqls.custom()
                        .andEqualTo("schedName", dto.getSchedName())
                        .andEqualTo("triggerName", dto.getTriggerName())
                        .andEqualTo("triggerGroup", dto.getTriggerGroup()))
                .build();
        QuartzSchedulerJobTriggerInfo jobTriggerInfo = infoMapper.selectOneByExample(example);

        JobTriggerDTO jobTriggerDTO = new JobTriggerDTO();
        BeanUtils.copyProperties(jobTriggerInfo, jobTriggerDTO);
        return jobTriggerDTO;
    }

    private void refreshJobTriggerInfoInternal(RefreshJobTriggerDTO dto) {
        QuartzSchedulerInstance instance = getQuartzSchedulerInstance(dto.getSchedName());

        String url = "http://" + instance.getInstanceHost() + ":" + instance.getInstancePort() + JOB_TRIGGER_INFO_QUERY;
        Map<String, String> queryParams = MapUtils.buildMap("triggerName", dto.getTriggerName(),
                "triggerGroup", dto.getTriggerGroup());
        ApiResult<QuartzJobTriggerInfo> result = HttpTemplate.doGet(url, queryParams,
                new TypeReference<ApiResult<QuartzJobTriggerInfo>>() {
                });
        if (result.getStatus() != 0) {
            throw new JobManageException("刷新job和trigger信息失败：" + result.getMessage());
        }

        // 更新数据
        QuartzSchedulerJobTriggerInfo info = QuartzSchedulerJobTriggerInfo.valueOf(result.getData());
        info.setUpdateTime(new Date());
        infoMapper.updateByPrimaryKeySelective(info);
    }

    @Override
    public boolean removeLocal(RemoveLocalJobTriggerDTO dto) {
        QuartzSchedulerJobTriggerInfo info = new QuartzSchedulerJobTriggerInfo();
        info.setSchedName(dto.getSchedName());
        info.setTriggerName(dto.getTriggerName());
        info.setTriggerGroup(dto.getTriggerGroup());
        int i = infoMapper.deleteByPrimaryKey(info);
        return i > 0;
    }

    @Override
    public void triggerJob(JobManageDTO jobManageDTO) {
        String result = executeCommand(jobManageDTO, TRIGGER_JOB);
        if (OK.equals(result)) {
            return;
        }
        throw new JobManageException("触发job失败：" + result);
    }

    @Override
    public void pauseJob(JobManageDTO jobManageDTO) {
        String result = executeCommand(jobManageDTO, PAUSE_JOB);
        if (OK.equals(result)) {
            return;
        }
        throw new JobManageException("暂停job失败：" + result);
    }

    @Override
    public void resumeJob(JobManageDTO jobManageDTO) {
        String result = executeCommand(jobManageDTO, RESUME_JOB);
        if (OK.equals(result)) {
            return;
        }
        throw new JobManageException("恢复job失败：" + result);
    }

    @Override
    public void deleteJob(JobManageDTO jobManageDTO) {
        String result = executeCommand(jobManageDTO, DELETE_JOB);
        if (OK.equals(result)) {
            QuartzSchedulerJobTriggerInfo info = new QuartzSchedulerJobTriggerInfo();
            info.setSchedName(jobManageDTO.getSchedName());
            info.setJobName(jobManageDTO.getJobName());
            info.setJobGroup(jobManageDTO.getJobGroup());
            infoMapper.delete(info);
            return;
        }
        throw new JobManageException("删除job失败：" + result);
    }

    private String executeCommand(JobManageDTO jobManageDTO, String api) {
        QuartzSchedulerInstance instance = getQuartzSchedulerInstance(jobManageDTO.getSchedName());
        String url = "http://" + instance.getInstanceHost() + ":" + instance.getInstancePort() + api;
        Map<String, String> formParams = MapUtils.buildMap("jobName", jobManageDTO.getJobName(),
                "jobGroup", jobManageDTO.getJobGroup());
        ApiResult<Boolean> result = HttpTemplate.doPost(url, formParams,
                new TypeReference<ApiResult<Boolean>>() {
                });
        if (result.getStatus() == 0 && Boolean.TRUE.equals(result.getData())) {
            return OK;
        }
        return result.getMessage();
    }

    private QuartzSchedulerInstance getQuartzSchedulerInstance(String schedName) {
        Example example = Example.builder(QuartzSchedulerInstance.class)
                .select("instanceHost", "instancePort")
                .where(Sqls.custom().andEqualTo("schedName", schedName))
                .build();
        List<QuartzSchedulerInstance> instances = instanceMapper.selectByExample(example);
        return instances.get(0);
    }
}
