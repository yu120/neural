package org.micro.neural.isolation;

/**
 * 隔离类型
 * 
 * @author lry
 */
public enum IsolationStrategy {

	/**
	 * 线程池隔离
	 */
	THREAD,

	/**
	 * 信号量隔离
	 */
	SEMAPHORE;

}
