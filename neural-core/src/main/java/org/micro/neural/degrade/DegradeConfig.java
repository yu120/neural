package org.micro.neural.degrade;

import lombok.*;
import org.micro.neural.config.RuleConfig;

/**
 * The Degrade Config
 *
 * @author lry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DegradeConfig extends RuleConfig {

    private static final long serialVersionUID = -5445833020633975979L;

    /**
     * The degrade level, default is Level.NEED
     */
    private DegradeGlobalConfig.Level level = DegradeGlobalConfig.Level.NEED;
    /**
     * The degrade strategy, default is Strategy.FALLBACK
     */
    private Strategy strategy = Strategy.FALLBACK;
    /**
     * The mock type of degrade strategy
     */
    private Mock mock = Mock.NULL;
    /**
     * The mock clazz of degrade strategy
     */
    private String clazz;
    /**
     * The mock data of degrade strategy
     */
    private String data;

    /**
     * The degrade strategy
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Strategy {

        /**
         * The skip request
         */
        NON("The skip of degrade"),
        /**
         * The return mock data
         */
        MOCK("The return mock data of degrade"),
        /**
         * The fallback process
         */
        FALLBACK("The fallback process of degrade");

        String message;

    }


    @Getter
    @AllArgsConstructor
    public enum Mock {

        /**
         * The return null type data
         */
        NULL("The return null type data"),
        /**
         * The return String type data
         */
        STRING("The return String type data"),
        /**
         * The return Integer type data
         */
        INTEGER("The return Integer type data"),
        /**
         * The return Float type data
         */
        FLOAT("The return Float type data"),
        /**
         * The return Double type data
         */
        DOUBLE("The return Double type data"),
        /**
         * The return Long type data
         */
        LONG("The return Long type data"),
        /**
         * The return Boolean type data
         */
        BOOLEAN("The return Boolean type data"),
        /**
         * The return T type data with Class
         */
        CLASS("The return T type data with Class"),
        /**
         * The return Array_String type data
         */
        ARRAY("The return Array_String type data"),
        /**
         * The return Map<Object,Object> type data
         */
        MAP("The return Map<Object,Object> type data"),
        /**
         * The return Map<String,String> type data
         */
        MAP_STR("The return Map<String,String> type data"),
        /**
         * The return Map<String,Object> type data
         */
        MAP_OBJ("The return Map<String,Object> type data"),
        /**
         * The return List<Object> type data
         */
        LIST("The return List<Object> type data"),
        /**
         * The return List<String> type data
         */
        LIST_STR("The return List<String> type data"),
        /**
         * The return List<T> type data with Class
         */
        LIST_CLASS("The return List<T> type data with Class");

        String message;

    }

}
