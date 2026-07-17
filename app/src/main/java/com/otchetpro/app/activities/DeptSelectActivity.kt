package com.otchetpro.app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.otchetpro.app.R
import com.otchetpro.app.utils.SharedPrefs

class DeptSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dept_select)

        val deptList = findViewById<LinearLayout>(R.id.dept_list_container)

        val depts = SharedPrefs.getDepts(this)
        if (depts.isEmpty()) {
            // Если список пуст — создаем дефолтный
            SharedPrefs.saveDepts(this, listOf("БпЛА", "Миномет", "Артиллерия", "Танки"))
            recreate()
            return
        }

        deptList.removeAllViews()

        depts.forEach { deptName ->
            val btn = Button(this).apply {
                text = deptName
                setPadding(16, 24, 16, 24)
                textSize = 16f
                setBackgroundResource(R.drawable.btn_outline)
                setOnClickListener {
                    SharedPrefs.saveDept(this@DeptSelectActivity, deptName)
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
    }
}
