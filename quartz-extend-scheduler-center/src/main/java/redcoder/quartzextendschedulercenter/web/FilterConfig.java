package redcoder.quartzextendschedulercenter.web;

import redcoder.quartzextendschedulercenter.service.LoginService;
import redcoder.quartzextendschedulercenter.web.filter.AuthenticateFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wuxiaoyuan
 * @since 2021-03-10
 */
@Configuration
public class FilterConfig {

    // @Bean
    // public FilterRegistrationBean<SimpleCorsFilter> corsFilterRegisterBean() {
    //     FilterRegistrationBean<SimpleCorsFilter> filterRegistrationBean = new FilterRegistrationBean<>();
    //     filterRegistrationBean.setFilter(new SimpleCorsFilter());
    //     filterRegistrationBean.addUrlPatterns("/api/*");
    //     filterRegistrationBean.setOrder(0);
    //     return filterRegistrationBean;
    // }

    @Bean
    public FilterRegistrationBean<AuthenticateFilter> authenticateFilterRegistrationBean(LoginService loginService) {
        FilterRegistrationBean<AuthenticateFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new AuthenticateFilter(loginService));
        filterRegistrationBean.addUrlPatterns("/api/instance/*", "/api/job/*");
        filterRegistrationBean.setOrder(0);
        return filterRegistrationBean;
    }
}
