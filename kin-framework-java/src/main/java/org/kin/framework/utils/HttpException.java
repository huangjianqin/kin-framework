package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2019/7/6
 */
public class HttpException extends RuntimeException {
    private static final long serialVersionUID = -7890577017518004771L;

    public HttpException(int statusCode, String reasonPhrase, String objStr) {
        super("http error!!! code=" + statusCode + ", reason=" + reasonPhrase + " >>>" + objStr);
    }
}
