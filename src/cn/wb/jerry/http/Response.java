package cn.wb.jerry.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 用户相应的封装
 */
public class Response extends BaseResponse {

    private StringWriter stringWriter;

    private PrintWriter writer;

    // 响应内容的类型
    private String contentType;

    // 响应内容的主体
    private byte[] body;

    // 响应的状态码 200 404 500
    private int status;

    // 响应中请求设置的cookie
    private List<Cookie> cookies;

    // 重定向地址
    private String redirectPath;

    public Response() {
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public PrintWriter getWriter() {
        return this.writer;
    }

    public byte[] getBody() {
        if (null == body) {
            String content = stringWriter.toString();
            body = content.getBytes(StandardCharsets.UTF_8);
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public String getCookiesHeader() {
        if (null == this.cookies) {
            return "";
        } else {
            String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
            StringBuffer stringBuffer = new StringBuffer();
            for (Cookie cookie : getCookies()) {
                stringBuffer.append("\r\n");
                stringBuffer.append("Set-Cookie: ");
                stringBuffer.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
                if (-1 != cookie.getMaxAge()) {
                    stringBuffer.append("Expires=");
                    Date now = new Date();
                    Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                    stringBuffer.append(sdf.format(expire));
                    stringBuffer.append("; ");
                }

                if (null != cookie.getPath()) {
                    stringBuffer.append("Path=").append(cookie.getPath());
                }
            }

            return stringBuffer.toString();
        }
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }
}
