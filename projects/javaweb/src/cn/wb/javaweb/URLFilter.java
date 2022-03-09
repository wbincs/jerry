package cn.wb.javaweb;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class URLFilter implements Filter {

    public void init(FilterConfig filterConfig) {
        System.out.println("URLFilter初始化");
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        String url = request.getRequestURL().toString();
        System.out.println("url filter:" + url);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
        System.out.println("URLFilter 的 destroy() 被调用");
    }

}
