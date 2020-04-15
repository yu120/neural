package cn.micro.neural.limiter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * LimiterConfig
 *
 * @author lry
 */
@Data
@ConfigurationProperties(prefix = "neural.limiter")
public class LimiterConfig implements Serializable {

    private String extension = "redis-template";
    private String prefix = "NEURAL_LIMITER";

}
