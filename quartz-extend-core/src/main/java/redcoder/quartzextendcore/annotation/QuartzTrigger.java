package redcoder.quartzextendcore.annotation;

import redcoder.quartzextendcore.core.DefaultQuartzKeyNameGenerator;
import redcoder.quartzextendcore.core.QuartzJobBeanPostProcessor;
import org.quartz.Scheduler;
import redcoder.quartzextendcore.core.QuartzKeyNameGenerator;

import java.lang.annotation.*;

/**
 * 用于定义quartz job's trigger
 *
 * @author redcoder54
 * @see QuartzJobBeanPostProcessor
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface QuartzTrigger {

    /**
     * the name of Trigger's TriggerKey
     */
    String triggerKeyName() default "";

    /**
     * the group of Trigger's TriggerKey
     */
    String triggerKeyGroup() default Scheduler.DEFAULT_GROUP;

    /**
     * trigger's  description
     */
    String triggerDescription() default "";

    /**
     * the cron expression string to base the schedule on
     */
    String cron();

    /**
     * Specifies the strategy indicates how to generate Quartz JobKey name and Trigger name.
     */
    Class<? extends QuartzKeyNameGenerator> quartzKeyNameGenerator() default DefaultQuartzKeyNameGenerator.class;
}
