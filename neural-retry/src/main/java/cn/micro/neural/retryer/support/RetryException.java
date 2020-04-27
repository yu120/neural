package cn.micro.neural.retryer.support;

import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The Retry Exception
 *
 * @author lry
 */
@Getter
public final class RetryException extends Exception {

    private static final long serialVersionUID = 5397356900902505417L;

    private final int numberOfFailedAttempts;
    private final Attempt<?> lastFailedAttempt;

    public RetryException(int numberOfFailedAttempts, Attempt<?> lastFailedAttempt) {
        this("Retrying failed to complete successfully after " + numberOfFailedAttempts + " attempts.", numberOfFailedAttempts, lastFailedAttempt);
    }

    public RetryException(String message, int numberOfFailedAttempts, Attempt<?> lastFailedAttempt) {
        super(message, checkNotNull(lastFailedAttempt, "Last attempt was null").hasException() ? lastFailedAttempt.getExceptionCause() : null);
        this.numberOfFailedAttempts = numberOfFailedAttempts;
        this.lastFailedAttempt = lastFailedAttempt;
    }

}
