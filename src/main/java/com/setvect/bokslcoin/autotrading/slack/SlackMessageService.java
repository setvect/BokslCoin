package com.setvect.bokslcoin.autotrading.slack;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SlackMessageService {

    @Value("${com.setvect.bokslcoin.autotrading.slack.enable}")
    private boolean enable;

    @Value("${com.setvect.bokslcoin.autotrading.slack.token:#{null}}")
    private String token;

    @Value("${com.setvect.bokslcoin.autotrading.slack.channelId:#{null}}")
    private String channelId;

    private static final String MESSAGE_POST = "https://slack.com/api/chat.postMessage";

    @SneakyThrows
    public void sendMessage(String message) {
        if (!enable) {
            log.debug("skip");
            return;
        }

        Map<String, String> param = new HashMap<>();
        param.put("channel", channelId);
        param.put("text", message);
        param.put("link_names", "true");

        String url = MESSAGE_POST + "?" + ApplicationUtil.getQueryString(param);
        HttpPost request = new HttpPost(url);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("token", token));
        request.setEntity(new UrlEncodedFormEntity(params));

        String response = ApplicationUtil.request(url, request);
        if (response.contains("{\"ok\":false")) {
            log.warn(response);
        }
    }
}
