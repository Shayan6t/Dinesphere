package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
    private lateinit var verifyBtn: RelativeLayout
    private lateinit var backButton: ImageView
    private lateinit var otpNotificationBar: RelativeLayout
    private lateinit var otpNotificationText: TextView
    private lateinit var closeNotificationBtn: ImageView
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText
    private lateinit var otp6: EditText

    private var generatedOtp: String? = null
    private var userEmail: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        // Get user email from Intent (passed from profile)
        userEmail = intent.getStringExtra("USER_EMAIL")

        // Initialize views
        emailInput = findViewById(R.id.email_input)
        sendOtpBtn = findViewById(R.id.sendOTP_btn)
        verifyBtn = findViewById(R.id.verify_btn)
        backButton = findViewById(R.id.back)
        otpNotificationBar = findViewById(R.id.otp_notification_bar)
        otpNotificationText = findViewById(R.id.otp_notification_text)
        closeNotificationBtn = findViewById(R.id.close_notification)

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

        // Hide notification initially
        otpNotificationBar.visibility = View.GONE

        // Disable OTP fields initially
        enableOtpFields(false)

        // Setup OTP auto-focus
        setupOtpInputs()

        // Close notification button
        closeNotificationBtn.setOnClickListener {
            otpNotificationBar.visibility = View.GONE
        }

        // Send OTP button
        sendOtpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate a random 6-digit OTP
            generatedOtp = (100000..999999).random().toString()
            userEmail = email

            // Show the OTP notification
            showOtpNotification(generatedOtp!!)

            // Enable OTP input fields
            enableOtpFields(true)
            otp1.requestFocus()

            Toast.makeText(this, "OTP sent successfully", Toast.LENGTH_SHORT).show()
        }

        // Verify OTP button
        verifyBtn.setOnClickListener {
            if (generatedOtp == null) {
                Toast.makeText(this, "Please request OTP first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val otp = otp1.text.toString() + otp2.text.toString() + otp3.text.toString() +
                    otp4.text.toString() + otp5.text.toString() + otp6.text.toString()

            if (otp.length != 6) {
                Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(otp)
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

    private fun showOtpNotification(otp: String) {
        otpNotificationText.text = "Your OTP is: $otp"
        otpNotificationBar.visibility = View.VISIBLE

        // Auto-hide after 15 seconds
        otpNotificationBar.postDelayed({
            otpNotificationBar.visibility = View.GONE
        }, 15000)
    }

    private fun verifyOtp(enteredOtp: String) {
        if (enteredOtp == generatedOtp) {
            Toast.makeText(this, "OTP verified successfully!", Toast.LENGTH_SHORT).show()

            // Hide notification
            otpNotificationBar.visibility = View.GONE

            // Navigate to change password screen with USER_EMAIL
            val intent = Intent(this, change_password::class.java)

            // PASS THE EMAIL HERE
            intent.putExtra("USER_EMAIL", userEmail)

            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
            clearOtpFields()
        }
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