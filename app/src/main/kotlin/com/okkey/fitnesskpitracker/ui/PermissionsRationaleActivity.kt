package com.okkey.fitnesskpitracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.ui.theme.FitnessKpiTheme

private val CONTENT_PADDING = 16.dp
private val CONTENT_SPACING = 12.dp

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessKpiTheme {
                Surface {
                    PermissionsRationaleScreen()
                }
            }
        }
    }
}

@Composable
private fun PermissionsRationaleScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(CONTENT_PADDING),
        verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING),
    ) {
        Text(
            text = stringResource(R.string.permissions_rationale_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(text = stringResource(R.string.permissions_rationale_body))
    }
}
