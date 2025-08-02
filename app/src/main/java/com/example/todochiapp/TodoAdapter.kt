package com.example.todochiapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val todoList: List<TodoModel>,
    private val showPriority: Boolean = false
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore = FirebaseFirestore.getInstance()
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Tambahan untuk filter tanggal
    private var selectedDate: String? = null
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun setSelectedDate(date: String?) {
        selectedDate = date
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todoList[position]
        val context = holder.itemView.context

        val todoDate = todo.deadline?.toDate()?.let { dateOnlyFormat.format(it) }

        // Sembunyikan jika tanggal tidak cocok
        if (selectedDate != null && todoDate != selectedDate) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        } else {
            holder.itemView.visibility = View.VISIBLE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = todo.done

        holder.tvTitle.text = todo.title
        holder.tvDeadline.text = "Deadline: " + (todo.deadline?.toDate()?.let {
            dateTimeFormat.format(it)
        } ?: "-")

        // Status Badge
        holder.tvStatus.text = if (todo.done) "Done" else "Progress"
        holder.tvStatus.setBackgroundResource(
            if (todo.done) R.drawable.bg_status_done else R.drawable.bg_status_progress
        )

        // Priority
        if (showPriority && !todo.priority.isNullOrEmpty()) {
            holder.tvPriority.visibility = View.VISIBLE
            holder.tvPriority.text = todo.priority.capitalize(Locale.ROOT)
            holder.tvPriority.setBackgroundResource(
                when (todo.priority.lowercase(Locale.ROOT)) {
                    "tinggi", "high" -> R.drawable.bg_priority_high
                    "sedang", "medium" -> R.drawable.bg_priority_medium
                    "rendah", "low" -> R.drawable.bg_priority_low
                    else -> R.drawable.bg_status_progress
                }
            )
        } else {
            holder.tvPriority.visibility = View.GONE
        }

        // Checkbox logic
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (uid != null && todo.id.isNotEmpty()) {
                firestore.collection("users")
                    .document(uid)
                    .collection("todos")
                    .document(todo.id)
                    .update("done", isChecked)
            }

            holder.tvStatus.text = if (isChecked) "Done" else "Progress"
            holder.tvStatus.setBackgroundResource(
                if (isChecked) R.drawable.bg_status_done else R.drawable.bg_status_progress
            )
        }

        // Edit title
        holder.tvTitle.setOnClickListener {
            val input = EditText(context).apply {
                setText(todo.title)
                inputType = InputType.TYPE_CLASS_TEXT
            }

            AlertDialog.Builder(context)
                .setTitle("Edit Judul To-do")
                .setView(input)
                .setPositiveButton("Simpan") { _, _ ->
                    val newTitle = input.text.toString().trim()
                    if (newTitle.isNotEmpty() && uid != null && todo.id.isNotEmpty()) {
                        firestore.collection("users")
                            .document(uid)
                            .collection("todos")
                            .document(todo.id)
                            .update("title", newTitle)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // Edit deadline
        holder.tvDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            todo.deadline?.toDate()?.let { calendar.time = it }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(context, { _, y, m, d ->
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                TimePickerDialog(context, { _, h, min ->
                    calendar.set(y, m, d, h, min)
                    val timestamp = Timestamp(calendar.time)

                    if (uid != null && todo.id.isNotEmpty()) {
                        firestore.collection("users")
                            .document(uid)
                            .collection("todos")
                            .document(todo.id)
                            .update("deadline", timestamp)
                    }

                }, hour, minute, true).show()

            }, year, month, day).show()
        }

        // Delete
        holder.btnDelete.setOnClickListener {
            if (uid != null && todo.id.isNotEmpty()) {
                firestore.collection("users")
                    .document(uid)
                    .collection("todos")
                    .document(todo.id)
                    .delete()
            }
        }
    }

    override fun getItemCount(): Int = todoList.size

    inner class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTodoTitle)
        val checkBox: CheckBox = view.findViewById(R.id.cbDone)
        val tvDeadline: TextView = view.findViewById(R.id.tvTodoDeadline)
        val tvStatus: TextView = view.findViewById(R.id.tvTodoStatus)
        val tvPriority: TextView = view.findViewById(R.id.tvPriority)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }
}
