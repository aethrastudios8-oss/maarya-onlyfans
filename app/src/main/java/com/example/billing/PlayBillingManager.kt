package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayBillingState(
    val isConnected: Boolean = false,
    val productsById: Map<String, ProductDetails> = emptyMap(),
    val statusMessage: String = "Google Play Billing ready (configure products in Play Console to enable in-app checkout)."
)

/**
 * Google Play Billing Library manager for Play Store distribution.
 * Never bypasses Google Play billing policies and never grants unverified/fake purchases.
 * Real subscription activation occurs only after server-side Google Play Developer API verification.
 */
class PlayBillingManager(
    context: Context,
    private val onVerifiedPurchasePendingBackend: (Purchase) -> Unit = {}
) : PurchasesUpdatedListener {

    private val _state = MutableStateFlow(PlayBillingState())
    val state: StateFlow<PlayBillingState> = _state.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            querySubscriptionProducts()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(
                        isConnected = true,
                        statusMessage = "Connected to Google Play Store Billing."
                    )
                    querySubscriptionProducts()
                    queryActivePurchases()
                } else {
                    Log.w(TAG, "Play Billing setup unavailable: ${billingResult.debugMessage}")
                    _state.value = _state.value.copy(
                        isConnected = false,
                        statusMessage = "Play Store Billing unavailable in preview/sideload mode. Configure Play Console SKUs before release."
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = _state.value.copy(isConnected = false)
            }
        })
    }

    private fun querySubscriptionProducts() {
        val productList = SUBSCRIPTION_PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                _state.value = _state.value.copy(
                    productsById = map,
                    statusMessage = if (map.isEmpty()) {
                        "Play Billing connected. Publish SKUs (maarya_tier_50..500) in Play Console to activate."
                    } else {
                        "Google Play Billing active for ${map.size} tier(s)."
                    }
                )
            }
        }
    }

    fun queryActivePurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    fun launchSubscriptionFlow(activity: Activity, productId: String): String? {
        if (!billingClient.isReady) {
            return "Google Play Billing service is not connected. Install via Play Store testing track."
        }
        val details = _state.value.productsById[productId]
            ?: return "Play Console product \"$productId\" is not published yet. Real purchases are disabled until Play Console setup."
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return "No active Play Console base plan offer found for \"$productId\"."

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) null else result.debugMessage
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled Play Billing flow.")
            }
            else -> {
                Log.w(TAG, "Play Billing error ${billingResult.responseCode}: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            // Pending transactions remain locked until Play settles them to PURCHASED
            return
        }
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { ackResult ->
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onVerifiedPurchasePendingBackend(purchase)
                }
            }
        } else {
            onVerifiedPurchasePendingBackend(purchase)
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    companion object {
        private const val TAG = "PlayBilling"
        val SUBSCRIPTION_PRODUCT_IDS = listOf(
            "maarya_tier_50",
            "maarya_tier_100",
            "maarya_tier_200",
            "maarya_tier_500"
        )
    }
}
