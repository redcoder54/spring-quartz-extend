package redcoder.quartzextendschedulercenter.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import redcoder.quartzextendschedulercenter.mapper.QuartzSchedulerInstanceMapper;
import redcoder.quartzextendschedulercenter.model.dto.ApiResult;
import redcoder.quartzextendschedulercenter.model.dto.instance.QuartzInstanceDTO;
import redcoder.quartzextendschedulercenter.model.entity.QuartzSchedulerInstance;
import redcoder.quartzextendschedulercenter.service.QuartzJobSchedulerService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author redcoder54
 * @since 2021-04-26
 */
@RestController
@RequestMapping("/api/instance")
@Api(tags = "Quartz实例管理")
public class QuartzInstanceController {

    @Resource
    private QuartzSchedulerInstanceMapper instanceMapper;
    @Resource
    private QuartzJobSchedulerService quartzJobSchedulerService;

    @GetMapping("/list")
    @ApiOperation(value = "获取实例", httpMethod = "GET")
    public ApiResult<List<QuartzInstanceDTO>> getInstancesList(@RequestParam(required = false) String schedName) {
        Example.Builder builder = Example.builder(QuartzSchedulerInstance.class)
                .orderByDesc("schedName");
        if (StringUtils.hasText(schedName)) {
            builder.where(Sqls.custom().andEqualTo("schedName", schedName));
        }
        Example example = builder.build();

        List<QuartzSchedulerInstance> list = instanceMapper.selectByExample(example);
        List<QuartzInstanceDTO> data = list.stream().map(QuartzInstanceDTO::valueOf).collect(Collectors.toList());
        return ApiResult.success(data);
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "删除实例", httpMethod = "DELETE")
    public ApiResult<Boolean> delete(@Valid @RequestBody QuartzInstanceDTO dto) {
        return ApiResult.success(quartzJobSchedulerService.deleteInstance(dto));
    }
}
