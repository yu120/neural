package org.micro.neural.isolation;


public class IsolationBuilder {

	private String groupKey;
	private String commondKey;
	private IsolationStrategy strategy = IsolationStrategy.THREAD;
	private IsolationThreadBuilder isolationThreadBuilder;

	public String getGroupKey() {
		return groupKey;
	}

	public IsolationBuilder setGroupKey(String groupKey) {
		this.groupKey = groupKey;
		return this;
	}

	public String getCommondKey() {
		return commondKey;
	}

	public IsolationBuilder setCommondKey(String commondKey) {
		this.commondKey = commondKey;
		return this;
	}

	public IsolationStrategy getStrategy() {
		return strategy;
	}

	public IsolationBuilder setStrategy(IsolationStrategy strategy) {
		this.strategy = strategy;
		return this;
	}

	public IsolationThreadBuilder getIsolationThreadBuilder() {
		return isolationThreadBuilder;
	}

	public IsolationBuilder setIsolationThreadBuilder(
			IsolationThreadBuilder isolationThreadBuilder) {
		this.isolationThreadBuilder = isolationThreadBuilder;
		return this;
	}

	public class IsolationThreadBuilder {
		private ThreadType threadType;
		private String threadName;
		private int coreThread;
		private int maxThread;
		private int keepAliveTime;
		private int queues;
		public ThreadType getThreadType() {
			return threadType;
		}
		public void setThreadType(ThreadType threadType) {
			this.threadType = threadType;
		}
		public String getThreadName() {
			return threadName;
		}
		public void setThreadName(String threadName) {
			this.threadName = threadName;
		}
		public int getCoreThread() {
			return coreThread;
		}
		public void setCoreThread(int coreThread) {
			this.coreThread = coreThread;
		}
		public int getMaxThread() {
			return maxThread;
		}
		public void setMaxThread(int maxThread) {
			this.maxThread = maxThread;
		}
		public int getKeepAliveTime() {
			return keepAliveTime;
		}
		public void setKeepAliveTime(int keepAliveTime) {
			this.keepAliveTime = keepAliveTime;
		}
		public int getQueues() {
			return queues;
		}
		public void setQueues(int queues) {
			this.queues = queues;
		}
	}

	public enum ThreadType {
		FIXED, CACHED;
	}
	
}
