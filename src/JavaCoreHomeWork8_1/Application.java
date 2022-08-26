package JavaCoreHomeWork8_1;

import java.io.IOException;
import java.sql.SQLException;

import JavaCoreHomeWork8_1.view.WeatherViewer;

public class Application {
    public static void main(String[] args) throws IOException, SQLException {
        // Консольный интерфейс
        WeatherViewer.processCity();

    }

}