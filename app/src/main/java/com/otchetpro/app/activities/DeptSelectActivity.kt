package com.otchetpro.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.otchetpro.app.R
import com.otchetpro.app.utils.SharedPrefs

class DeptSelectActivity : AppCompatActivity() {

    private lateinit var deptList: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnSettings: Button
    private var currentDept = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dept_select)

        deptList = findViewById(R.id.dept_list_container)
        progressBar = findViewById(R.id.progress_bar)
        tvEmpty = findViewById(R.id.tv_empty)
        btnSettings = findViewById(R.id.btn_goto_settings)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        currentDept = SharedPrefs.getDept(this)
        loadDepts()
    }

    override fun onResume() {
        super.onResume()
        val newDept = SharedPrefs.getDept(this)
        if (newDept != currentDept) {
            currentDept = newDept
            loadDepts()
        }
    }

    private fun loadDepts() {
        progressBar.visibility = View.VISIBLE
        deptList.removeAllViews()
        tvEmpty.visibility = View.GONE
        btnSettings.visibility = View.GONE

        val depts = SharedPrefs.getDepts(this)
        if (depts.isEmpty()) {
            tvEmpty.text = "Нет подразделений.\nПерейдите в настройки."
            tvEmpty.visibility = View.VISIBLE
            btnSettings.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            return
        }

        depts.forEach { deptName ->
            val btn = Button(this).apply {
                text = deptName
                setPadding(16, 24, 16, 24)
                textSize = 16f

                if (deptName == currentDept) {
                    setBackgroundResource(R.drawable.btn_primary)
                    setTextColor(0xFFFFFFFF.toInt())
                } else {
                    setBackgroundResource(R.drawable.btn_outline)
                    setTextColor(0xFF0B1A2F.toInt())
                }

                setOnClickListener {
                    SharedPrefs.saveDept(this@DeptSelectActivity, deptName)
                    currentDept = deptName
                    loadDepts()
                    startActivity(Intent(this@DeptSelectActivity, MainActivity::class.java))
                    finish()
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
            btn.layoutParams = params
            deptList.addView(btn)
        }

        progressBar.visibility = View.GONE
    }
}
