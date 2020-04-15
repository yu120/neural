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

    private String extension = "redis-lua";
    private String prefix = "NEURAL_LIMITER";

}
