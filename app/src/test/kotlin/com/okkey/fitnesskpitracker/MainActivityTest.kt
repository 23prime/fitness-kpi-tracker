package com.okkey.fitnesskpitracker

import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.okkey.fitnesskpitracker.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun launches() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }
}
