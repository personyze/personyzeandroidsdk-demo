package com.personyze.androidsdkdemo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.personyze.android.PersonyzeAction
import com.personyze.android.PersonyzeActionView
import com.personyze.android.PersonyzeClick
import com.personyze.android.PersonyzeClickListener
import com.personyze.android.compose.PersonyzeActionContent
import java.io.File

/**	The old ActionActivity: one action rendered, and what the visitor does in it. **/
@Composable
fun ActionScreen(action: PersonyzeAction?)
{	val context = LocalContext.current
	var useCompose by remember { mutableStateOf(true) }
	var lastClick by remember { mutableStateOf("") }
	var note by remember { mutableStateOf("") }
	var productArg by remember { mutableStateOf("") }

	Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
		if (action == null)
		{	Text("Open an action from the Demo tab.")
			return@Column
		}
		Text("#${action.id}  ${action.name ?: "…"}", style = MaterialTheme.typography.titleMedium)
		Mono("${action.contentType}  loaded: ${action.isLoaded}  libs: ${action.libs}  placeholders: ${action.placeholders}")

		if (action.isHtml)
		{	Row(verticalAlignment = Alignment.CenterVertically) {
				Switch(checked = useCompose, onCheckedChange = { useCompose = it })
				Text(if (useCompose) "  Compose (PersonyzeActionContent)" else "  View (PersonyzeActionView)")
			}
			val onClick = { click: PersonyzeClick ->
				lastClick = "${click.kind}  arg=${click.arg}  href=${click.href}"
				false	// let the view do the default: open the link, hide on close
			}
			val box = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)
			if (useCompose)
			{	PersonyzeActionContent(action, box, onClick = onClick)
			}
			else
			{	var view by remember { mutableStateOf<PersonyzeActionView?>(null) }
				AndroidView(
					factory = { PersonyzeActionView(it).also { v -> view = v } },
					modifier = box,
					update = { v ->
						v.onClick = PersonyzeClickListener { onClick(it) }
						v.render(action)
					}
				)
				DisposableEffect(action.id) { onDispose { view?.destroy() } }
			}
			Text("Last tap: ${lastClick.ifEmpty { "-" }}", Modifier.padding(top = 6.dp))
			Text("Taps are reported by the view; executed when it has loaded.", style = MaterialTheme.typography.bodySmall)
		}

		Section("Report it yourself")
		Buttons(
			"Executed" to { action.reportExecuted(); note = "executed queued (once per page)" },
			"Engaged" to { action.reportEngaged(); note = "target queued" },
			"Closed" to { action.reportClosed(); note = "close queued" }
		)
		Buttons(
			"Close, 3 sessions" to { action.reportClosed(3); note = "blocked for 3 sessions" },
			"Error" to { action.reportError("Shown by the test app's error button"); note = "error queued" }
		)
		Row {
			Field("Product id", productArg, Modifier.weight(1f)) { productArg = it }
		}
		Buttons("Product clicked" to { action.reportProductClicked(productArg); note = "product $productArg queued" })
		if (note.isNotEmpty())
		{	Text(note)
		}

		action.contentJsonArray()?.let { rows ->
			Section("JSON rows (${rows.size})")
			rows.forEach { Mono(it.toString(), Modifier.padding(vertical = 2.dp)) }
		}
		Section("Content")
		Mono(action.content ?: "(template not loaded yet)")
		action.contentHtmlDocument()?.let { html ->
			Buttons("Save the HTML page" to {
				val file = File(context.cacheDir, "action-${action.id}.html")
				file.writeText(html)
				note = "Saved ${file.absolutePath}"
			})
		}
	}
}
