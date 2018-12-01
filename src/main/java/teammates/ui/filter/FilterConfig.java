package teammates.ui.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<WebApiOriginCheckFilter> webApiOriginCheckFilter() {
        FilterRegistrationBean<WebApiOriginCheckFilter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(new WebApiOriginCheckFilter());
        registrationBean.addUrlPatterns("/webapi/*");

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<WebApiLoggingFilter> webApiloggingFilter() {
        FilterRegistrationBean<WebApiLoggingFilter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(new WebApiLoggingFilter());
        registrationBean.addUrlPatterns("/webapi/*");

        return registrationBean;
    }
}
