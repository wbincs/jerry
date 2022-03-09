package cn.wb.jerry.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.wb.jerry.catalina.Context;
import cn.wb.jerry.catalina.Host;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspC;

import java.io.File;

public class JspUtil {

    // java关键字
    private static final String[] javaKeywords = new String[]{"abstract", "assert", "boolean", "break",
            "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"};

    // jsp编译测试
    public static void main(String[] args) {
        try {
            Context context = new Context("/javaweb",
                    "g:/jProject/javaweb/web",
                    null, true);
            File file = new File("g:/jProject/javaweb/web/index.jsp");
            compileJsp(context, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 编译jsp页面
    public static void compileJsp(Context context, File file) throws JasperException {
        String path = context.getPath();
        String subFolder;
        if ("/".equals(path)) {
            subFolder = "_";
        } else {
            subFolder = StrUtil.subAfter(path, '/', false);
        }

        String workPath = (new File(Constant.workFolder, subFolder)).getAbsolutePath() + File.separator;
        String[] args = new String[]{"-webapp", context.getDocBase().toLowerCase(), "-d", workPath.toLowerCase(), "-compile"};
        JspC jspc = new JspC();
        jspc.setArgs(args);
        jspc.execute(file);
    }

    public static String makeJavaIdentifier(String identifier) {
        return makeJavaIdentifier(identifier, true);
    }

    public static String makeJavaIdentifier(String identifier, boolean periodToUnderscore) {
        StringBuilder modifiedIdentifier = new StringBuilder(identifier.length());
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }

        for (int i = 0; i < identifier.length(); ++i) {
            char ch = identifier.charAt(i);
            if (!Character.isJavaIdentifierPart(ch) || ch == '_' && periodToUnderscore) {
                if (ch == '.' && periodToUnderscore) {
                    modifiedIdentifier.append('_');
                } else {
                    modifiedIdentifier.append(mangleChar(ch));
                }
            } else {
                modifiedIdentifier.append(ch);
            }
        }

        if (isJavaKeyword(modifiedIdentifier.toString())) {
            modifiedIdentifier.append('_');
        }

        return modifiedIdentifier.toString();
    }

    public static String mangleChar(char ch) {
        char[] result = new char[]{'_', Character.forDigit(ch >> 12 & 15, 16), Character.forDigit(ch >> 8 & 15, 16), Character.forDigit(ch >> 4 & 15, 16), Character.forDigit(ch & 15, 16)};
        return new String(result);
    }

    public static boolean isJavaKeyword(String key) {
        int i = 0;
        int j = javaKeywords.length;

        while (i < j) {
            int k = (i + j) / 2;
            int result = javaKeywords[k].compareTo(key);
            if (result == 0) {
                return true;
            }

            if (result < 0) {
                i = k + 1;
            } else {
                j = k;
            }
        }

        return false;
    }

    public static String getServletClassPath(String uri, String subFolder) {
        return getServletPath(uri, subFolder) + ".class";
    }

    public static String getServletJavaPath(String uri, String subFolder) {
        return getServletPath(uri, subFolder) + ".java";
    }

    public static String getJspServletClassName(String uri, String subFolder) {
        File tempFile = FileUtil.file(Constant.workFolder, subFolder);
        String tempPath = tempFile.getAbsolutePath() + File.separator;
        String servletPath = getServletPath(uri, subFolder);
        // jsp编译成servlet的类路径
        String jspServletClassPath = StrUtil.subAfter(servletPath, tempPath, false);
        // jsp变异成servlet的类名
        String jspServletClassName = StrUtil.replace(jspServletClassPath, File.separator, ".");
        return jspServletClassName;
    }

    public static String getServletPath(String uri, String subFolder) {
        String tempPath = "org/apache/jsp/" + uri;
        File tempFile = FileUtil.file(Constant.workFolder, subFolder, tempPath);
        String fileNameOnly = tempFile.getName();
        String classFileName = makeJavaIdentifier(fileNameOnly);
        File servletFile = new File(tempFile.getParent(), classFileName);
        return servletFile.getAbsolutePath();
    }

}
