package org.micro.neural.retryer.strategy;

import org.micro.neural.retryer.support.Attempt;

/**
 * A strategy used to decide if a retryer must stop retrying after a failed attempt or not.
 *
 * @author lry
 */
public interface StopStrategy {

    /**
     * Returns <code>true</code> if the retryer should stop retrying.
     *
     * @param failedAttempt the previous failed {@code Attempt}
     * @return <code>true</code> if the retryer must stop, <code>false</code> otherwise
     */
	boolean shouldStop(Attempt failedAttempt);
    
}
