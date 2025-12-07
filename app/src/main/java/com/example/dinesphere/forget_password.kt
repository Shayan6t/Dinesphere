package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
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
    private lateinit var otpInputLayout: LinearLayout
    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText
    private lateinit var otp6: EditText
    private lateinit var sendOtpBtn: RelativeLayout
    private lateinit var changeBtn: RelativeLayout
    private lateinit var backBtn: ImageView

    private var isEmailVerified = false
    private var currentResetToken: String? = null
    private var currentEmail: String = ""
    private var otpVerificationComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forget_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        emailInput = findViewById(R.id.email_input)
        sendOtpBtn = findViewById(R.id.sendOTP_btn)
        changeBtn = findViewById(R.id.change_btn)
        backBtn = findViewById(R.id.back)
        otpInputLayout = findViewById(R.id.OTP_input)

        // Initialize OTP input fields (6 digits)
        otp1 = findViewById(R.id.otp1)
        otp2 = findViewById(R.id.otp2)
        otp3 = findViewById(R.id.otp3)
        otp4 = findViewById(R.id.otp4)
        otp5 = findViewById(R.id.otp5)
        otp6 = findViewById(R.id.otp6)

        // Fix click stealing
        findViewById<View>(R.id.sendOTP_back).isClickable = false
        findViewById<View>(R.id.change_btn_back).isClickable = false

        backBtn.setOnClickListener { finish() }

        // Initially disable change button
        setChangeButtonState(false)

        // Check if an email was passed (authenticated flow from Profile)
        val incomingEmail = intent.getStringExtra("USER_EMAIL")

        if (!incomingEmail.isNullOrEmpty()) {
            // --- AUTHENTICATED FLOW (from Profile) ---
            emailInput.setText(incomingEmail)
            emailInput.isEnabled = false
            sendOtpBtn.alpha = 0.5f
            sendOtpBtn.isEnabled = false
            otpInputLayout.visibility = View.GONE
            isEmailVerified = true
            otpVerificationComplete = true
            currentEmail = incomingEmail
            setChangeButtonState(true)
            updateChangeButtonText("Change Password")
            showToast("Ready to set new password")
        }

        // Send OTP Button
        sendOtpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                generateOTP(email)
            } else {
                showToast("Enter your email first")
            }
        }

        // Change Password Button - Does different things based on flow state
        changeBtn.setOnClickListener {
            if (otpVerificationComplete) {
                // OTP already verified, proceed to change password
                navigateToChangePassword()
            } else if (otpInputLayout.visibility == View.VISIBLE) {
                // OTP input is visible, user wants to verify OTP
                val otp = getOTPFromInputs()
                if (otp.length == 6) {
                    verifyOTP(currentEmail, otp)
                } else {
                    showToast("Enter valid 6-digit OTP")
                }
            }
        }
    }

    private fun generateOTP(email: String) {
        val url = Global.BASE_URL + "generate_otp.php"

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
                        currentEmail = email
                        otpInputLayout.visibility = View.VISIBLE
                        updateChangeButtonText("Verify OTP")
                        setChangeButtonState(true)
                        showToast("OTP sent to your email")
                        focusFirstOTPField()
                    } else {
                        showToast(message)
                    }
                } catch (e: Exception) {
                    showToast("Error: Could not parse response")
                }
            },
            Response.ErrorListener {
                sendOtpBtn.isEnabled = true
                sendOtpBtn.alpha = 1.0f
                showToast("Connection failed")
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

    private fun verifyOTP(email: String, otp: String) {
        val url = Global.BASE_URL + "verify_otp.php"

        changeBtn.isEnabled = false
        changeBtn.alpha = 0.5f

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                changeBtn.isEnabled = true
                changeBtn.alpha = 1.0f

                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    val message = json.optString("message")

                    if (success) {
                        currentResetToken = json.optString("reset_token")
                        isEmailVerified = true
                        otpVerificationComplete = true
                        updateChangeButtonText("Change Password")
                        setChangeButtonState(true)
                        showToast("OTP verified! Ready to reset password")
                    } else {
                        showToast(message)
                        clearOTPInputs()
                    }
                } catch (e: Exception) {
                    showToast("Error: Could not parse response")
                }
            },
            Response.ErrorListener {
                changeBtn.isEnabled = true
                changeBtn.alpha = 1.0f
                showToast("Verification failed")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = email
                params["otp"] = otp
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun navigateToChangePassword() {
        val intent = Intent(this, change_password::class.java)
        intent.putExtra("USER_EMAIL", currentEmail)
        intent.putExtra("RESET_TOKEN", currentResetToken)
        startActivity(intent)
        finish()
    }

    private fun getOTPFromInputs(): String {
        val otp = otp1.text.toString().trim() +
                otp2.text.toString().trim() +
                otp3.text.toString().trim() +
                otp4.text.toString().trim() +
                otp5.text.toString().trim() +
                otp6.text.toString().trim()
        return otp
    }

    private fun clearOTPInputs() {
        otp1.text.clear()
        otp2.text.clear()
        otp3.text.clear()
        otp4.text.clear()
        otp5.text.clear()
        otp6.text.clear()
        focusFirstOTPField()
    }

    private fun focusFirstOTPField() {
        otp1.requestFocus()
    }

    private fun setChangeButtonState(enabled: Boolean) {
        changeBtn.isEnabled = enabled
        changeBtn.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun updateChangeButtonText(text: String) {
        val changeText: TextView = findViewById(R.id.change_text)
        changeText.text = text
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}