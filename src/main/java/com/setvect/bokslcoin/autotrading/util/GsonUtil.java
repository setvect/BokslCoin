package com.setvect.bokslcoin.autotrading.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

public class GsonUtil {

    public static final Gson GSON;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder().setFieldNamingStrategy(f -> {
            // 숫자로 구분된 필드명도 변환 가능하도록 이름을 변경
            // 예)
            // accTradePrice24h => accTradePrice_24h
            // highest52WeekDate => highest_52WeekDate
            String name = f.getName().replaceAll("(\\d+)", "_$1");
            return separateCamelCase(name, "_").toLowerCase(Locale.ENGLISH);
        });
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) -> {
                    String dateTimeString = getString(json);
                    // 날짜 표현 문자열 일관성 맞추기
                    if (dateTimeString.length() == "2021-05-15T11:26:30+09:00".length()) {
                        dateTimeString = dateTimeString.substring(0, 19);
                    }
                    return DateUtil.getLocalDateTime(dateTimeString, DateUtil.yyyy_MM_ddTHH_mm_ss);
                }
        );
        gsonBuilder.registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, jsonDeserializationContext) -> {
                    String dateString = getString(json);
                    if (dateString.length() == 10) {
                        return DateUtil.getLocalDate(dateString, DateUtil.yyyy_MM_dd);
                    }
                    return DateUtil.getLocalDate(dateString, DateUtil.yyyyMMdd);
                }
        );
        gsonBuilder.registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, jsonDeserializationContext) -> {
                    String timeStr = getString(json);
                    if (timeStr.length() == "12:00:00".length()) {
                        return DateUtil.getLocalTime(timeStr, DateUtil.HH_mm_ss);
                    }
                    return DateUtil.getLocalTime(timeStr, DateUtil.HHmmss);
                }
        );
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (localDateTime, type, jsonDeserializationContext)
                -> new JsonPrimitive(DateUtil.format(localDateTime, DateUtil.yyyy_MM_ddTHH_mm_ss))
        );

        GSON = gsonBuilder.create();
    }


    static String separateCamelCase(String name, String separator) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0, length = name.length(); i < length; i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }
            translation.append(character);
        }
        return translation.toString();
    }

    private static String getString(JsonElement json) {
        JsonPrimitive asJsonPrimitive = json.getAsJsonPrimitive();
        return asJsonPrimitive.getAsString();
    }
}
