package org.micro.neural.degrade;

import org.micro.neural.common.URL;
import org.micro.neural.OriginalCall;

import java.util.Random;

/**
 * @author lry
 **/
public class DegradeTest {
    public static void main(String[] args) throws Throwable {
        String application = "gateway";
        Degrade degrade = new Degrade();
        degrade.initialize(URL.valueOf("redis://localhost:6379/limiter?minIdle=2"));

        String identity1 = application + ":" + "order" + ":" + "sendEms";
        DegradeConfig config1 = new DegradeConfig(DegradeGlobalConfig.Level.HINT,
                DegradeConfig.Strategy.FALLBACK, DegradeConfig.Mock.BOOLEAN, null, "true");
        config1.setApplication(application);
        config1.setGroup("order");
        config1.setResource("sendEms");
        config1.setName("发送短信");
        degrade.addConfig(config1);

        DegradeConfig config2 = new DegradeConfig(DegradeGlobalConfig.Level.NEED,
                DegradeConfig.Strategy.FALLBACK, DegradeConfig.Mock.ARRAY, null, "zhangsan,lisi");
        config1.setApplication(application);
        config1.setGroup("order");
        config1.setResource("sendEmail");
        config2.setName("发送邮件");
        degrade.addConfig(config2);

        for (int i = 0; i < 100000; i++) {
            Object result = degrade.originalCall(identity1, new OriginalCall() {
                @Override
                public Object call() throws Throwable {
                    Thread.sleep(new Random().nextInt(100) + 20);
                    return "ok";
                }

                @Override
                public Object fallback() throws Throwable {
                    return "fallback";
                }
            });
        }

        Thread.sleep(600000);

        degrade.destroy();
    }
}
