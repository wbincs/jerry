package cn.wb.jerry.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.wb.jerry.classloader.WebappClassLoader;
import cn.wb.jerry.exception.WebConfigDuplicatedException;
import cn.wb.jerry.http.ApplicationContext;
import cn.wb.jerry.http.StandardFilterConfig;
import cn.wb.jerry.http.StandardServletConfig;
import cn.wb.jerry.util.ContextXMLUtil;
import cn.wb.jerry.watcher.ContextFileChangeWatcher;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用上下文对象，一个Context对象对应一个应用
 */
public class Context {

    // 应用path，例如   "/"   "/a"   "/b"
    private String path;

    // 应用在磁盘上的位置
    private String docBase;

    //热部署需要用到
    private Host host;

    // 是否可重复加载
    private boolean reloadable;

    // 配置文件 web.xml
    private File contextWebXmlFile;

    // 每个web应用独立的类加载器
    private WebappClassLoader webappClassLoader;

    // 热部署监听器
    private ContextFileChangeWatcher contextFileChangeWatcher;

    // 存储上下文对象，即applicationContext，里边存储Attribute和context对象
    private ServletContext servletContext;

    // Servlet相关的类
    // 存储servlet单例
    private Map<Class<?>, HttpServlet> servletPool;
    // 地址对应Servlet的类名
    private Map<String, String> url_servletClassName;
    // 地址对应Servlet的名称
    private Map<String, String> url_ServletName;
    // Servlet的名称对应类名
    private Map<String, String> servletName_className;
    // Servlet类名对应的名称
    private Map<String, String> className_servletName;
    // servlet的类名对应的初始化参数
    private Map<String, Map<String, String>> servlet_className_init_params;

    // Filter相关的类
    // 存储filter单例
    private Map<String, Filter> filterPool;
    // Servlet自启动使用
    private List<String> loadOnStartupServletClassNames;
    // url对应filter的类名
    private Map<String, List<String>> url_filterClassName;
    // url对应filter的名称
    private Map<String, List<String>> url_FilterNames;
    // filter的名称对应类名
    private Map<String, String> filterName_className;
    // filter的类名对应名称
    private Map<String, String> className_filterName;
    // filter的类名对应的初始化参数
    private Map<String, Map<String, String>> filter_className_init_params;

    // Listener集合
    private List<ServletContextListener> listeners;

    public Context(String path, String docBase, Host host, boolean reloadable) {
        this.path = path;
        this.docBase = docBase;
        this.host = host;
        this.reloadable = reloadable;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);
        this.loadOnStartupServletClassNames = new ArrayList<>();
        this.servletContext = new ApplicationContext(this);

        this.servletPool = new ConcurrentHashMap<>();
        this.url_servletClassName = new HashMap<>();
        this.url_ServletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        this.servlet_className_init_params = new HashMap<>();

        this.filterPool = new ConcurrentHashMap<>();
        this.url_filterClassName = new HashMap<>();
        this.url_FilterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filter_className_init_params = new HashMap<>();

        this.listeners = new ArrayList<>();

