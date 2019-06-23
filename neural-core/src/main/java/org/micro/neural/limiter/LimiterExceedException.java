package org.micro.neural.limiter;

/**
 * The Excess Exception of Limiter.
 *
 * @author lry
 */
public class LimiterExceedException extends RuntimeException {

    private static final long serialVersionUID = -8228538343786169063L;

    public LimiterExceedException(String message) {
        super(message);
    }

}
