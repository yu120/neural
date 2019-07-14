package org.micro.neural.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.micro.neural.AbstractNeural;
import org.micro.neural.NeuralContext;
import org.micro.neural.OriginalCall;
import org.micro.neural.common.utils.StreamUtils;
import org.micro.neural.config.store.IStore;
import org.micro.neural.config.store.StorePool;
import org.micro.neural.extension.Extension;

import java.util.ArrayList;
import java.util.List;

/**
 * Idempotent
 *
 * @author lry
 */
@Slf4j
@Extension(IdempotentGlobalConfig.IDENTITY)
public class Idempotent extends AbstractNeural<IdempotentConfig, IdempotentGlobalConfig> {

    private static String IDEMPOTENT_SCRIPT = StreamUtils.loadScript("/script/idempotent.lua");

    private StorePool storePool = StorePool.getInstance();

    @Override
    public Object wrapperCall(NeuralContext neuralContext, String identity, OriginalCall originalCall) throws Throwable {
        List<Object> keys = new ArrayList<>();
        keys.add(neuralContext.getId());
        IStore store = storePool.getStore();
        // might contain
        List<Object> result = store.eval(IDEMPOTENT_SCRIPT, globalConfig.getTimeout(), keys);
        if (result == null || result.size() != 1) {
            return super.wrapperCall(neuralContext, identity, originalCall);
        } else {
            if ((Boolean) result.get(0)) {
                throw new RuntimeException();
            }

            return super.wrapperCall(neuralContext, identity, originalCall);
        }
    }

}
