package cn.wb.jerry.http;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter配置类
 */
public class StandardFilterConfig implements FilterConfig {

    // 应用上下文
    private ServletContext servletContext;

    // filter名称
    private String filterName;

    // filter初始化参数
    private Map<String, String> initParameters;

    public StandardFilterConfig(ServletContext servletContext, String filterName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.filterName = filterName;
        this.initParameters = initParameters;
        if (null == this.initParameters) {
            this.initParameters = new HashMap<>();
        }
    }

    public String getFilterName() {
        return this.filterName;
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }

    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }

    public String getInitParameter(String name) {
        return this.initParameters.get(name);
    }
}
