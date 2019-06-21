package org.micro.neural.admin.configuration;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Map;

@Data
@ToString
@Configuration
@ConfigurationProperties(prefix = "neural")
public class StaticConfig extends WebMvcConfigurerAdapter {

    public static final String CLASS_PATH = "classpath:";
    private Map<String, String> resource;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (Map.Entry<String, String> entry : resource.entrySet()) {
            registry.addResourceHandler(entry.getValue()).addResourceLocations(CLASS_PATH + entry.getKey());
        }

        super.addResourceHandlers(registry);
    }

}
