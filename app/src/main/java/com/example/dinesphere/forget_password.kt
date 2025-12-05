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

        // "Send OTP" acts as Check User
        sendOtpBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                checkUserExists(email, isFinalAction = false)
            } else {
                showToast("Enter email first")
            }
        }

        // "Change Password" Button - moves to next screen
        verifyBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                checkUserExists(email, isFinalAction = true)
            } else {
                showToast("Enter email first")
            }
        }
    }

    private fun checkUserExists(email: String, isFinalAction: Boolean) {
        val url = Global.BASE_URL + "check_user.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    val message = json.optString("message")

                    if (success) {
                        if (isFinalAction) {
                            // User exists, move to Change Password screen
                            val intent = Intent(this, change_password::class.java)
                            intent.putExtra("USER_EMAIL", email) // Pass email to next activity
                            startActivity(intent)
                            finish()
                        } else {
                            showToast("User found! Please verify OTP (Simulated)")
                        }
                    } else {
                        showToast(message) // e.g., "Email not found"
                    }
                } catch (e: Exception) {
                    showToast("Error parsing response")
                }
            },
            Response.ErrorListener {
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

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}