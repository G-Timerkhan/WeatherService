package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WeatherApp {


    private static final String API_KEY = "f5b2e807-4136-4eda-a413-846a62074ab7";

    private static final String LAT = "55.75";   // широта (Москва)
    private static final String LON = "37.62";   // долгота (Москва)
    private static final int LIMIT = 3;          // сколько суток прогноза взять

    public static void main(String[] args) {
        // 1. Собираем URL запроса: путь + параметры lat, lon, limit
        HttpUrl url = HttpUrl.parse("https://api.weather.yandex.ru/v2/forecast")
                .newBuilder()
                .addQueryParameter("lat", LAT)
                .addQueryParameter("lon", LON)
                .addQueryParameter("limit", String.valueOf(LIMIT))
                .build();

        // 2. Создаём HTTP-запрос с  заголовком X-Yandex-Weather-Key
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Yandex-Weather-Key", API_KEY)
                .get()
                .build();

        OkHttpClient client = new OkHttpClient();

        // 3. Выполняем запрос и разбираем ответ
        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                System.out.println("Ошибка запроса. Код: " + response.code());
                System.out.println("Ответ сервера: " + response.body().string());
                System.out.println("Скорее всего, ключ API неверный или истёк срок его действия (7 дней).");
                return;
            }

            String rawJson = response.body().string();

            // 2 (по критериям). Выводим весь JSON "как есть"
            System.out.println("========= ПОЛНЫЙ JSON-ОТВЕТ =========");
            System.out.println(rawJson);
            System.out.println("=====================================\n");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            // 3 (по критериям). Текущая температура: fact → temp
            JsonNode fact = root.get("fact");
            if (fact != null && fact.has("temp")) {
                int currentTemp = fact.get("temp").asInt();
                System.out.println("ТЕКУЩАЯ ТЕМПЕРАТУРА (fact.temp): " + currentTemp + "°C");
            } else {
                System.out.println("Не удалось получить текущую температуру.");
            }

            // 4 (по критериям). Средняя температура по прогнозу
            JsonNode forecasts = root.get("forecasts");
            List<Double> temps = new ArrayList<>();

            if (forecasts != null && forecasts.isArray()) {
                System.out.println("\nДанные по дням (первые " + LIMIT + "):");
                int count = Math.min(LIMIT, forecasts.size());
                for (int i = 0; i < count; i++) {
                    JsonNode day = forecasts.get(i);
                    String date = day.path("date").asText("?");
                    JsonNode tempAvgNode = day.path("parts").path("day").path("temp_avg");

                    if (!tempAvgNode.isMissingNode() && !tempAvgNode.isNull()) {
                        double tempAvg = tempAvgNode.asDouble();
                        temps.add(tempAvg);
                        System.out.println("  " + date + ": " + tempAvg + "°C");
                    } else {
                        System.out.println("  " + date + ": нет данных о средней температуре дня");
                    }
                }
            }

            if (!temps.isEmpty()) {
                double sum = 0;
                for (double t : temps) sum += t;
                double average = sum / temps.size();
                System.out.printf("\nСРЕДНЯЯ ТЕМПЕРАТУРА ЗА %d СУТОК: %.2f°C%n", temps.size(), average);
            } else {
                System.out.println("\nНе удалось вычислить среднюю температуру.");
            }

        } catch (IOException e) {
            System.out.println("Ошибка сети: " + e.getMessage());
        }
    }
}