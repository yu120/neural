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

    // ==== commons statistics

    public static final String REQUEST_KEY = "statistics:request:%s:%s";
    public static final String SUCCESS_KEY = "statistics:success:%s:%s";
    public static final String FAILURE_KEY = "statistics:failure:%s:%s";
    public static final String TIMEOUT_KEY = "statistics:timeout:%s:%s";
    public static final String REJECTION_KEY = "statistics:rejection:%s:%s";
    public static final String ELAPSED_KEY = "statistics:elapsed:%s:%s";
    public static final String MAX_ELAPSED_KEY = "statistics:max_elapsed:%s:%s";
    public static final String CONCURRENCY_KEY = "statistics:concurrency:%s:%s";
    public static final String MAX_CONCURRENCY_KEY = "statistics:max_concurrency:%s:%s";
    public static final String RATE_KEY = "statistics:rate:%s:%s";
    public static final String MAX_RATE_KEY = "statistics:max_rate:%s:%s";

    // ==== limiter statistics

    public static final String CONCURRENCY_EXCEED_KEY = "statistics:concurrency_exceed:%s:%s";
    public static final String RATE_EXCEED_KEY = "statistics:rate_exceed:%s:%s";

    // ==== degrade statistics

    public static final String TOTAL_DEGRADE = "statistics:degrade:%s:%s";

}
