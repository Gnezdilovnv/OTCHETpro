package com.otchetpro.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.adapters.ReportAdapter
import com.otchetpro.app.data.*
import com.otchetpro.app.utils.DocxGenerator
import com.otchetpro.app.utils.SharedPrefs
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvDeptInfo: TextView
    private lateinit var btnCreate: Button
    private lateinit var btnSettings: Button
    private lateinit var btnSwitch: Button
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvCount: TextView
    private lateinit var filterAll: Button
    private lateinit var filterSaved: Button
    private lateinit var filterSent: Button
    private lateinit var progressBar: ProgressBar
    
    private var dept = "БпЛА"
    private var currentFilter = "all"
    private lateinit var adapter: ReportAdapter

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)

        initViews()
        dept = SharedPrefs.getDept(this)
        updateUI()

        adapter = ReportAdapter(
            onItemClick = { report ->
                startActivity(Intent(this, ReportDetailActivity::class.java).putExtra("id", report.id))
            },
            onLongClick = { report ->
                showDeleteConfirmDialog(report)
            }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        setupListeners()
        loadReports()
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tv_title)
        tvDeptInfo = findViewById(R.id.tv_dept_info)
        btnCreate = findViewById(R.id.btn_create_report)
        btnSettings = findViewById(R.id.btn_settings)
        btnSwitch = findViewById(R.id.btn_switch_dept)
        rv = findViewById(R.id.rv_reports)
        tvEmpty = findViewById(R.id.tv_empty)
        tvCount = findViewById(R.id.tv_count)
        filterAll = findViewById(R.id.filter_all)
        filterSaved = findViewById(R.id.filter_saved)
        filterSent = findViewById(R.id.filter_sent)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun updateUI() {
        val u = when(dept) {
            "БпЛА" -> "ПВР №2 «Пчела»"
            "Миномет" -> "расчет миномета «ТИГР»"
            "Артиллерия" -> "152-мм гаубица «Гиацинт»"
            "Танки" -> "танковый взвод Т-72"
            else -> ""
        }
        tvDeptInfo.text = "Текущее подразделение: $dept — $u"
        tvTitle.text = "Боевой журнал"
    }

    private fun setupListeners() {
        btnCreate.setOnClickListener { 
            startActivity(Intent(this, CreateReportActivity::class.java))
        }
        btnSettings.setOnClickListener { 
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        btnSwitch.setOnClickListener { 
            startActivity(Intent(this, DeptSelectActivity::class.java))
            finish()
        }
        filterAll.setOnClickListener { setFilter("all") }
        filterSaved.setOnClickListener { setFilter("saved") }
        filterSent.setOnClickListener { setFilter("sent") }
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        filterAll.background = if (filter == "all") getDrawable(R.drawable.btn_primary) else getDrawable(R.drawable.btn_outline)
        filterSaved.background = if (filter == "saved") getDrawable(R.drawable.btn_primary) else getDrawable(R.drawable.btn_outline)
        filterSent.background = if (filter == "sent") getDrawable(R.drawable.btn_primary) else getDrawable(R.drawable.btn_outline)
        filterAll.setTextColor(if (filter == "all") 0xFFFFFFFF.toInt() else 0xFF0B1A2F.toInt())
        filterSaved.setTextColor(if (filter == "saved") 0xFFFFFFFF.toInt() else 0xFF0B1A2F.toInt())
        filterSent.setTextColor(if (filter == "sent") 0xFFFFFFFF.toInt() else 0xFF0B1A2F.toInt())
        loadReports()
    }

    private fun showDeleteConfirmDialog(report: Report) {
        AlertDialog.Builder(this)
            .setTitle("Удалить отчет?")
            .setMessage("Вы уверены, что хотите удалить отчет \"${report.templateName}\"?\nЭто действие нельзя отменить.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Удалить") { _, _ ->
                deleteReport(report)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteReport(report: Report) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(this@MainActivity)
                db.reportDao().delete(report.id)
                val reportsDir = DocxGenerator.getReportsDir()
                val file = File(reportsDir, "Отчет_${report.id}.docx")
                if (file.exists()) file.delete()
                withContext(Dispatchers.Main) {
                    loadReports()
                    Toast.makeText(this@MainActivity, "✅ Отчет удален", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadReports() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rv.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(this@MainActivity)
                val all = db.reportDao().getByDept(dept)
                val list = when (currentFilter) {
                    "saved" -> all.filter { it.status == "saved" }
                    "sent" -> all.filter { it.status == "sent" }
                    else -> all
                }
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (list.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rv.visibility = View.GONE
                        tvCount.text = "Всего: 0"
                    } else {
                        tvEmpty.visibility = View.GONE
                        rv.visibility = View.VISIBLE
                        adapter.submitList(list)
                        tvCount.text = "Всего: ${list.size}  (${all.size} всего)"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "❌ Ошибка загрузки:\n${e.message}"
                    Toast.makeText(this@MainActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
