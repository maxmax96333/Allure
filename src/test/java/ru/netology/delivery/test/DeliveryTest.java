package ru.netology.delivery.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeOptions;
import ru.netology.delivery.data.DataGenerator;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.openqa.selenium.By.cssSelector;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

class DeliveryTest {
    @BeforeEach
    void setup() {
        SelenideLogger.addListener("AllureSelenide",
                new AllureSelenide().screenshots(true).savePageSource(true));
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments(
                "--no-sandbox",
                "--no-first-run",
                "--disable-extensions",
                "--disable-gpu",
                "--disable-gpu-compositing",
                "--use-gl=angle",
                "--use-angle=swiftshader"
        );
        Configuration.browserCapabilities = chromeOptions;
        open(System.getProperty("sut.url", "http://localhost:9999"));
    }

    private void attachFinalScreenshot() {
        byte[] screenshot = ((TakesScreenshot) WebDriverRunner.getWebDriver())
                .getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment(
                "Итоговый экран после перепланирования",
                "image/png",
                new ByteArrayInputStream(screenshot),
                ".png"
        );
    }

    private String generateDate(long addDays, String pattern) {
        return LocalDate.now()
                .plusDays(addDays)
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    private long nextWeekdayOffset(long addDays) {
        LocalDate plannedDate = LocalDate.now().plusDays(addDays);
        while (plannedDate.getDayOfWeek().getValue() > 5) {
            plannedDate = plannedDate.plusDays(1);
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), plannedDate);
    }

    private String selectDate(long requestedOffset, long currentCalendarDateOffset) {
        String planningDate = generateDate(requestedOffset, "dd.MM.yyyy");
        String planningDay = generateDate(requestedOffset, "dd").replaceFirst("^0", "");

        // Приложение блокирует выходные, поэтому при необходимости выбираем ближайший будний день.
        long selectedOffset = nextWeekdayOffset(requestedOffset);
        if (selectedOffset != requestedOffset) {
            planningDate = generateDate(selectedOffset, "dd.MM.yyyy");
            planningDay = generateDate(selectedOffset, "dd").replaceFirst("^0", "");
        }

        $("[data-test-id='date'] button").click();
        if (generateDate(currentCalendarDateOffset, "MM").equals(generateDate(selectedOffset, "MM"))) {
            $$(cssSelector("[data-day]"))
                    .findBy(text(planningDay))
                    .click();
        } else {
            // В этой версии приложения месячная стрелка имеет класс calendar__arrow_direction_right.
            $(cssSelector(".calendar__arrow_direction_right:not(.calendar__arrow_double)")).click();
            $$(cssSelector("[data-day]"))
                    .findBy(text(planningDay))
                    .click();
        }

        $("[data-test-id='date'] input").shouldHave(value(planningDate));
        return planningDate;
    }

    @Test
    @DisplayName("Should successfully plan and replan meeting")
    void shouldSuccessfullyPlanAndReplanMeeting() {
        var validUser = DataGenerator.Registration.generateUser("ru");
        long firstMeetingOffset = nextWeekdayOffset(7);
        long secondMeetingOffset = nextWeekdayOffset(firstMeetingOffset + 3);

        $("[data-test-id='city'] input").setValue(validUser.getCity());
        String firstMeetingDate = selectDate(firstMeetingOffset, 3);

        $("[data-test-id='name'] input").setValue(validUser.getName());
        $("[data-test-id='phone'] input").setValue(validUser.getPhone());
        $("[data-test-id='agreement']").click();

        $$("button").findBy(text("Запланировать")).click();

        $("[data-test-id='success-notification']")
                .shouldBe(visible)
                .shouldHave(text("Встреча успешно запланирована на " + firstMeetingDate));

        String secondMeetingDate = selectDate(secondMeetingOffset, firstMeetingOffset);

        $$("button").findBy(text("Запланировать")).click();

        $("[data-test-id='replan-notification']")
                .shouldBe(visible)
                .shouldHave(
                        text("У вас уже запланирована встреча на другую дату. Перепланировать?")
                );

        $("[data-test-id='replan-notification']")
                .$$("button")
                .findBy(text("Перепланировать"))
                .shouldBe(Condition.enabled)
                .click();

        $("[data-test-id='success-notification']")
                .shouldBe(visible)
                .shouldHave(text("Встреча успешно запланирована на " + secondMeetingDate));

        attachFinalScreenshot();
    }
}
