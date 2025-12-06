package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class forget_password : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var sendOtpBtn: RelativeLayout
    private lateinit var verifyBtn: RelativeLayout
    private lateinit var backBtn: ImageView

    // Flag to track if the email has been authenticated (either via API or passed from Profile)
    private var isEmailVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forget_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emailInput = findViewById(R.id.email_input)
        sendOtpBtn = findViewById(R.id.sendOTP_btn)
        verifyBtn = findViewById(R.id.change_btn)
        backBtn = findViewById(R.id.back)

        // Fix click stealing
        findViewById<View>(R.id.sendOTP_back).isClickable = false
        findViewById<View>(R.id.change_btn_back).isClickable = false

        backBtn.setOnClickListener { finish() }

        // Initially disable change button
        setVerifyButtonState(false)

        // 1. Check if an email was passed (meaning user came from Profile/authenticated state)
        val incomingEmail = intent.getStringExtra("USER_EMAIL")

        if (!incomingEmail.isNullOrEmpty()) {
            // --- AUTHENTICATED FLOW (SKIP OTP) ---
            emailInput.setText(incomingEmail)
            emailInput.isEnabled = false // Prevent editing the email

            sendOtpBtn.alpha = 0.5f // Visually disable Send OTP
            sendOtpBtn.isEnabled = false

            isEmailVerified = true // Mark as verified since authentication is known
            setVerifyButtonState(true) // Immediately enable Change Password
            showToast("Ready to set new password.")
        }

        // 2. Standard Flow: Send OTP Button
        sendOtpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                // In standard flow, we check existence and simulate sending OTP
                checkUserExists(email, isFinalAction = false)
            } else {
                showToast("Enter email first")
            }
        }

        // 3. Standard Flow: Change Password Button
        verifyBtn.setOnClickListener {
            if (isEmailVerified) {
                val email = emailInput.text.toString().trim()
                // Navigate to Change Password Screen, passing the confirmed email
                val intent = Intent(this, change_password::class.java)
                intent.putExtra("USER_EMAIL", email)
                startActivity(intent)
                finish()
            } else {
                showToast("Please verify OTP first (or send OTP).")
            }
        }
    }

    private fun setVerifyButtonState(enabled: Boolean) {
        verifyBtn.isEnabled = enabled
        verifyBtn.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun checkUserExists(email: String, isFinalAction: Boolean) {
        val url = Global.BASE_URL + "check_user.php"

        // Disable interaction while loading
        sendOtpBtn.isEnabled = false
        sendOtpBtn.alpha = 0.5f

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                sendOtpBtn.isEnabled = true
                sendOtpBtn.alpha = 1.0f

                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    val message = json.optString("message")

                    if (success) {
                        // User found (Success in both standard and final steps)

                        if (isFinalAction) {
                            // This path is only reached if the user successfully verifies OTP (TBD)
                            val intent = Intent(this, change_password::class.java)
                            intent.putExtra("USER_EMAIL", email)
                            startActivity(intent)
                            finish()
                        } else {
                            // Standard flow: User exists, now simulate OTP send
                            showToast("OTP sent to email. Enter the code and verify.")
                            isEmailVerified = true // Simulate OTP verified successfully
                            setVerifyButtonState(true)
                        }
                    } else {
                        showToast(message)
                        setVerifyButtonState(false)
                    }
                } catch (e: Exception) {
                    showToast("Error parsing response")
                    setVerifyButtonState(false)
                }
            },
            Response.ErrorListener {
                sendOtpBtn.isEnabled = true
                sendOtpBtn.alpha = 1.0f
                showToast("Connection failed")
                setVerifyButtonState(false)
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = email
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}