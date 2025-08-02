package com.example.todochiapp

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var uid: String

    private lateinit var tvTotal: TextView
    private lateinit var tvDone: TextView
    private lateinit var tvUndone: TextView
    private lateinit var tvNextDeadline: TextView
    private lateinit var pieChart: PieChart

    private lateinit var tvGreetingName: TextView
    private lateinit var tvGreetingDate: TextView
    private lateinit var layoutGreeting: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Silakan login ulang", Toast.LENGTH_SHORT).show()
            return view
        } else {
            uid = user.uid
        }

        // Bind views
        tvTotal = view.findViewById(R.id.tvTotalTodos)
        tvDone = view.findViewById(R.id.tvDoneTodos)
        tvUndone = view.findViewById(R.id.tvUndoneTodos)
        tvNextDeadline = view.findViewById(R.id.tvNextDeadline)
        pieChart = view.findViewById(R.id.pieChart)
        tvGreetingName = view.findViewById(R.id.tvGreetingName)
        tvGreetingDate = view.findViewById(R.id.tvGreetingDate)
        layoutGreeting = view.findViewById(R.id.layoutGreeting)

        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        tvGreetingDate.text = dateFormat.format(currentDate)

        layoutGreeting.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        refreshUserInfo()
        loadDashboardData()

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshUserInfo()
        loadDashboardData()
    }

    private fun refreshUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            // Tampilkan username sementara dari displayName Firebase (kalau ada)
            val defaultName = user.displayName ?: "Pengguna"
            tvGreetingName.text = "Hai, $defaultName"

            // Ambil nama yang lebih akurat dari Firestore secara diam-diam
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val username = doc.getString("username") ?: defaultName
                    if (tvGreetingName.text != "Hai, $username") {
                        tvGreetingName.text = "Hai, $username"
                    }
                }
                .addOnFailureListener {
                    // Tidak perlu ubah teks jika gagal
                }
        }
    }


    private fun loadDashboardData() {
        firestore.collection("users")
            .document(uid)
            .collection("todos")
            .get()
            .addOnSuccessListener { snapshot ->
                val todos = snapshot.documents.mapNotNull { it.toObject(TodoModel::class.java) }

                val total = todos.size
                val done = todos.count { it.done }
                val progress = total - done

                tvTotal.text = "Total: $total"
                tvDone.text = "Selesai: $done"
                tvUndone.text = "Progress: $progress"

                val nearest = todos
                    .mapNotNull { it.deadline?.toDate() }
                    .filter { it.after(Date()) }
                    .minOrNull()

                val formattedDeadline = nearest?.let {
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(it)
                } ?: "Tidak ada"

                tvNextDeadline.text = formattedDeadline

                setupPieChart(done, progress)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat dashboard: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPieChart(done: Int, progress: Int) {
        if (done == 0 && progress == 0) {
            pieChart.clear()
            pieChart.centerText = "Belum ada data"
            pieChart.invalidate()
            return
        }

        val entries = listOf(
            PieEntry(done.toFloat(), "Done"),
            PieEntry(progress.toFloat(), "Progress")
        )

        val dataSet = PieDataSet(entries, null) // no label
        dataSet.colors = listOf(
            resources.getColor(R.color.successGreen, null),
            resources.getColor(R.color.dangerRed, null)
        )
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f

        pieChart.data = PieData(dataSet)
        pieChart.centerText = "To-do Stats"
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(android.graphics.Color.BLACK)
        pieChart.setDrawEntryLabels(true)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
