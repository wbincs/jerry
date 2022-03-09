package cn.wb.jerry.http;

import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

/**
 * Filter拦截器链，采用责任链设计模式
 */
public class ApplicationFilterChain implements FilterChain {

    // filter责任链涉及到的所有filter
    private Filter[] filters;

    // 责任链末尾执行servlet的service方法
    private Servlet servlet;

    // pos记录存储当前执行到那个filter
    int pos;

    public ApplicationFilterChain(List<Filter> filterList, Servlet servlet) {
        this.filters = ArrayUtil.toArray(filterList, Filter.class);
        this.servlet = servlet;
    }

    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (pos < filters.length) {
            // filter没有执行完，继续执行下一个filter
            Filter filter = filters[pos];
            pos++;
            filter.doFilter(request, response, this);
        } else {
            // filter执行完了，调用servlet
            servlet.service(request, response);
        }
    }

}
