package com.example.healthsync

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.healthsync.databinding.ActivityOtpBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OTP : AppCompatActivity() {

    private lateinit var otpBinding: ActivityOtpBinding
    private lateinit var mAuth: FirebaseAuth
    private var resendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var verificationCode: String? = null
    private var timeoutSeconds = 60L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        otpBinding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(otpBinding.root)

        mAuth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        otpBinding.sendOtp.setOnClickListener {
            val phoneNumber = otpBinding.mobileEditText.text.toString()
            if (phoneNumber.isNotEmpty()) {
                sendOtp(phoneNumber, false)
            } else {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        otpBinding.loginButton.setOnClickListener {
            val enteredOtp = otpBinding.otpText.text.toString()
            if (verificationCode != null && enteredOtp.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(verificationCode!!, enteredOtp)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
            }
        }

//        otpBinding.resendBtn.setOnClickListener {
//            val phoneNumber = otpBinding.mobileEditText.text.toString()
//            if (phoneNumber.isNotEmpty()) {
//                sendOtp(phoneNumber, true)
//            } else {
//                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    private fun sendOtp(phoneNumber: String, isResend: Boolean) {
        setInProgress(true)

        val formattedPhoneNumber = if (!phoneNumber.startsWith("+91")) {
            "+91$phoneNumber"
        } else {
            phoneNumber
        }

        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(formattedPhoneNumber)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                    setInProgress(false)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        applicationContext,
                        "OTP verification failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    setInProgress(false)
                }

                override fun onCodeSent(
                    s: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(s, forceResendingToken)
                    verificationCode = s
                    resendingToken = forceResendingToken
                    Toast.makeText(applicationContext, "OTP sent successfully", Toast.LENGTH_SHORT)
                        .show()
                    setInProgress(false)

                    otpBinding.otpText.visibility = View.VISIBLE
                    otpBinding.loginButton.visibility = View.VISIBLE
                    otpBinding.mobileNum.visibility = View.GONE
                    otpBinding.sendOtp.visibility = View.GONE
                }
            })

        if (isResend) {
            resendingToken?.let {
                PhoneAuthProvider.verifyPhoneNumber(options.setForceResendingToken(it).build())
            } ?: run {
                Toast.makeText(this, "Resending token is null", Toast.LENGTH_SHORT).show()
                setInProgress(false)
            }
        } else {
            PhoneAuthProvider.verifyPhoneNumber(options.build())
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        setInProgress(true)
        mAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            setInProgress(false)
            if (task.isSuccessful) {
                Toast.makeText(this, "Sign-in successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomePage::class.java))
            } else {
                Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            otpBinding.loginButton.visibility = View.GONE
        } else {
            otpBinding.loginButton.visibility = View.VISIBLE
        }
    }
}
