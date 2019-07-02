package org.micro.neural.degrade;

import com.google.common.annotations.Beta;
import lombok.*;
import org.micro.neural.common.Constants;
import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.config.RuleConfig;

import java.util.function.Function;

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

    private static final long serialVersionUID = 4795101106357214550L;

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
        NULL(s -> null, "The return null type data"),
        /**
         * The return String type data
         */
        STRING(s -> s, "The return String type data"),
        /**
         * The return Integer type data
         */
        INTEGER(Integer::valueOf, "The return Integer type data"),
        /**
         * The return Float type data
         */
        FLOAT(Float::valueOf, "The return Float type data"),
        /**
         * The return Double type data
         */
        DOUBLE(Double::valueOf, "The return Double type data"),
        /**
         * The return Long type data
         */
        LONG(Long::valueOf, "The return Long type data"),
        /**
         * The return Boolean type data
         */
        BOOLEAN(Boolean::valueOf, "The return Boolean type data"),
        /**
         * The return T type data with Class
         */
        @Beta
        CLASS(Integer::valueOf, "The return T type data with Class"),
        /**
         * The return Array_String type data
         */
        ARRAY(s -> s.split(Constants.SEPARATOR), "The return Array_String type data"),
        /**
         * The return Map<Object,Object> type data
         */
        MAP(SerializeUtils::parseMap, "The return Map<Object,Object> type data"),
        /**
         * The return Map<String,String> type data
         */
        MAP_STR(SerializeUtils::parseStringMap, "The return Map<String,String> type data"),
        /**
         * The return Map<String,Object> type data
         */
        MAP_OBJ(SerializeUtils::parseObjMap, "The return Map<String,Object> type data"),
        /**
         * The return List<Object> type data
         */
        LIST(SerializeUtils::parseList, "The return List<Object> type data"),
        /**
         * The return List<String> type data
         */
        LIST_STR(SerializeUtils::parseListString, "The return List<String> type data"),
        /**
         * The return List<T> type data with Class
         */
        @Beta
        LIST_CLASS(SerializeUtils::parseListString, "The return List<T> type data with Class");

        Function<String, Object> function;
        String message;

    }

}
