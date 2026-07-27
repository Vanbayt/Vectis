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
                title = { Text(androidx.compose.ui.res.stringResource(io.nekohasekai.sfa.R.string.profile_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(io.nekohasekai.sfa.R.string.content_description_back))
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
                text = userProfile?.username ?: "Загрузка...",
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
                    val tierName = when (rawTier?.lowercase(java.util.Locale.ROOT)) {
                        "free" -> "Бесплатный (5 ГБ)"
                        "premium" -> "Премиум"
                        "unlimited" -> "Безлимитный"
                        null -> "Загрузка..."
                        else -> rawTier.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    }
                    Text("Тариф: $tierName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))

                    
                    val dateText = remember(userProfile?.subscription_end) {
                        if (userProfile?.subscription_end == null) {
                            if (userProfile?.subscription_tier == "free") {
                                "Постоянный (Обновляется раз в месяц)"
                            } else {
                                "Безлимитно по времени"
                            }
                        } else {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                val date = sdf.parse(userProfile.subscription_end)
                                val outSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                "Активна до ${date?.let { outSdf.format(it) } ?: "Неизвестно"}"
                            } catch (e: Exception) {
                                "Активна до ${userProfile.subscription_end}"
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
                    title = if (isUploadingLogs) "Отправка логов..." else "Отправить логи разработчику",
                    onClick = {
                        if (!isUploadingLogs) {
                            isUploadingLogs = true
                            coroutineScope.launch {
                                val result = io.nekohasekai.sfa.network.AppLogCollector.uploadLogs(context)
                                isUploadingLogs = false
                                result.onSuccess { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message ?: "Ошибка отправки", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
                SettingTile(
                    icon = Icons.Rounded.Lock,
                    title = "Изменить пароль",
                    onClick = { showPasswordDialog = true }
                )
                SettingTile(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = "Выйти",
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Смена пароля") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Старый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Новый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Повторите новый пароль") },
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
                        errorText = "Новые пароли не совпадают"
                    } else if (newPassword.length < 6) {
                        errorText = "Пароль должен быть не менее 6 символов"
                    } else {
                        onChangePassword(oldPassword, newPassword)
                    }
                }
            ) {
                Text("Изменить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
