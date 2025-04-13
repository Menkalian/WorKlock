package de.menkalian.worklock.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.menkalian.worklock.R
import de.menkalian.worklock.controller.DummyLogicController
import de.menkalian.worklock.controller.LogicController
import de.menkalian.worklock.ui.theme.WorklockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TrackingView(viewModel: LogicController = LogicController.get()) {
    val ioScope = rememberCoroutineScope { Dispatchers.IO }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val totalTime by viewModel.currentTimeForDayMinutes
        val minutes = totalTime % 60
        val hours = totalTime / 60
        Text(
            stringResource(R.string.tracking_current_time, hours, minutes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        AnimatedVisibility(viewModel.currentDayHasError.value) {
            Text(
                stringResource(R.string.tracking_current_day_error),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        val started by viewModel.isStarted
        val paused by viewModel.isPaused
        val dayTime by viewModel.currentTimeForDayMinutes
        val currentStateText = when {
            !started && dayTime == 0 -> stringResource(R.string.tracking_currentstate_home_before)
            started && !paused       -> stringResource(R.string.tracking_currentstate_working)
            paused                   -> stringResource(R.string.tracking_currentstate_pause)
            else                     -> stringResource(R.string.tracking_currentstate_home_after)
        }
        Text(
            currentStateText,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(50.dp))
        Button(
            onClick = { ioScope.launch { viewModel.start() } },
            modifier = Modifier.size(300.dp, 60.dp),
            enabled = viewModel.isStarted.value.not()
        ) {
            Icon(painterResource(R.drawable.icon_work), stringResource(R.string.tracking_start))
            Spacer(modifier = Modifier.width(3.dp))
            Text(stringResource(R.string.tracking_start))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { ioScope.launch { viewModel.end() } },
            modifier = Modifier.size(300.dp, 60.dp),
            colors = ButtonDefaults.buttonColors().copy(containerColor = MaterialTheme.colorScheme.error),
            enabled = viewModel.isStarted.value
        ) {
            Icon(painterResource(R.drawable.icon_endwork), stringResource(R.string.tracking_end))
            Spacer(modifier = Modifier.width(3.dp))
            Text(stringResource(R.string.tracking_end))
        }
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = { ioScope.launch { viewModel.togglePause() } },
            modifier = Modifier.size(300.dp, 40.dp),
            colors = ButtonDefaults.buttonColors().copy(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = viewModel.isStarted.value
        ) {
            if (viewModel.isPaused.value) {
                Icon(painterResource(R.drawable.icon_continue), stringResource(R.string.tracking_unpause))
                Spacer(modifier = Modifier.width(3.dp))
                Text(stringResource(R.string.tracking_unpause))
            } else {
                Icon(painterResource(R.drawable.icon_pause), stringResource(R.string.tracking_pause))
                Spacer(modifier = Modifier.width(3.dp))
                Text(stringResource(R.string.tracking_pause))
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = { ioScope.launch { viewModel.undoLastAction() } },
            modifier = Modifier.size(300.dp, 40.dp),
            colors = ButtonDefaults.buttonColors().copy(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = viewModel.allowUndo.value
        ) {
            Icon(painterResource(R.drawable.icon_undo), stringResource(R.string.tracking_undo))
            Spacer(modifier = Modifier.width(3.dp))
            Text(stringResource(R.string.tracking_undo))
        }
    }
}

@Preview
@Composable
fun TrackingPreview() {
    WorklockTheme(darkTheme = false, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            TrackingView(DummyLogicController())
        }
    }
}

@Preview
@Composable
fun TrackingDarkPreview() {
    WorklockTheme(darkTheme = true, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            TrackingView(DummyLogicController())
        }
    }
}