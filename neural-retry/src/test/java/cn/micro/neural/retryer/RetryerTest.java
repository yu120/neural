package cn.micro.neural.retryer;

import cn.micro.neural.retryer.strategy.StopStrategies;
import cn.micro.neural.retryer.strategy.WaitStrategies;
import cn.micro.neural.retryer.support.RetryException;
import com.google.common.base.Predicates;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RetryerTest {

	public static void main(String[] args) {
		Callable<Boolean> callable = new Callable<Boolean>() {
			int time = 1;
		    public Boolean call() throws Exception {
		    	System.out.println(time);
		    	if(time > 2){
		    		//return true; // do something useful here
		    	}
		    	time++;
		    	throw new IOException();
		    }
		};

		Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
		        .retryIfResult(Predicates.<Boolean>isNull())
		        .retryIfExceptionOfType(IOException.class)
		        .retryIfRuntimeException()
		        .withStopStrategy(StopStrategies.stopAfterAttempt(3))
		        .withWaitStrategy(WaitStrategies.fixedWait(3000, TimeUnit.MILLISECONDS))
		        //.withBlockStrategy(BlockStrategies.threadSleepStrategy())
		        .build();
		try {
		    System.out.println("执行结果:"+retryer.call(callable));
		} catch (RetryException e) {
		    e.printStackTrace();
		} catch (ExecutionException e) {
		    e.printStackTrace();
		}
	}
	
}
