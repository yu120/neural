package org.micro.neural.isolation;

import org.micro.neural.isolation.IsolationBuilder.IsolationThreadBuilder;
import org.micro.neural.isolation.IsolationBuilder.ThreadType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 隔离
 * 
 * @author lry
 */
public class Isolation {

	private IsolationBuilder builder;

	public Isolation build() {
		if (IsolationStrategy.THREAD == builder.getStrategy()) {
			IsolationThreadBuilder threadBuilder = builder
					.getIsolationThreadBuilder();
			if (ThreadType.CACHED == threadBuilder.getThreadType()) {
				this.newCachedExecutor(threadBuilder);
			} else if (ThreadType.FIXED == threadBuilder.getThreadType()) {
				this.newFixedExecutor(threadBuilder);
			} else {
				throw new RuntimeException();
			}
		} else if (IsolationStrategy.SEMAPHORE == builder.getStrategy()) {

		} else {
			throw new RuntimeException();
		}

		return this;
	}

	public Executor newCachedExecutor(IsolationThreadBuilder threadBuilder) {
		BlockingQueue<Runnable> workQueue = null;
		if (threadBuilder.getQueues() == 0) {
			workQueue = new SynchronousQueue<Runnable>();
		} else if (threadBuilder.getQueues() < 0) {
			workQueue = new LinkedBlockingQueue<Runnable>();
		} else {
			workQueue = new LinkedBlockingQueue<Runnable>(
					threadBuilder.getQueues());
		}

		return new ThreadPoolExecutor(threadBuilder.getCoreThread(),
				threadBuilder.getMaxThread(), threadBuilder.getKeepAliveTime(),
				TimeUnit.MILLISECONDS, workQueue, new NamedThreadFactory(
						threadBuilder.getThreadName()),
				new AbortPolicyWithReport(threadBuilder.getThreadName()));
	}

	public Executor newFixedExecutor(IsolationThreadBuilder threadBuilder) {
		return new ThreadPoolExecutor(
				threadBuilder.getCoreThread(),
				threadBuilder.getCoreThread(),
				0,
				TimeUnit.MILLISECONDS,
				threadBuilder.getQueues() == 0 ? new SynchronousQueue<Runnable>()
						: (threadBuilder.getQueues() < 0 ? new LinkedBlockingQueue<Runnable>()
								: new LinkedBlockingQueue<Runnable>(
										threadBuilder.getQueues())),
				new NamedThreadFactory(threadBuilder.getThreadName()),
				new AbortPolicyWithReport(threadBuilder.getThreadName()));
	}

	private class NamedThreadFactory implements ThreadFactory {
		private AtomicInteger POOL_SEQ = new AtomicInteger(1);
		private String threadName;

		public NamedThreadFactory(String threadName) {
			this.threadName = threadName;
		}

		@Override
		public Thread newThread(Runnable runnable) {
			String name = threadName + "-thread-pool-"
					+ POOL_SEQ.getAndIncrement();
			Thread ret = new Thread(Thread.currentThread().getThreadGroup(),
					runnable, name, 0);
			ret.setDaemon(false);
			return null;
		}
	}

	private class AbortPolicyWithReport extends ThreadPoolExecutor.AbortPolicy {
		private final String threadName;

		public AbortPolicyWithReport(String threadName) {
			this.threadName = threadName;
		}

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
			String msg = String
					.format("Thread pool is EXHAUSTED! "
							+ "Thread Name: %s, "
							+ "Pool Size: %d (active: %d, core: %d, max: %d, largest: %d), "
							+ "Task: %d (completed: %d), "
							+ "Executor status:(isShutdown:%s, isTerminated:%s, isTerminating:%s)!",
							threadName, e.getPoolSize(), e.getActiveCount(),
							e.getCorePoolSize(), e.getMaximumPoolSize(),
							e.getLargestPoolSize(), e.getTaskCount(),
							e.getCompletedTaskCount(), e.isShutdown(),
							e.isTerminated(), e.isTerminating());
			throw new RejectedExecutionException(msg);
		}
	}

}
