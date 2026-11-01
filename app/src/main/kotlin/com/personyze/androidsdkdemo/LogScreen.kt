package com.personyze.androidsdkdemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personyze.android.Personyze
import com.personyze.android.PersonyzeDiagnostics

/**	What the SDK sent and got, what it reported to onError, and its stored state. **/
@OptIn(PersonyzeDiagnostics::class)
@Composable
fun LogScreen()
{	val wire by Logs.wire.collectAsState()
	val errors by Logs.errors.collectAsState()
	val result by Personyze.result.collectAsState()
	var state by remember { mutableStateOf(if (Personyze.isInitialized) Personyze.client.diagnostics() else "") }
	var open by remember { mutableStateOf(setOf<Int>()) }

	Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
		Buttons(
			"Refresh state" to { state = if (Personyze.isInitialized) Personyze.client.diagnostics() else "Not initialized" },
			"Clear logs" to { Logs.clear() }
		)
		Section("Stored state")
		Mono(state)
		Section("onError (${errors.size})")
		errors.forEach { Text("${it.time}  ${it.title}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 2.dp)) }
		Section("Requests (${wire.size}) - tap for the bodies")
		wire.forEachIndexed { i, e ->
			Column(Modifier.fillMaxWidth().clickable { open = if (i in open) open - i else open + i }.padding(vertical = 4.dp)) {
				Mono("${e.time}  ${e.title}")
				if (i in open)
				{	Mono(e.detail)
				}
			}
			HorizontalDivider()
		}
		Section("Last response")
		Mono(result.rawJson ?: "-")
	}
}
