package com.okkey.fitnesskpitracker.ui

import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class PermissionsRationaleActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(PermissionsRationaleActivity::class.java)

    @Test
    fun launches() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }
}
