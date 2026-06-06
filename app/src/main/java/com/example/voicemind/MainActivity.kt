package com.example.voicemind

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicemind.data.speech.SpeechRecognition
import com.example.voicemind.ui.screens.ConfirmReminderScreen
import com.example.voicemind.ui.screens.HomeScreen
import com.example.voicemind.ui.screens.ManualReminderScreen
import com.example.voicemind.ui.screens.ReminderDetailScreen
import com.example.voicemind.ui.screens.ReminderListScreen
import com.example.voicemind.ui.screens.SettingsScreen
import com.example.voicemind.ui.theme.VoiceMindTheme
import com.example.voicemind.ui.widget.WidgetUpdater
import com.example.voicemind.util.ReminderPermissions
import com.example.voicemind.viewmodel.VoiceMindViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: VoiceMindViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceMindTheme {
                VoiceMindApp(viewModel = viewModel)
            }
        }
        viewModel.handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            WidgetUpdater.updateAll(applicationContext)
        }
    }
}

@Composable
fun VoiceMindApp(viewModel: VoiceMindViewModel = viewModel()) {
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.LIST) }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val pendingConfirm by viewModel.pendingConfirm.collectAsState()
    val manualDraft by viewModel.manualDraft.collectAsState()
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val historyReminders by viewModel.historyReminders.collectAsState()
    val listTab by viewModel.listTab.collectAsState()
    val detailReminder by viewModel.detailReminder.collectAsState()
    val confirmBeforeSchedule by viewModel.confirmBeforeSchedule.collectAsState()
    val useAlarmSound by viewModel.useAlarmSound.collectAsState()
    val usePushNotification by viewModel.usePushNotification.collectAsState()
    val useVibration by viewModel.useVibration.collectAsState()
    val alarmRingtoneUri by viewModel.alarmRingtoneUri.collectAsState()
    val alarmVolume by viewModel.alarmVolume.collectAsState()
    val dismissBehavior by viewModel.dismissBehavior.collectAsState()
    val fallbackToSystemSpeech by viewModel.fallbackToSystemSpeech.collectAsState()

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val isDefault = uri == null || uri == defaultUri
            viewModel.setAlarmRingtoneUri(if (isDefault) null else uri.toString())
        }
    }

    fun launchRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.settings_alarm_ringtone))
            alarmRingtoneUri?.let { uri ->
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(uri))
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val fullScreenIntentPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val text = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                if (text.isNullOrBlank()) {
                    viewModel.onSpeechSessionCancelled()
                } else {
                    viewModel.onSpeechResult(text)
                }
            }
            else -> viewModel.onSpeechSessionCancelled()
        }
    }

    fun startVoiceInput() {
        // Prefer the system speech dialog — it works on every OEM (Huawei, Samsung, etc.).
        val intent = SpeechRecognition.recognizerIntent().apply {
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.home_listening))
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            viewModel.beginExternalVoiceSession()
            systemSpeechLauncher.launch(intent)
            return
        }
        // Fallback to inline SpeechRecognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            viewModel.startListening()
        } else {
            viewModel.showError(context.getString(R.string.error_speech_unavailable))
        }
    }

    LaunchedEffect(Unit) {
        if (ReminderPermissions.needsPostNotificationsPermission() &&
            !ReminderPermissions.hasPostNotifications(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(fallbackToSystemSpeech) {
        if (fallbackToSystemSpeech) {
            viewModel.consumeFallbackToSystemSpeech()
            val intent = SpeechRecognition.recognizerIntent().apply {
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.home_listening))
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                viewModel.beginExternalVoiceSession()
                systemSpeechLauncher.launch(intent)
            } else {
                viewModel.showError(context.getString(R.string.error_speech_unavailable))
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val activity = context as? Activity

    BackHandler {
        when {
            detailReminder != null -> viewModel.dismissDetail()
            manualDraft != null -> viewModel.dismissManual()
            pendingConfirm != null -> viewModel.dismissConfirm()
            currentDestination == AppDestinations.SETTINGS -> currentDestination = AppDestinations.HOME
            currentDestination == AppDestinations.LIST -> currentDestination = AppDestinations.HOME
            else -> activity?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 80.dp),
                ) { data ->
                    androidx.compose.material3.Snackbar(
                        snackbarData = data,
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionColor = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        ) { innerPadding ->
            NavigationSuiteScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                navigationSuiteItems = {
                    AppDestinations.entries.forEach { destination ->
                        item(
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            selected = destination == currentDestination,
                            onClick = { currentDestination = destination },
                        )
                    }
                },
            ) {
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(
                        nextReminder = upcomingReminders.firstOrNull(),
                        upcomingReminders = upcomingReminders,
                        onMicClick = { startVoiceInput() },
                        onManualCreateClick = { viewModel.openManualCreate() },
                        onNextReminderClick = {
                            upcomingReminders.firstOrNull()?.let {
                                viewModel.openReminderForEdit(it.id)
                            }
                        },
                        onViewAllClick = { currentDestination = AppDestinations.LIST },
                        onUpcomingClick = { viewModel.openReminderForEdit(it) },
                    )
                    AppDestinations.LIST -> ReminderListScreen(
                        selectedTab = listTab,
                        onTabSelected = { viewModel.setListTab(it) },
                        upcomingReminders = upcomingReminders,
                        historyReminders = historyReminders,
                        onUpcomingClick = { viewModel.openReminderForEdit(it) },
                        onHistoryClick = { viewModel.openReminderDetail(it) },
                        onCancel = { viewModel.cancelReminder(it) },
                        onComplete = { viewModel.completeReminder(it) },
                        onVoiceClick = { startVoiceInput() },
                        onMicPermissionDenied = {
                            viewModel.showError(context.getString(R.string.error_mic_denied))
                        },
                        onManualCreateClick = { viewModel.openManualCreate() },
                    )
                    AppDestinations.SETTINGS -> SettingsScreen(
                        confirmBeforeSchedule = confirmBeforeSchedule,
                        useAlarmSound = useAlarmSound,
                        usePushNotification = usePushNotification,
                        useVibration = useVibration,
                        alarmRingtoneUri = alarmRingtoneUri,
                        alarmVolume = alarmVolume,
                        dismissBehavior = dismissBehavior,
                        onConfirmBeforeScheduleChange = { viewModel.setConfirmBeforeSchedule(it) },
                        onAlarmVolumeChange = { viewModel.setAlarmVolume(it) },
                        onUseAlarmSoundChange = { viewModel.setUseAlarmSound(it) },
                        onUsePushNotificationChange = { viewModel.setUsePushNotification(it) },
                        onUseVibrationChange = { viewModel.setUseVibration(it) },
                        onSelectRingtone = { launchRingtonePicker() },
                        onRequestNotificationPermission = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onRequestFullScreenIntentPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                fullScreenIntentPermissionLauncher.launch(Manifest.permission.USE_FULL_SCREEN_INTENT)
                            }
                        },
                        onDismissBehaviorChange = { viewModel.setDismissBehavior(it) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = pendingConfirm != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            pendingConfirm?.let { pending ->
                ConfirmReminderScreen(
                    pending = pending,
                    onBack = { viewModel.dismissConfirm() },
                    onSave = { body, fireAt ->
                        viewModel.updatePending(body, fireAt)
                    },
                    onConfirm = { viewModel.confirmVoiceReminder() },
                )
            }
        }

        AnimatedVisibility(
            visible = manualDraft != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            manualDraft?.let { draft ->
                ManualReminderScreen(
                    draft = draft,
                    onBack = { viewModel.dismissManual() },
                    onSave = { body, fireAt ->
                        viewModel.saveManualReminder(body, fireAt)
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = detailReminder != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            detailReminder?.let { reminder ->
                ReminderDetailScreen(
                    reminder = reminder,
                    onBack = { viewModel.dismissDetail() },
                    onEdit = { viewModel.openReminderForEdit(reminder.id) },
                    onDelete = { viewModel.deleteReminder(reminder.id) },
                    onCancel = { viewModel.cancelReminder(reminder.id) },
                    onComplete = { viewModel.completeReminder(reminder.id) },
                    onSnooze = { minutes -> viewModel.snoozeReminder(reminder.id, minutes) },
                    onDuplicate = { viewModel.duplicateReminder(reminder) },
                )
            }
        }

    }
}
