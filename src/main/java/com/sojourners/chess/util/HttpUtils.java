package com.sojourners.chess.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtils {

    private static final String CONTENT_TYPE = "Content-Type";

    public static final String JSON_TYPE = "application/json;charset=utf-8";
    public static final String FORM_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";

    /**
     *
     * 方法说明：<br>
     * @param url 请求URL
     * @param content post参数 key1=val1&key2=val2&key3=val3
     * @return
     * @throws Exception
     */
    public static String sendByPost(String url, String content) throws IOException {

        return sendByPost(url, content, null, FORM_TYPE);
    }

    /**
     *
     * 方法说明：<br>
     * @param url 请求URL
     * @param content post参数 key1=val1&key2=val2&key3=val3
     * @return
     * @throws Exception
     */
    public static String sendByPost(String url, String content, String contentType) throws IOException {
        return sendByPost(url, content, null, contentType);
    }

    /**
     * post 请求
     * @param url
     * @param json
     * @param header
     * @param contentType
     * @return
     * @throws IOException
     */
    public static String sendByPost(String url, String json, Map<String, String> header, String contentType) throws IOException {
        StringBuilder result = new StringBuilder();

        URL u = new URL(url);
        HttpURLConnection con = (HttpURLConnection)u.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setAllowUserInteraction(false);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
        con.setRequestProperty(CONTENT_TYPE, contentType);
        if (header != null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        con.setConnectTimeout(10000);
        con.setReadTimeout(20000);

        OutputStream out = null;
        BufferedReader reader = null;

        String line = null;
        try {
            out = con.getOutputStream();
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
            reader = new BufferedReader(new InputStreamReader(con.getResponseCode() == HttpURLConnection.HTTP_OK ? con.getInputStream() : con.getErrorStream(), StandardCharsets.UTF_8));
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } finally {
            if (out != null) {
                out.close();
            }
            if (reader != null) {
                reader.close();
            }
        }

        return result.toString();
    }


    public static String sendByPost(String url, String json, Map<String, String> headerParam) throws IOException {

        return sendByPost(url, json, headerParam, JSON_TYPE);
    }

    public static InputStream getInputStreamBySendByPost(String url, String content) throws IOException {
        URL u = new URL(url);
        HttpURLConnection con = (HttpURLConnection)u.openConnection();

        con.setDoInput(true);
        con.setDoOutput(true);
        con.setAllowUserInteraction(false);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
        con.setRequestProperty(CONTENT_TYPE, FORM_TYPE);
        con.setConnectTimeout(10000);
        con.setReadTimeout(20000);
        OutputStream out = null;
        try
        {
            out = con.getOutputStream();
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return con.getInputStream();
        }
        finally
        {
            if (out != null)
            {
                out.close();
            }

        }
    }

    /**
     * HTTP GET方法
     * @param url 请求URL
     * @param content get参数 key1=val1&key2=val2&key3=val3
     * @return
     */
    public static String sendByGet(String url, String content, int timeout) {
        try {
            URL u = new URL(url + "?" + content);
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setUseCaches(false);
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
            StringBuffer result = new StringBuffer();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))){
                String line;
                while (null != (line = reader.readLine())) {
                    result.append(line);
                }
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}