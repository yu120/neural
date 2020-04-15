package cn.micro.neural.limiter;

/**
 * LimiterException
 *
 * @author lry
 */
public class LimiterException extends RuntimeException {

    public LimiterException() {
        super();
    }

    public LimiterException(String message) {
        super(message);
    }

    public LimiterException(Throwable cause) {
        super(cause);
    }

    public LimiterException(String message, Throwable cause) {
        super(message, cause);
    }

}
