package org.micro.neural.extension.singleton;

import org.micro.neural.extension.SPI;

@SPI(single = true)
public interface NpiSingleton {
	long spiHello();
}
