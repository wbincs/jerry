package cn.wb.jerry.util;

import cn.hutool.http.HttpUtil;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 自定义的http工具类，用来对url进行请求并获取请求结果
 * getContent   获取对应url的response的content部分，不包括header
 * getHttp      获取请求url的response的所有部分，包括header
 */
public class MyHttpUtil {

    // getContent和getHttp的测试
    public static void main(String[] args) {
        String url = "http://wubin.site/";
        // 第二个参数 是否压缩
        String contentString = getContentString(url, false);
        System.out.println(contentString);
        System.out.println("------------------------------------------");
        String httpString = getHttpString(url, false);
        System.out.println(httpString);
    }

    // getContent
    public static byte[] getContentBytes(String url, boolean gzip) {
        return getContentBytes(url, gzip, null, true);
    }

    public static String getContentString(String url, Map<String, Object> params, boolean isGet) {
        return getContentString(url, false, params, isGet);
    }

    public static String getContentString(String url, boolean gzip) {
        return getContentString(url, gzip, null, true);
    }

    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    public static String getContentString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        byte[] result = getContentBytes(url, gzip, params, isGet);
        if (null == result) {
            return null;
        } else {
            return (new String(result, StandardCharsets.UTF_8)).trim();
        }
    }

    public static byte[] getContentBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        byte[] response = getHttpBytes(url, gzip, params, isGet);
        byte[] doubleReturn = "\r\n\r\n".getBytes();
        int pos = -1;

        for (int i = 0; i < response.length - doubleReturn.length; ++i) {
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);
            if (Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }

        if (-1 == pos) {
            return null;
        } else {
            pos += doubleReturn.length;
            return Arrays.copyOfRange(response, pos, response.length);
        }
    }

    // getHttp
    public static String getHttpString(String url, boolean gzip) {
        return getHttpString(url, gzip, null, true);
    }

    public static String getHttpString(String url) {
        return getHttpString(url, false, null, true);
    }

    public static String getHttpString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        byte[] bytes = getHttpBytes(url, gzip, params, isGet);
        return (new String(bytes)).trim();
    }

    public static byte[] getHttpBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        String method = isGet ? "GET" : "POST";
        byte[] result;

        try {
            URL u = new URL(url);
            Socket client = new Socket();
            int port = u.getPort();
            if (-1 == port) {
                port = 80;
            }

            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            client.connect(inetSocketAddress, 1000);
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Host", u.getHost() + ":" + port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "mini brower / java1.8");
            if (gzip) {
                requestHeaders.put("Accept-Encoding", "gzip");
            }

            String path = u.getPath();
            if (path.length() == 0) {
                path = "/";
            }

            String firstLine;
            if (null != params && isGet) {
                firstLine = HttpUtil.toParams(params);
                path = path + "?" + firstLine;
            }

            firstLine = method + " " + path + " HTTP/1.1\r\n";
            StringBuffer httpRequestString = new StringBuffer();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();

            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header) + "\r\n";
                httpRequestString.append(headerLine);
            }

            if (null != params && !isGet) {
                String paramsString = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(paramsString);
            }

            PrintWriter pWriter = new PrintWriter(client.getOutputStream(), true);
            pWriter.println(httpRequestString);

            InputStream is = client.getInputStream();
            result = readBytes(is, true);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
            result = e.toString().getBytes(StandardCharsets.UTF_8);
        }

        return result;
    }

    /**
     * @param is    输入流
     * @param fully 是否读完
     * @return 字节数组
     * @throws IOException
     */
    public static byte[] readBytes(InputStream is, boolean fully) throws IOException {
        int buffer_size = 1024;
        byte[] buffer = new byte[buffer_size];

        // 将InputStream的内容输出到内存
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            int length = is.read(buffer);
            if (-1 == length) {
                break;
            }
            byteArrayOutputStream.write(buffer, 0, length);
            if (!fully && length != buffer_size) {
                break;
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

}
