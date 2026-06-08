package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.utils.CommandClient

@Composable
fun DashboardCardRenderer(
    cardGroup: CardGroup,
    cardWidth: CardWidth,
    uiState: DashboardUiState,
    serviceStatus: Status = Status.Stopped,
    onClashModeSelected: (String) -> Unit,
    onSystemProxyToggle: (Boolean) -> Unit,

    commandClient: CommandClient? = null,
    modifier: Modifier = Modifier,
) {
    when (cardGroup) {
        CardGroup.ClashMode -> {
            if (uiState.clashModeVisible) {
                ClashModeCard(
                    modes = uiState.clashModes,
                    selectedMode = uiState.selectedClashMode,
                    onModeSelected = onClashModeSelected,
                    modifier = modifier,
                )
            }
        }

        CardGroup.UploadTraffic -> {
            if (uiState.trafficVisible) {
                UploadTrafficCard(
                    uplink = uiState.uplink,
                    uplinkTotal = uiState.uplinkTotal,
                    uplinkHistory = uiState.uplinkHistory,
                    modifier = modifier,
                )
            }
        }

        CardGroup.DownloadTraffic -> {
            if (uiState.trafficVisible) {
                DownloadTrafficCard(
                    downlink = uiState.downlink,
                    downlinkTotal = uiState.downlinkTotal,
                    downlinkHistory = uiState.downlinkHistory,
                    modifier = modifier,
                )
            }
        }

        CardGroup.Debug -> {
            if (uiState.isStatusVisible) {
                DebugCard(
                    memory = uiState.memory,
                    goroutines = uiState.goroutines,
                    modifier = modifier,
                )
            }
        }

        CardGroup.Connections -> {
            if (uiState.trafficVisible) {
                ConnectionsCard(
                    connectionsIn = uiState.connectionsIn,
                    connectionsOut = uiState.connectionsOut,
                    modifier = modifier,
                )
            }
        }

        CardGroup.SystemProxy -> {
            if (uiState.systemProxyVisible) {
                SystemProxyCard(
                    enabled = uiState.systemProxyEnabled,
                    isSwitching = uiState.systemProxySwitching,
                    onToggle = onSystemProxyToggle,
                    modifier = modifier,
                )
            }
        }


    }
}
