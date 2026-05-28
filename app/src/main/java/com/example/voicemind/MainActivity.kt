package com.example.voicemind

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicemind.data.speech.SpeechRecognition
import com.example.voicemind.ui.screens.ConfirmReminderScreen
import com.example.voicemind.ui.screens.HomeScreen
import com.example.voicemind.ui.screens.ManualReminderScreen
import com.example.voicemind.ui.screens.ReminderDetailScreen
import com.example.voicemind.ui.screens.ReminderListScreen
import com.example.voicemind.ui.screens.SettingsScreen
import com.example.voicemind.ui.theme.VoiceMindTheme
import com.example.voicemind.util.ReminderPermissions
import com.example.voicemind.viewmodel.VoiceMindViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceMindTheme {
                VoiceMindApp()
            }
        }
    }
}

@Composable
fun VoiceMindApp(viewModel: VoiceMindViewModel = viewModel()) {
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val listeningState by viewModel.listeningState.collectAsState()
    val pendingConfirm by viewModel.pendingConfirm.collectAsState()
    val manualDraft by viewModel.manualDraft.collectAsState()
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val historyReminders by viewModel.historyReminders.collectAsState()
    val nextReminder by viewModel.nextReminder.collectAsState()
    val listTab by viewModel.listTab.collectAsState()
    val detailReminder by viewModel.detailReminder.collectAsState()
    val defaultDeliveryMode by viewModel.defaultDeliveryMode.collectAsState()
    val confirmBeforeSchedule by viewModel.confirmBeforeSchedule.collectAsState()
    val fallbackToSystemSpeech by viewModel.fallbackToSystemSpeech.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize(),
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
                        listeningState = listeningState,
                        nextReminder = nextReminder,
                        onVoiceClick = { startVoiceInput() },
                        onMicPermissionDenied = {
                            viewModel.showError(context.getString(R.string.error_mic_denied))
                        },
                        onManualCreateClick = { viewModel.openManualCreate() },
                    )
                    AppDestinations.LIST -> ReminderListScreen(
                        selectedTab = listTab,
                        onTabSelected = { viewModel.setListTab(it) },
                        upcomingReminders = upcomingReminders,
                        historyReminders = historyReminders,
                        onUpcomingClick = { viewModel.openReminderForEdit(it) },
                        onHistoryClick = { viewModel.openReminderDetail(it) },
                        onCancel = { viewModel.cancelReminder(it) },
                    )
                    AppDestinations.SETTINGS -> SettingsScreen(
                        defaultDeliveryMode = defaultDeliveryMode,
                        confirmBeforeSchedule = confirmBeforeSchedule,
                        onDefaultDeliveryModeChange = { viewModel.setDefaultDeliveryMode(it) },
                        onConfirmBeforeScheduleChange = { viewModel.setConfirmBeforeSchedule(it) },
                    )
                }
            }
        }

        pendingConfirm?.let { pending ->
            ConfirmReminderScreen(
                pending = pending,
                onBack = { viewModel.dismissConfirm() },
                onSave = { body, fireAt, mode ->
                    viewModel.updatePending(body, fireAt, mode)
                },
                onConfirm = { viewModel.confirmVoiceReminder() },
            )
        }

        manualDraft?.let { draft ->
            ManualReminderScreen(
                draft = draft,
                onBack = { viewModel.dismissManual() },
                onSave = { body, fireAt, mode ->
                    viewModel.saveManualReminder(body, fireAt, mode)
                },
            )
        }

        detailReminder?.let { reminder ->
            ReminderDetailScreen(
                reminder = reminder,
                onBack = { viewModel.dismissDetail() },
            )
        }
    }
}
