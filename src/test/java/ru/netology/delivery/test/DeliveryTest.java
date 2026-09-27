package ru.netology.delivery.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import ru.netology.delivery.data.DataGenerator;

import java.io.ByteArrayInputStream;

import static com.codeborne.selenide.Condition.text;
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

    private String selectDate(long requestedOffset, long currentCalendarDateOffset) {
        String planningDate = DataGenerator.generateDate(requestedOffset, "dd.MM.yyyy");
        String planningDay = DataGenerator.generateDate(requestedOffset, "d");

        $("[data-test-id='date'] button").click();
        if (!DataGenerator.generateDate(currentCalendarDateOffset, "MM")
                .equals(DataGenerator.generateDate(requestedOffset, "MM"))) {
            $(cssSelector(".calendar__arrow_direction_right:not(.calendar__arrow_double)")).click();
        }
        $$(cssSelector("[data-day]"))
                .findBy(text(planningDay))
                .click();
        return planningDate;
    }

    @Test
    @DisplayName("Should successfully plan and replan meeting")
    void shouldSuccessfullyPlanAndReplanMeeting() {
        var validUser = DataGenerator.Registration.generateUser("ru");
        long firstMeetingOffset = 7;
        long secondMeetingOffset = firstMeetingOffset + 3;

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
