package com.dafacha.inventory.config;

import com.dafacha.inventory.config.properties.CorsProperties;
import com.dafacha.inventory.config.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class AppPropertiesConfig {
}
