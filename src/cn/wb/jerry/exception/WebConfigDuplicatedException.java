package cn.wb.jerry.exception;

/***
 * 应用配置重复异常
 * 用户配置的servlet的url、name和class，如果重复，抛此异常
 */
public class WebConfigDuplicatedException extends Exception {

    public WebConfigDuplicatedException(String msg) {
        super(msg);
    }

}
