package com.hc.hospitalservice.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Create a clean ObjectMapper for HTTP responses (NO type info)
        ObjectMapper httpMapper = new ObjectMapper();
        httpMapper.registerModule(new JavaTimeModule());
        httpMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Add it as the HTTP message converter
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(httpMapper);
        converters.add(0, converter); // Add at the beginning to take precedence
    }
}
