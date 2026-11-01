package com.personyze.androidsdkdemo

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.personyze.android.Personyze
import com.personyze.android.PersonyzeException
import com.personyze.android.PersonyzeResult
import kotlinx.coroutines.launch
import java.util.zip.CRC32
import kotlin.random.Random

private class Check(val name: String, val ok: Boolean?, val detail: String)

/**	SPEC section 9 on the device, through the public API, in storages of their own (the app's own
	visitor is left alone; its client is initialized again afterwards). Step 4 needs the SDK's
	internal store and runs in the JVM test (ConformanceLiveTest).
 **/
private suspend fun runChecks(context: Context): List<Check>
{	val out = ArrayList<Check>()
	val base = 777_100_000 + Random.nextInt(0, 9_000_000)
	fun visitorOf(r: PersonyzeResult): Long? = r.sessionId?.split(' ')?.getOrNull(1)?.toLongOrNull()
	try
	{	val c = Personyze.initialize(context, Settings.config("conformance"))
		val a = base + 1L
		c.setUserId(a)
		val r1 = c.navigate("androidsdk-demo/checks/home")
		out.add(Check("1. a session comes back", r1.sessionId != null && r1.error == null, r1.sessionId ?: r1.error?.message ?: ""))
		val r2 = c.navigate("androidsdk-demo/checks/product")
		out.add(Check("2. the visitor persists in it", visitorOf(r2) == a, "field 1 = ${visitorOf(r2)}, expected $a"))
		val errorsBefore = Logs.errors.value.size
		r2.actions.firstOrNull()?.let { c.reportExecuted(it.id) } ?: c.setProfile("checks", "android")
		c.flush()
		out.add(Check("3. a report goes through", Logs.errors.value.size == errorsBefore, if (r2.actions.isEmpty()) "no action decided; sent a profile event instead" else "executed for #${r2.actions[0].id}"))
		out.add(Check("4. a corrupt session recovers", null, "in the JVM test only"))
		val counters = (1..3).map { c.navigate("androidsdk-demo/checks/page$it").sessionId?.split(' ')?.getOrNull(3) }
		out.add(Check("7. three decisions keep one session", counters[0] != null && counters.toSet().size == 1, "session counters $counters"))
		val b = "androidsdk-demo-$base@example.com"
		c.setUserId(b)
		val rb = c.navigate("androidsdk-demo/checks/home")
		val crc = CRC32().apply { update(b.toByteArray(Charsets.UTF_8)) }.value.toInt().toLong()
		out.add(Check("8. a visitor switch is the new visitor", visitorOf(rb) == crc, "field 1 = ${visitorOf(rb)}, expected $crc"))

		val bad = Personyze.initialize(context, Settings.config("conformance-bad", key = "0".repeat(40)))
		val r5 = bad.navigate("androidsdk-demo/checks/home")
		out.add(Check("5. a bad key is the auth error", r5.error is PersonyzeException.Auth, r5.error?.javaClass?.simpleName ?: "no error"))
		val down = Personyze.initialize(context, Settings.config("conformance-down", base = "https://127.0.0.1:9"))
		val r6 = down.navigate("androidsdk-demo/checks/home")
		out.add(Check("6. an unreachable host fails open", r6.isEmpty && r6.error is PersonyzeException.Transport, r6.error?.message ?: "no error"))
	}
	catch (e: Exception)
	{	out.add(Check("threw", false, "${e.javaClass.simpleName}: ${e.message} - the SDK should never throw"))
	}
	finally
	{	Settings.apply(context)
	}
	return out
}

@Composable
fun ChecksScreen()
{	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	var checks by remember { mutableStateOf(listOf<Check>()) }
	var running by remember { mutableStateOf(false) }
	var conditionId by remember { mutableStateOf("") }
	var verdict by remember { mutableStateOf("") }

	Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
		Text("The shared conformance steps (SPEC section 9) against the real endpoint. Writes to the account.")
		Buttons((if (running) "Running..." else "Run the checks") to {
			if (!running && Settings.apiKey.isNotBlank())
			{	running = true
				scope.launch {
					checks = runChecks(context)
					running = false
				}
			}
		})
		checks.forEach { c ->
			val color = when (c.ok)
			{	true -> Color(0xFF10B981)
				false -> MaterialTheme.colorScheme.error
				null -> MaterialTheme.colorScheme.outline
			}
			Text((when (c.ok) { true -> "PASS  "; false -> "FAIL  "; null -> "SKIP  " }) + c.name, color = color, modifier = Modifier.padding(top = 6.dp))
			Mono(c.detail)
		}

		Section("A/B verdict")
		Field("Campaign (condition) id", conditionId, Modifier.fillMaxWidth(), number = true) { conditionId = it }
		Buttons("Ask" to {
			val id = conditionId.toIntOrNull() ?: return@to
			scope.launch {
				verdict = Personyze.verdict(id).fold(
					onSuccess = { v -> "has winner: ${v.hasWinner} (${v.winnerActiongroupId})\n${v.message}\n" + v.goals.joinToString("\n") { "${it.title}: ${it.summary} reached=${it.reached}" } },
					onFailure = { e -> "Failed: ${e.message}" }
				)
			}
		})
		Mono(verdict)
	}
}
