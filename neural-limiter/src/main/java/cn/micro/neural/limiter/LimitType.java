package cn.micro.neural.limiter;

/**
 * 限流类型
 *
 * @author lry
 */
public enum LimitType {

    /**
     * 自定义key
     */
    CUSTOMER,

    /**
     * 请求者IP
     */
    IP;

}