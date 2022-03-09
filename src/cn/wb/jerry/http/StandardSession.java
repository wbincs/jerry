package cn.wb.jerry.http;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户会话对象
 */
public class StandardSession implements HttpSession {

    // 当前Session的唯一id
    private String id;

    // 实际存放数据，采用线程安全的ConcurrentHashMap
    private Map<String, Object> attributesMap = new ConcurrentHashMap<>();

    // 创建时间
    private long creationTime;

    // 最后一次访问时间
    private long lastAccessedTime;

    // 最大持续时间的分钟数
    private int maxInactiveInterval;

    // ApplicationContext上下文
    private ServletContext servletContext;

    public StandardSession(String jsessionid, ServletContext servletContext) {
        this.id = jsessionid;
        this.creationTime = System.currentTimeMillis();
        this.servletContext = servletContext;
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attributesMap.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributesMap.get(name);
    }

    @Override
    public void removeAttribute(String name) {
        this.attributesMap.remove(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = this.attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getValue(String arg0) {
        return null;
    }

    @Override
    public String[] getValueNames() {
        return null;
    }

    @Override
    public void invalidate() {
        this.attributesMap.clear();
    }

    @Override
    public boolean isNew() {
        return this.creationTime == this.lastAccessedTime;
    }

    @Override
    public void putValue(String arg0, Object arg1) { }

    @Override
    public void removeValue(String arg0) { }
}
