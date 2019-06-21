package org.micro.neural.filter;

import org.micro.neural.extension.Extension;

@Extension(order = 1)
public class DefaultTest3Filter extends Filter<Message> {
	
	@Override
	public void doFilter(Chain<Message> chain, Message m) throws Exception {
		System.out.println(this.getClass().getName());
		chain.doFilter(chain, m);
	}

}
