package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterStatistics;
import cn.neural.common.extension.Extension;
import cn.neural.common.utils.BeanUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The Abstract Check Limiter.
 *
 * @author lry
 **/
@Slf4j
@Getter
public abstract class AbstractCheckLimiter implements ILimiter {

    protected volatile LimiterConfig limiterConfig = null;
    protected volatile LimiterStatistics statistics = new LimiterStatistics();
    protected final Extension extension;

    AbstractCheckLimiter() {
        this.extension = this.getClass().getAnnotation(Extension.class);
        if (null == extension) {
            throw new IllegalStateException("The " + this.getClass().getName() + " must has @Extension");
        }
    }

    @Override
    public boolean refresh(LimiterConfig limiterConfig) throws Exception {
        log.debug("The refresh {}", limiterConfig);
        if (null == limiterConfig) {
            return true;
        }
        if (null == this.limiterConfig) {
            this.limiterConfig = limiterConfig;
            return true;
        }
        if (this.limiterConfig.equals(limiterConfig)) {
            return true;
        }

        BeanUtils.copyProperties(limiterConfig, this.limiterConfig);
        return true;
    }

    /**
     * Whether the check does not need process
     *
     * @return true indicates that it does not need to be handled
     */
    boolean checkDisable() {
        if (null == limiterConfig) {
            return true;
        }

        return null == limiterConfig.getEnable() || LimiterConfig.Switch.OFF == limiterConfig.getEnable();
    }

    /**
     * The check the need for concurrent limiting enable
     *
     * @return true indicates that it need to be concurrent handled
     */
    boolean checkConcurrentEnable() {
        if (LimiterConfig.Switch.OFF == limiterConfig.getConcurrent().getEnable()
                || limiterConfig.getConcurrent().getPermitUnit() < 1) {
            return false;
        }

        return limiterConfig.getConcurrent().getMaxPermit() >= limiterConfig.getConcurrent().getPermitUnit();
    }

    /**
     * The check the need for rate limiting enable
     *
     * @return true indicates that it need to be rate handled
     */
    boolean checkRateEnable() {
        if (LimiterConfig.Switch.OFF == limiterConfig.getRate().getEnable()
                || limiterConfig.getRate().getRateUnit() < 1) {
            return false;
        }

        return limiterConfig.getRate().getMaxRate() >= limiterConfig.getRate().getRateUnit();
    }

    /**
     * The check the need for request limiting enable
     *
     * @return true indicates that it need to be rate handled
     */
    boolean checkRequestEnable() {
        if (LimiterConfig.Switch.OFF == limiterConfig.getRate().getEnable()
                || limiterConfig.getRequest().getRequestUnit() < 1) {
            return false;
        }

        return limiterConfig.getRequest().getMaxRequest() >= limiterConfig.getRequest().getRequestUnit();
    }

}
