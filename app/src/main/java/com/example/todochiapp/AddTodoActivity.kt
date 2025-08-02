package com.example.todochiapp

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddTodoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etTitle: EditText
    private lateinit var etDeadline: EditText
    private lateinit var btnSave: Button
    private lateinit var spinnerPriority: Spinner

    private var selectedCalendar: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_todo)

        // View binding manual
        etTitle = findViewById(R.id.etTodoTitle)
        etDeadline = findViewById(R.id.etTodoDeadline)
        btnSave = findViewById(R.id.btnSaveTodo)
        spinnerPriority = findViewById(R.id.spinnerPriority)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Silakan login ulang.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup Spinner Priority
        val priorityAdapter = ArrayAdapter.createFromResource(
            this, R.array.priority_levels, android.R.layout.simple_spinner_item
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter

        // Disable keyboard popup
        etDeadline.inputType = InputType.TYPE_NULL
        etDeadline.setOnClickListener { showDateTimePicker() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val calendar = selectedCalendar
            val priority = spinnerPriority.selectedItem.toString()

            if (title.isEmpty()) {
                Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (calendar == null) {
                Toast.makeText(this, "Pilih deadline terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val deadlineTimestamp = Timestamp(calendar.time)

            val todo = mapOf(
                "title" to title,
                "done" to false,
                "timestamp" to System.currentTimeMillis(),
                "deadline" to deadlineTimestamp,
                "priority" to priority // ✅ Tambahkan prioritas ke Firestore
            )

            firestore.collection("users")
                .document(uid)
                .collection("todos")
                .add(todo)
                .addOnSuccessListener {
                    setReminder(title, calendar)
                    Toast.makeText(this, "To-do ditambahkan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("AddTodo", "Gagal menyimpan: ${e.message}", e)
                    Toast.makeText(this, "Gagal menyimpan to-do", Toast.LENGTH_SHORT).show()
                }
        }

        val btnCancel = findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Batalkan To-do?")
                .setMessage("Data yang telah diisi akan hilang. Yakin ingin membatalkan?")
                .setPositiveButton("Ya") { _, _ ->
                    Toast.makeText(this, "Dibatalkan", Toast.LENGTH_SHORT).show()
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                .setNegativeButton("Tidak", null)
                .show()
        }

    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, h, min ->
                val picked = Calendar.getInstance().apply {
                    set(y, m, d, h, min, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                selectedCalendar = picked
                val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(picked.time)
                etDeadline.setText(formatted)
            }, hour, minute, true).show()

        }, year, month, day).show()
    }

    private fun setReminder(title: String, calendar: Calendar) {
        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            Log.w("Reminder", "Reminder tidak diset karena waktu sudah lewat: ${calendar.time}")
            return
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("title", title)
        }

        val requestCode = calendar.timeInMillis.toInt() // unik berdasarkan waktu
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d("Reminder", "Reminder berhasil diset untuk ${calendar.time}")
    }
}