        TimeInterval timeInterval = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms\r\n",
                this.docBase, timeInterval.intervalMs());
    }

    // 发布加载应用
    private void deploy() {
        // 加载监听器
        loadListeners();
        // 初始化
        init();
        // 如果允许重复加载的话，就开启文件监听，实现热部署
        if (reloadable) {
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
//      这里进行了JspRuntimeContext的初始化，就是为了能够在jsp所转换的java文件里的
//      javax.sevlet.isp.JspFactory.getDefaulFactory()这行能够有返回值
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
    }

    // 初始化该应用
    private void init() {
        if (contextWebXmlFile.exists()) {
            try {
                // 检查servlet配置是否符合规范
                checkDuplicated();
            } catch (WebConfigDuplicatedException e) {
                e.printStackTrace();
                return;
            }
            String xml = FileUtil.readUtf8String(this.contextWebXmlFile);
            Document d = Jsoup.parse(xml);

            // 解析servlet映射
            parseServletMapping(d);
            // 解析filter映射
            parseFilterMapping(d);

            // 解析servlet初始化参数
            // 解析之后并不是立即使用，而是等到实际初始化servlet实例的时候才取出来使用
            parseServletInitParams(d);
            // 解析filter初始化参数
            parseFilterInitParams(d);
            // 初始化filter
            initFilter();

            // 解析需要在启动时就实例化的servlet类
            parseLoadOnStartup(d);
            // 将上一步解析出来的serlvet
            handleLoadOnStartup();
            // 监听事件
            fireEvent("init");
        }
    }

    // 停止应用
    public void stop() {
        // 先关掉类加载器
        webappClassLoader.stop();
        // 关掉监听器
        contextFileChangeWatcher.stop();
        // 销毁servlet，调用已经加载的servlet的destroy方法
        destroyServlets();
        // 处理destroy事件，通知监听器做处理
        fireEvent("destroy");
    }

    // 重新加载，热部署使用
    public void reload() {
        this.host.reload(this);
    }

    // 检查servlet配置是否符合规范
    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");
    }

    private void checkDuplicated(Document d, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = d.select(mapping);
        List<String> contents = new ArrayList<>();
        for (Element e : elements) {
            contents.add(e.text());
        }
        Collections.sort(contents);

        for (int i = 0; i < contents.size() - 1; ++i) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }
    }

    // 解析servlet映射
    // 初始化四个map
    // url_ServletName
    // url_servletClassName
    // servletName_className
    // className_servletName
    private void parseServletMapping(Document d) {
        // url_ServletName
        Elements mappingurlElements = d.select("servlet-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url_ServletName.put(urlPattern, servletName);
        }

        Elements servletNameElements = d.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }

        // url_servletClassName
        Set<String> urls = url_ServletName.keySet();
        for (String url : urls) {
            String servletName = url_ServletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public String getDocBase() {
        return docBase;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    // 感觉synchronized效率低，改用单例工厂，注意需要进行两次判断null == servlet
    public HttpServlet getServlet(Class<?> clazz) throws InstantiationException, IllegalAccessException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);
        if (null == servlet) {
            synchronized (this) {
                servlet = servletPool.get(clazz);
                if (null == servlet) {
//                    System.out.println("servletClass:" + clazz);
//                    System.out.println("servletClass' classLoader:" + clazz.getClassLoader());
                    // 创建servlet实例
                    servlet = (HttpServlet) clazz.newInstance();

                    // 获取上下文对象
                    ServletContext servletContext = getServletContext();
                    String className = clazz.getName();
                    String servletName = className_servletName.get(className);
                    Map<String, String> initParameters = servlet_className_init_params.get(className);
                    // 创建servlet配置对象，包含上下文，servlet名称，初始化参数集合
                    ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
                    // 执行servlet初始化逻辑
                    servlet.init(servletConfig);

                    servletPool.put(clazz, servlet);
                }
            }
        }
        return servlet;
    }

    // 解析servlet初始化参数
    private void parseServletInitParams(Document d) {
        Elements servletClassNameElements = d.select("servlet-class");
        for (Element servletClassNameElement : servletClassNameElements) {
            // 遍历每个servlet
            String servletClassName = servletClassNameElement.text();
            // 遍历每个servlet里的init参数
            Elements initElements = servletClassNameElement.parent().select("init-param");
            if (initElements.isEmpty()) {
                continue;
            }
            Map<String, String> initParams = new HashMap<>();
            for (Element element : initElements) {
                String name = (element.select("param-name").get(0)).text();
                String value = (element.select("param-value").get(0)).text();
                initParams.put(name, value);
            }
            this.servlet_className_init_params.put(servletClassName, initParams);
        }
    }

    private void destroyServlets() {
        Collection<HttpServlet> servlets = this.servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    public void parseLoadOnStartup(Document d) {
        Elements es = d.select("load-on-startup");
        for (Element e : es) {
            String loadOnStartupServletClassName = e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }

    public void handleLoadOnStartup() {
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                this.getServlet(clazz);
            } catch (InstantiationException | IllegalAccessException | ServletException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public WebappClassLoader getWebClassLoader() {
        return this.webappClassLoader;
    }

    // 解析filter映射
    public void parseFilterMapping(Document d) {
        // url_FilterNames
        Elements mappingurlElements = d.select("filter-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String filterName = mappingurlElement.parent().select("filter-name").first().text();
            List<String> filterNames = url_FilterNames.get(urlPattern);
            if (null == filterNames) {
                filterNames = new ArrayList<>();
                this.url_FilterNames.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }

        // class_name_filter_name
        Elements filterNameElements = d.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String fiterClass = filterNameElement.parent().select("filter-class").first().text();
            filterName_className.put(filterName, fiterClass);
            className_filterName.put(fiterClass, filterName);
        }

        // url_filterClassName
        Set<String> urls = url_FilterNames.keySet();
        for (String url : urls) {
            List<String> filterNames = url_FilterNames.get(url);
            if (null == filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(url, filterNames);
            }

            for (String filterName : filterNames) {
                String filterClassName = filterName_className.get(filterName);
                List<String> filterClassNames = url_filterClassName.get(url);
                if (null == filterClassNames) {
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    // 解析filter初始化参数，存入filter_className_init_params
    private void parseFilterInitParams(Document d) {
        Elements filterClassNameElements = d.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();
            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;
            Map<String, String> initParams = new HashMap<>();
            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }
            filter_className_init_params.put(filterClassName, initParams);
        }
    }

    // 初始化filter
    private void initFilter() {
        Set<String> classNames = this.className_filterName.keySet();
        for (String className : classNames) {
            try {
                Class<?> clazz = getWebClassLoader().loadClass(className);
                System.out.println("clazz " + clazz);
                System.out.println(className);
                Map<String, String> initParameters = filter_className_init_params.get(className);
                String filterName = className_filterName.get(className);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, filterName, initParameters);
                Filter filter = filterPool.get(className);
                if (null == filter) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * 根据uri获取匹配到的filter
     */
    public List<Filter> getMatchedFilters(String uri) {
        // 存储所有匹配到的filter
        List<Filter> filters = new ArrayList<>();

        Set<String> patterns = url_filterClassName.keySet();
        // 存储所有匹配到的pattern
        Set<String> matchedPatterns = new HashSet<>();
        for (String pattern : patterns) {
            if (this.match(pattern, uri)) {
                matchedPatterns.add(pattern);
            }
        }

        // 根据匹配到的pattern，获取filter的类名
        Set<String> matchedFilterClassNames = new HashSet<>();
        for (String pattern : matchedPatterns) {
            List<String> filterClassName = url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }

        // 根据类名从filterPoll中拿到filter实例
        for (String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }

        return filters;
    }

    // 判断uri和模式是否匹配
    private boolean match(String pattern, String uri) {
        // 完全匹配
        if (StrUtil.equals(pattern, uri)) {
            return true;
        // /*模式
        } else if (StrUtil.equals(pattern, "/*")) {
            return true;
        // 后缀名模式
        } else if (StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            return StrUtil.equals(patternExtName, uriExtName);
        }
        // 其他匹配模式暂未考虑
        return false;
    }

    public void addListener(ServletContextListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ServletContextListener listener) {
        this.listeners.remove(listener);
    }

    private void loadListeners() {
        try {
            if (contextWebXmlFile.exists()) {
                String xml = FileUtil.readUtf8String(contextWebXmlFile);
                Document d = Jsoup.parse(xml);
                Elements es = d.select("listener listener-class");
                for (Element e : es) {
                    String listenerClassName = e.text();
                    Class<?> clazz = this.getWebClassLoader().loadClass(listenerClassName);
                    ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                    addListener(listener);
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IORuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener servletContextListener : listeners) {
            if ("init".equals(type)) {
                servletContextListener.contextInitialized(event);
            }
            if ("destroy".equals(type)) {
                servletContextListener.contextDestroyed(event);
            }
        }
    }

}
