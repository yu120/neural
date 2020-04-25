package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.storage.FactoryStorage;
import cn.neural.common.extension.Extension;
import cn.neural.common.utils.StreamUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Cluster Limiter by Redis.
 * <p>
 * 1.Limit instantaneous concurrent
 * 2.Limit the maximum number of counter for a time window
 * 3.Token Bucket
 *
 * @author lry
 **/
@Slf4j
@Extension("cluster")
public class ClusterLimiter extends AbstractCallLimiter {

    private static String CONCURRENT_SCRIPT = StreamUtils.loadScript("/script/limiter_concurrent.lua");
    private static String RATE_SCRIPT = StreamUtils.loadScript("/script/limiter_rate.lua");
    private static String REQUEST_SCRIPT = StreamUtils.loadScript("/script/limiter_request.lua");

    @Override
    protected boolean tryRefresh(LimiterConfig limiterConfig) {
        return true;
    }

    @Override
    protected Acquire tryAcquireConcurrent() {
        List<Object> keys = new ArrayList<>();
        keys.add(config.identity());
        keys.add(config.getConcurrent().getPermitUnit());
        keys.add(config.getConcurrent().getMaxPermit());

        try {
            EvalResult evalResult = eval(CONCURRENT_SCRIPT, config.getConcurrent().getTimeout(), keys);
            System.out.println("The increment concurrent:" + evalResult);
            return evalResult.getCode();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void releaseConcurrent() {
        List<Object> keys = new ArrayList<>();
        keys.add(config.identity());
        keys.add(-config.getConcurrent().getPermitUnit());
        keys.add(config.getConcurrent().getMaxPermit());

        try {
            EvalResult evalResult = eval(CONCURRENT_SCRIPT, config.getConcurrent().getTimeout(), keys);
            System.out.println("The decrement concurrent:" + evalResult);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected Acquire tryAcquireRate() {
        List<Object> keys = new ArrayList<>();
        keys.add(config.identity());
        keys.add(config.getRate().getRateUnit());
        keys.add(System.currentTimeMillis());
        keys.add("app");

        try {
            EvalResult evalResult = eval(RATE_SCRIPT, config.getRate().getTimeout(), keys);
            return evalResult.getCode();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireCounter() {
        List<Object> keys = new ArrayList<>();
        keys.add(config.identity());
        keys.add(config.getCounter().getCountUnit());
        keys.add(config.getCounter().getMaxCount());
        keys.add(config.getCounter().getTimeout());

        try {
            EvalResult evalResult = eval(REQUEST_SCRIPT, config.getCounter().getTimeout(), keys);
            return evalResult.getCode();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Acquire.EXCEPTION;
        }
    }

    private EvalResult eval(String script, Long timeout, List<Object> keys) {
        Number[] result = FactoryStorage.INSTANCE.getStorage().eval(script, null, keys);
        if (result == null || result.length != 2) {
            return new EvalResult(Acquire.EXCEPTION, 0L);
        }

        Acquire acquire = Acquire.valueOf(result[0].intValue());
        return new EvalResult(acquire, result[1].longValue());
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class EvalResult implements Serializable {

        private static final long serialVersionUID = 965512125433109743L;

        private Acquire code;
        private Long num;

    }

}
