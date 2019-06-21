package org.micro.neural.filter;

import org.micro.neural.extension.Extension;

@Extension(category = FilterChain.ERROR, order = 3)
public class ErrorTest1Filter extends Filter<Message> {
	
	@Override
	public void doFilter(Chain<Message> chain, Message m) throws Exception {
		System.out.println(this.getClass().getName());
		chain.doFilter(chain, m);
	}

}
