package com.otchetpro.app.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.*
import com.otchetpro.app.utils.SharedPrefs
import java.util.*

class TemplateEditorActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var etName: EditText
    private lateinit var etText: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var rvVariables: RecyclerView
    
    private var templateId: String? = null
    private var isEditMode = false
    private var dept = ""
    private var allVariables = listOf<Variable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_editor)

        tvTitle = findViewById(R.id.tv_template_editor_title)
        etName = findViewById(R.id.et_template_name)
        etText = findViewById(R.id.et_template_text)
        btnSave = findViewById(R.id.btn_template_save)
        btnCancel = findViewById(R.id.btn_template_cancel)
        rvVariables = findViewById(R.id.rv_template_variables)

        dept = SharedPrefs.getDept(this)
        allVariables = SharedPrefs.getVariables(this).filter { it.dept == dept || it.typeGlobal == "common" }

        templateId = intent.getStringExtra("template_id")
        if (templateId != null) {
            isEditMode = true
            tvTitle.text = "Редактировать шаблон"
            etName.setText(intent.getStringExtra("template_name") ?: "")
            etText.setText(intent.getStringExtra("template_text") ?: "")
        } else {
            tvTitle.text = "Новый шаблон"
        }

        // Кнопки-вставки переменных
        setupVariableButtons()

        btnSave.setOnClickListener { saveTemplate() }
        btnCancel.setOnClickListener { finish() }

        // Подсветка синтаксиса
        etText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                highlightSyntax()
            }
        })
    }

    private fun setupVariableButtons() {
        val layout = findViewById<LinearLayout>(R.id.ll_variable_buttons)
        layout.removeAllViews()

        allVariables.forEach { variable ->
            val btn = Button(this).apply {
                text = variable.name
                setPadding(8, 8, 8, 8)
                setOnClickListener {
                    val cursorPosition = etText.selectionStart
                    val text = etText.text.toString()
                    val newText = text.substring(0, cursorPosition) + 
                                   "{{${variable.name}}}" + 
                                   text.substring(cursorPosition)
                    etText.setText(newText)
                    etText.setSelection(cursorPosition + "{{${variable.name}}}".length)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 8; marginBottom = 8 }
            btn.layoutParams = params
            layout.addView(btn)
        }
    }

    private fun highlightSyntax() {
        // Простая подсветка — можно расширить
        val text = etText.text.toString()
        // В реальном приложении здесь можно использовать SpannableString
    }

    private fun saveTemplate() {
        val name = etName.text.toString().trim()
        val text = etText.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название шаблона", Toast.LENGTH_SHORT).show()
            return
        }
        if (text.isEmpty()) {
            Toast.makeText(this, "Введите текст шаблона", Toast.LENGTH_SHORT).show()
            return
        }

        val templates = SharedPrefs.getTemplates(this).toMutableList()
        
        if (isEditMode && templateId != null) {
            val index = templates.indexOfFirst { it.id == templateId }
            if (index != -1) {
                templates[index] = templates[index].copy(name = name, text = text)
            }
        } else {
            templates.add(Template(
                id = UUID.randomUUID().toString(),
                name = name,
                text = text,
                type = "own",
                dept = dept
            ))
        }

        SharedPrefs.saveTemplates(this, templates)
        Toast.makeText(this, "✅ Шаблон сохранен", Toast.LENGTH_SHORT).show()
        finish()
    }
}
