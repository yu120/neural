package org.micro.neural.limiter.core;

import lombok.Getter;
import org.micro.neural.common.utils.BeanUtils;
import org.micro.neural.config.GlobalConfig;
import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.LimiterConfig;
import org.micro.neural.limiter.LimiterGlobalConfig;
import org.micro.neural.limiter.LimiterStatistics;
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
    protected volatile LimiterGlobalConfig limiterGlobalConfig = null;
    protected volatile LimiterStatistics statistics = new LimiterStatistics();
    protected final Extension extension;

    AbstractCheckLimiter() {
        this.extension = this.getClass().getAnnotation(Extension.class);
        if (null == extension) {
            throw new IllegalStateException("The " + this.getClass().getName() + " must has @Extension");
        }
    }

    @Override
    public boolean refresh(LimiterGlobalConfig limiterGlobalConfig, LimiterConfig limiterConfig) throws Exception {
        log.debug("The refresh {}", limiterConfig);
        if (null == limiterGlobalConfig) {
            this.limiterGlobalConfig = limiterGlobalConfig;
        }
        if (null == limiterConfig || this.limiterConfig.equals(limiterConfig)) {
            return true;
        }

        BeanUtils.copyProperties(limiterConfig, this.limiterConfig);
        return doRefresh(limiterConfig);
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

        return null == limiterConfig.getEnable() || GlobalConfig.Switch.OFF == limiterConfig.getEnable();
    }

    /**
     * The check the need for concurrent limiting exceed
     *
     * @return true indicates that it need to be concurrent handled
     */
    boolean checkConcurrencyExceed() {
        return limiterConfig.getMaxConcurrent() > 0L;
    }

    /**
     * The check the need for rate limiting exceed
     *
     * @return true indicates that it need to be rate handled
     */
    boolean checkRateExceed() {
        return limiterConfig.getRatePermit() > 0L;
    }

    protected boolean doRefresh(LimiterConfig limiterConfig) throws Exception {
        return true;
    }

}
