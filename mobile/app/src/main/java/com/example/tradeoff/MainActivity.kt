package com.example.tradeoff

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.tradeoff.model.AuthRequest
import com.example.tradeoff.model.ChatMessage
import com.example.tradeoff.model.ChatInboxThread
import com.example.tradeoff.model.Item
import com.example.tradeoff.model.LoginRequest
import com.example.tradeoff.model.NotificationItem
import com.example.tradeoff.model.SendMessageRequest
import com.example.tradeoff.model.UserSummary
import com.example.tradeoff.network.RetrofitClient
import com.example.tradeoff.ui.ChatInboxAdapter
import com.example.tradeoff.ui.ChatMessageAdapter
import com.example.tradeoff.ui.ItemListAdapter
import com.example.tradeoff.ui.NotificationAdapter
import com.example.tradeoff.utils.PriceFormatter
import com.example.tradeoff.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private data class InboxSnapshot(
        val threads: List<ChatInboxThread>,
        val notifications: List<NotificationItem>,
        val unreadChatCount: Int,
        val unreadBellCount: Int
    )

    private enum class DashboardTab {
        MARKETPLACE,
        MY_LISTINGS,
        PROFILE,
        SETTINGS
    }

    private data class ChatContact(
        val email: String,
        val displayName: String,
        val channel: String? = null,
        val transactionId: Long? = null
    )

    private lateinit var sessionManager: SessionManager
    private lateinit var landingAdapter: ItemListAdapter
    private lateinit var dashboardAdapter: ItemListAdapter

    private val allItems = mutableListOf<Item>()
    private val allUsers = mutableListOf<UserSummary>()
    private val notificationItems = mutableListOf<NotificationItem>()
    private var isLoadingItems = false
    private var isLoadingUsers = false
    private var isRefreshingInboxMetadata = false
    private var unreadChatBadgeCount = 0
    private var currentTab = DashboardTab.MARKETPLACE
    private var imagePickerCallback: ((Uri?) -> Unit)? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            imagePickerCallback?.invoke(uri)
            imagePickerCallback = null
        }

    private val chatStatePrefs by lazy {
        getSharedPreferences("CHAT_STATE", MODE_PRIVATE)
    }

    private lateinit var layoutLanding: LinearLayout
    private lateinit var btnLandingLogin: Button
    private lateinit var btnLandingRegister: Button
    private lateinit var etLandingSearch: EditText
    private lateinit var rvLandingItems: RecyclerView
    private lateinit var tvLandingStatus: TextView
    private lateinit var progressLanding: ProgressBar

    private lateinit var layoutLogin: ScrollView
    private lateinit var etLoginEmail: EditText
    private lateinit var etLoginPassword: EditText
    private lateinit var btnLoginSubmit: Button
    private lateinit var btnLoginGoRegister: Button
    private lateinit var btnLoginBack: Button

    private lateinit var layoutRegister: ScrollView
    private lateinit var etRegisterFullName: EditText
    private lateinit var etRegisterDisplayName: EditText
    private lateinit var etRegisterEmail: EditText
    private lateinit var etRegisterPassword: EditText
    private lateinit var btnRegisterSubmit: Button
    private lateinit var btnRegisterBack: Button

    private lateinit var layoutDashboard: NestedScrollView
    private lateinit var btnDashboardLogout: Button
    private lateinit var etDashboardSearch: EditText
    private lateinit var rvDashboardItems: RecyclerView
    private lateinit var tvDashboardWelcome: TextView
    private lateinit var tvDashboardStatus: TextView
    private lateinit var progressDashboard: ProgressBar
    private lateinit var layoutDashboardHero: LinearLayout

    private lateinit var btnTabMarketplace: Button
    private lateinit var btnTabMyListings: Button
    private lateinit var btnTabProfile: Button
    private lateinit var btnTabSettings: Button
    private lateinit var spDashboardCategory: Spinner
    private lateinit var spDashboardSort: Spinner
    private lateinit var btnDashboardSell: Button
    private lateinit var btnDashboardRefresh: Button
    private lateinit var btnDashboardChat: Button
    private lateinit var layoutDashboardChatFab: FrameLayout
    private lateinit var tvDashboardChatBadge: TextView
    private lateinit var btnDashboardNotifications: ImageButton
    private lateinit var tvDashboardNotifBadge: TextView
    private lateinit var layoutDashboardBottomNav: LinearLayout
    private lateinit var layoutDashboardFilters: LinearLayout
    private lateinit var layoutDashboardListHeader: LinearLayout

    private lateinit var layoutDashboardProfile: LinearLayout
    private lateinit var layoutDashboardSettings: LinearLayout
    private lateinit var viewDashboardNotifScrim: View
    private lateinit var layoutDashboardNotificationsPanel: LinearLayout
    private lateinit var rvDashboardNotifications: RecyclerView
    private lateinit var tvDashboardNotificationsEmpty: TextView
    private lateinit var btnDashboardNotificationsClose: Button
    private lateinit var btnDashboardNotificationsClearAll: Button

    private lateinit var tvProfileDisplayName: TextView
    private lateinit var tvProfileFullName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileListingCount: TextView
    private lateinit var dashboardNotificationAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)
        bindViews()
        setupRecyclerViews()
        setupDashboardSpinners()
        setupClickListeners()
        setupSearchInputs()

        if (sessionManager.isLoggedIn()) {
            showDashboard()
        } else {
            showLanding()
        }
        refreshUsersInBackground()
        loadListings(showLoading = true)
        refreshInboxMetadataInBackground()
    }

    private fun bindViews() {
        layoutLanding = findViewById(R.id.layoutLanding)
        btnLandingLogin = findViewById(R.id.btnLandingLogin)
        btnLandingRegister = findViewById(R.id.btnLandingRegister)
        etLandingSearch = findViewById(R.id.etLandingSearch)
        rvLandingItems = findViewById(R.id.rvLandingItems)
        tvLandingStatus = findViewById(R.id.tvLandingStatus)
        progressLanding = findViewById(R.id.progressLanding)
        layoutLogin = findViewById(R.id.layoutLogin)
        etLoginEmail = findViewById(R.id.etLoginEmail)
        etLoginPassword = findViewById(R.id.etLoginPassword)
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit)
        btnLoginGoRegister = findViewById(R.id.btnLoginGoRegister)
        btnLoginBack = findViewById(R.id.btnLoginBack)
        layoutRegister = findViewById(R.id.layoutRegister)
        etRegisterFullName = findViewById(R.id.etRegisterFullName)
        etRegisterDisplayName = findViewById(R.id.etRegisterDisplayName)
        etRegisterEmail = findViewById(R.id.etRegisterEmail)
        etRegisterPassword = findViewById(R.id.etRegisterPassword)
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit)
        btnRegisterBack = findViewById(R.id.btnRegisterBack)
        layoutDashboard = findViewById(R.id.layoutDashboard)
        btnDashboardLogout = findViewById(R.id.btnDashboardLogout)
        etDashboardSearch = findViewById(R.id.etDashboardSearch)
        rvDashboardItems = findViewById(R.id.rvDashboardItems)
        tvDashboardWelcome = findViewById(R.id.tvDashboardWelcome)
        tvDashboardStatus = findViewById(R.id.tvDashboardStatus)
        progressDashboard = findViewById(R.id.progressDashboard)
        layoutDashboardHero = findViewById(R.id.layoutDashboardHero)
        btnTabMarketplace = findViewById(R.id.btnTabMarketplace)
        btnTabMyListings = findViewById(R.id.btnTabMyListings)
        btnTabProfile = findViewById(R.id.btnTabProfile)
        btnTabSettings = findViewById(R.id.btnTabSettings)
        spDashboardCategory = findViewById(R.id.spDashboardCategory)
        spDashboardSort = findViewById(R.id.spDashboardSort)
        btnDashboardSell = findViewById(R.id.btnDashboardSell)
        btnDashboardRefresh = findViewById(R.id.btnDashboardRefresh)
        btnDashboardNotifications = findViewById(R.id.btnDashboardNotifications)
        tvDashboardNotifBadge = findViewById(R.id.tvDashboardNotifBadge)
        layoutDashboardChatFab = findViewById(R.id.layoutDashboardChatFab)
        btnDashboardChat = findViewById(R.id.btnDashboardChat)
        tvDashboardChatBadge = findViewById(R.id.tvDashboardChatBadge)
        layoutDashboardBottomNav = findViewById(R.id.layoutDashboardBottomNav)
        layoutDashboardFilters = findViewById(R.id.layoutDashboardFilters)
        layoutDashboardListHeader = findViewById(R.id.layoutDashboardListHeader)
        layoutDashboardProfile = findViewById(R.id.layoutDashboardProfile)
        layoutDashboardSettings = findViewById(R.id.layoutDashboardSettings)
        viewDashboardNotifScrim = findViewById(R.id.viewDashboardNotifScrim)
        layoutDashboardNotificationsPanel = findViewById(R.id.layoutDashboardNotificationsPanel)
        rvDashboardNotifications = findViewById(R.id.rvDashboardNotifications)
        tvDashboardNotificationsEmpty = findViewById(R.id.tvDashboardNotificationsEmpty)
        btnDashboardNotificationsClose = findViewById(R.id.btnDashboardNotificationsClose)
        btnDashboardNotificationsClearAll = findViewById(R.id.btnDashboardNotificationsClearAll)
        tvProfileDisplayName = findViewById(R.id.tvProfileDisplayName)
        tvProfileFullName = findViewById(R.id.tvProfileFullName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfileListingCount = findViewById(R.id.tvProfileListingCount)
    }

    private fun setupRecyclerViews() {
        landingAdapter = ItemListAdapter { item ->
            showItemDetailsDialog(item)
        }
        dashboardAdapter = ItemListAdapter { item ->
            showItemDetailsDialog(item)
        }

        rvLandingItems.layoutManager = LinearLayoutManager(this)
        rvLandingItems.adapter = landingAdapter
        rvDashboardItems.layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean = false
        }
        rvDashboardItems.adapter = dashboardAdapter
        rvDashboardItems.isNestedScrollingEnabled = false
        rvDashboardItems.setHasFixedSize(false)

        dashboardNotificationAdapter = NotificationAdapter { item ->
            hideNotificationsPanel()
            showChatDialog(
                receiverEmail = item.contactEmail,
                receiverName = item.contactName,
                messageChannel = item.channel,
                transactionId = item.transactionId
            )
        }
        rvDashboardNotifications.layoutManager = LinearLayoutManager(this)
        rvDashboardNotifications.adapter = dashboardNotificationAdapter
    }

    private fun setupDashboardSpinners() {
        val categoryAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.listing_categories_with_all,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spDashboardCategory.adapter = categoryAdapter

        val sortAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.listing_sort_options,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spDashboardSort.adapter = sortAdapter

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                applyListingFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        spDashboardCategory.onItemSelectedListener = listener
        spDashboardSort.onItemSelectedListener = listener
    }

    private fun setupSearchInputs() {
        etLandingSearch.doAfterTextChanged {
            applyListingFilters()
        }
        etDashboardSearch.doAfterTextChanged {
            applyListingFilters()
        }
    }

    private fun setupClickListeners() {
        btnLandingLogin.setOnClickListener {
            showLogin()
        }
        btnLandingRegister.setOnClickListener {
            showRegister()
        }
        btnLoginBack.setOnClickListener {
            showLanding()
        }
        btnRegisterBack.setOnClickListener {
            showLanding()
        }
        btnLoginGoRegister.setOnClickListener {
            showRegister()
        }
        btnLoginSubmit.setOnClickListener {
            login()
        }
        btnRegisterSubmit.setOnClickListener {
            register()
        }
        btnDashboardLogout.setOnClickListener {
            logout()
        }
        btnTabMarketplace.setOnClickListener {
            showMarketplace()
        }
        btnTabMyListings.setOnClickListener {
            showMyListings()
        }
        btnTabProfile.setOnClickListener {
            showProfile()
        }
        btnTabSettings.setOnClickListener {
            showSettings()
        }
        btnDashboardRefresh.setOnClickListener {
            loadListings(showLoading = true)
        }
        btnDashboardSell.setOnClickListener {
            showListingFormDialog(null)
        }
        btnDashboardNotifications.setOnClickListener {
            toggleNotificationsPanel()
        }
        btnDashboardNotificationsClose.setOnClickListener {
            hideNotificationsPanel()
        }
        btnDashboardNotificationsClearAll.setOnClickListener {
            clearNotifications()
        }
        viewDashboardNotifScrim.setOnClickListener {
            hideNotificationsPanel()
        }
        btnDashboardChat.setOnClickListener {
            hideNotificationsPanel()
            showChatContactPicker()
        }
    }

    private fun login() {
        val email = etLoginEmail.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_login), Toast.LENGTH_SHORT).show()
            return
        }

        btnLoginSubmit.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    RetrofitClient.api.login(LoginRequest(email, password))
                }
            }
            btnLoginSubmit.isEnabled = true

            result.onSuccess { response ->
                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.data != null) {
                    sessionManager.saveToken("local-session")
                    sessionManager.saveUserProfile(body.data)
                    Toast.makeText(
                        this@MainActivity,
                        body.message.ifBlank { getString(R.string.login_success) },
                        Toast.LENGTH_SHORT
                    ).show()
                    etLoginPassword.text?.clear()
                    showDashboard()
                    refreshUsersInBackground(force = true)
                    loadListings(showLoading = true)
                    refreshInboxMetadataInBackground(force = true)
                } else {
                    val message = body?.message?.takeIf { it.isNotBlank() }
                        ?: extractHttpError(response)
                        ?: getString(R.string.login_failed)
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.server_error)} ${it.message.orEmpty()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun register() {
        val fullName = etRegisterFullName.text.toString().trim()
        val displayName = etRegisterDisplayName.text.toString().trim()
        val email = etRegisterEmail.text.toString().trim()
        val password = etRegisterPassword.text.toString().trim()

        if (fullName.isEmpty() || displayName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_register), Toast.LENGTH_SHORT).show()
            return
        }

        btnRegisterSubmit.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    RetrofitClient.api.register(
                        AuthRequest(
                            fullName = fullName,
                            displayName = displayName,
                            email = email,
                            password = password
                        )
                    )
                }
            }
            btnRegisterSubmit.isEnabled = true

            result.onSuccess { response ->
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    Toast.makeText(
                        this@MainActivity,
                        body.message.ifBlank { getString(R.string.register_success) },
                        Toast.LENGTH_SHORT
                    ).show()
                    etLoginEmail.setText(email)
                    etRegisterPassword.text?.clear()
                    showLogin()
                } else {
                    val message = body?.message?.takeIf { it.isNotBlank() }
                        ?: extractHttpError(response)
                        ?: getString(R.string.register_failed)
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.server_error)} ${it.message.orEmpty()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadListings(showLoading: Boolean) {
        if (isLoadingItems) return
        isLoadingItems = true

        if (showLoading) {
            progressLanding.isVisible = layoutLanding.isVisible
            progressDashboard.isVisible = layoutDashboard.isVisible &&
                (currentTab == DashboardTab.MARKETPLACE || currentTab == DashboardTab.MY_LISTINGS)
        }

        if (layoutLanding.isVisible) {
            tvLandingStatus.text = getString(R.string.landing_loading)
        }
        if (layoutDashboard.isVisible && (currentTab == DashboardTab.MARKETPLACE || currentTab == DashboardTab.MY_LISTINGS)) {
            tvDashboardStatus.text = getString(R.string.dashboard_loading)
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { RetrofitClient.api.getItems() }
            }
            isLoadingItems = false
            progressLanding.isVisible = false
            progressDashboard.isVisible = false

            result.onSuccess { items ->
                allItems.clear()
                allItems.addAll(items.sortedByDescending { it.createdAt.orEmpty() })
                refreshUsersInBackground()
                applyListingFilters()
                refreshInboxMetadataInBackground()
            }.onFailure { error ->
                if (allItems.isEmpty()) {
                    landingAdapter.submitItems(emptyList())
                    dashboardAdapter.submitItems(emptyList())
                }
                tvLandingStatus.text = getString(R.string.landing_error)
                tvDashboardStatus.text = getString(R.string.dashboard_error)
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.server_error)} ${error.message.orEmpty()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyListingFilters() {
        applyLandingFilter()
        applyDashboardFilter()
        updateProfileSection()
    }

    private fun applyLandingFilter() {
        val query = etLandingSearch.text.toString().trim().lowercase(Locale.ROOT)
        val filtered = allItems.filter { item ->
            matchesQuery(item, query)
        }
        landingAdapter.submitItems(filtered)
        tvLandingStatus.text = when {
            filtered.isNotEmpty() -> resources.getQuantityString(
                R.plurals.landing_items_count,
                filtered.size,
                filtered.size
            )
            query.isNotBlank() -> getString(R.string.landing_no_results)
            else -> getString(R.string.landing_empty)
        }
    }

    private fun applyDashboardFilter() {
        if (!sessionManager.isLoggedIn()) return

        val base = when (currentTab) {
            DashboardTab.MY_LISTINGS -> allItems.filter { isOwnedByCurrentUser(it) }
            DashboardTab.MARKETPLACE -> allItems
            DashboardTab.PROFILE, DashboardTab.SETTINGS -> {
                dashboardAdapter.submitItems(emptyList())
                return
            }
        }

        val query = etDashboardSearch.text.toString().trim().lowercase(Locale.ROOT)
        val selectedCategory = spDashboardCategory.selectedItem?.toString().orEmpty()

        var filtered = base.filter { item ->
            matchesQuery(item, query)
        }
        if (selectedCategory.isNotBlank() && !selectedCategory.equals("All", ignoreCase = true)) {
            filtered = filtered.filter { item ->
                item.category.equals(selectedCategory, ignoreCase = true)
            }
        }

        filtered = when (spDashboardSort.selectedItemPosition) {
            1 -> filtered.sortedBy { it.price }
            2 -> filtered.sortedByDescending { it.price }
            else -> filtered.sortedByDescending { it.createdAt.orEmpty() }
        }

        dashboardAdapter.submitItems(filtered)

        val hasActiveFilter = query.isNotBlank() ||
            (selectedCategory.isNotBlank() && !selectedCategory.equals("All", ignoreCase = true))

        tvDashboardStatus.text = when {
            base.isEmpty() && currentTab == DashboardTab.MY_LISTINGS -> getString(R.string.my_listings_empty)
            base.isEmpty() -> getString(R.string.dashboard_empty)
            filtered.isEmpty() -> getString(R.string.dashboard_no_results)
            hasActiveFilter -> getString(
                R.string.items_count_filtered,
                filtered.size,
                base.size
            )
            else -> resources.getQuantityString(
                R.plurals.dashboard_items_count,
                filtered.size,
                filtered.size
            )
        }
    }

    private fun matchesQuery(item: Item, query: String): Boolean {
        if (query.isBlank()) return true
        val searchable = listOf(
            item.title,
            item.description,
            item.category,
            item.condition,
            item.location,
            parseSellerName(item.sellerName),
            item.sellerEmail,
            item.price.toString()
        ).joinToString(" ").lowercase(Locale.ROOT)
        return searchable.contains(query)
    }

    private fun showItemDetailsDialog(item: Item) {
        val view = layoutInflater.inflate(R.layout.dialog_item_details, null)
        val ivDetailImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val tvDetailPrice = view.findViewById<TextView>(R.id.tvDetailPrice)
        val tvDetailTitle = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailMeta = view.findViewById<TextView>(R.id.tvDetailMeta)
        val tvDetailSeller = view.findViewById<TextView>(R.id.tvDetailSeller)
        val tvDetailDescription = view.findViewById<TextView>(R.id.tvDetailDescription)
        val btnDetailMessage = view.findViewById<Button>(R.id.btnDetailMessage)
        val btnDetailEdit = view.findViewById<Button>(R.id.btnDetailEdit)
        val btnDetailDelete = view.findViewById<Button>(R.id.btnDetailDelete)
        val btnDetailClose = view.findViewById<Button>(R.id.btnDetailClose)

        val title = item.title?.takeIf { it.isNotBlank() } ?: getString(R.string.item_unknown_title)
        val description = item.description?.takeIf { it.isNotBlank() } ?: getString(R.string.item_no_description)
        val category = item.category?.takeIf { it.isNotBlank() } ?: getString(R.string.item_unknown_category)
        val condition = item.condition?.takeIf { it.isNotBlank() } ?: getString(R.string.item_unknown_condition)
        val location = item.location?.takeIf { it.isNotBlank() } ?: getString(R.string.item_unknown_location)
        val sellerName = parseSellerName(item.sellerName).ifBlank { getString(R.string.item_unknown_seller) }

        ivDetailImage.load(resolvePrimaryImage(item.imageUrl)) {
            placeholder(R.drawable.bg_listing_image_placeholder)
            error(R.drawable.bg_listing_image_placeholder)
            crossfade(true)
        }
        tvDetailPrice.text = getString(R.string.item_price, PriceFormatter.format(item.price))
        tvDetailTitle.text = title
        tvDetailMeta.text = getString(R.string.listing_meta_detail, category, condition, location)
        tvDetailSeller.text = getString(R.string.listing_seller_label, sellerName)
        tvDetailDescription.text = description

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.listing_details_title))
            .setView(view)
            .create()

        btnDetailClose.setOnClickListener {
            dialog.dismiss()
        }

        val loggedIn = sessionManager.isLoggedIn()
        val ownedByCurrentUser = loggedIn && isOwnedByCurrentUser(item)
        btnDetailEdit.isVisible = ownedByCurrentUser
        btnDetailDelete.isVisible = ownedByCurrentUser
        btnDetailMessage.isVisible = !ownedByCurrentUser
        if (!loggedIn) {
            btnDetailMessage.text = getString(R.string.action_sign_in)
        }

        btnDetailMessage.setOnClickListener {
            if (!sessionManager.isLoggedIn()) {
                dialog.dismiss()
                showLogin()
                return@setOnClickListener
            }

            btnDetailMessage.isEnabled = false
            lifecycleScope.launch {
                ensureUserDirectoryLoaded()
                btnDetailMessage.isEnabled = true

                val contact = resolveChatContactForItem(item)
                if (contact == null) {
                    Toast.makeText(
                        this@MainActivity,
                        if (isAdminContactForItem(item)) {
                            getString(R.string.chat_admin_restricted)
                        } else {
                            getString(R.string.chat_missing_contact)
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
                if (contact.email == currentEmail) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.chat_self_not_allowed),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                dialog.dismiss()
                showChatDialog(
                    receiverEmail = contact.email,
                    receiverName = contact.displayName,
                    messageChannel = contact.channel,
                    transactionId = contact.transactionId
                )
            }
        }

        btnDetailEdit.setOnClickListener {
            dialog.dismiss()
            showListingFormDialog(item)
        }

        btnDetailDelete.setOnClickListener {
            dialog.dismiss()
            confirmDeleteItem(item)
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun showListingFormDialog(editingItem: Item?) {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            showLogin()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_listing_form, null)
        val etFormTitle = view.findViewById<EditText>(R.id.etFormTitle)
        val etFormDescription = view.findViewById<EditText>(R.id.etFormDescription)
        val etFormPrice = view.findViewById<EditText>(R.id.etFormPrice)
        val spFormCategory = view.findViewById<Spinner>(R.id.spFormCategory)
        val spFormCondition = view.findViewById<Spinner>(R.id.spFormCondition)
        val etFormLocation = view.findViewById<EditText>(R.id.etFormLocation)
        val btnFormPickImage = view.findViewById<Button>(R.id.btnFormPickImage)
        val btnFormClearImage = view.findViewById<Button>(R.id.btnFormClearImage)
        val tvFormImageState = view.findViewById<TextView>(R.id.tvFormImageState)

        val categoryAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.listing_categories,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val conditionAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.listing_conditions,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spFormCategory.adapter = categoryAdapter
        spFormCondition.adapter = conditionAdapter

        var selectedImageUri: Uri? = null
        val hasExistingImage = !resolvePrimaryImage(editingItem?.imageUrl).isNullOrBlank()
        tvFormImageState.text = if (hasExistingImage) {
            getString(R.string.image_selected_label)
        } else {
            getString(R.string.image_not_selected_label)
        }

        if (editingItem != null) {
            etFormTitle.setText(editingItem.title.orEmpty())
            etFormDescription.setText(editingItem.description.orEmpty())
            etFormPrice.setText(if (editingItem.price > 0) PriceFormatter.format(editingItem.price) else "")
            etFormLocation.setText(editingItem.location.orEmpty())
            setSpinnerSelection(spFormCategory, editingItem.category)
            setSpinnerSelection(spFormCondition, editingItem.condition)
        }

        btnFormPickImage.setOnClickListener {
            pickImage { uri ->
                if (uri != null) {
                    selectedImageUri = uri
                    tvFormImageState.text = getString(R.string.image_selected_label)
                }
            }
        }

        btnFormClearImage.setOnClickListener {
            selectedImageUri = null
            tvFormImageState.text = if (hasExistingImage && editingItem != null) {
                getString(R.string.image_selected_label)
            } else {
                getString(R.string.image_not_selected_label)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (editingItem == null) {
                    getString(R.string.sell_dialog_title)
                } else {
                    getString(R.string.edit_dialog_title)
                }
            )
            .setView(view)
            .setPositiveButton(getString(R.string.action_save), null)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val title = etFormTitle.text.toString().trim()
                val description = etFormDescription.text.toString().trim()
                val priceText = etFormPrice.text.toString().trim()
                val category = spFormCategory.selectedItem?.toString().orEmpty()
                val condition = spFormCondition.selectedItem?.toString().orEmpty()
                val location = etFormLocation.text.toString().trim()
                val price = PriceFormatter.parse(priceText)

                if (
                    title.isBlank() ||
                    description.isBlank() ||
                    category.isBlank() ||
                    condition.isBlank() ||
                    location.isBlank() ||
                    price == null ||
                    price <= 0
                ) {
                    Toast.makeText(
                        this,
                        getString(R.string.listing_form_required),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (editingItem == null && selectedImageUri == null) {
                    Toast.makeText(
                        this,
                        getString(R.string.listing_pick_image_required),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                saveListing(
                    dialog = dialog,
                    editingItem = editingItem,
                    title = title,
                    description = description,
                    price = price,
                    category = category,
                    condition = condition,
                    location = location,
                    imageUri = selectedImageUri
                )
            }
        }
        dialog.show()
    }

    private fun saveListing(
        dialog: AlertDialog,
        editingItem: Item?,
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        location: String,
        imageUri: Uri?
    ) {
        val profile = sessionManager.getUserProfile()
        val sellerEmail = normalizeEmail(profile.email)
        val sellerName = profile.displayName.ifBlank { profile.fullName }.trim()
        if (sellerEmail.isBlank() || sellerName.isBlank()) {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            showLogin()
            return
        }

        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        saveButton.isEnabled = false
        cancelButton.isEnabled = false

        lifecycleScope.launch {
            val imageParts = if (imageUri != null) {
                val builtPart = withContext(Dispatchers.IO) {
                    buildImagePart(imageUri)
                }
                if (builtPart == null) {
                    saveButton.isEnabled = true
                    cancelButton.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.server_error),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                listOf(builtPart)
            } else {
                emptyList()
            }

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val titlePart = title.toTextPart()
                    val descriptionPart = description.toTextPart()
                    val pricePart = price.toString().toTextPart()
                    val categoryPart = category.toTextPart()
                    val conditionPart = condition.toTextPart()
                    val locationPart = location.toTextPart()
                    val sellerNamePart = sellerName.toTextPart()
                    val sellerEmailPart = sellerEmail.toTextPart()

                    if (editingItem == null) {
                        RetrofitClient.api.uploadItem(
                            title = titlePart,
                            description = descriptionPart,
                            price = pricePart,
                            category = categoryPart,
                            condition = conditionPart,
                            location = locationPart,
                            sellerName = sellerNamePart,
                            sellerEmail = sellerEmailPart,
                            images = imageParts
                        )
                    } else {
                        RetrofitClient.api.updateItem(
                            id = editingItem.id,
                            title = titlePart,
                            description = descriptionPart,
                            price = pricePart,
                            category = categoryPart,
                            condition = conditionPart,
                            location = locationPart,
                            sellerName = sellerNamePart,
                            sellerEmail = sellerEmailPart,
                            images = imageParts
                        )
                    }
                }
            }

            saveButton.isEnabled = true
            cancelButton.isEnabled = true

            result.onSuccess { response ->
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.listing_save_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    loadListings(showLoading = true)
                } else {
                    val message = extractHttpError(response) ?: getString(R.string.listing_save_failed)
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.server_error)} ${it.message.orEmpty()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDeleteItem(item: Item) {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            showLogin()
            return
        }

        AlertDialog.Builder(this)
            .setMessage(getString(R.string.listing_delete_confirm))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun deleteItem(item: Item) {
        val profile = sessionManager.getUserProfile()
        val sellerEmail = normalizeEmail(profile.email)
        val sellerName = profile.displayName.ifBlank { profile.fullName }.trim()
        if (sellerEmail.isBlank()) {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            showLogin()
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    RetrofitClient.api.deleteItem(
                        id = item.id,
                        sellerEmail = sellerEmail,
                        sellerName = sellerName
                    )
                }
            }

            result.onSuccess { response ->
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.listing_delete_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadListings(showLoading = true)
                } else {
                    val message = extractHttpError(response) ?: getString(R.string.listing_delete_failed)
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.server_error)} ${it.message.orEmpty()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showChatContactPicker() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            showLogin()
            return
        }

        btnDashboardChat.isEnabled = false
        lifecycleScope.launch {
            ensureUserDirectoryLoaded()
            btnDashboardChat.isEnabled = true

            val contacts = buildInboxContacts()
            if (contacts.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.chat_no_contacts),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val inboxView = layoutInflater.inflate(R.layout.dialog_chat_inbox, null)
            val rvInboxThreads = inboxView.findViewById<RecyclerView>(R.id.rvInboxThreads)
            val progressInbox = inboxView.findViewById<ProgressBar>(R.id.progressInbox)
            val tvInboxEmpty = inboxView.findViewById<TextView>(R.id.tvInboxEmpty)
            val btnInboxClose = inboxView.findViewById<Button>(R.id.btnInboxClose)

            var dialog: AlertDialog? = null
            val inboxAdapter = ChatInboxAdapter { thread ->
                dialog?.dismiss()
                showChatDialog(
                    receiverEmail = thread.email,
                    receiverName = thread.displayName,
                    messageChannel = thread.channel,
                    transactionId = thread.transactionId
                )
            }
            rvInboxThreads.layoutManager = LinearLayoutManager(this@MainActivity)
            rvInboxThreads.adapter = inboxAdapter

            dialog = AlertDialog.Builder(this@MainActivity)
                .setView(inboxView)
                .create()
            dialog.setOnShowListener {
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
            btnInboxClose.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()

            progressInbox.isVisible = true
            val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
            val snapshot = buildInboxSnapshot(currentEmail = currentEmail, contacts = contacts)
            if (!dialog.isShowing) return@launch

            progressInbox.isVisible = false
            notificationItems.clear()
            notificationItems.addAll(snapshot.notifications)
            updateNotificationBadges(
                unreadChatCount = snapshot.unreadChatCount,
                bellUnreadCount = snapshot.unreadBellCount
            )
            renderNotificationsPanel()
            inboxAdapter.submitThreads(snapshot.threads)
            tvInboxEmpty.isVisible = snapshot.threads.isEmpty()
            rvInboxThreads.isVisible = snapshot.threads.isNotEmpty()
        }
    }

    private fun buildInboxContacts(): List<ChatContact> {
        val contacts = buildChatContacts().toMutableList()
        buildCustomerSupportContact()?.let { supportContact ->
            if (contacts.none { it.email == supportContact.email && it.channel == supportContact.channel }) {
                contacts.add(0, supportContact)
            }
        }
        return contacts
    }

    private fun buildChatContacts(): List<ChatContact> {
        val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
        val currentUserIsAdmin = isCurrentUserAdmin(currentEmail)
        val adminEmails = allUsers.asSequence()
            .filter { isAdminRole(it.role) }
            .map { normalizeEmail(it.email) }
            .filter { it.isNotBlank() }
            .toSet()
        val contactMap = linkedMapOf<String, ChatContact>()

        allUsers.forEach { user ->
            val email = normalizeEmail(user.email)
            if (email.isBlank() || email == currentEmail) return@forEach
            if (!currentUserIsAdmin && isAdminRole(user.role)) return@forEach
            val displayName = user.displayName.ifBlank { user.fullName }.ifBlank { email }
            contactMap[email] = ChatContact(
                email = email,
                displayName = displayName
            )
        }

        allItems.forEach { item ->
            val sellerEmail = normalizeEmail(item.sellerEmail)
            if (sellerEmail.isBlank() || sellerEmail == currentEmail) return@forEach
            if (!currentUserIsAdmin && adminEmails.contains(sellerEmail)) return@forEach
            if (!contactMap.containsKey(sellerEmail)) {
                contactMap[sellerEmail] = ChatContact(
                    email = sellerEmail,
                    displayName = parseSellerName(item.sellerName)
                        .ifBlank { sellerEmail }
                )
            }
        }

        return contactMap.values
            .sortedBy { it.displayName.lowercase(Locale.ROOT) }
    }

    private fun buildCustomerSupportContact(): ChatContact? {
        val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
        if (isCurrentUserAdmin(currentEmail)) return null

        val adminUser = allUsers.firstOrNull { user ->
            isAdminRole(user.role) && normalizeEmail(user.email).isNotBlank()
        } ?: return null

        return ChatContact(
            email = normalizeEmail(adminUser.email),
            displayName = getString(R.string.customer_support_label),
            channel = "SUPPORT"
        )
    }

    private fun isAdminRole(role: String?): Boolean {
        return role.equals("ADMIN", ignoreCase = true)
    }

    private fun isCurrentUserAdmin(currentEmail: String = normalizeEmail(sessionManager.getUserProfile().email)): Boolean {
        if (currentEmail.isBlank()) return false
        return allUsers.firstOrNull { normalizeEmail(it.email) == currentEmail }
            ?.let { isAdminRole(it.role) }
            ?: false
    }

    private suspend fun buildInboxSnapshot(
        currentEmail: String,
        contacts: List<ChatContact>
    ): InboxSnapshot {
        val openedAt = getNotificationsOpenedAt(currentEmail)
        val clearedAt = getNotificationsClearedAt(currentEmail)

        val threads = mutableListOf<ChatInboxThread>()
        val notifications = mutableListOf<NotificationItem>()
        var unreadChatCount = 0

        contacts.forEach { contact ->
            val messages = withContext(Dispatchers.IO) {
                runCatching {
                    RetrofitClient.api.getConversation(
                        user1 = currentEmail,
                        user2 = contact.email
                    )
                }.getOrDefault(emptyList())
            }
            val latest = messages.lastOrNull()

            val threadLastSeenAt = getThreadLastSeenAt(currentEmail, contact)
            val unreadIncoming = messages.filter { message ->
                normalizeEmail(message.senderEmail) != currentEmail &&
                    isAfterThreshold(message.createdAt, threadLastSeenAt)
            }
            unreadChatCount += unreadIncoming.size

            val preview = when {
                latest == null && contact.channel.equals("SUPPORT", ignoreCase = true) ->
                    getString(R.string.inbox_support_preview)
                latest == null -> getString(R.string.chat_preview_start)
                normalizeEmail(latest.senderEmail) == currentEmail ->
                    getString(R.string.chat_preview_you, latest.content)
                else -> latest.content
            }

            threads.add(
                ChatInboxThread(
                    email = contact.email,
                    displayName = contact.displayName,
                    preview = preview,
                    timeLabel = formatInboxTimestamp(latest?.createdAt),
                    lastMessageAt = latest?.createdAt,
                    channel = contact.channel,
                    transactionId = contact.transactionId
                )
            )

            val latestUnread = unreadIncoming.lastOrNull()
            val latestUnreadAt = parseDateTime(latestUnread?.createdAt)
            if (latestUnread != null && latestUnreadAt != null &&
                (clearedAt == null || latestUnreadAt.isAfter(clearedAt))
            ) {
                notifications.add(
                    NotificationItem(
                        id = "${contact.email}|${contact.channel.orEmpty()}|${contact.transactionId ?: 0}|${latestUnread.id}",
                        title = contact.displayName,
                        body = getString(R.string.notification_new_messages, latestUnread.content),
                        createdAt = latestUnread.createdAt.orEmpty(),
                        timeLabel = formatInboxTimestamp(latestUnread.createdAt),
                        contactEmail = contact.email,
                        contactName = contact.displayName,
                        channel = contact.channel,
                        transactionId = contact.transactionId
                    )
                )
            }
        }

        val sortedThreads = threads.sortedWith(
            compareByDescending<ChatInboxThread> { it.lastMessageAt != null }
                .thenByDescending { it.lastMessageAt.orEmpty() }
                .thenByDescending { it.channel.equals("SUPPORT", ignoreCase = true) }
                .thenBy { it.displayName.lowercase(Locale.ROOT) }
        )
        val sortedNotifications = notifications.sortedByDescending { it.createdAt }
        val unreadBellCount = sortedNotifications.count { notification ->
            val createdAt = parseDateTime(notification.createdAt)
            createdAt != null && (openedAt == null || createdAt.isAfter(openedAt))
        }

        return InboxSnapshot(
            threads = sortedThreads,
            notifications = sortedNotifications,
            unreadChatCount = unreadChatCount,
            unreadBellCount = unreadBellCount
        )
    }

    private fun refreshInboxMetadataInBackground(force: Boolean = false) {
        if (!sessionManager.isLoggedIn()) {
            updateNotificationBadges(unreadChatCount = 0, bellUnreadCount = 0)
            return
        }
        if (isRefreshingInboxMetadata && !force) return

        isRefreshingInboxMetadata = true
        lifecycleScope.launch {
            try {
                ensureUserDirectoryLoaded()
                val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
                if (currentEmail.isBlank()) {
                    updateNotificationBadges(unreadChatCount = 0, bellUnreadCount = 0)
                    return@launch
                }

                val contacts = buildInboxContacts()
                val snapshot = buildInboxSnapshot(currentEmail = currentEmail, contacts = contacts)
                notificationItems.clear()
                notificationItems.addAll(snapshot.notifications)
                updateNotificationBadges(
                    unreadChatCount = snapshot.unreadChatCount,
                    bellUnreadCount = snapshot.unreadBellCount
                )
                renderNotificationsPanel()
            } finally {
                isRefreshingInboxMetadata = false
            }
        }
    }

    private fun toggleNotificationsPanel() {
        if (layoutDashboardNotificationsPanel.isVisible) {
            hideNotificationsPanel()
        } else {
            showNotificationsPanel()
        }
    }

    private fun showNotificationsPanel() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            showLogin()
            return
        }

        val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
        if (currentEmail.isBlank()) return
        setNotificationsOpenedNow(currentEmail)
        updateNotificationBadges(unreadChatCount = unreadChatBadgeCount, bellUnreadCount = 0)
        renderNotificationsPanel()
        viewDashboardNotifScrim.isVisible = true
        layoutDashboardNotificationsPanel.isVisible = true
    }

    private fun hideNotificationsPanel() {
        viewDashboardNotifScrim.isVisible = false
        layoutDashboardNotificationsPanel.isVisible = false
    }

    private fun renderNotificationsPanel() {
        dashboardNotificationAdapter.submitItems(notificationItems)
        tvDashboardNotificationsEmpty.isVisible = notificationItems.isEmpty()
        rvDashboardNotifications.isVisible = notificationItems.isNotEmpty()
    }

    private fun clearNotifications() {
        if (!sessionManager.isLoggedIn()) return

        val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
        if (currentEmail.isBlank()) return
        setNotificationsClearedNow(currentEmail)
        setNotificationsOpenedNow(currentEmail)
        notificationItems.clear()
        renderNotificationsPanel()
        updateNotificationBadges(unreadChatCount = unreadChatBadgeCount, bellUnreadCount = 0)
        refreshInboxMetadataInBackground(force = true)
    }

    private fun updateNotificationBadges(unreadChatCount: Int, bellUnreadCount: Int) {
        unreadChatBadgeCount = unreadChatCount
        tvDashboardChatBadge.isVisible = unreadChatCount > 0
        tvDashboardChatBadge.text = formatBadgeCount(unreadChatCount)
        tvDashboardNotifBadge.isVisible = bellUnreadCount > 0
        tvDashboardNotifBadge.text = formatBadgeCount(bellUnreadCount)
    }

    private fun formatBadgeCount(count: Int): String {
        return if (count > 99) "99+" else count.toString()
    }

    private fun threadStateKey(currentEmail: String, contact: ChatContact): String {
        val channel = contact.channel?.uppercase(Locale.ROOT).orEmpty()
        val tx = contact.transactionId ?: 0L
        return "${currentEmail}|${normalizeEmail(contact.email)}|$channel|$tx"
    }

    private fun getThreadLastSeenAt(currentEmail: String, contact: ChatContact): LocalDateTime? {
        val key = "thread_seen_${threadStateKey(currentEmail, contact)}"
        return parseDateTime(chatStatePrefs.getString(key, null))
    }

    private fun setThreadLastSeenAt(currentEmail: String, contact: ChatContact, timestamp: LocalDateTime) {
        val key = "thread_seen_${threadStateKey(currentEmail, contact)}"
        chatStatePrefs.edit()
            .putString(key, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .apply()
    }

    private fun markThreadAsSeen(
        currentEmail: String,
        receiverEmail: String,
        channel: String?,
        transactionId: Long?,
        messages: List<ChatMessage>
    ) {
        val latestTimestamp = messages.lastOrNull()?.createdAt
        val seenAt = parseDateTime(latestTimestamp) ?: LocalDateTime.now()
        setThreadLastSeenAt(
            currentEmail = currentEmail,
            contact = ChatContact(
                email = receiverEmail,
                displayName = receiverEmail,
                channel = channel,
                transactionId = transactionId
            ),
            timestamp = seenAt
        )
    }

    private fun getNotificationsOpenedAt(currentEmail: String): LocalDateTime? {
        return parseDateTime(chatStatePrefs.getString("notifications_opened_$currentEmail", null))
    }

    private fun setNotificationsOpenedNow(currentEmail: String) {
        chatStatePrefs.edit()
            .putString(
                "notifications_opened_$currentEmail",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            .apply()
    }

    private fun getNotificationsClearedAt(currentEmail: String): LocalDateTime? {
        return parseDateTime(chatStatePrefs.getString("notifications_cleared_$currentEmail", null))
    }

    private fun setNotificationsClearedNow(currentEmail: String) {
        chatStatePrefs.edit()
            .putString(
                "notifications_cleared_$currentEmail",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            .apply()
    }

    private fun parseDateTime(rawTimestamp: String?): LocalDateTime? {
        if (rawTimestamp.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(rawTimestamp) }.getOrNull()
    }

    private fun isAfterThreshold(rawTimestamp: String?, threshold: LocalDateTime?): Boolean {
        val timestamp = parseDateTime(rawTimestamp) ?: return threshold == null
        return threshold == null || timestamp.isAfter(threshold)
    }

    private fun formatInboxTimestamp(rawTimestamp: String?): String {
        val parsed = parseDateTime(rawTimestamp) ?: return ""
        val locale = Locale.getDefault()
        val date = parsed.toLocalDate()
        val today = LocalDate.now()
        return when {
            date.isEqual(today) -> parsed.format(DateTimeFormatter.ofPattern("h:mm a", locale))
            date.isAfter(today.minusDays(6)) -> parsed.format(DateTimeFormatter.ofPattern("EEE", locale))
            else -> parsed.format(DateTimeFormatter.ofPattern("MMM d", locale))
        }
    }

    private fun isAdminContactForItem(item: Item): Boolean {
        val sellerEmail = normalizeEmail(item.sellerEmail)
        val sellerName = parseSellerName(item.sellerName)

        if (sellerEmail.isNotBlank()) {
            val userByEmail = allUsers.firstOrNull { normalizeEmail(it.email) == sellerEmail }
            if (userByEmail != null && isAdminRole(userByEmail.role)) {
                return true
            }
        }

        if (sellerName.isBlank()) return false
        val userByName = allUsers.firstOrNull { user ->
            user.displayName.equals(sellerName, ignoreCase = true) ||
                user.fullName.equals(sellerName, ignoreCase = true)
        }
        return userByName?.let { isAdminRole(it.role) } ?: false
    }

    private fun resolveChatContactForItem(item: Item): ChatContact? {
        val currentEmail = normalizeEmail(sessionManager.getUserProfile().email)
        val currentUserIsAdmin = isCurrentUserAdmin(currentEmail)
        val sellerEmail = normalizeEmail(item.sellerEmail)
        val sellerName = parseSellerName(item.sellerName)
        val adminEmails = allUsers.asSequence()
            .filter { isAdminRole(it.role) }
            .map { normalizeEmail(it.email) }
            .filter { it.isNotBlank() }
            .toSet()

        if (sellerEmail.isNotBlank() && sellerEmail != currentEmail) {
            val user = allUsers.firstOrNull { normalizeEmail(it.email) == sellerEmail }
            if (!currentUserIsAdmin && (adminEmails.contains(sellerEmail) || (user != null && isAdminRole(user.role)))) {
                return null
            }
            val displayName = user?.displayName
                ?.ifBlank { user.fullName }
                ?.ifBlank { sellerName }
                ?: sellerName.ifBlank { sellerEmail }
            return ChatContact(email = sellerEmail, displayName = displayName)
        }

        if (sellerName.isBlank()) return null

        val userByName = allUsers.firstOrNull { user ->
            user.displayName.equals(sellerName, ignoreCase = true) ||
                user.fullName.equals(sellerName, ignoreCase = true)
        } ?: return null

        if (!currentUserIsAdmin && isAdminRole(userByName.role)) return null

        val resolvedEmail = normalizeEmail(userByName.email)
        if (resolvedEmail.isBlank() || resolvedEmail == currentEmail) return null
        val displayName = userByName.displayName
            .ifBlank { userByName.fullName }
            .ifBlank { sellerName }

        return ChatContact(email = resolvedEmail, displayName = displayName)
    }

    private fun refreshUsersInBackground(force: Boolean = false) {
        if (isLoadingUsers) return
        if (!force && allUsers.isNotEmpty()) return

        isLoadingUsers = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { RetrofitClient.api.getUsers() }
            }
            isLoadingUsers = false
            result.onSuccess { users ->
                allUsers.clear()
                allUsers.addAll(users)
            }
        }
    }

    private suspend fun ensureUserDirectoryLoaded(): Boolean {
        if (allUsers.isNotEmpty()) return true
        val result = withContext(Dispatchers.IO) {
            runCatching { RetrofitClient.api.getUsers() }
        }
        result.onSuccess { users ->
            allUsers.clear()
            allUsers.addAll(users)
        }
        return result.isSuccess
    }

    private fun showChatDialog(
        receiverEmail: String,
        receiverName: String,
        messageChannel: String? = null,
        transactionId: Long? = null
    ) {
        val currentUser = sessionManager.getUserProfile()
        val currentEmail = normalizeEmail(currentUser.email)
        if (currentEmail.isBlank()) {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            showLogin()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_chat, null)
        val layoutChatBar = view.findViewById<LinearLayout>(R.id.layoutChatBar)
        val btnChatBack = view.findViewById<Button>(R.id.btnChatBack)
        val tvChatBarTitle = view.findViewById<TextView>(R.id.tvChatBarTitle)
        val layoutChatContent = view.findViewById<LinearLayout>(R.id.layoutChatContent)
        val tvChatToggle = view.findViewById<TextView>(R.id.tvChatToggle)
        val btnChatClose = view.findViewById<Button>(R.id.btnChatClose)
        val tvChatPartner = view.findViewById<TextView>(R.id.tvChatPartner)
        val rvChatMessages = view.findViewById<RecyclerView>(R.id.rvChatMessages)
        val tvChatState = view.findViewById<TextView>(R.id.tvChatState)
        val etChatInput = view.findViewById<EditText>(R.id.etChatInput)
        val btnChatSend = view.findViewById<Button>(R.id.btnChatSend)

        tvChatBarTitle.text = receiverName
        tvChatPartner.text = getString(R.string.chat_partner_label, receiverName, receiverEmail)
        val adapter = ChatMessageAdapter(currentEmail)
        rvChatMessages.layoutManager = LinearLayoutManager(this)
        rvChatMessages.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        var isChatExpanded = true
        fun setChatExpanded(expanded: Boolean) {
            isChatExpanded = expanded
            layoutChatContent.isVisible = expanded
            tvChatToggle.text = getString(
                if (expanded) {
                    R.string.action_minimize
                } else {
                    R.string.action_open
                }
            )
        }

        layoutChatBar.setOnClickListener {
            setChatExpanded(!isChatExpanded)
        }
        btnChatBack.setOnClickListener {
            dialog.dismiss()
            showChatContactPicker()
        }
        btnChatClose.setOnClickListener {
            dialog.dismiss()
        }
        setChatExpanded(expanded = true)

        fun loadConversation() {
            tvChatState.text = getString(R.string.dashboard_loading)
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        RetrofitClient.api.getConversation(
                            user1 = currentEmail,
                            user2 = receiverEmail
                        )
                    }
                }
                if (!dialog.isShowing) return@launch
                result.onSuccess { messages ->
                    adapter.submitMessages(messages)
                    markThreadAsSeen(
                        currentEmail = currentEmail,
                        receiverEmail = receiverEmail,
                        channel = messageChannel,
                        transactionId = transactionId,
                        messages = messages
                    )
                    refreshInboxMetadataInBackground()
                    if (messages.isNotEmpty()) {
                        rvChatMessages.scrollToPosition(messages.lastIndex)
                        tvChatState.text = ""
                    } else {
                        tvChatState.text = getString(R.string.chat_no_messages)
                    }
                }.onFailure {
                    tvChatState.text = getString(R.string.chat_load_error)
                }
            }
        }

        btnChatSend.setOnClickListener {
            val content = etChatInput.text.toString().trim()
            if (content.isBlank()) return@setOnClickListener

            btnChatSend.isEnabled = false
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        RetrofitClient.api.sendMessage(
                            SendMessageRequest(
                                senderEmail = currentEmail,
                                receiverEmail = receiverEmail,
                                content = content,
                                channel = messageChannel,
                                transactionId = transactionId
                            )
                        )
                    }
                }
                if (!dialog.isShowing) return@launch
                btnChatSend.isEnabled = true
                result.onSuccess {
                    etChatInput.text?.clear()
                    loadConversation()
                }.onFailure {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.chat_send_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
        loadConversation()
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String?) {
        val normalized = value.orEmpty().trim()
        if (normalized.isBlank()) return
        val adapter = spinner.adapter ?: return
        for (index in 0 until adapter.count) {
            val entry = adapter.getItem(index)?.toString().orEmpty()
            if (entry.equals(normalized, ignoreCase = true)) {
                spinner.setSelection(index)
                break
            }
        }
    }

    private fun pickImage(onResult: (Uri?) -> Unit) {
        imagePickerCallback = onResult
        imagePickerLauncher.launch("image/*")
    }

    private fun buildImagePart(uri: Uri): MultipartBody.Part? {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val fileName = resolveFileName(uri) ?: "listing_${System.currentTimeMillis()}.jpg"
        val mimeType = contentResolver.getType(uri).orEmpty().ifBlank { "image/*" }
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("images", fileName, body)
    }

    private fun resolveFileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else {
                null
            }
        }
    }

    private fun String.toTextPart(): RequestBody {
        return toRequestBody("text/plain".toMediaType())
    }

    private fun extractHttpError(response: Response<*>): String? {
        val raw = response.errorBody()?.string().orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            JSONObject(raw).optString("message").ifBlank { raw }
        }.getOrElse { raw }
    }

    private fun normalizeEmail(value: String?): String {
        val normalized = value.orEmpty().trim().lowercase(Locale.ROOT)
        return when (normalized) {
            "",
            "null",
            "undefined" -> ""
            else -> normalized
        }
    }

    private fun parseSellerName(raw: String?): String {
        val trimmed = raw.orEmpty().trim()
        if (
            trimmed.isBlank() ||
            trimmed.equals("null", ignoreCase = true) ||
            trimmed.equals("undefined", ignoreCase = true)
        ) {
            return ""
        }

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val parsed = runCatching { JSONObject(trimmed) }.getOrNull()
            val displayName = parsed?.optString("displayName").orEmpty().trim()
            if (displayName.isNotBlank()) return displayName
            val fullName = parsed?.optString("fullName").orEmpty().trim()
            if (fullName.isNotBlank()) return fullName
        }
        return trimmed
    }

    private fun isOwnedByCurrentUser(item: Item): Boolean {
        val profile = sessionManager.getUserProfile()
        val currentEmail = normalizeEmail(profile.email)
        val sellerEmail = normalizeEmail(item.sellerEmail)
        if (currentEmail.isNotBlank() && sellerEmail.isNotBlank()) {
            return currentEmail == sellerEmail
        }

        val sellerName = parseSellerName(item.sellerName).lowercase(Locale.ROOT)
        if (sellerName.isBlank()) return false

        val displayName = profile.displayName.trim().lowercase(Locale.ROOT)
        val fullName = profile.fullName.trim().lowercase(Locale.ROOT)
        return sellerName == displayName || sellerName == fullName
    }

    private fun resolvePrimaryImage(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        if (value.startsWith("[")) {
            val first = value.removePrefix("[").removeSuffix("]")
                .split(",")
                .map { it.trim().trim('"') }
                .firstOrNull { it.isNotBlank() }
            if (!first.isNullOrBlank()) return first
        }
        if (value.contains(",")) {
            return value.split(",")
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
        }
        return value
    }

    private fun showLanding() {
        hideNotificationsPanel()
        layoutLanding.visibility = View.VISIBLE
        layoutLogin.visibility = View.GONE
        layoutRegister.visibility = View.GONE
        layoutDashboard.visibility = View.GONE
        layoutDashboardBottomNav.visibility = View.GONE
        layoutDashboardChatFab.visibility = View.GONE
        btnDashboardChat.visibility = View.GONE
        if (allItems.isNotEmpty()) {
            applyLandingFilter()
        }
    }

    private fun showLogin() {
        hideNotificationsPanel()
        layoutLanding.visibility = View.GONE
        layoutLogin.visibility = View.VISIBLE
        layoutRegister.visibility = View.GONE
        layoutDashboard.visibility = View.GONE
        layoutDashboardBottomNav.visibility = View.GONE
        layoutDashboardChatFab.visibility = View.GONE
        btnDashboardChat.visibility = View.GONE
    }

    private fun showRegister() {
        hideNotificationsPanel()
        layoutLanding.visibility = View.GONE
        layoutLogin.visibility = View.GONE
        layoutRegister.visibility = View.VISIBLE
        layoutDashboard.visibility = View.GONE
        layoutDashboardBottomNav.visibility = View.GONE
        layoutDashboardChatFab.visibility = View.GONE
        btnDashboardChat.visibility = View.GONE
    }

    private fun showDashboard() {
        if (!sessionManager.isLoggedIn()) {
            showLanding()
            return
        }

        hideNotificationsPanel()
        layoutLanding.visibility = View.GONE
        layoutLogin.visibility = View.GONE
        layoutRegister.visibility = View.GONE
        layoutDashboard.visibility = View.VISIBLE
        layoutDashboardBottomNav.visibility = View.VISIBLE
        layoutDashboardChatFab.visibility = View.VISIBLE
        btnDashboardChat.visibility = View.VISIBLE

        val profile = sessionManager.getUserProfile()
        val displayName = profile.displayName.ifBlank { getString(R.string.profile_name_fallback) }
        tvDashboardWelcome.text = getString(R.string.dashboard_welcome, displayName)
        etDashboardSearch.text?.clear()
        if (spDashboardCategory.selectedItemPosition != 0) {
            spDashboardCategory.setSelection(0)
        }
        if (spDashboardSort.selectedItemPosition != 0) {
            spDashboardSort.setSelection(0)
        }
        showMarketplace()
        refreshInboxMetadataInBackground()
    }

    private fun showMarketplace() {
        hideNotificationsPanel()
        currentTab = DashboardTab.MARKETPLACE
        layoutDashboardHero.visibility = View.VISIBLE
        rvDashboardItems.visibility = View.VISIBLE
        layoutDashboardFilters.visibility = View.VISIBLE
        layoutDashboardListHeader.visibility = View.VISIBLE
        layoutDashboardProfile.visibility = View.GONE
        layoutDashboardSettings.visibility = View.GONE
        applyActiveTabStyle()
        applyDashboardFilter()
    }

    private fun showMyListings() {
        hideNotificationsPanel()
        currentTab = DashboardTab.MY_LISTINGS
        layoutDashboardHero.visibility = View.GONE
        etDashboardSearch.text?.clear()
        rvDashboardItems.visibility = View.VISIBLE
        layoutDashboardFilters.visibility = View.VISIBLE
        layoutDashboardListHeader.visibility = View.VISIBLE
        layoutDashboardProfile.visibility = View.GONE
        layoutDashboardSettings.visibility = View.GONE
        applyActiveTabStyle()
        applyDashboardFilter()
    }

    private fun showProfile() {
        hideNotificationsPanel()
        currentTab = DashboardTab.PROFILE
        layoutDashboardHero.visibility = View.GONE
        etDashboardSearch.text?.clear()
        rvDashboardItems.visibility = View.GONE
        layoutDashboardFilters.visibility = View.GONE
        layoutDashboardListHeader.visibility = View.GONE
        progressDashboard.visibility = View.GONE
        layoutDashboardProfile.visibility = View.VISIBLE
        layoutDashboardSettings.visibility = View.GONE
        applyActiveTabStyle()
        updateProfileSection()
    }

    private fun showSettings() {
        hideNotificationsPanel()
        currentTab = DashboardTab.SETTINGS
        layoutDashboardHero.visibility = View.GONE
        etDashboardSearch.text?.clear()
        rvDashboardItems.visibility = View.GONE
        layoutDashboardFilters.visibility = View.GONE
        layoutDashboardListHeader.visibility = View.GONE
        progressDashboard.visibility = View.GONE
        layoutDashboardProfile.visibility = View.GONE
        layoutDashboardSettings.visibility = View.VISIBLE
        applyActiveTabStyle()
    }

    private fun applyActiveTabStyle() {
        fun style(button: Button, isActive: Boolean) {
            button.setBackgroundResource(
                if (isActive) R.drawable.bg_nav_tab_active else R.drawable.bg_nav_tab_inactive
            )
            val tintColor = getColor(if (isActive) R.color.primary_blue_dark else R.color.text_sub)
            button.setTextColor(tintColor)
            button.compoundDrawablesRelative.forEach { drawable ->
                drawable?.mutate()?.setTint(tintColor)
            }
        }

        style(btnTabMarketplace, currentTab == DashboardTab.MARKETPLACE)
        style(btnTabMyListings, currentTab == DashboardTab.MY_LISTINGS)
        style(btnTabProfile, currentTab == DashboardTab.PROFILE)
        style(btnTabSettings, currentTab == DashboardTab.SETTINGS)
    }

    private fun updateProfileSection() {
        if (!sessionManager.isLoggedIn()) {
            tvProfileDisplayName.text = getString(R.string.profile_name_fallback)
            tvProfileFullName.text = getString(R.string.profile_full_name_label, "-")
            tvProfileEmail.text = getString(R.string.profile_email_label, "-")
            tvProfileListingCount.text = getString(R.string.profile_listing_count, 0)
            return
        }

        val profile = sessionManager.getUserProfile()
        val displayName = profile.displayName.ifBlank { getString(R.string.profile_name_fallback) }
        val fullName = profile.fullName.ifBlank { "-" }
        val email = profile.email.ifBlank { "-" }
        val ownCount = allItems.count { isOwnedByCurrentUser(it) }

        tvProfileDisplayName.text = displayName
        tvProfileFullName.text = getString(R.string.profile_full_name_label, fullName)
        tvProfileEmail.text = getString(R.string.profile_email_label, email)
        tvProfileListingCount.text = getString(R.string.profile_listing_count, ownCount)
    }

    private fun logout() {
        sessionManager.clear()
        hideNotificationsPanel()
        notificationItems.clear()
        unreadChatBadgeCount = 0
        updateNotificationBadges(unreadChatCount = 0, bellUnreadCount = 0)
        tvDashboardWelcome.text = getString(R.string.dashboard_welcome_generic)
        etLoginPassword.text?.clear()
        showLanding()
        applyListingFilters()
    }

    private fun handleBackNavigation(): Boolean {
        return when {
            layoutDashboardNotificationsPanel.isVisible -> {
                hideNotificationsPanel()
                true
            }
            layoutDashboard.isVisible && currentTab != DashboardTab.MARKETPLACE -> {
                showMarketplace()
                true
            }
            layoutDashboard.isVisible -> {
                showLanding()
                true
            }
            layoutRegister.isVisible || layoutLogin.isVisible -> {
                showLanding()
                true
            }
            else -> false
        }
    }

    override fun onBackPressed() {
        if (!handleBackNavigation()) {
            super.onBackPressed()
        }
    }
}
