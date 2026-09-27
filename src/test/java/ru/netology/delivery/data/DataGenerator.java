package ru.netology.delivery.data;

import com.github.javafaker.Faker;
import lombok.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

public class DataGenerator {

    private DataGenerator() {
    }

    public static String generateDate(int shift) {
        return LocalDate.now()
                .plusDays(shift)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public static String generateCity(Faker faker) {
        String[] cities = {
                "Москва",
                "Санкт-Петербург",
                "Казань",
                "Тула",
                "Воронеж",
                "Калуга"
        };

        return cities[new Random().nextInt(cities.length)];
    }

    public static String generateName(Faker faker) {
        return faker.name().fullName().replace('ё', 'е').replace('Ё', 'Е');
    }

    public static String generatePhone(Faker faker) {
        return "+7" + faker.number().digits(10);
    }

    public static class Registration {
        private static Faker faker;

        private Registration() {
        }

        public static UserInfo generateUser(String locale) {
            faker = new Faker(new Locale(locale));

            return new UserInfo(
                    generateCity(faker),
                    generateName(faker),
                    generatePhone(faker)
            );
        }
    }

    @Value
    public static class UserInfo {
        String city;
        String name;
        String phone;
    }
}