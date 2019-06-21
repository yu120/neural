package org.micro.neural.filter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

/**
 * The Filter Chain.
 *
 * @param <M>
 * @author lry
 */
@Slf4j
public class Chain<M> extends Filter<M> {

    private final List<Filter<M>> filters;
    private final AtomicInteger index;

    public Chain(List<Filter<M>> filters) {
        this.filters = filters;
        this.index = new AtomicInteger(0);
    }

    @Override
    public void doFilter(Chain<M> chain, M m) throws Exception {
        if (index.get() == filters.size()) {
            return;
        }

        Filter<M> filter = filters.get(index.getAndIncrement());
        log.debug("The next filter to be executed is: {}", filter.getId());

        filter.doFilter(chain, m);
    }

}