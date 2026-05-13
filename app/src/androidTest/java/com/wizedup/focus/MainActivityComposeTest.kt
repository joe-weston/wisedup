package com.wizedup.focus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun coldLaunch_showsOnboardingOrHome() {
        composeRule.waitForIdle()
        val welcome = composeRule.activity.getString(R.string.onboarding_title)
        val activate = composeRule.activity.getString(R.string.home_activate_button)
        composeRule.onNode(hasText(welcome).or(hasText(activate))).assertIsDisplayed()
    }
}
