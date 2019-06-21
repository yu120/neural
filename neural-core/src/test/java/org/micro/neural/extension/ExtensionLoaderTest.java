package org.micro.neural.extension;

import org.micro.neural.extension.prototype.NpiPrototype;
import org.micro.neural.extension.prototype.NpiPrototypeImpl2;
import org.micro.neural.extension.singleton.NpiSingleton;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ExtensionLoaderTest {
	
	@Test
	public void test() {
		List<NpiPrototype> spiPrototype = ExtensionLoader.getLoader(NpiPrototype.class).getExtensions("ddd");
		System.out.println(spiPrototype);
	}
	
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testExtensionNormal() {
        // 单例模式下只会构造一次实例
        Assert.assertEquals(1, ExtensionLoader.getLoader(NpiSingleton.class).getExtension("spiSingletonImpl").spiHello());
        Assert.assertEquals(1, ExtensionLoader.getLoader(NpiSingleton.class).getExtension("spiSingletonImpl").spiHello());

        // 多例模式下在每次获取的时候进行实例化
        Assert.assertEquals(1, ExtensionLoader.getLoader(NpiPrototype.class).getExtension("spiPrototypeImpl1").spiHello());
        Assert.assertEquals(2, ExtensionLoader.getLoader(NpiPrototype.class).getExtension("spiPrototypeImpl1").spiHello());

        // 手动添加实现类
        Assert.assertEquals(1, ExtensionLoader.getLoader(NpiPrototype.class).getExtensions("").size());
        ExtensionLoader loader = ExtensionLoader.getLoader(NpiPrototype.class);
        loader.addExtensionClass(NpiPrototypeImpl2.class);

        // 返回所有实现类
        ExtensionLoader.initExtensionLoader(NpiPrototype.class);
        Assert.assertEquals(1, ExtensionLoader.getLoader(NpiSingleton.class).getExtensions("").size());
        Assert.assertEquals(2, ExtensionLoader.getLoader(NpiPrototype.class).getExtensions("").size());

    }

    @Test
    public void testExtensionAbNormal() {
        // 没有注解spi的接口无法进行扩展
        try {
            ExtensionLoader.getLoader(NotSpiInterface.class);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("without @SPI annotation"));
        }

        Assert.assertNull(ExtensionLoader.getLoader(SpiWithoutImpl.class).getExtension("default"));
    }

    // not spi
    public interface NotSpiInterface {}

    // not impl
    @SPI
    public interface SpiWithoutImpl {}
}
