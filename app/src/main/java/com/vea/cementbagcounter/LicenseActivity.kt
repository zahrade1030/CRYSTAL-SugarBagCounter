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
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class LicenseActivity : Activity() {

    private val gold = Color.rgb(212, 175, 55)
    private val backgroundColor = Color.rgb(12, 12, 12)
    private val cardColor = Color.rgb(27, 27, 27)
    private val white = Color.WHITE
    private val gray = Color.rgb(160, 160, 160)
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
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(backgroundColor)
            layoutDirection = if (isEnglish) {
                View.LAYOUT_DIRECTION_LTR
            } else {
                View.LAYOUT_DIRECTION_RTL
            }
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        val languageButton = Button(this).apply {
            text = if (isEnglish) "فارسی" else "English"
            textSize = 13f
            setTextColor(gold)
            isAllCaps = false
            background = roundedBackground(Color.rgb(35, 35, 35), 12)
        }

        root.addView(
            languageButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
            ).apply {
                gravity = Gravity.END
            }
        )

        languageButton.setOnClickListener {
            getSharedPreferences("app_settings", MODE_PRIVATE)
                .edit()
                .putString("language", if (isEnglish) "fa" else "en")
                .apply()
            recreate()
        }

        addSpace(root, 15)

        val title = TextView(this).apply {
            text = "CRYSTAL.BIN"
            textSize = 30f
            setTextColor(gold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        addSpace(root, 8)

        val subtitle = TextView(this).apply {
            text = tr("شمارشگر صنعتی کیسه قند", "Industrial Sugar Bag Counter")
            textSize = 17f
            setTextColor(white)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle)

        addSpace(root, 10)

        val description = TextView(this).apply {
            text = tr(
                "سیستم هوشمند شمارش و مدیریت تولید",
                "Intelligent Production Counting System"
            )
            textSize = 13f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }
        root.addView(description)

        addSpace(root, 24)

        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = roundedBackground(cardColor, 22)
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
            setTextColor(gold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        cardView.addView(heading)

        addSpace(cardView, 10)

        val activationDescription = TextView(this).apply {
            text = tr(
                "برای استفاده از نرم‌افزار، لایسنس اختصاصی دستگاه را وارد کنید.",
                "Enter your device-specific license to unlock the software."
            )
            textSize = 13f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }
        cardView.addView(activationDescription)

        addSpace(cardView, 22)

        val deviceLabel = TextView(this).apply {
            text = tr("شناسه اختصاصی دستگاه", "Device ID")
            setTextColor(gold)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        cardView.addView(deviceLabel)

        addSpace(cardView, 8)

        deviceIdText = TextView(this).apply {
            text = LicenseManager.getDeviceId(this@LicenseActivity)
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(14), dp(12), dp(14))
            typeface = Typeface.MONOSPACE
            background = roundedBackground(Color.rgb(40, 40, 40), 12)
        }
        cardView.addView(deviceIdText)

        addSpace(cardView, 10)

        val copyButton = createButton(tr("کپی شناسه دستگاه", "Copy Device ID"), false)
        cardView.addView(
            copyButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText("Device ID", deviceIdText.text.toString())
            )
            Toast.makeText(
                this,
                tr("شناسه دستگاه کپی شد", "Device ID copied"),
                Toast.LENGTH_SHORT
            ).show()
        }

        addSpace(cardView, 24)

        val licenseLabel = TextView(this).apply {
            text = tr("کد لایسنس", "License Key")
            setTextColor(gold)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        cardView.addView(licenseLabel)

        addSpace(cardView, 8)

        licenseInput = EditText(this).apply {
            hint = tr("کد فعال‌سازی را وارد کنید", "Enter your activation code")
            setTextColor(white)
            setHintTextColor(gray)
            textSize = 12f
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedBackground(Color.rgb(40, 40, 40), 12)
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

        addSpace(cardView, 20)

        activateButton = createButton(
            tr("فعال‌سازی و ورود به برنامه", "Activate & Enter App"),
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

        addSpace(cardView, 14)

        statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(red)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        cardView.addView(statusText)

        addSpace(root, 22)

        val footer = TextView(this).apply {
            text = "CRYSTAL.BIN"
            textSize = 14f
            setTextColor(gold)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(footer)

        addSpace(root, 5)

        val footerDescription = TextView(this).apply {
            text = tr("سیستم هوشمند صنعتی", "Industrial Vision System")
            textSize = 11f
            setTextColor(gray)
            gravity = Gravity.CENTER
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
        statusText.text = tr("در حال بررسی کد...", "Verifying activation code...")

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
                    statusText.text = tr("فعال‌سازی موفق بود", "Activation successful")
                    Toast.makeText(
                        this,
                        tr("فعال‌سازی موفق بود", "Activation successful"),
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
            tr("در حال بررسی لایسنس...", "Verifying license...")
        } else {
            tr("فعال‌سازی و ورود به برنامه", "Activate & Enter App")
        }
        activateButton.alpha = if (loading) 0.6f else 1f
    }

    private fun createButton(text: String, primary: Boolean): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (primary) Color.rgb(12, 12, 12) else gold)
            background = roundedBackground(
                if (primary) gold else Color.rgb(40, 40, 40),
                14
            )
            isAllCaps = false
            minHeight = dp(48)
            minimumHeight = dp(48)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), gold)
        }
    }
}
