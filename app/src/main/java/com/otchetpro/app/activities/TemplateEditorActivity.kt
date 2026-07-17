package com.otchetpro.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var spinnerDept: Spinner
    private lateinit var llDeptSelect: LinearLayout
    
    private var templateId: String? = null
    private var isEditMode = false
    private var dept = ""
    private var allVariables = listOf<Variable>()
    private var allDepts = listOf<String>()
    private var selectedDept = ""

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
        spinnerDept = findViewById(R.id.spinner_template_dept)
        llDeptSelect = findViewById(R.id.ll_dept_select)

        // Настройка для корректного ввода текста
        etText.isFocusable = true
        etText.isFocusableInTouchMode = true
        etText.requestFocus()

        dept = SharedPrefs.getDept(this)
        allDepts = SharedPrefs.getDepts(this)
        allVariables = SharedPrefs.getVariables(this)

        setupDeptSpinner()

        templateId = intent.getStringExtra("template_id")
        if (templateId != null) {
            isEditMode = true
            tvTitle.text = "Редактировать шаблон"
            etName.setText(intent.getStringExtra("template_name") ?: "")
            etText.setText(intent.getStringExtra("template_text") ?: "")
            val templateType = intent.getStringExtra("template_type") ?: "own"
            cbCommon.isChecked = templateType == "common"
            if (templateType == "common") {
                llDeptSelect.visibility = View.GONE
            }
        } else {
            tvTitle.text = "Новый шаблон"
            cbCommon.isChecked = false
            llDeptSelect.visibility = View.VISIBLE
        }

        cbCommon.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                llDeptSelect.visibility = View.GONE
            } else {
                llDeptSelect.visibility = View.VISIBLE
            }
            setupVariableButtons()
        }

        spinnerDept.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDept = parent?.getItemAtPosition(position).toString() ?: dept
                setupVariableButtons()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        setupVariableButtons()

        btnSave.setOnClickListener { saveTemplate() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun setupDeptSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allDepts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDept.adapter = adapter
        
        val currentIndex = allDepts.indexOf(dept)
        if (currentIndex >= 0) spinnerDept.setSelection(currentIndex)
        selectedDept = spinnerDept.selectedItem.toString()
    }

    private fun setupVariableButtons() {
        llVariableContainer.removeAllViews()

        val isCommon = cbCommon.isChecked
        val deptVars = allVariables.filter { 
            it.typeGlobal == "dept" && it.dept == selectedDept 
        }

        // 1. Системные переменные
        val systemVars = listOf(
            "Подразделение" to "{{Подразделение}}",
            "Расчет" to "{{Расчет}}",
            "Соисполнители" to "{{Соисполнители}}"
        )
        addGroup("Системные переменные", systemVars.map { it.first }, true)

        // 2. Общие переменные
        val commonVars = allVariables.filter { it.typeGlobal == "common" }
        if (commonVars.isNotEmpty()) {
            addGroup("Общие переменные (${commonVars.size})", commonVars.map { it.name }, false)
        }

        // 3. Переменные подразделения
        if (!isCommon && deptVars.isNotEmpty()) {
            addGroup("Подразделение: $selectedDept (${deptVars.size})", deptVars.map { it.name }, false)
        }
    }

    private fun addGroup(title: String, items: List<String>, isOpen: Boolean) {
        if (items.isEmpty()) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)
        }

        val header = TextView(this).apply {
            text = if (isOpen) "▼ $title" else "▶ $title"
            textSize = 13f
            setTextColor(0xFF0B1A2F.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(8, 8, 8, 8)
            setBackgroundResource(android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
            tag = if (isOpen) "open" else "closed"
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = if (isOpen) View.VISIBLE else View.GONE
            setPadding(8, 4, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        items.forEach { name ->
            val btn = Button(this).apply {
                text = name
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    val cursorPosition = etText.selectionStart
                    val text = etText.text.toString()
                    val placeholder = when (name) {
                        "Подразделение" -> "{{Подразделение}}"
                        "Расчет" -> "{{Расчет}}"
                        "Соисполнители" -> "{{Соисполнители}}"
                        else -> "{{$name}}"
                    }
                    val newText = text.substring(0, cursorPosition) + 
                                   placeholder + 
                                   text.substring(cursorPosition)
                    etText.setText(newText)
                    etText.setSelection(cursorPosition + placeholder.length)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 8, 8) }
            btn.layoutParams = params
            content.addView(btn)
        }

        val scrollView = HorizontalScrollView(this).apply {
            addView(content)
            visibility = if (isOpen) View.VISIBLE else View.GONE
        }

        container.addView(header)
        container.addView(scrollView)

        header.setOnClickListener {
            val isOpenNow = header.tag == "open"
            if (isOpenNow) {
                header.text = "▶ " + header.text.toString().substring(2)
                scrollView.visibility = View.GONE
                header.tag = "closed"
            } else {
                header.text = "▼ " + header.text.toString().substring(2)
                scrollView.visibility = View.VISIBLE
                header.tag = "open"
            }
        }

        llVariableContainer.addView(container)
    }

    private fun saveTemplate() {
        val name = etName.text.toString().trim()
        val text = etText.text.toString().trim()

        // ============================================================
        // ВАЛИДАЦИЯ
        // ============================================================
        var hasError = false

        if (name.isEmpty()) {
            etName.error = "Введите название шаблона"
            etName.requestFocus()
            hasError = true
        } else if (name.length < 3) {
            etName.error = "Название должно быть не менее 3 символов"
            etName.requestFocus()
            hasError = true
        }

        if (text.isEmpty()) {
            etText.error = "Введите текст шаблона"
            if (!hasError) etText.requestFocus()
            hasError = true
        } else if (text.length < 3) {
            etText.error = "Текст должен быть не менее 3 символов"
            if (!hasError) etText.requestFocus()
            hasError = true
        }

        if (hasError) {
            Toast.makeText(this, "Пожалуйста, исправьте ошибки в форме", Toast.LENGTH_LONG).show()
            return
        }

        val type = if (cbCommon.isChecked) "common" else "own"
        val selectedDept = if (type == "common") "" else spinnerDept.selectedItem.toString()
        
        val templates = SharedPrefs.getTemplates(this).toMutableList()
        
        if (isEditMode && templateId != null) {
            val index = templates.indexOfFirst { it.id == templateId }
            if (index != -1) {
                templates[index] = templates[index].copy(
                    name = name,
                    text = text,
                    type = type,
                    dept = selectedDept
                )
            }
        } else {
            templates.add(Template(
                id = UUID.randomUUID().toString(),
                name = name,
                text = text,
                type = type,
                dept = selectedDept
            ))
        }

        SharedPrefs.saveTemplates(this, templates)
        Toast.makeText(this, "✅ Шаблон сохранен", Toast.LENGTH_SHORT).show()
        finish()
    }
}
