package cn.micro.neural.retryer.support;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class AttemptTimeLimitersTest {

	@Test
	public void fixedTimeLimitTest() {
		try {
			AttemptTimeLimiters.fixedTimeLimit(3000, TimeUnit.MILLISECONDS).call(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					Thread.sleep(2000);
					return null;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
