package org.micro.neural.common;

/**
 * The Constants.
 *
 * @author lry
 **/
public final class Constants {

    public static final String SEQ = "-";
    public static final String SEPARATOR = ",";
    public static final String DELIMITER = ":";
    public static final String CHANNEL = "CHANNEL";
    public static final String PUSH_STATISTICS = "push-statistics";

    // ==== limiter statistics

    public static final String CONCURRENCY_EXCEED_KEY = "statistics:concurrency_exceed:%s:%s";
    public static final String RATE_EXCEED_KEY = "statistics:rate_exceed:%s:%s";

    // ==== degrade statistics

    public static final String TOTAL_DEGRADE = "statistics:degrade:%s:%s";

}
