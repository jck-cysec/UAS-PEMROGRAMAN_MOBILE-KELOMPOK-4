package com.example.todochiapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class TaskFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var todoAdapter: TodoAdapter
    private val todoList = mutableListOf<TodoModel>()
    private var listenerRegistration: ListenerRegistration? = null

    private lateinit var spinnerFilter: Spinner
    private lateinit var rvTodos: RecyclerView
    private lateinit var btnAdd: Button
    private lateinit var uid: String

    private val filterOptions = arrayOf("All", "Done", "Progress")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "Silakan login ulang", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        } else {
            uid = user.uid
        }

        // Bind Views
        spinnerFilter = view.findViewById(R.id.spinnerFilter)
        rvTodos = view.findViewById(R.id.rvTodos)
        btnAdd = view.findViewById(R.id.btnAddTodo)

        // RecyclerView setup
        todoAdapter = TodoAdapter(todoList)
        rvTodos.layoutManager = LinearLayoutManager(requireContext())
        rvTodos.adapter = todoAdapter

        // Spinner setup
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = filterOptions[position]
                val filter = when (selected) {
                    "Done" -> "done" to true
                    "Progress" -> "done" to false
                    else -> null
                }
                loadTodos(filter)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                loadTodos(null)
            }
        }

        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddTodoActivity::class.java))
        }

        // Load default
        loadTodos(null)

        return view
    }

    private fun loadTodos(filter: Pair<String, Any?>?) {
        listenerRegistration?.remove()

        var query: Query = firestore.collection("users")
            .document(uid)
            .collection("todos")

        if (filter != null) {
            query = query.whereEqualTo(filter.first, filter.second)
        }

        listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            todoList.clear()
            snapshots?.forEach { doc ->
                val todo = doc.toObject(TodoModel::class.java).copy(id = doc.id)
                todoList.add(todo)
            }
            todoAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }
}

