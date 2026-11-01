package com.personyze.androidsdkdemo

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.personyze.android.Personyze

@Composable
fun SetupScreen()
{	val context = LocalContext.current
	var key by remember { mutableStateOf(Settings.apiKey) }
	var base by remember { mutableStateOf(Settings.baseUrl) }
	var timeout by remember { mutableStateOf(Settings.timeoutSec.toString()) }
	var notifications by remember { mutableStateOf(Settings.notifications) }
	var userId by remember { mutableStateOf(Settings.userId) }
	var status by remember { mutableStateOf(if (Personyze.isInitialized) "Initialized" else "Not initialized") }
	val visitor by Personyze.visitor.collectAsState()
	val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
		status = if (granted) "Notifications allowed" else "Notifications refused"
	}

	fun apply()
	{	Settings.apiKey = key
		Settings.baseUrl = base
		Settings.timeoutSec = timeout.toIntOrNull() ?: 5
		Settings.notifications = notifications
		Settings.userId = userId
		Settings.save(context)
		status = Settings.apply(context) ?: "Initialized"
		Push.start(context)
	}

	Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
		Text("Writes to the real account: use account 1, the test account. The key is the account's full-featured API key (Settings > Integrations).")
		Section("Account")
		Field("API key", key, Modifier.fillMaxWidth()) { key = it }
		Field("Base URL", base, Modifier.fillMaxWidth()) { base = it }
		Field("Timeout, seconds", timeout, Modifier.fillMaxWidth(), number = true) { timeout = it }
		Row(verticalAlignment = Alignment.CenterVertically) {
			Switch(checked = notifications, onCheckedChange = { notifications = it })
			Text("  Notifications (checked as the app opens, and in the background)")
		}
		Buttons(
			"Apply" to { apply() },
			"Ask permission" to {
				if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else status = "No runtime permission before Android 13"
			}
		)
		Text(status, Modifier.padding(top = 6.dp))
		val push by Push.state.collectAsState()
		Text("Push: $push", Modifier.padding(top = 6.dp))

		Section("Visitor")
		Field("Your user id (login)", userId, Modifier.fillMaxWidth()) { userId = it }
		Buttons(
			"Log in" to { Settings.userId = userId; Settings.save(context); Personyze.setUserId(userId.ifBlank { null }) },
			"Log out" to { userId = ""; Settings.userId = ""; Settings.save(context); Personyze.setUserId(null) }
		)
		val session = visitor.sessionId
		Mono(
			"user id:    ${visitor.userId ?: "(anonymous install visitor)"}\n" +
			"visitor id: ${visitor.visitorId}\n" +
			"session:    $session\n" +
			"  " + sessionFields(session),
			Modifier.padding(top = 6.dp)
		)
	}
}

/**	The session string's readable fields: 0 the start, 1 the visitor, 3 the counter (SPEC section 3). **/
fun sessionFields(session: String?): String
{	val f = session?.split(' ').orEmpty()
	return "start ${f.getOrNull(0)}, visitor ${f.getOrNull(1)}, counter ${f.getOrNull(3)}"
}
