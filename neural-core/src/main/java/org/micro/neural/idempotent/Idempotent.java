package org.micro.neural.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.micro.neural.AbstractNeural;
import org.micro.neural.NeuralContext;
import org.micro.neural.OriginalCall;
import org.micro.neural.extension.Extension;

/**
 * Idempotent
 *
 * @author lry
 */
@Slf4j
@Extension(IdempotentGlobalConfig.IDENTITY)
public class Idempotent extends AbstractNeural<IdempotentConfig, IdempotentGlobalConfig> {

    @Override
    public Object wrapperCall(NeuralContext neuralContext, String identity, OriginalCall originalCall) throws Throwable {
        return super.wrapperCall(neuralContext, identity, originalCall);
    }

}
