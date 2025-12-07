package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class forget_password : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var sendOtpBtn: RelativeLayout
    private lateinit var changeBtn: RelativeLayout
    private lateinit var backButton: ImageView

    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText
    private lateinit var otp6: EditText

    private var userEmail: String? = null
    private var isOtpSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        // Get email from intent (if passed from profile)
        userEmail = intent.getStringExtra("USER_EMAIL")

        // Initialize views
        emailInput = findViewById(R.id.email_input)
        sendOtpBtn = findViewById(R.id.sendOTP_btn)
        changeBtn = findViewById(R.id.change_btn)
        backButton = findViewById(R.id.back)

        otp1 = findViewById(R.id.otp1)
        otp2 = findViewById(R.id.otp2)
        otp3 = findViewById(R.id.otp3)
        otp4 = findViewById(R.id.otp4)
        otp5 = findViewById(R.id.otp5)
        otp6 = findViewById(R.id.otp6)

        // Pre-fill email if available
        if (!userEmail.isNullOrEmpty()) {
            emailInput.setText(userEmail)
        }

        // Disable OTP fields initially
        enableOtpFields(false)

        // Setup OTP auto-focus
        setupOtpInputs()

        // Send OTP button
        sendOtpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendOtp(email)
        }

        // Verify OTP and navigate to change password
        changeBtn.setOnClickListener {
            if (!isOtpSent) {
                Toast.makeText(this, "Please request OTP first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val otp = otp1.text.toString() + otp2.text.toString() + otp3.text.toString() +
                    otp4.text.toString() + otp5.text.toString() + otp6.text.toString()

            if (otp.length != 6) {
                Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(emailInput.text.toString().trim(), otp)
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupOtpInputs() {
        val otpFields = arrayOf(otp1, otp2, otp3, otp4, otp5, otp6)

        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        // Move to next field
                        if (i < otpFields.size - 1) {
                            otpFields[i + 1].requestFocus()
                        }
                    } else if (s?.isEmpty() == true) {
                        // Move to previous field on backspace
                        if (i > 0) {
                            otpFields[i - 1].requestFocus()
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun sendOtp(email: String) {
        val url = "${Global.BASE_URL}send_otp.php"

        android.util.Log.d("ForgetPassword", "Sending OTP to: $email")
        android.util.Log.d("ForgetPassword", "API URL: $url")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                android.util.Log.d("ForgetPassword", "Response: $response")
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        isOtpSent = true
                        Toast.makeText(this, "OTP sent to your email", Toast.LENGTH_LONG).show()
                        // Enable OTP input fields
                        enableOtpFields(true)
                        otp1.requestFocus()
                    } else {
                        val message = json.optString("message", "Failed to send OTP")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        android.util.Log.e("ForgetPassword", "Error: $message")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("ForgetPassword", "Parse Error: ${e.message}")
                    android.util.Log.e("ForgetPassword", "Raw Response: $response")
                }
            },
            { error ->
                val errorMsg = error.networkResponse?.let {
                    "Status: ${it.statusCode}, Data: ${String(it.data)}"
                } ?: "Network error: ${error.message}"

                Toast.makeText(this, "Network error. Check connection.", Toast.LENGTH_SHORT).show()
                android.util.Log.e("ForgetPassword", "Network Error: $errorMsg")
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("email" to email)
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun verifyOtp(email: String, otp: String) {
        val url = "${Global.BASE_URL}verify_otp.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        Toast.makeText(this, "OTP verified successfully", Toast.LENGTH_SHORT).show()

                        // Navigate to change password screen
                        val intent = Intent(this, change_password::class.java)
                        intent.putExtra("USER_EMAIL", email)
                        startActivity(intent)
                        finish()
                    } else {
                        val message = json.optString("message", "Invalid OTP")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        clearOtpFields()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf(
                    "email" to email,
                    "otp" to otp
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun enableOtpFields(enable: Boolean) {
        otp1.isEnabled = enable
        otp2.isEnabled = enable
        otp3.isEnabled = enable
        otp4.isEnabled = enable
        otp5.isEnabled = enable
        otp6.isEnabled = enable
    }

    private fun clearOtpFields() {
        otp1.text.clear()
        otp2.text.clear()
        otp3.text.clear()
        otp4.text.clear()
        otp5.text.clear()
        otp6.text.clear()
        otp1.requestFocus()
    }
}