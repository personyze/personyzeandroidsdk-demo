package com.personyze.androidsdkdemo

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.personyze.android.Personyze
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**	Push, as an app wires it (the SDK's README, "Pushed at once"): the app uses Firebase Cloud
	Messaging itself, and passes on the device's token and the messages. With the account's Firebase
	service account in the Personyze panel (Integrations > App push), a campaign's notification then
	arrives at once, the app closed.

	The REGISTRATION TOKEN, which Personyze sends to: firebase-messaging 26 marks `onNewToken` and
	`getToken()` deprecated in favour of `register()`, which identifies the device by its Firebase
	installation id instead - a different address. The token calls still work, hence the suppressions.
 **/
class PushService : FirebaseMessagingService()
{	@Suppress("OVERRIDE_DEPRECATION")
	override fun onNewToken(token: String)
	{	Push.got(token)
	}

	override fun onMessageReceived(message: RemoteMessage)
	{	if (Personyze.handlePushMessage(message.data))
		{	return
		}
		// An app's own messages would be handled here.
	}
}

/**	The device's token, for the Setup screen; and asking Firebase for it once the SDK is initialized. **/
object Push
{	private val stateFlow = MutableStateFlow("Not asked yet")
	val state: StateFlow<String> = stateFlow

	/**	Once at start, and again when the Setup screen re-initializes the SDK: the token may have come
		before there was a client to give it to.
	 **/
	fun start(context: Context)
	{	if (FirebaseApp.getApps(context).isEmpty())
		{	stateFlow.value = "Off: no app/google-services.json (README, \"Push\")"
			return
		}
		@Suppress("DEPRECATION")
		FirebaseMessaging.getInstance().token
			.addOnSuccessListener { got(it) }
			.addOnFailureListener { stateFlow.value = "Firebase gave no token: ${it.message}" }
	}

	fun got(token: String)
	{	stateFlow.value = "Token ${token.take(12)}… passed on"
		Personyze.setPushToken(token)
	}
}
