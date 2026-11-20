package com.vea.cementbagcounter

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class LicenseActivity : Activity() {

    private val gold = Color.rgb(212, 175, 55)
    private val lightGold = Color.rgb(235, 205, 120)
    private val backgroundColor = Color.rgb(12, 12, 12)
    private val cardColor = Color.rgb(24, 25, 27)
    private val inputColor = Color.rgb(36, 37, 40)
    private val white = Color.WHITE
    private val gray = Color.rgb(155, 158, 162)
    private val red = Color.rgb(235, 75, 75)
    private val green = Color.rgb(75, 210, 120)

    private lateinit var deviceIdText: TextView
    private lateinit var licenseInput: EditText
    private lateinit var statusText: TextView
    private lateinit var activateButton: Button

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activationInProgress = false

    private val isEnglish: Boolean
        get() = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getString("language", "fa") == "en"

    private fun tr(fa: String, en: String): String {
        return if (isEnglish) en else fa
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val allowRenewal = intent.getBooleanExtra("allowRenewal", false)

        if (LicenseManager.isActivated(this) && !allowRenewal) {
            openMainActivity()
            return
        }

        createUI()
    }

    private fun createUI() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(20))
            setBackgroundColor(backgroundColor)
            layoutDirection = if (isEnglish) {
                View.LAYOUT_DIRECTION_LTR
            } else {
                View.LAYOUT_DIRECTION_RTL
            }
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false

            addView(
                root,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = TextView(this).apply {
            text = "CRYSTAL.BIN"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(gold)
            gravity = Gravity.CENTER_VERTICAL
        }

        topBar.addView(
            brand,
            LinearLayout.LayoutParams(0, dp(45), 1f)
        )

        val languageButton = createSmallButton(
            if (isEnglish) "فارسی" else "English"
        )

        topBar.addView(
            languageButton,
            LinearLayout.LayoutParams(
                dp(90),
                dp(42)
            )
        )

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )

        languageButton.setOnClickListener {
            getSharedPreferences("app_settings", MODE_PRIVATE)
                .edit()
                .putString("language", if (isEnglish) "fa" else "en")
                .apply()

            recreate()
        }

        addSpace(root, 18)

        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val logo = TextView(this).apply {
            text = "C"
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(gold)
            gravity = Gravity.CENTER
            background = createCircleBackground(Color.rgb(28, 28, 28), gold)
        }

        headerContainer.addView(
            logo,
            LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                gravity = Gravity.CENTER
            }
        )

        addSpace(headerContainer, 14)

        val title = TextView(this).apply {
            text = "CRYSTAL.BIN"
            textSize = 28f
            setTextColor(gold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }

        headerContainer.addView(title)

        addSpace(headerContainer, 8)

        val subtitle = TextView(this).apply {
            text = tr(
                "شمارشگر هوشمند صنعتی کیسه قند",
                "Industrial Smart Sugar Bag Counter"
            )
            textSize = 15f
            setTextColor(white)
            gravity = Gravity.CENTER
        }

        headerContainer.addView(subtitle)

        addSpace(headerContainer, 6)

        val description = TextView(this).apply {
            text = tr(
                "سیستم هوشمند شمارش و مدیریت تولید",
                "Intelligent Production Counting System"
            )
            textSize = 12f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }

        headerContainer.addView(description)

        root.addView(
            headerContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(root, 22)

        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = roundedBackground(cardColor, 22, Color.rgb(65, 65, 65))
            layoutDirection = if (isEnglish) {
                View.LAYOUT_DIRECTION_LTR
            } else {
                View.LAYOUT_DIRECTION_RTL
            }
        }

        root.addView(
            cardView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val heading = TextView(this).apply {
            text = tr("فعال‌سازی نرم‌افزار", "Software Activation")
            textSize = 21f
            setTextColor(lightGold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        cardView.addView(heading)

        addSpace(cardView, 12)

        val activationDescription = TextView(this).apply {
            text = tr(
                "برای استفاده از نرم‌افزار، لایسنس اختصاصی دستگاه را وارد کنید.",
                "Enter your device-specific license to unlock the software."
            )
            textSize = 13f
            setTextColor(gray)
            gravity = Gravity.CENTER
            setPadding(dp(4), 0, dp(4), 0)
        }

        cardView.addView(activationDescription)

        addSpace(cardView, 24)

        val deviceLabel = TextView(this).apply {
            text = tr("شناسه اختصاصی دستگاه", "DEVICE ID")
            setTextColor(gold)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }

        cardView.addView(deviceLabel)

        addSpace(cardView, 10)

        deviceIdText = TextView(this).apply {
            text = LicenseManager.getDeviceId(this@LicenseActivity)
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(16), dp(12), dp(16))
            typeface = Typeface.MONOSPACE
            background = roundedBackground(inputColor, 12, Color.rgb(65, 65, 65))
        }

        cardView.addView(
            deviceIdText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(cardView, 12)

        val copyButton = createButton(
            tr("کپی شناسه دستگاه", "Copy Device ID"),
            false
        )

        cardView.addView(
            copyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        copyButton.setOnClickListener {
            val clipboard =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Device ID",
                    deviceIdText.text.toString()
                )
            )

            Toast.makeText(
                this,
                tr("شناسه دستگاه کپی شد", "Device ID copied"),
                Toast.LENGTH_SHORT
            ).show()
        }

        addSpace(cardView, 24)

        val licenseLabel = TextView(this).apply {
            text = tr("کد لایسنس", "LICENSE KEY")
            setTextColor(gold)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }

        cardView.addView(licenseLabel)

        addSpace(cardView, 10)

        licenseInput = EditText(this).apply {
            hint = tr(
                "کد فعال‌سازی را وارد کنید",
                "Enter your activation code"
            )
            setTextColor(white)
            setHintTextColor(gray)
            textSize = 13f
            setPadding(dp(15), dp(15), dp(15), dp(15))
            background = roundedBackground(inputColor, 12, Color.rgb(65, 65, 65))
            isSingleLine = false
            minLines = 3
            maxLines = 6
            gravity = Gravity.TOP or Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        cardView.addView(
            licenseInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(cardView, 22)

        activateButton = createButton(
            tr("فعال‌سازی و ورود به برنامه", "ACTIVATE & ENTER"),
            true
        )

        cardView.addView(
            activateButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        )

        activateButton.setOnClickListener {
            activateLicense()
        }

        addSpace(cardView, 12)

        statusText = TextView(this).apply {
            text = ""
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(red)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }

        cardView.addView(statusText)

        addSpace(root, 24)

        val footerDivider = View(this).apply {
            setBackgroundColor(Color.rgb(45, 45, 45))
        }

        root.addView(
            footerDivider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        addSpace(root, 16)

        val footer = TextView(this).apply {
            text = "CRYSTAL.BIN"
            textSize = 13f
            setTextColor(gold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }

        root.addView(footer)

        addSpace(root, 5)

        val footerDescription = TextView(this).apply {
            text = tr(
                "سیستم هوشمند صنعتی",
                "INDUSTRIAL VISION SYSTEM"
            )
            textSize = 10f
            setTextColor(gray)
            gravity = Gravity.CENTER
            letterSpacing = 0.05f
        }

        root.addView(footerDescription)

        setContentView(scroll)
    }

    private fun activateLicense() {

        if (activationInProgress) return

        val code = licenseInput.text.toString().trim()

        statusText.text = ""
        statusText.setTextColor(red)

        if (code.isEmpty()) {
            statusText.text = tr(
                "لطفاً کد فعال‌سازی را وارد کنید",
                "Please enter your activation code"
            )
            return
        }

        activationInProgress = true
        setLoading(true)

        statusText.text = tr(
            "در حال بررسی کد...",
            "Verifying activation code..."
        )

        Thread({
            val success = try {
                LicenseManager.activate(this, code)
            } catch (_: Exception) {
                false
            }

            mainHandler.post {
                if (isFinishing || isDestroyed) return@post

                activationInProgress = false
                setLoading(false)

                if (success) {
                    statusText.setTextColor(green)
                    statusText.text = tr(
                        "فعال‌سازی موفق بود",
                        "Activation successful"
                    )

                    Toast.makeText(
                        this,
                        tr(
                            "فعال‌سازی موفق بود",
                            "Activation successful"
                        ),
                        Toast.LENGTH_LONG
                    ).show()

                    openMainActivity()
                } else {
                    statusText.setTextColor(red)

                    statusText.text = if (
                        LicenseManager.getStatus(this@LicenseActivity) ==
                            LicenseManager.LicenseStatus.CLOCK_TAMPERED
                    ) {
                        tr(
                            "تغییر ساعت دستگاه شناسایی شده است. تاریخ و ساعت را اصلاح کنید؛ اگر قفل باقی ماند، نصب مجدد برنامه لازم است.",
                            "Clock tampering was detected. Correct the device date and time; if the lock remains, reinstalling the app may be required."
                        )
                    } else {
                        tr(
                            "کد نامعتبر است، منقضی شده یا متعلق به دستگاه دیگری است",
                            "Invalid, expired, or belongs to another device"
                        )
                    }
                }
            }
        }, "LicenseVerification").start()
    }

    private fun setLoading(loading: Boolean) {

        activateButton.isEnabled = !loading

        activateButton.text = if (loading) {
            tr(
                "در حال بررسی لایسنس...",
                "VERIFYING LICENSE..."
            )
        } else {
            tr(
                "فعال‌سازی و ورود به برنامه",
                "ACTIVATE & ENTER"
            )
        }

        activateButton.alpha = if (loading) 0.6f else 1f
    }

    private fun createButton(text: String, primary: Boolean): Button {

        return Button(this).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD

            setTextColor(
                if (primary) Color.rgb(15, 15, 15)
                else lightGold
            )

            background = roundedBackground(
                if (primary) gold else Color.rgb(38, 39, 42),
                14,
                if (primary) gold else Color.rgb(75, 75, 75)
            )

            isAllCaps = false
            minHeight = dp(48)
            minimumHeight = dp(48)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
    }

    private fun createSmallButton(text: String): Button {

        return Button(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBackground(
                Color.rgb(35, 35, 35),
                12,
                Color.rgb(75, 75, 75)
            )
            isAllCaps = false
            minHeight = dp(40)
            minimumHeight = dp(40)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
    }

    private fun openMainActivity() {

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun addSpace(parent: LinearLayout, height: Int) {

        parent.addView(
            View(this),
            LinearLayout.LayoutParams(dp(1), dp(height))
        )
    }

    private fun createCircleBackground(
        color: Int,
        strokeColor: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(2), strokeColor)
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int,
        strokeColor: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), strokeColor)
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return roundedBackground(
            color,
            radius,
            gold
        )
    }
}
