package org.micro.neural.limiter;

import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.config.GlobalConfig.*;

import java.util.Map;

/**
 * @author lry
 **/
public class LimiterConfigTest {

    public static void main(String[] args) {
        LimiterConfig config = new LimiterConfig();
        config.setConcurrentTimeout(1000L);
        config.setMaxPermitConcurrent(100);
        config.setRateTimeout(1000L);
        config.setRatePermit(2000);
        config.setStrategy(LimiterConfig.Strategy.EXCEPTION);
        config.setName("标题");
        config.setRemarks("备注信息");
        config.setEnable(Switch.OFF);
        String json = SerializeUtils.serialize(config);
        System.out.println(json);
        Map<String, String> map = SerializeUtils.parseStringMap(json);
        System.out.println(map.toString());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "->" + entry.getValue());
        }
        System.out.println("===========");

        String json2 = SerializeUtils.serialize(map);
        System.out.println(json2);
        LimiterConfig config1 = SerializeUtils.deserialize(LimiterConfig.class, json2);
        System.out.println(config1);
    }

}
