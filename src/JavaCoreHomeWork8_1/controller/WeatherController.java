package JavaCoreHomeWork8_1.controller;

import JavaCoreHomeWork8_1.model.DBRow;
import JavaCoreHomeWork8_1.model.SituateWeather;
import JavaCoreHomeWork8_1.model.WeatherResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.sqlite.JDBC;
import java.sql.*;
import java.util.ArrayList;

public class WeatherController {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static String APPID = "ecf4e6659dd0d37623f54d80b22a7fb7";

    static String host = "api.openweathermap.org";
    static String segment1 = "data";
    static String segment2 = "2.5";
    static String segment3 = "forecast";

    /**
     * Получение погоды по названию города (вариант без использования http запроса)
     * @param city
     * @return WeatherResponse
     * @throws IOException
     */
    public static WeatherResponse getWeatherFromCity(String city) throws IOException {
        String urlAddress = "http://api.openweathermap.org/data/2.5/forecast?q="+city+"&appid="+APPID+"&units=metric";
        StringBuffer content = new StringBuffer();
        try {
            URL url = new URL(urlAddress);
            URLConnection urlConn = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            String line;
            while((line = bufferedReader.readLine()) != null) {
                content.append(line + "\n");
            }
            bufferedReader.close();
        } catch(Exception e) {
            System.out.println("Такого города нет в Openweathermap!");
            return new WeatherResponse();
        }
        WeatherResponse response = objectMapper.readValue(content.toString(), WeatherResponse.class);

        return response;
    }

    /**
     * Получение погоды по названию города (вариант с http запросом через внешнюю библиотеку)
     * @param city
     * @return
     * @throws IOException
     */
    public static WeatherResponse getWeatherFromCityV2(String city) throws IOException {
        String urlAddress = "http://api.openweathermap.org/data/2.5/forecast?q="+city+"&appid="+APPID+"&units=metric";
        StringBuffer content = new StringBuffer();
        try {
            // тут http запрос через okhttp
            OkHttpClient client = new OkHttpClient();

            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("http")
                    .host(host)
                    .addPathSegment(segment1)
                    .addPathSegment(segment2)
                    .addPathSegment(segment3)
                    .addQueryParameter("q",city)
                    .addQueryParameter("lang","ru")
                    .addQueryParameter("units","metric")
                    .addQueryParameter("APPID",APPID)
                    .build();

            Request request = new Request.Builder()
                    .url(httpUrl)
                    .build();

            Response response = client.newCall(request).execute();
            String res = response.body().string();
            WeatherResponse result = objectMapper.readValue(res, WeatherResponse.class);
            return result;

        } catch(Exception e) {
            //System.out.println("Такого города нет в Openweathermap!");
            return new WeatherResponse();
        }
    }
}