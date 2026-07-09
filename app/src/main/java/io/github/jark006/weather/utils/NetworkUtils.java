package io.github.jark006.weather.utils;

import static android.content.ContentValues.TAG;

import static io.github.jark006.weather.utils.Utils.saveLog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

/**
 * Created by JARK006 on 2022-5-22 01:59:13
 */

public class NetworkUtils {

    private static final String BASE_URL = "https://h5.caiyunapp.com";

    private static final HashMap<String, String> COMMON_HEADERS = new HashMap<>() {{
        put("accept", "application/json");
        put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        put("origin", "https://caiyunapp.com");
        put("referer", "https://caiyunapp.com/");
        put("sec-ch-ua", "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"");
        put("sec-ch-ua-mobile", "?0");
        put("sec-ch-ua-platform", "\"Windows\"");
        put("sec-fetch-dest", "empty");
        put("sec-fetch-mode", "cors");
        put("sec-fetch-site", "same-site");
        put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36");
    }};

    @SuppressLint("DefaultLocale")
    public static String getData(Context context, String link) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(link).openConnection();
            conn.setConnectTimeout(8 * 1000);
            conn.setReadTimeout(8 * 1000);

            int resCode = conn.getResponseCode();
            if (resCode == 200) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                int len;
                byte[] buffer = new byte[1024 * 200];  // 200 KiB
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                is.close();
                os.close();
                return os.toString();
            } else {
                saveLog(context, String.format("NetworkUtils 返回码:%d 异常 %s", resCode, link));
                return null;
            }
        } catch (IOException e) {
            saveLog(context, String.format("NetworkUtils IOException IO异常 %s\n%s",link, e.getMessage()));
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    // 新版获取天气接口，入参是间接链接
    @SuppressLint("DefaultLocale")
    @Nullable
    public static String getWeatherJson(Context context, String payloadUrl) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            // 第一步：获取 ticket
            Request ticketRequest = new Request.Builder()
                    .url(BASE_URL + "/api/ticket")
                    .post(RequestBody.create(null, ""))
                    .build();

            for (var entry : COMMON_HEADERS.entrySet()) {
                ticketRequest = ticketRequest.newBuilder()
                        .header(entry.getKey(), entry.getValue())
                        .build();
            }

            String ticket;
            try (Response ticketResponse = client.newCall(ticketRequest).execute()) {
                if (!ticketResponse.isSuccessful()) {
                    saveLog(context, String.format("getWeatherJson 获取ticket失败 返回码:%d", ticketResponse.code()));
                    return null;
                }
                String ticketBody = ticketResponse.body() != null ? ticketResponse.body().string() : null;
                if (ticketBody == null) {
                    saveLog(context, "getWeatherJson 获取ticket响应体为空");
                    return null;
                }
                JSONObject ticketJson = new JSONObject(ticketBody);
                ticket = ticketJson.getString("ticket");
            }

            // 第二步：用 ticket 鉴权，发送天气请求
            MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON_MEDIA, payloadUrl);

            Request.Builder weatherRequestBuilder = new Request.Builder()
                    .url(BASE_URL + "/api/?ticket=" + ticket)
                    .post(requestBody);

            for (var entry : COMMON_HEADERS.entrySet()) {
                weatherRequestBuilder.header(entry.getKey(), entry.getValue());
            }
            weatherRequestBuilder.header("content-type", "application/json");

            Request weatherRequest = weatherRequestBuilder.build();

            try (Response weatherResponse = client.newCall(weatherRequest).execute()) {
                if (!weatherResponse.isSuccessful()) {
                    saveLog(context, String.format("getWeatherJson 天气请求失败 返回码:%d", weatherResponse.code()));
                    return null;
                }
                String weatherBody = weatherResponse.body() != null ? weatherResponse.body().string() : null;
                if (weatherBody == null) {
                    saveLog(context, "getWeatherJson 天气响应体为空");
                    return null;
                }
                return weatherBody;
            }

        } catch (Exception e) {
            saveLog(context, "getWeatherJson 异常: " + e.getMessage());
        }
        return null;
    }
}
