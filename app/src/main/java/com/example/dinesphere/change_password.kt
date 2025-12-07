package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class change_password : AppCompatActivity() {

    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var passwordToggle1: ImageView
    private lateinit var passwordToggle2: ImageView
    private lateinit var changeBtn: RelativeLayout
    private lateinit var backButton: ImageView

    private var userEmail: String? = null
    private var isPassword1Visible = false
    private var isPassword2Visible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Get email from intent
        userEmail = intent.getStringExtra("USER_EMAIL")

        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Email not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        passwordInput = findViewById(R.id.input_password)
        confirmPasswordInput = findViewById(R.id.input_confirm)
        passwordToggle1 = findViewById(R.id.password_toggle1)
        passwordToggle2 = findViewById(R.id.password_toggle2)
        changeBtn = findViewById(R.id.change_btn)
        backButton = findViewById(R.id.back)

        // Password visibility toggles
        passwordToggle1.setOnClickListener {
            isPassword1Visible = !isPassword1Visible
            if (isPassword1Visible) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle1.setImageResource(R.drawable.eye_open)
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle1.setImageResource(R.drawable.hide_eye)
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        passwordToggle2.setOnClickListener {
            isPassword2Visible = !isPassword2Visible
            if (isPassword2Visible) {
                confirmPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle2.setImageResource(R.drawable.eye_open)
            } else {
                confirmPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle2.setImageResource(R.drawable.hide_eye)
            }
            confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
        }

        // Reset Password button
        changeBtn.setOnClickListener {
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(userEmail!!, password)
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun resetPassword(email: String, newPassword: String) {
        val url = "${Global.BASE_URL}reset_password.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_LONG).show()

                        // Navigate to login screen
                        val intent = Intent(this, login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val message = json.optString("message", "Failed to reset password")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                    "new_password" to newPassword
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}