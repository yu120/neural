package org.micro.neural.bloomfilter;

import lombok.extern.slf4j.Slf4j;
import org.micro.neural.AbstractNeural;
import org.micro.neural.NeuralContext;
import org.micro.neural.OriginalCall;
import org.micro.neural.bloomfilter.core.NeuralBitSet;
import org.micro.neural.common.URL;
import org.micro.neural.extension.Extension;
import org.micro.neural.extension.ExtensionLoader;

/**
 * The Bloom Filter
 *
 * @author lry
 */
@Slf4j
@Extension(BloomFilterGlobalConfig.IDENTITY)
public class BloomFilter extends AbstractNeural<BloomFilterConfig, BloomFilterGlobalConfig> {

    private BloomFilterFactory<String> bloomFilterFactory;

    @Override
    public void initialize(URL url) {
        super.initialize(url);

        double falsePositiveProbability = 0.0001;
        int expectedNumberOfElements = 10000;
        this.bloomFilterFactory = new BloomFilterFactory<>(falsePositiveProbability, expectedNumberOfElements);

        NeuralBitSet neuralBitSet = ExtensionLoader.getLoader(NeuralBitSet.class).getExtension();
        bloomFilterFactory.bind(neuralBitSet);
    }

    @Override
    public Object wrapperCall(NeuralContext neuralContext, String identity, OriginalCall originalCall) throws Throwable {
        try {
            if (bloomFilterFactory.contains(neuralContext.getId())) {
                throw new RuntimeException("Repeated requests");
            }

            bloomFilterFactory.add(neuralContext.getId());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return super.wrapperCall(neuralContext, identity, originalCall);
    }

}
