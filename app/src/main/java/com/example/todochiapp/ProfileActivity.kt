package com.example.todochiapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnSave: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val user = auth.currentUser
        val uid = user?.uid ?: return

        // Bind View
        etEmail = findViewById(R.id.etEmail)
        etUsername = findViewById(R.id.etUsername)
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)

        // Load data awal
        etEmail.setText(user.email ?: "")
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.getString("username") ?: ""
                etUsername.setText(username)
            }

        // Tombol kembali
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Tombol simpan data
        btnSave.setOnClickListener {
            val newEmail = etEmail.text.toString().trim()
            val newUsername = etUsername.text.toString().trim()

            if (newEmail.isEmpty() || newUsername.isEmpty()) {
                Toast.makeText(this, "Email dan Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update email
            user.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                if (emailTask.isSuccessful) {
                    // Update ke Firestore
                    val updates = mapOf("username" to newUsername)
                    firestore.collection("users").document(uid).update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal menyimpan username", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Gagal memperbarui email. Login ulang mungkin diperlukan.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
