package com.biqasoft.microservice.communicator.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by ya on 4/8/2017.
 */
@Configuration
public class DefaultObjectMapperConfiguration {

    @Bean
    DefaultObjectMapperConfigurationData objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return new DefaultObjectMapperConfigurationData(objectMapper);
    }

    public static class DefaultObjectMapperConfigurationData {

        private ObjectMapper objectMapper;

        public DefaultObjectMapperConfigurationData() {
        }

        public DefaultObjectMapperConfigurationData(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }

        public void setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }
    }

}
