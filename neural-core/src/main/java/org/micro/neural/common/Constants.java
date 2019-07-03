package org.micro.neural.common;

/**
 * The Constants.
 *
 * @author lry
 **/
public final class Constants {

    public static final String SEPARATOR = ",";
    public static final String DELIMITER = ":";
    public static final String CHANNEL = "CHANNEL";

    public static final String STATISTICS = "STATISTICS";

    // ==== common statistics

    public static final String SUCCESS_KEY = "success";
    public static final String REQUEST_KEY = "request";
    public static final String FAILURE_KEY = "failure";
    public static final String AVG_ELAPSED_KEY = "avg_elapsed";
    public static final String MAX_ELAPSED_KEY = "max_elapsed";
    public static final String CONCURRENT_KEY = "concurrent";
    public static final String MAX_CONCURRENT_KEY = "max_concurrent";

    // ==== limiter statistics

    public static final String CONCURRENT_EXCEED_KEY = "concurrent_exceed";
    public static final String RATE_EXCEED_KEY = "rate_exceed";

    // ==== degrade statistics

    public static final String DEGRADE_TIMES_KEY = "degrade";

}
