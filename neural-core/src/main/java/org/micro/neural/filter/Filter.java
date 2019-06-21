package org.micro.neural.filter;

import org.micro.neural.extension.SPI;

import lombok.extern.slf4j.Slf4j;

/**
 * The Abstract Filter
 *
 * @param <M>
 * @author lry
 */
@SPI
@Slf4j
public abstract class Filter<M> {

    public String getId() {
        return this.getClass().getName();
    }

    public void init() throws Exception {
        log.debug("The initializing...");
    }

    public void destroy() throws Exception {
        log.debug("The destroy...");
    }

    public abstract void doFilter(Chain<M> chain, M m) throws Exception;

}
