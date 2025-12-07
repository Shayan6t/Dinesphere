package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.InputType
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

class change_password : AppCompatActivity() {

    private lateinit var inputPassword: EditText
    private lateinit var inputConfirm: EditText
    private lateinit var changeBtn: RelativeLayout
    private lateinit var backBtn: ImageView
    private lateinit var passwordToggle1: ImageView
    private lateinit var passwordToggle2: ImageView

    private var isPassword1Visible = false
    private var isPassword2Visible = false

    private var currentEmail: String = ""
    private var resetToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        inputPassword = findViewById(R.id.input_password)
        inputConfirm = findViewById(R.id.input_confirm)
        changeBtn = findViewById(R.id.change_btn)
        backBtn = findViewById(R.id.back)
        passwordToggle1 = findViewById(R.id.password_toggle1)
        passwordToggle2 = findViewById(R.id.password_toggle2)

        // Prevent click stealing
        findViewById<android.widget.ImageButton>(R.id.change_btn_back).isClickable = false

        // Get passed data
        currentEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        resetToken = intent.getStringExtra("RESET_TOKEN")

        // Back Button
        backBtn.setOnClickListener {
            finish()
        }

        // Password Toggle 1
        passwordToggle1.setOnClickListener {
            togglePasswordVisibility(inputPassword, passwordToggle1, isPassword1Visible)
            isPassword1Visible = !isPassword1Visible
        }

        // Password Toggle 2
        passwordToggle2.setOnClickListener {
            togglePasswordVisibility(inputConfirm, passwordToggle2, isPassword2Visible)
            isPassword2Visible = !isPassword2Visible
        }

        // Change Password Button
        changeBtn.setOnClickListener {
            handlePasswordReset()
        }
    }

    private fun togglePasswordVisibility(
        inputField: EditText,
        toggleIcon: ImageView,
        isVisible: Boolean
    ) {
        if (isVisible) {
            // Hide password
            inputField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.alpha = 0.4f
        } else {
            // Show password
            inputField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.alpha = 1.0f
        }
        inputField.setSelection(inputField.text.length)
    }

    private fun handlePasswordReset() {
        val password = inputPassword.text.toString().trim()
        val confirmPassword = inputConfirm.text.toString().trim()

        // Validation
        if (password.isEmpty()) {
            showToast("Please enter a password")
            inputPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            showToast("Please confirm your password")
            inputConfirm.requestFocus()
            return
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match")
            return
        }

        // Determine which flow we're in
        if (!resetToken.isNullOrEmpty()) {
            // OTP Flow: Use reset token
            resetPasswordWithToken(password)
        } else if (currentEmail.isNotEmpty()) {
            // Authenticated Flow: Direct password update (for logged-in users)
            updatePasswordDirect(password)
        } else {
            showToast("Error: Missing required information")
        }
    }

    private fun resetPasswordWithToken(newPassword: String) {
        val url = Global.BASE_URL + "confirm_password_reset.php"

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
                        showToast("Password reset successfully!")
                        // Navigate back to login
                        val intent = Intent(this, login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        showToast(message)
                    }
                } catch (e: Exception) {
                    showToast("Error: Could not parse response")
                }
            },
            Response.ErrorListener {
                changeBtn.isEnabled = true
                changeBtn.alpha = 1.0f
                showToast("Connection failed")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["reset_token"] = resetToken ?: ""
                params["new_password"] = newPassword
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun updatePasswordDirect(newPassword: String) {
        val url = Global.BASE_URL + "update_password.php"

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
                        showToast("Password changed successfully!")
                        finish()
                    } else {
                        showToast(message)
                    }
                } catch (e: Exception) {
                    showToast("Error: Could not parse response")
                }
            },
            Response.ErrorListener {
                changeBtn.isEnabled = true
                changeBtn.alpha = 1.0f
                showToast("Connection failed")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = currentEmail
                params["password"] = newPassword
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}