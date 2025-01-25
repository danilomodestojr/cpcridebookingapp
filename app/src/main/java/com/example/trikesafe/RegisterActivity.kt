package com.example.trikesafe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var roleGroup: RadioGroup
    private lateinit var driverLicenseInput: EditText
    private lateinit var tricycleNumberInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // These must match IDs from the above XML
        usernameInput = findViewById(R.id.usernameInput)
        nameInput = findViewById(R.id.nameInput)
        lastNameInput = findViewById(R.id.lastNameInput)
        phoneInput = findViewById(R.id.phoneInput)
        emailInput = findViewById(R.id.regEmailInput)
        passwordInput = findViewById(R.id.regPasswordInput)
        roleGroup = findViewById(R.id.roleGroup)
        driverLicenseInput = findViewById(R.id.driverLicenseInput)
        tricycleNumberInput = findViewById(R.id.tricycleNumberInput)

        roleGroup.setOnCheckedChangeListener { _, checkedId ->
            val isDriver = (checkedId == R.id.driverRadio)
            driverLicenseInput.visibility = if (isDriver) View.VISIBLE else View.GONE
            tricycleNumberInput.visibility = if (isDriver) View.VISIBLE else View.GONE
        }

        val registerButton = findViewById<Button>(R.id.registerButton)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        registerButton.setOnClickListener {
            val role = if (roleGroup.checkedRadioButtonId == R.id.driverRadio) "Driver" else "Passenger"

            if (validateInputs(role)) {
                val userData = UserData(
                    username = usernameInput.text.toString(),
                    name = nameInput.text.toString(),
                    last_name = lastNameInput.text.toString(),
                    phone = phoneInput.text.toString(),
                    email = emailInput.text.toString(),
                    password = passwordInput.text.toString(),
                    role = role,
                    driver_license = if (role == "Driver") driverLicenseInput.text.toString() else null,
                    tricycle_number = if (role == "Driver") tricycleNumberInput.text.toString() else null
                )



                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val response = ApiClient.api.registerUser(userData)
                        if (response.isSuccessful) {
                            Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validateInputs(role: String): Boolean {
        if (usernameInput.text.isBlank()) {
            usernameInput.error = "Required"
            return false
        }
        if (nameInput.text.isBlank()) {
            nameInput.error = "Required"
            return false
        }
        if (lastNameInput.text.isBlank()) {
            lastNameInput.error = "Required"
            return false
        }
        if (phoneInput.text.isBlank()) {
            phoneInput.error = "Required"
            return false
        }
        if (emailInput.text.isBlank()) {
            emailInput.error = "Required"
            return false
        }
        if (passwordInput.text.isBlank()) {
            passwordInput.error = "Required"
            return false
        }

        // If "driver," also require the driverLicense and tricycleNumber
        if (role == "Driver") {
            if (driverLicenseInput.text.isBlank()) {
                driverLicenseInput.error = "Required for drivers"
                return false
            }
            if (tricycleNumberInput.text.isBlank()) {
                tricycleNumberInput.error = "Required for drivers"
                return false
            }
        }

        return true
    }
}
