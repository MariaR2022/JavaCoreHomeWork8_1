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

    /**
     * Вложенный класс-репозиторий, предназначенный для подключения к БД и операций с нею.
     */
    private static class DBController {
        // контроллер бызы данных
        //private final String PATH_TO_DB = "jdbc:sqlite:src/main/resources/weather.db";
        // не хочу чтобы база упаковывалась в jar, поэтому положу ее в папку в программой
        private final String PATH_TO_DB = "jdbc:sqlite:weather.db";
        private Connection connection;

        public DBController() throws SQLException {
            DriverManager.registerDriver(new JDBC());
            this.connection = DriverManager.getConnection(PATH_TO_DB);
            try (PreparedStatement preparedStatement = this.connection.prepareStatement(
                    " CREATE TABLE `weatherTable` (\n" +
                            "`city`\tTEXT,\n" +
                            " `localDate`\tTEXT,\n" +
                            " `weatherText`\tTEXT,\n" +
                            " `temperature`\tREAL\n" +
                            ");"
            )) {
                preparedStatement.execute();
                // если такой таблицы небыло то она появилась
            } catch (Exception e) {
                // ничего не делаем, сообщения нам не нужны.
            }
        }

        public void addRow(DBRow row) {
            // добавим запись в базу данных
            try (PreparedStatement preparedStatement = this.connection.prepareStatement(
                    "INSERT INTO weatherTable('city','localDate','weatherText','temperature') VALUES (?,?,?,?)"
            )) {
                preparedStatement.setObject(1, row.getCity());
                preparedStatement.setObject(2, row.getLocalDate());
                preparedStatement.setObject(3, row.getWeatherText());
                preparedStatement.setObject(4, row.getTemperature());
                preparedStatement.execute();
            } catch (Exception e) {
                System.out.println(e);
                // дадим сообщение об ошибке в отладку (вообще конечно логировать бы, но задача такая сейчас не стоит)
            }
        }

        public ArrayList<DBRow> getWeatherHistory(String city) {
            // получим из бд историю по городу.
            ArrayList<DBRow> result = new ArrayList<>();
            try (PreparedStatement preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM weatherTable WHERE city=?"
            )) {
                preparedStatement.setObject(1, city);
                ResultSet resultSet= preparedStatement.executeQuery();
                while (resultSet.next()) {
                    DBRow row = new DBRow(
                            resultSet.getString("city"),
                            resultSet.getString("localDate"),
                            resultSet.getString("weatherText"),
                            resultSet.getDouble("temperature")
                    );
                    result.add(row);
                }
            } catch (Exception e) {
                System.out.println(e);
            }
            return result;
        }
    }

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
        String urlAddres = "http://api.openweathermap.org/data/2.5/forecast?q="+city+"&appid="+APPID+"&units=metric";
        StringBuffer content = new StringBuffer();
        try {
            URL url = new URL(urlAddres);
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
     * ЭТОТ ЗАПРОС СОХРАНЯЕТСЯ В БД!!!
     * @param city
     * @return
     * @throws IOException
     */
    public static WeatherResponse getWeatherFromCityV2(String city) throws IOException {
        String urlAddres = "http://api.openweathermap.org/data/2.5/forecast?q="+city+"&units=metric&appid="+APPID;
        StringBuffer content = new StringBuffer();
        try {
            // а тут http запрос через okhttp
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
            // поскольку на этом этапе понятно что запрос успешен - дадим данные о нем в БД
            DBController controller = new DBController();
            for (SituateWeather i: result.getList()) {
                controller.addRow(new DBRow(
                        result.getCity().getName(),
                        i.getDt_txt(),
                        i.getWeather().toString().replace("[", "").replace("]", ""),
                        i.getMain().getTemp()));
            }
            return result;

        } catch(Exception e) {
            //System.out.println("Такого города нет в Openweathermap!");
            return new WeatherResponse();
        }
    }
    /**
     * получение из БД истории по городу используя вложенный приватный контроллер
     * @param city
     */
    public static void printCityHistory(String city) throws SQLException{
        DBController controller = new DBController();
        ArrayList<DBRow> result = controller.getWeatherHistory(city);
        for (DBRow i: result) {
            System.out.println(i.toCuteString());
        }
    }
}