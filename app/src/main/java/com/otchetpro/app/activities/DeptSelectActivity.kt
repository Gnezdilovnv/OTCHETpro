package com.otchetpro.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.otchetpro.app.R
import com.otchetpro.app.utils.SharedPrefs

class DeptSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dept_select)

        findViewById<Button>(R.id.btn_dept_bpla).setOnClickListener { selectDept("БпЛА") }
        findViewById<Button>(R.id.btn_dept_minomet).setOnClickListener { selectDept("Миномет") }
        findViewById<Button>(R.id.btn_dept_artillery).setOnClickListener { selectDept("Артиллерия") }
        findViewById<Button>(R.id.btn_dept_tanks).setOnClickListener { selectDept("Танки") }
    }

    private fun selectDept(dept: String) {
        SharedPrefs.saveDept(this, dept)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
