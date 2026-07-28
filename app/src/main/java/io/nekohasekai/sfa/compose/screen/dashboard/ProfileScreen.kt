package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.compose.screen.settings.SettingTile
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

import androidx.compose.ui.res.stringResource
import io.nekohasekai.sfa.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: DashboardViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val userProfile = uiState.userProfile
    
    LaunchedEffect(Unit) {
        viewModel.refreshUserProfile()
    }
    
    var showPasswordDialog by remember { mutableStateOf(false) }


    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onChangePassword = { old, new ->
                viewModel.changePassword(old, new, 
                    onSuccess = {
                        showPasswordDialog = false
                    },
                    onError = {
                        // Ideally show a snackbar or toast here
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { paddingValues ->


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

        // Avatar and Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = userProfile?.username ?: stringResource(R.string.dashboard_loading),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Subscription Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val rawTier = userProfile?.subscription_tier
                    val tierFree = stringResource(R.string.profile_tier_free)
                    val tierPremium = stringResource(R.string.profile_tier_premium)
                    val tierUnlimited = stringResource(R.string.profile_tier_unlimited)
                    val loadingStr = stringResource(R.string.dashboard_loading)
                    val tierName = when (rawTier?.lowercase(java.util.Locale.ROOT)) {
                        "free" -> tierFree
                        "premium" -> tierPremium
                        "unlimited" -> tierUnlimited
                        null -> loadingStr
                        else -> rawTier.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    }
                    Text(stringResource(R.string.profile_label_tier, tierName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))

                    val subPermanent = stringResource(R.string.profile_sub_permanent)
                    val subUnlimited = stringResource(R.string.profile_sub_unlimited_time)
                    val unknownStr = stringResource(R.string.dashboard_unknown)
                    val activeUntilFormat = stringResource(R.string.profile_sub_active_until, "%s")
                    
                    val dateText = remember(userProfile?.subscription_end) {
                        if (userProfile?.subscription_end == null) {
                            if (userProfile?.subscription_tier == "free") {
                                subPermanent
                            } else {
                                subUnlimited
                            }
                        } else {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                val date = sdf.parse(userProfile.subscription_end)
                                val outSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                val formattedDate = date?.let { outSdf.format(it) } ?: unknownStr
                                String.format(activeUntilFormat, formattedDate)
                            } catch (e: Exception) {
                                String.format(activeUntilFormat, userProfile.subscription_end)
                            }
                        }
                    }
                    Text(dateText, style = MaterialTheme.typography.labelMedium)
                }
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Settings list
        val context = LocalContext.current
        var isUploadingLogs by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingTile(
                    icon = Icons.Rounded.BugReport,
                    title = if (isUploadingLogs) stringResource(R.string.profile_sending_logs) else stringResource(R.string.profile_send_logs),
                    onClick = {
                        if (!isUploadingLogs) {
                            isUploadingLogs = true
                            coroutineScope.launch {
                                val result = io.nekohasekai.sfa.network.AppLogCollector.uploadLogs(context)
                                isUploadingLogs = false
                                result.onSuccess { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message ?: "Error sending logs", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
                SettingTile(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.profile_change_password),
                    onClick = { showPasswordDialog = true }
                )
                SettingTile(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = stringResource(R.string.profile_logout),
                    onClick = {
                        coroutineScope.launch {
                            io.nekohasekai.sfa.bg.BoxService.stop()
                            io.nekohasekai.sfa.database.Settings.clearSession()
                            io.nekohasekai.sfa.compose.base.GlobalEventBus.emit(io.nekohasekai.sfa.compose.base.UiEvent.Logout)
                        }
                    },
                    badgeColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val mismatchError = stringResource(R.string.profile_passwords_mismatch)
    val shortError = stringResource(R.string.profile_password_too_short)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_change_password_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(stringResource(R.string.profile_old_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.profile_new_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.profile_repeat_new_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword != confirmPassword) {
                        errorText = mismatchError
                    } else if (newPassword.length < 6) {
                        errorText = shortError
                    } else {
                        onChangePassword(oldPassword, newPassword)
                    }
                }
            ) {
                Text(stringResource(R.string.profile_change_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
