package teammates.ui.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import teammates.common.util.HttpRequestHelper;
import teammates.common.util.Logger;

public class WebApiLoggingFilter implements Filter {

    private static final Logger log = Logger.getLogger();

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        response.setHeader("Strict-Transport-Security", "max-age=31536000");

        log.info("Request received: [" + request.getMethod() + "] " + request.getRequestURL().toString()
                + ", Params: " + HttpRequestHelper.getRequestParametersAsString(request)
                + ", Headers: " + HttpRequestHelper.getRequestHeadersAsString(request));

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}
