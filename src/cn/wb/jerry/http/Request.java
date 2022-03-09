package cn.wb.jerry.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.wb.jerry.catalina.Connector;
import cn.wb.jerry.catalina.Context;
import cn.wb.jerry.catalina.Engine;
import cn.wb.jerry.catalina.Host;
import cn.wb.jerry.util.MyHttpUtil;
import cn.wb.jerry.util.WebXMLUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;

import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 用户请求的封装
 */
public class Request extends BaseRequest {

    private Socket socket;

    private Connector connector;

    // http的请求字符串
    private String requestString;

    // 去掉的应用名称的uri
    private String uri;

    // 上下文，即应用
    private Context context;

    // 请求方法 get post
    private String method;

    // 请求中 ? 之后的请求字符串
    private String queryString;

    // 请求中 ? 之后的参数map
    private Map<String, String[]> parameterMap;

    // 请求头map
    private Map<String, String> headerMap;

    // cookie
    private Cookie[] cookies;

    // session，默认创建的request是没有session的，在httpProcessor处理的时候，会添加进来
    private HttpSession session;

    // 服务端跳转
    private boolean forwarded;

    // 服务端跳转传参
    private Map<String, Object> attributesMap;

    public Request(Socket socket, Connector connector) throws IOException {
        this.socket = socket;
        this.connector = connector;

        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();

        // 解析HttpRequest，将HTTP请求字符串保存到requestString中
        parseHttpRequest();
        // 请求字符串不为空的话，继续进行解析
        if (!StrUtil.isEmpty(requestString)) {
            // 以下都要在requestString的基础上进行解析
            // 解析请求方法 get post ..
            parseMethod();
            // 解析uri
            parseUri();
            // 解析上下文
            parseContext();
            // 如果上下文不是/，即不是webapps下的ROOT应用，需要将uri前边的context前缀去掉
            if (!"/".equals(context.getPath())) {
                uri = StrUtil.removePrefix(uri, context.getPath());
                // 上下文之后没有路径的话，那么添加上/，例如请求/javaweb，上下文是/javaweb，uri则是/
                if (StrUtil.isEmpty(uri)) {
                    uri = "/";
                }
            }
            // 如果请求的uri是根路径，那么获取欢迎页
            if ("/".equals(uri)) {
                uri = "/" + WebXMLUtil.getWelcomeFile(context);
            }
            // 从请求中解析参数
            parseParameters();
            // 从请求中解析请求头信息
            parseHeaders();
            // 从请求中解析cookie
            parseCookies();
        }
    }

