package com.personyze.androidsdkdemo

import android.app.Application
import android.content.Context
import com.personyze.android.Personyze
import com.personyze.android.PersonyzeConfig
import com.personyze.android.PersonyzeErrorListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Buffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**	Initializes Personyze in `Application.onCreate`, as the SDK asks: its background workers
	(queued events, the notification check) find the key only in a process the app initialized -
	and so does a push that wakes the app. Then the device's push token, when Firebase is set up.
 **/
class DemoApp : Application()
{	override fun onCreate()
	{	super.onCreate()
		Settings.load(this)
		Settings.apply(this)
		Push.start(this)
	}
}

/**	What the Setup screen edits, kept in SharedPreferences (the test app's own; the SDK never
	stores the key).
 **/
object Settings
{	var apiKey = ""
	var baseUrl = PersonyzeConfig.DEFAULT_BASE_URL
	var timeoutSec = 5
	var notifications = false
	var userId = ""

	private const val PREFS = "androidsdk-demo"

	fun load(context: Context)
	{	val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
		apiKey = p.getString("apiKey", "") ?: ""
		baseUrl = p.getString("baseUrl", PersonyzeConfig.DEFAULT_BASE_URL) ?: PersonyzeConfig.DEFAULT_BASE_URL
		timeoutSec = p.getInt("timeoutSec", 5)
		notifications = p.getBoolean("notifications", false)
		userId = p.getString("userId", "") ?: ""
	}

	fun save(context: Context)
	{	context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
			.putString("apiKey", apiKey)
			.putString("baseUrl", baseUrl)
			.putInt("timeoutSec", timeoutSec)
			.putBoolean("notifications", notifications)
			.putString("userId", userId)
			.apply()
	}

	/**	(Re)initialize the SDK from these settings. Answers an error message, or null. **/
	fun apply(context: Context, storageName: String = "default"): String?
	{	if (apiKey.isBlank())
		{	return "No API key yet"
		}
		return try
		{	val client = Personyze.initialize(context, config(storageName))
			client.setUserId(userId.ifBlank { null })
			null
		}
		catch (e: IllegalArgumentException)
		{	e.message
		}
	}

	fun config(storageName: String = "default", key: String = apiKey, base: String = baseUrl): PersonyzeConfig =
		PersonyzeConfig(key.trim()) {
			baseUrl = base.trim()
			timeout = timeoutSec.coerceIn(1, 60).seconds
			notifications = this@Settings.notifications
			this.storageName = storageName
			okHttpClient = Logs.http
			onError = PersonyzeErrorListener { error, context -> Logs.error("${context.operation}: ${error.javaClass.simpleName}: ${error.message}" + (if (context.dropped > 0) " (dropped ${context.dropped})" else "") + (if (context.deferred > 0) " (kept ${context.deferred})" else "")) }
		}
}

/**	What went over the wire, and what the SDK reported to `onError` - without the API key. **/
object Logs
{	class Entry(val time: String, val title: String, val detail: String)

	private val wireFlow = MutableStateFlow<List<Entry>>(emptyList())
	private val errorFlow = MutableStateFlow<List<Entry>>(emptyList())
	val wire: StateFlow<List<Entry>> = wireFlow
	val errors: StateFlow<List<Entry>> = errorFlow

	private fun now(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

	fun error(message: String)
	{	errorFlow.update { (listOf(Entry(now(), message, "")) + it).take(200) }
	}

	fun clear()
	{	wireFlow.value = emptyList()
		errorFlow.value = emptyList()
	}

	/**	An app-supplied client: the SDK derives its own from it, so this interceptor sees its calls.
		It records bodies, never the Authorization header.
	 **/
	val http: OkHttpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain -> record(chain) }).build()

	private fun record(chain: Interceptor.Chain): Response
	{	val request = chain.request()
		val body = request.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() } ?: ""
		val started = System.currentTimeMillis()
		val title = request.method + " " + request.url.encodedPath.removePrefix("/rest/") + (request.url.encodedQuery?.let { "?$it" } ?: "")
		return try
		{	val response = chain.proceed(request)
			val text = response.peekBody(16_384).string()
			wireFlow.update { (listOf(Entry(now(), "$title -> ${response.code} (${System.currentTimeMillis() - started} ms)", "> $body\n< $text")) + it).take(100) }
			response
		}
		catch (e: Exception)
		{	wireFlow.update { (listOf(Entry(now(), "$title -> ${e.javaClass.simpleName}", "> $body\n! ${e.message}")) + it).take(100) }
			throw e
		}
	}
}
