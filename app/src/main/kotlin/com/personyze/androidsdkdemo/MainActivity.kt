package com.personyze.androidsdkdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personyze.android.PersonyzeAction

/**	One activity, five tabs. The old demo's three activities (key, simulate, action) are Setup,
	Demo and Action; Log and Checks are new.
 **/
class MainActivity : ComponentActivity()
{	override fun onCreate(savedInstanceState: Bundle?)
	{	enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		setContent {
			MaterialTheme {
				Surface(Modifier.fillMaxSize()) { App() }
			}
		}
	}
}

private val TABS = listOf("Setup", "Demo", "Action", "Log", "Checks")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App()
{	var tab by rememberSaveable { mutableIntStateOf(if (Settings.apiKey.isBlank()) 0 else 1) }
	var selected by remember { mutableStateOf<PersonyzeAction?>(null) }
	Scaffold(
		topBar = {
			Column {
				TopAppBar(title = { Text("Personyze SDK demo") })
				PrimaryTabRow(selectedTabIndex = tab) {
					TABS.forEachIndexed { i, title -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title, maxLines = 1) }) }
				}
			}
		}
	) { padding ->
		Box(Modifier.padding(padding).fillMaxSize()) {
			when (tab)
			{	0 -> SetupScreen()
				1 -> DemoScreen(onOpenAction = { selected = it; tab = 2 })
				2 -> ActionScreen(selected)
				3 -> LogScreen()
				else -> ChecksScreen()
			}
		}
	}
}

// ------------------------------------------------------------------------------------- widgets

@Composable
fun Section(title: String)
{	Spacer(Modifier.height(12.dp))
	Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
	Spacer(Modifier.height(4.dp))
}

@Composable
fun Field(label: String, value: String, modifier: Modifier = Modifier, number: Boolean = false, onChange: (String) -> Unit)
{	OutlinedTextField(
		value = value,
		onValueChange = onChange,
		label = { Text(label) },
		singleLine = true,
		keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
		modifier = modifier
	)
}

@Composable
fun Choice(label: String, options: List<String>, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit)
{	var open by remember { mutableStateOf(false) }
	Box(modifier) {
		OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $value", maxLines = 1) }
		DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
			options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); open = false }) }
		}
	}
}

@Composable
fun Buttons(vararg buttons: Pair<String, () -> Unit>)
{	Row(Modifier.fillMaxWidth()) {
		buttons.forEachIndexed { i, (title, action) ->
			if (i > 0)
			{	Spacer(Modifier.width(6.dp))
			}
			if (i == 0)
			{	Button(onClick = action, modifier = Modifier.weight(1f)) { Text(title, maxLines = 1, fontSize = 13.sp) }
			}
			else
			{	OutlinedButton(onClick = action, modifier = Modifier.weight(1f)) { Text(title, maxLines = 1, fontSize = 13.sp) }
			}
		}
	}
}

@Composable
fun Mono(text: String, modifier: Modifier = Modifier)
{	Text(text, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = modifier)
}

@Composable
fun LinkButton(title: String, onClick: () -> Unit)
{	TextButton(onClick = onClick) { Text(title) }
}