    /**
     * 从socket解析请求，将请求字符串保存到requestString
     */
    private void parseHttpRequest() throws IOException {
        InputStream is = socket.getInputStream();
        byte[] bytes = MyHttpUtil.readBytes(is, false);
        requestString = new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 解析请求方法
     */
    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    /**
     * 根据requestString来解析uri，从请求开始，两个空格之间的信息即为uri字符串
     */
    private void parseUri() {
        String temp = StrUtil.subBetween(requestString, " ", " ");
        // 截掉路径中的参数部分
        if (StrUtil.contains(temp, '?')) {
            temp = StrUtil.subBefore(temp, '?', false);
        }
        uri = temp;
    }

    /**
     * 解析上下文，即应用名称
     * 当前只从defaultHost中取出应用，暂不支持多host
     */
    private void parseContext() {
        Engine engine = connector.getService().getEngine();
        Host host = engine.getDefaultHost();
        // 直接根据uri取上下文，如果uri是/，可以直接取出来，否则的话进入if逻辑，需要对uri进行截取
        context = host.getContext(uri);
        if (null == context) {
            String path = StrUtil.subBetween(uri, "/", "/");
            if (null == path) {
                path = "/";
            } else {
                path = "/" + path;
            }
            context = host.getContext(path);
            if (null == context) {
                context = host.getContext("/");
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public String getRequestString() {
        return requestString;
    }

    public String getUri() {
        return uri;
    }

    public Context getContext() {
        return context;
    }

    public Connector getConnector() {
        return connector;
    }

    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext() {
        return this.context.getServletContext();
    }

    public String getRealPath(String path) {
        return this.getServletContext().getRealPath(path);
    }

    // 有关请求参数 get和post
    private void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(this.requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                this.queryString = StrUtil.subAfter(url, '?', false);
            }
        }

        if ("POST".equals(this.getMethod())) {
            this.queryString = StrUtil.subAfter(this.requestString, "\r\n\r\n", false);
        }

        if (null != this.queryString && 0 != this.queryString.length()) {
            this.queryString = URLUtil.decode(this.queryString);
            String[] parameterValues = this.queryString.split("&");
            for (String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                // 参数名称
                String name = nameValues[0];
                // 参数值
                String value = nameValues[1];
                String[] values = this.parameterMap.get(name);
                if (null == values) {
                    values = new String[]{value};
                } else {
                    values = ArrayUtil.append(values, value);
                }
                this.parameterMap.put(name, values);
            }
        }
    }

    public String getParameter(String name) {
        String[] values = this.parameterMap.get(name);
        return null != values && 0 != values.length ? values[0] : null;
    }

    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(this.parameterMap.keySet());
    }

    public String[] getParameterValues(String name) {
        return this.parameterMap.get(name);
    }

    // http的头信息
    public void parseHeaders() {
        StringReader stringReader = new StringReader(this.requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        for (int i = 1; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (0 == line.length()) {
                break;
            }
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            this.headerMap.put(headerName, headerValue);
        }
    }

    public String getHeader(String name) {
        if (null == name) {
            return null;
        } else {
            name = name.toLowerCase();
            return this.headerMap.get(name);
        }
    }

    public Enumeration<String> getHeaderNames() {
        Set<String> keys = this.headerMap.keySet();
        return Collections.enumeration(keys);
    }

    public int getIntHeader(String name) {
        String value = this.headerMap.get(name);
        return Convert.toInt(value, 0);
    }

    // Request的常见方法
    public String getLocalAddr() {
        return this.socket.getLocalAddress().getHostAddress();
    }

    public String getLocalName() {
        return this.socket.getLocalAddress().getHostName();
    }

    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    public String getProtocol() {
        return "HTTP:/1.1";
    }

    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) this.socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }

    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) this.socket.getRemoteSocketAddress();
        return isa.getHostName();
    }

    public int getRemotePort() {
        return this.socket.getPort();
    }

    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        return this.getHeader("host").trim();
    }

    public int getServerPort() {
        return this.getLocalPort();
    }

    public String getContextPath() {
        String result = this.context.getPath();
        return "/".equals(result) ? "" : result;
    }

    public String getRequestURI() {
        return this.uri;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = this.getScheme();
        int port = this.getServerPort();
        if (port < 0) {
            port = 80;
        }
        // 添加url请求前缀，包括协议，主机名，端口
        url.append(scheme);
        url.append("://");
        url.append(this.getServerName());
        if (scheme.equals("http") && port != 80 || scheme.equals("https") && port != 443) {
            url.append(':');
            url.append(port);
        }
        // 添加上下文path
        url.append(this.getContext().getPath());
        // 添加请求的资源uri
        url.append(this.getRequestURI());
        return url;
    }

    public String getServletPath() {
        return this.uri;
    }

    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = this.headerMap.get("cookie");
        if (null != cookies) {
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (!StrUtil.isBlank(pair)) {
                    String[] segs = StrUtil.split(pair, "=");
                    String name = segs[0].trim();
                    String value = segs[1].trim();
                    Cookie cookie = new Cookie(name, value);
                    cookieList.add(cookie);
                }
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    public Cookie[] getCookies() {
        return this.cookies;
    }

    // 从Cookies中获取JSessionId
    public String getJSessionIdFromCookie() {
        if (null != this.cookies) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public HttpSession getSession() {
        return this.session;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isForwarded() {
        return this.forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }

    public void setAttribute(String name, Object value) {
        this.attributesMap.put(name, value);
    }

    public Object getAttribute(String name) {
        return this.attributesMap.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        Set<String> keys = this.attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    public void removeAttribute(String name) {
        this.attributesMap.remove(name);
    }

}
