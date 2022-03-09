package cn.wb.jerry.http;

import cn.wb.jerry.catalina.Context;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用上下文类，Context中用来存储应用的上下文数据
 */
public class ApplicationContext extends BaseServletContext {

    // 应用上下文
    private Context context;

    // 存储attribute
    private Map<String, Object> attributesMap = new ConcurrentHashMap<>();

    public ApplicationContext(Context context) {
        this.context = context;
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

    // 基于docBase取
    @Override
    public String getRealPath(String path) {
        return (new File(this.context.getDocBase(), path)).getAbsolutePath();
    }

}
