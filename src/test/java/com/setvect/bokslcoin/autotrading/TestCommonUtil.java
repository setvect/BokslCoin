package com.setvect.bokslcoin.autotrading;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.setvect.bokslcoin.autotrading.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestCommonUtil {

    public static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

        gsonBuilder.registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (localDateTime, typeOfSrc, context) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtil.yyyy_MM_ddTHH_mm_ss);
            return new JsonPrimitive(formatter.format(localDateTime));
        });
        // 지수 표현 사용하지 않고, 소수점으로 표현
        gsonBuilder.registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
            BigDecimal value = BigDecimal.valueOf(src);
            try {
                value = new BigDecimal(value.toBigIntegerExact());
            } catch (ArithmeticException e) {
                // ignore
            }
            return new JsonPrimitive(value);
        });

        Gson gson = gsonBuilder.setPrettyPrinting().create();
        return gson;
    }
}
