package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.InputType
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

class change_password : AppCompatActivity() {

    private lateinit var inputPass: EditText
    private lateinit var inputConfirm: EditText
    private lateinit var resetBtn: RelativeLayout
    private lateinit var backBtn: ImageView
    private lateinit var toggle1: ImageView
    private lateinit var toggle2: ImageView

    private var isPass1Visible = false
    private var isPass2Visible = false
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get Email passed from previous screen
        userEmail = intent.getStringExtra("USER_EMAIL")
        if (userEmail == null) {
            Toast.makeText(this, "Error: No email provided", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        inputPass = findViewById(R.id.input_password)
        inputConfirm = findViewById(R.id.input_confirm)
        resetBtn = findViewById(R.id.change_btn)
        backBtn = findViewById(R.id.back)
        toggle1 = findViewById(R.id.password_toggle1)
        toggle2 = findViewById(R.id.password_toggle2)

        findViewById<View>(R.id.change_btn_back).isClickable = false

        backBtn.setOnClickListener { finish() }

        toggle1.setOnClickListener { isPass1Visible = toggleVisibility(inputPass, toggle1, isPass1Visible) }
        toggle2.setOnClickListener { isPass2Visible = toggleVisibility(inputConfirm, toggle2, isPass2Visible) }

        resetBtn.setOnClickListener {
            handlePasswordReset()
        }
    }

    private fun handlePasswordReset() {
        val pass = inputPass.text.toString().trim()
        val confirm = inputConfirm.text.toString().trim()

        if (pass.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass != confirm) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        updatePasswordInDB(pass)
    }

    private fun updatePasswordInDB(newPassword: String) {
        val url = Global.BASE_URL + "update_password.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    val message = json.optString("message")

                    if (success) {
                        Toast.makeText(this, "Password Updated Successfully!", Toast.LENGTH_LONG).show()
                        // Go back to Login
                        val intent = Intent(this, login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Failed: $message", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing server response", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = userEmail!!
                params["password"] = newPassword
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }

    private fun toggleVisibility(editText: EditText, toggleIcon: ImageView, isVisible: Boolean): Boolean {
        if (!isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.alpha = 1.0f
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.alpha = 0.4f
        }
        editText.setSelection(editText.text.length)
        return !isVisible
    }
}