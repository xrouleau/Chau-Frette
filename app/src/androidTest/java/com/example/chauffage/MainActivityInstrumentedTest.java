package com.example.chauffage;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testSwitchChauffageClickSwitchACOff() {
        Espresso.onView(withId(R.id.switchChauffage)).perform(click());

        Espresso.onView(withId(R.id.switchAC)).check(matches(isNotChecked()));
    }

    @Test
    public void testSwitchACClickSwitchChauffageOff() {
        Espresso.onView(withId(R.id.switchAC)).perform(click());

        Espresso.onView(withId(R.id.switchChauffage)).check(matches(isNotChecked()));
    }

    @Test
    public void testEditTextNumberIsEmptyAfterButtonIntensiteClick() {
        Espresso.onView(withId(R.id.buttonIntensite)).perform(click());

        Espresso.onView(withId(R.id.editTextNumber)).check(matches(withText("")));
    }
}
