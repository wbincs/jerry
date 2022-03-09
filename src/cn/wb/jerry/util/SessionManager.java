package cn.wb.jerry.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.wb.jerry.http.Request;
import cn.wb.jerry.http.Response;
import cn.wb.jerry.http.StandardSession;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理Session的工具类，用来对session进行创建、存储、更新、过期检查
 */
public class SessionManager {

    // 默认开启自动检查Session是否失效的线程
    static {
        startSessionOutdateCheckThread();
    }

    // 需要用线程安全的map来实现
    private static Map<String, StandardSession> sessionMap = new ConcurrentHashMap<>();

    // session过期时间，默认30分钟，可以通过web.xml
    private static int defaultTimeout = WebXMLUtil.getTimeout();

    // 这是获取session的主逻辑
    // 如果浏览器没有传递jsessionid 过来，那么就创建一个新的session
    // 如果浏览器传递过来的jsessionid无效，那么也创建一个新的session
    // 否则就使用现成的session,并且修改它的lastAccessedTime，以及创建对应的cookie
    public static HttpSession getSession(String jsessionid, Request request, Response response) {
        if (null == jsessionid) {
            return newSession(request, response);
        } else {
            StandardSession currentSession = sessionMap.get(jsessionid);
            if (null == currentSession) {
                return newSession(request, response);
            } else {
                currentSession.setLastAccessedTime(System.currentTimeMillis());
                // 更新cookie的session过期时间
                createCookieBySession(currentSession, request, response);
                return currentSession;
            }
        }
    }

    // 创建session，并创建cookie放入到response
    private static HttpSession newSession(Request request, Response response) {
        // 获取ApplicationContext上下文
        ServletContext servletContext = request.getServletContext();
        // 生成JSessionId
        String sid = generateSessionId();
        // 创建session
        StandardSession session = new StandardSession(sid, servletContext);
        session.setMaxInactiveInterval(defaultTimeout);
        sessionMap.put(sid, session);
        createCookieBySession(session, request, response);
        return session;
    }

    // 创建cookie放入到response，更新cookie的session过期时间
    private static void createCookieBySession(HttpSession session, Request request, Response response) {
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        // Path表示访问服务器的应用会提交这个cookie到服务端
        // 如果其值是 /a, 那么就表示仅仅访问 /a 路径的时候才会提交 cookie
        // 如果其值是 / , 那么表示访问服务器的所有应用都会提交这个cookie到服务端
        cookie.setPath(request.getContext().getPath());
//        cookie.setMaxAge(session.getMaxInactiveInterval());
        // 设置为0表示关闭浏览器就校徽
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // 生成sessionid
    // TODO synchronized效率不高
    private static synchronized String generateSessionId() {
        String result = null;
        byte[] bytes = RandomUtil.randomBytes(16);
        result = new String(bytes);
        result = SecureUtil.md5(result);
        result = result.toUpperCase();
        return result;
    }

    // 每隔30秒调用一次checkOutDateSession，判断session是否失效
    private static void startSessionOutdateCheckThread() {
        (new Thread(() -> {
            while (true) {
                SessionManager.checkOutDateSession();
                ThreadUtil.sleep(30 * 1000);
            }
        })).start();
    }

    // 从sessionMap里根据lastAccessedTime筛选出过期的jsessionid，然后把他们从sessionMap里删除
    private static void checkOutDateSession() {
        Set<String> jsessionids = sessionMap.keySet();
        List<String> outdateJessionIds = new ArrayList<>();
        for (String jsessionid : jsessionids) {
            StandardSession session = sessionMap.get(jsessionid);
            long interval = System.currentTimeMillis() - session.getLastAccessedTime();
            // 1分钟等于60000毫秒
            if (interval > (long) (session.getMaxInactiveInterval() * 60000)) {
                outdateJessionIds.add(jsessionid);
            }
        }
        for (String jsessionid : outdateJessionIds) {
            sessionMap.remove(jsessionid);
        }
    }

}
