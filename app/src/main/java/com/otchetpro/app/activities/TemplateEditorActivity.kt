package com.otchetpro.app.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
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
    private lateinit var llVariableContainer: LinearLayout
    private lateinit var cbCommon: CheckBox
    
    private var templateId: String? = null
    private var isEditMode = false
    private var dept = ""
    private var allVariables = listOf<Variable>()
    private var allDepts = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_editor)

        tvTitle = findViewById(R.id.tv_template_editor_title)
        etName = findViewById(R.id.et_template_name)
        etText = findViewById(R.id.et_template_text)
        btnSave = findViewById(R.id.btn_template_save)
        btnCancel = findViewById(R.id.btn_template_cancel)
        llVariableContainer = findViewById(R.id.ll_variable_buttons_container)
        cbCommon = findViewById(R.id.cb_template_common)

        dept = SharedPrefs.getDept(this)
        allDepts = SharedPrefs.getDepts(this)
        allVariables = SharedPrefs.getVariables(this)

        templateId = intent.getStringExtra("template_id")
        if (templateId != null) {
            isEditMode = true
            tvTitle.text = "Редактировать шаблон"
            etName.setText(intent.getStringExtra("template_name") ?: "")
            etText.setText(intent.getStringExtra("template_text") ?: "")
            cbCommon.isChecked = intent.getStringExtra("template_type") == "common"
        } else {
            tvTitle.text = "Новый шаблон"
            cbCommon.isChecked = false
        }

        setupVariableButtons()

        btnSave.setOnClickListener { saveTemplate() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun setupVariableButtons() {
        llVariableContainer.removeAllViews()
        
        // Собираем переменные по группам
        val systemVars = listOf(
            "Подразделение" to "{{Подразделение}}",
            "Расчет" to "{{Расчет}}",
            "Соисполнители" to "{{Соисполнители}}"
        )
        
        val commonVars = allVariables.filter { it.typeGlobal == "common" }
        val deptVars = allVariables.filter { it.typeGlobal == "dept" && it.dept == dept }
        val unitVars = allVariables.filter { it.typeGlobal == "unit" && it.dept == dept }
        
        // Все группы с их переменными
        val groups = mutableListOf<Pair<String, List<Any>>>()
        groups.add("Системные переменные" to systemVars)
        
        if (commonVars.isNotEmpty()) {
            groups.add("Общие переменные (${commonVars.size})" to commonVars)
        }
        if (deptVars.isNotEmpty()) {
            groups.add("Подразделение (${deptVars.size})" to deptVars)
        }
        if (unitVars.isNotEmpty()) {
            groups.add("Расчет (${unitVars.size})" to unitVars)
        }

        // Добавляем группы в аккордеон
        groups.forEach { (title, items) ->
            val groupView = createGroupView(title, items)
            llVariableContainer.addView(groupView)
        }
    }

    private fun createGroupView(title: String, items: List<Any>): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)
        }

        // Заголовок группы (аккордеон)
        val header = TextView(this).apply {
            text = "▶ $title"
            textSize = 13f
            setTextColor(0xFF0B1A2F.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(8, 8, 8, 8)
            background = getDrawable(android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
            tag = "closed"
        }

        // Контейнер для кнопок переменных
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            setPadding(8, 4, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Добавляем кнопки переменных
        items.forEach { item ->
            val btn = when (item) {
                is Pair<*, *> -> {
                    val name = item.first as String
                    Button(this).apply {
                        text = name
                        setPadding(16, 8, 16, 8)
                        setOnClickListener {
                            val cursorPosition = etText.selectionStart
                            val text = etText.text.toString()
                            val placeholder = if (name == "Подразделение") "{{Подразделение}}" else
                                           if (name == "Расчет") "{{Расчет}}" else
                                           if (name == "Соисполнители") "{{Соисполнители}}" else
                                           "{{$name}}"
                            val newText = text.substring(0, cursorPosition) + 
                                           placeholder + 
                                           text.substring(cursorPosition)
                            etText.setText(newText)
                            etText.setSelection(cursorPosition + placeholder.length)
                        }
                    }
                }
                is Variable -> {
                    Button(this).apply {
                        text = item.name
                        setPadding(16, 8, 16, 8)
                        setOnClickListener {
                            val cursorPosition = etText.selectionStart
                            val text = etText.text.toString()
                            val newText = text.substring(0, cursorPosition) + 
                                           "{{${item.name}}}" + 
                                           text.substring(cursorPosition)
                            etText.setText(newText)
                            etText.setSelection(cursorPosition + "{{${item.name}}}".length)
                        }
                    }
                }
                else -> null
            }
            
            btn?.let {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 8, 8) }
                it.layoutParams = params
                content.addView(it)
            }
        }

        // Добавляем горизонтальный скролл для кнопок
        val scrollView = HorizontalScrollView(this).apply {
            addView(content)
            visibility = View.GONE
        }

        container.addView(header)
        container.addView(scrollView)

        // Обработчик раскрытия/закрытия аккордеона
        header.setOnClickListener {
            val isOpen = header.tag == "open"
            if (isOpen) {
                header.text = "▶ " + header.text.toString().substring(2)
                scrollView.visibility = View.GONE
                header.tag = "closed"
            } else {
                header.text = "▼ " + header.text.toString().substring(2)
                scrollView.visibility = View.VISIBLE
                header.tag = "open"
            }
        }

        return container
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

        val type = if (cbCommon.isChecked) "common" else "own"
        val templates = SharedPrefs.getTemplates(this).toMutableList()
        
        if (isEditMode && templateId != null) {
            val index = templates.indexOfFirst { it.id == templateId }
            if (index != -1) {
                templates[index] = templates[index].copy(
                    name = name,
                    text = text,
                    type = type,
                    dept = if (type == "common") "" else dept
                )
            }
        } else {
            templates.add(Template(
                id = UUID.randomUUID().toString(),
                name = name,
                text = text,
                type = type,
                dept = if (type == "common") "" else dept
            ))
        }

        SharedPrefs.saveTemplates(this, templates)
        Toast.makeText(this, "✅ Шаблон сохранен", Toast.LENGTH_SHORT).show()
        finish()
    }
}
