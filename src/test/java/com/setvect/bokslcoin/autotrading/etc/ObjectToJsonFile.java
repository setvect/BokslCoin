package com.setvect.bokslcoin.autotrading.etc;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class ObjectToJsonFile {
    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();
        File saveFile = new File("./data", "abc.json");
        Map<String, String> aa = new HashMap<>();
        aa.put("aaa", "복슬이");

        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(aa, writer);
        }

        System.out.println("끝.");
    }
}
