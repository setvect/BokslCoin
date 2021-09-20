package com.setvect.bokslcoin.autotrading.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DateTimeFormatConfiguration implements WebMvcConfigurer {

    /**
     * 파라미터로 받는 날짜 포맷팅 허용 형식 정의 <br>
     * 예시: 2000-10-31T01:30:00.000-05:00
     *
     * @param registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
    }
}
