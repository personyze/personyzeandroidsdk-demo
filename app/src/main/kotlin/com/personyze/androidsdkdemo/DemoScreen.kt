package com.personyze.androidsdkdemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personyze.android.Personyze
import com.personyze.android.PersonyzeAction
import kotlinx.coroutines.launch

private val PROFILE_FIELDS = listOf("email", "first_name", "last_name", "phone", "custom")
private val PRODUCT_STATUSES = listOf("Viewed", "Added to cart", "Liked", "Purchased", "Unliked", "Removed from cart")
private val ARTICLE_STATUSES = listOf("Viewed", "Liked", "Commented", "Unliked", "Goal")
private val PROMOTION_STATUSES = listOf("Viewed", "Clicked", "Code revealed")

/**	The old DemoActivity: a document, the events of it, and what Personyze answers. Events are
	queued as they are added and go with "Navigate" (or by themselves a few seconds later).
 **/
@Composable
fun DemoScreen(onOpenAction: (PersonyzeAction) -> Unit)
{	val scope = rememberCoroutineScope()
	val result by Personyze.result.collectAsStateWithLifecycle()
	var document by rememberSaveable { mutableStateOf("Home") }
	var field by rememberSaveable { mutableStateOf(PROFILE_FIELDS[0]) }
	var customField by rememberSaveable { mutableStateOf("") }
	var fieldValue by rememberSaveable { mutableStateOf("") }
	var productId by rememberSaveable { mutableStateOf("") }
	var productStatus by rememberSaveable { mutableStateOf(PRODUCT_STATUSES[0]) }
	var actionId by rememberSaveable { mutableStateOf("") }
	var quantity by rememberSaveable { mutableStateOf("") }
	var articleId by rememberSaveable { mutableStateOf("") }
	var articleStatus by rememberSaveable { mutableStateOf(ARTICLE_STATUSES[0]) }
	var promotionId by rememberSaveable { mutableStateOf("") }
	var promotionStatus by rememberSaveable { mutableStateOf(PROMOTION_STATUSES[0]) }
	var coupon by rememberSaveable { mutableStateOf("") }
	var raw by rememberSaveable { mutableStateOf("") }
	var queued by remember { mutableStateOf(listOf<String>()) }
	var status by remember { mutableStateOf("") }

	fun queue(what: String, block: () -> Unit)
	{	block()
		queued = queued + what
	}

	fun run(what: String, block: suspend () -> String)
	{	status = "$what..."
		scope.launch {
			status = try
			{	block()
			}
			catch (e: Exception)
			{	"$what threw ${e.javaClass.simpleName}: ${e.message} - the SDK should never throw"
			}
		}
	}

	Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
		if (!Personyze.isInitialized)
		{	Text("Set the API key on the Setup tab first.", color = MaterialTheme.colorScheme.error)
		}
		Field("Document (screen)", document, Modifier.fillMaxWidth()) { document = it }
		Buttons(
			"Navigate" to {
				run("navigate") {
					val r = Personyze.navigate("androidsdk-test/$document")
					queued = emptyList()
					if (r.failed) "Failed: ${r.error?.message}" else "${r.actions.size} actions, ${r.conditions.size} campaigns"
				}
			},
			"Events only" to { run("flush") { Personyze.flush(); queued = emptyList(); "Sent" } },
			"Refresh" to { run("refresh") { val r = Personyze.refresh(); queued = emptyList(); r.error?.let { "Failed: ${it.message}" } ?: "${r.actions.size} actions" } }
		)
		Buttons(
			"New session" to { Personyze.startNewSession(); status = "The next request opens a new session" },
			"Clear cache" to { run("clearCache") { Personyze.clearCache(); "Templates reloaded" } },
			"Reset visitor" to { run("reset") { Personyze.reset(); "A new anonymous visitor" } }
		)
		Buttons("Check notification" to { run("checkNotifications") { Personyze.checkNotifications()?.let { "Shown: ${it.title}" } ?: "Nothing queued (or not allowed)" } })
		if (status.isNotEmpty())
		{	Text(status, Modifier.padding(top = 6.dp))
		}
		if (queued.isNotEmpty())
		{	Mono("Queued: " + queued.joinToString("; "), Modifier.padding(top = 4.dp))
		}

		Section("Profile")
		Row {
			Choice("Field", PROFILE_FIELDS, field, Modifier.weight(1f)) { field = it }
			Spacer(Modifier.width(6.dp))
			if (field == "custom")
			{	Field("Name", customField, Modifier.weight(1f)) { customField = it }
			}
		}
		Field("Value", fieldValue, Modifier.fillMaxWidth()) { fieldValue = it }
		Buttons("Add" to {
			val name = if (field == "custom") customField else field
			queue("profile $name") { Personyze.setProfile(name, fieldValue) }
		})

		Section("Product")
		Row {
			Field("Product id", productId, Modifier.weight(1f)) { productId = it }
			Spacer(Modifier.width(6.dp))
			Choice("Status", PRODUCT_STATUSES, productStatus, Modifier.weight(1f)) { productStatus = it }
		}
		Row {
			Field("Action id (attribution)", actionId, Modifier.weight(1f), number = true) { actionId = it }
			Spacer(Modifier.width(6.dp))
			Field("Quantity", quantity, Modifier.weight(1f), number = true) { quantity = it }
		}
		Buttons("Add" to {
			val a = actionId.toIntOrNull()
			val q = quantity.toIntOrNull()
			queue("product $productId $productStatus") {
				when (productStatus)
				{	"Viewed" -> Personyze.productViewed(productId, a)
					"Added to cart" -> Personyze.productAddedToCart(productId, a, q)
					"Liked" -> Personyze.productLiked(productId, a)
					"Purchased" -> Personyze.productPurchased(productId, a, q)
					"Unliked" -> Personyze.productUnliked(productId, a)
					else -> Personyze.productRemovedFromCart(productId, a, q)
				}
			}
		})
		Buttons(
			"Cart bought" to { queue("products purchased") { Personyze.productsPurchased() } },
			"Empty cart" to { queue("products removed") { Personyze.productsRemovedFromCart() } },
			"Unlike all" to { queue("products unliked") { Personyze.productsUnliked() } }
		)

		Section("Article")
		Row {
			Field("Article id", articleId, Modifier.weight(1f)) { articleId = it }
			Spacer(Modifier.width(6.dp))
			Choice("Status", ARTICLE_STATUSES, articleStatus, Modifier.weight(1f)) { articleStatus = it }
		}
		Buttons(
			"Add" to {
				val a = actionId.toIntOrNull()
				queue("article $articleId $articleStatus") {
					when (articleStatus)
					{	"Viewed" -> Personyze.articleViewed(articleId, a)
						"Liked" -> Personyze.articleLiked(articleId, a)
						"Commented" -> Personyze.articleCommented(articleId, a)
						"Unliked" -> Personyze.articleUnliked(articleId, a)
						else -> Personyze.articleGoal(articleId, a)
					}
				}
			},
			"Unlike all" to { queue("articles unliked") { Personyze.articlesUnliked() } }
		)

		Section("Promotions")
		Row {
			Field("Promotion id", promotionId, Modifier.weight(1f)) { promotionId = it }
			Spacer(Modifier.width(6.dp))
			Choice("Event", PROMOTION_STATUSES, promotionStatus, Modifier.weight(1f)) { promotionStatus = it }
		}
		Buttons("Add (uses the action id above)" to {
			val a = actionId.toIntOrNull() ?: 0
			queue("promotion $promotionId $promotionStatus") {
				when (promotionStatus)
				{	"Viewed" -> Personyze.promotionViewed(promotionId, a)
					"Clicked" -> Personyze.promotionClicked(promotionId, a)
					else -> Personyze.codeRevealed(promotionId, a)
				}
			}
		})
		Row {
			Field("Coupon code", coupon, Modifier.weight(1f)) { coupon = it }
			Spacer(Modifier.width(6.dp))
			Field("Raw command, comma separated", raw, Modifier.weight(1f)) { raw = it }
		}
		Buttons(
			"Coupon used" to { queue("coupon $coupon") { Personyze.couponUsed(coupon) } },
			"Track raw" to { queue("raw $raw") { Personyze.track(raw.split(',').map { it.trim() }) } }
		)

		Section("Campaigns (${result.conditions.size})")
		result.conditions.forEach { c -> Text("#${c.id}  ${c.name ?: "…"}") }
		Section("Actions (${result.actions.size}) - tap to open")
		result.actions.forEach { a ->
			Column(Modifier.fillMaxWidth().clickable { onOpenAction(a) }.padding(vertical = 6.dp)) {
				Text("#${a.id}  ${a.name ?: "…"}", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
				Mono("${a.contentType ?: "loading"}  placeholders: ${a.placeholders.joinToString { it.name }.ifEmpty { "-" }}  data: ${a.data.keys}")
			}
			HorizontalDivider()
		}
		result.error?.let { Text("Last decision failed: ${it.message}", color = MaterialTheme.colorScheme.error) }
	}
}
