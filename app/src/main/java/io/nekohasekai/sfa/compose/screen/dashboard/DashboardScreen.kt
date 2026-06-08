package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.constant.Status

@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    viewModel: DashboardViewModel? = null,
) {
    val isRunning = serviceStatus == Status.Started || serviceStatus == Status.Starting
    val isStopping = serviceStatus == Status.Stopping

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FloatingActionButton(
                onClick = {
                    if (isRunning || isStopping) {
                        viewModel?.toggleService()
                    } else {
                        viewModel?.toggleService()
                    }
                },
                modifier = Modifier.size(120.dp),
                containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = if (isRunning) "Disconnect" else "Connect",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val statusText = when {
                serviceStatus == Status.Started -> stringResource(R.string.status_started)
                serviceStatus == Status.Starting -> stringResource(R.string.status_starting)
                serviceStatus == Status.Stopping -> stringResource(R.string.status_stopping)
                else -> "Disconnected"
            }

            Text(
                text = "Status: $statusText",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
