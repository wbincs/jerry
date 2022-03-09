package cn.wb.jerry.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.wb.jerry.http.ApplicationFilterChain;
import cn.wb.jerry.http.Request;
import cn.wb.jerry.http.Response;
import cn.wb.jerry.servlet.DefaultServlet;
import cn.wb.jerry.servlet.InvokerServlet;
import cn.wb.jerry.servlet.JspServlet;
import cn.wb.jerry.util.Constant;
import cn.wb.jerry.util.SessionManager;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP请求处理器，用来对封装好的Request和Response进行处理
 */
public class HttpProcessor {

    public void execute(Request request, Response response) {
        try {
            // 获取请求uri
            String uri = request.getUri();
            if (null == uri) {
                return;      //多线程return
            }
            // 解析session
            // 为什么要在这里解析，而且同时传进去request和response
            // 因为要管理session，如果是第一次访问，request里是没有session的，response携带jsessionid
            prepareSession(request, response);

            // 获取应用上下文，从应用上下文中查找servlet、filter
            Context context = request.getContext();

            // 从应用的上下文中获取请求uri所对应的servlet类名
            String servletClassName = context.getServletClassName(uri);
            HttpServlet workingServlet;
            // 如果找到了servlet类名，那么交给InvokerServlet来处理
            if (null != servletClassName) {
                workingServlet = InvokerServlet.getInstance();
                // 如果请求uri后缀是jsp，那么交给JspServlet来处理
            } else if (uri.endsWith(".jsp")) {
                workingServlet = JspServlet.getInstance();
            } else {
                // 如果不满足前边的条件，则交给DefaultServlet处理，相当于请求静态资源
                workingServlet = DefaultServlet.getInstance();
            }

            // 根据请求的uri获取匹配的filters
            List<Filter> filters = context.getMatchedFilters(request.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);

            if (request.isForwarded()) {
                return;
            }

            if (Constant.CODE_200 == response.getStatus()) {
                handle200(request, response);
                return;
            }

            if (Constant.CODE_302 == response.getStatus()) {
                this.handle302(request, response);
                return;
            }

            if (Constant.CODE_404 == response.getStatus()) {
                // 404页面显示的路径需要加上上下文路径
                String uri_404 = uri;
                // 如果上下文是/,就不需要添加了
                if (!request.getContext().getPath().equals("/")) {
                    uri_404 = request.getContext().getPath() + uri_404;
                }
                handle404(request, uri_404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            handle500(request, e);
        } finally {
            Socket s = request.getSocket();
            if (!s.isClosed()) {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handle200(Request request, Response response) throws IOException {
        Socket s = request.getSocket();
        OutputStream os = s.getOutputStream();
        String contentType = response.getContentType();
        byte[] body = response.getBody();
        String cookiesHeader = response.getCookiesHeader();
        // gzip压缩
        boolean gzip = this.isGzip(request, body, contentType);
        String headText;
        if (gzip) {
            headText = Constant.response_head_200_gzip;
        } else {
            headText = Constant.response_head_200;
        }

        headText = StrUtil.format(headText, contentType, cookiesHeader);
        if (gzip) {
            body = ZipUtil.gzip(body);
        }

        byte[] head = headText.getBytes();
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        os.write(responseBytes, 0, responseBytes.length);
        os.flush();
        os.close();
    }

    private void handle302(Request request, Response response) throws IOException {
        System.out.println("handle 302");
        Socket s = request.getSocket();
        OutputStream os = s.getOutputStream();
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text, redirectPath);
        byte[] responseBytes = header.getBytes(StandardCharsets.UTF_8);
        os.write(responseBytes);
    }

    private void handle404(Request request, String uri) throws IOException {
        Socket s = request.getSocket();
        System.out.println("handle 404");
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
    }

    private void handle500(Request request, Exception e) {
        System.out.println("handle 500");

        Socket s = request.getSocket();
        try {
            StackTraceElement[] stes = e.getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append(e.toString());
            sb.append("\r\n");
            for (StackTraceElement ste : stes) {
                sb.append("\t");
                sb.append(ste.toString());
                sb.append("\r\n");
            }
//            String msg = e.getMessage();
//            if (null != msg && msg.length() > 20) {
//                msg = msg.substring(0,19);
//            }

            System.out.println(e.getMessage());
            System.out.println(e.toString());
            System.out.println(sb.toString());

            String text = StrUtil.format(Constant.textFormat_500, e.getMessage(), e.toString(), sb.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes(StandardCharsets.UTF_8);
            OutputStream os = s.getOutputStream();
            os.write(responseBytes);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 准备session
    public void prepareSession(Request request, Response response) {
        String jsessionid = request.getJSessionIdFromCookie();
        // jsessionid可能为空，没关系，交给SessionManager即可，它会进行处理，管理session的生命周期
        HttpSession session = SessionManager.getSession(jsessionid, request, response);
        request.setSession(session);
    }

    private boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings = request.getHeader("Accept-Encoding");
        if (!StrUtil.containsAny(acceptEncodings, "gzip")) {
            return false;
        } else {
            Connector connector = request.getConnector();
            if (mimeType.contains(";")) {
                mimeType = StrUtil.subBefore(mimeType, ";", false);
            }

            if (!"on".equals(connector.getCompression())) {
                return false;
            } else if (body.length < connector.getCompressionMinSize()) {
                return false;
            } else {
                String userAgents = connector.getNoCompressionUserAgents();
                String[] eachUserAgents = userAgents.split(",");
                for (int i = 0; i < eachUserAgents.length; i++) {
                    String eachUserAgent = eachUserAgents[i];
                    eachUserAgent = eachUserAgent.trim();
                    String userAgent = request.getHeader("User-Agent");
                    if (StrUtil.containsAny(userAgent, eachUserAgent)) {
                        return false;
                    }
                }
                String mimeTypes = connector.getCompressibleMimeType();
                String[] eachMimeTypes = mimeTypes.split(",");
                for (int i = 0; i < eachMimeTypes.length; i++) {
                    String eachMimeType = eachMimeTypes[i];
                    if (mimeType.equals(eachMimeType)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }
}
