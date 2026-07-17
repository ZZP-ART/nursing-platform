package com.nursing.common.config;

import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Applies the public JSON contract without converting non-ID long values such as totals. */
@Configuration
public class ApiJsonContractConfiguration {

    @Bean
    public SimpleModule apiIdSerializationModule() {
        SimpleModule module = new SimpleModule("api-id-as-string");
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                              com.fasterxml.jackson.databind.BeanDescription beanDesc,
                                                              List<BeanPropertyWriter> properties) {
                for (BeanPropertyWriter property : properties) {
                    if (property.getType().getRawClass().equals(Long.class) && isIdProperty(property.getName())) {
                        property.assignSerializer(ToStringSerializer.instance);
                    }
                }
                return properties;
            }
        });
        return module;
    }

    private static boolean isIdProperty(String name) {
        return "id".equals(name) || name.endsWith("Id");
    }
}
