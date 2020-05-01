package cn.micro.neural.idempotent.core;

import cn.micro.neural.idempotent.IdempotentConfig;
import cn.neural.common.extension.Extension;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * StandAloneIdempotent
 *
 * @author lry
 */
@Slf4j
@Getter
@Extension("stand-alone")
public class StandAloneIdempotent extends AbstractIdempotent {

    public StandAloneIdempotent(IdempotentConfig config) {
        super(config);
    }

    @Override
    protected boolean tryRefresh(IdempotentConfig config) {
        return true;
    }

}
