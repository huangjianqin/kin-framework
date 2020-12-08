package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020/12/8
 */
public class YamlParseException extends RuntimeException {
    private static final long serialVersionUID = -7484751090964641613L;

    public YamlParseException(String message) {
        super(message);
    }

    public YamlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlParseException(Throwable cause) {
        super(cause);
    }
}
