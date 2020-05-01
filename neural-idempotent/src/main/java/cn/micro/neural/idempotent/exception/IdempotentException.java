package cn.micro.neural.idempotent.exception;

/**
 * LimiterException
 *
 * @author lry
 */
public class IdempotentException extends RuntimeException {

    private static final long serialVersionUID = -8228538343786169063L;

    public IdempotentException(String message) {
        super(message);
    }

}
