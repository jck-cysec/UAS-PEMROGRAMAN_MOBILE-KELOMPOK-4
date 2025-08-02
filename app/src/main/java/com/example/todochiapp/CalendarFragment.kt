package com.example.todochiapp

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var rvTodos: RecyclerView
    private lateinit var todoList: MutableList<TodoModel>
    private lateinit var filteredList: MutableList<TodoModel>
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var uid: String = ""
    private var listenerRegistration: ListenerRegistration? = null
    private var selectedDateStr: String = ""

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val viewDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        // Inisialisasi View
        calendarView = view.findViewById(R.id.calendarView)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        rvTodos = view.findViewById(R.id.rvTodosCalendar)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Silakan login ulang", Toast.LENGTH_SHORT).show()
            return view
        } else {
            uid = user.uid
        }

        // Siapkan RecyclerView dan Adapter
        todoList = mutableListOf()
        filteredList = mutableListOf()
        todoAdapter = TodoAdapter(filteredList, showPriority = true)
        rvTodos.layoutManager = LinearLayoutManager(requireContext())
        rvTodos.adapter = todoAdapter

        // Set default hari ini
        val today = Calendar.getInstance().time
        selectedDateStr = dateFormat.format(today)
        tvSelectedDate.text = "Tugas pada tanggal: ${viewDateFormat.format(today)}"
        todoAdapter.setSelectedDate(selectedDateStr)

        // Load semua todos untuk difilter
        loadAllTodos()

        // Saat user memilih tanggal di kalender
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            val selectedDate = calendar.time
            selectedDateStr = dateFormat.format(selectedDate)

            tvSelectedDate.text = "Tugas pada tanggal: ${viewDateFormat.format(selectedDate)}"

            todoAdapter.setSelectedDate(selectedDateStr)
            filterTodosByDate()
        }

        return view
    }

    private fun loadAllTodos() {
        listenerRegistration?.remove()

        val query = firestore.collection("users")
            .document(uid)
            .collection("todos")

        listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Gagal memuat: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            todoList.clear()
            snapshots?.forEach { doc ->
                val todo = doc.toObject(TodoModel::class.java).copy(id = doc.id)
                todoList.add(todo)
            }

            filterTodosByDate()
        }
    }

    private fun filterTodosByDate() {
        filteredList.clear()

        for (todo in todoList) {
            val dateStr = todo.deadline?.toDate()?.let { dateFormat.format(it) }
            if (dateStr == selectedDateStr) {
                filteredList.add(todo)
            }
        }

        todoAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }
}
