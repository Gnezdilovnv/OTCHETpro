package com.otchetpro.app.activities

import android.app.AlertDialog
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
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
    private var isUpdatingText = false

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

        dept = SharedPrefs.getDept(this)
        allDepts = SharedPrefs.getDepts(this)
        allVariables = SharedPrefs.getVariables(this)

        if (allDepts.isEmpty()) allDepts = listOf("БпЛА", "Миномет", "Артиллерия", "Танки")
        setupDeptSpinner()

        templateId = intent.getStringExtra("template_id")
        if (templateId != null) {
            isEditMode = true
            tvTitle.text = "Редактировать шаблон"
            etName.setText(intent.getStringExtra("template_name") ?: "")
            etText.setText(intent.getStringExtra("template_text") ?: "")
            val templateType = intent.getStringExtra("template_type") ?: "own"
            cbCommon.isChecked = templateType == "common"
            llDeptSelect.visibility = if (templateType == "common") View.GONE else View.VISIBLE
        } else {
            tvTitle.text = "Новый шаблон"
            cbCommon.isChecked = false
            llDeptSelect.visibility = View.VISIBLE
        }

        cbCommon.setOnCheckedChangeListener { _, isChecked ->
            llDeptSelect.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        spinnerDept.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { if (!isUpdatingText) highlightSyntax() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        setupAutoComplete()
        setupVariableButtons()
        highlightSyntax()

        btnSave.setOnClickListener { saveTemplate() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun setupDeptSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allDepts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDept.adapter = adapter
        val idx = allDepts.indexOf(dept); if (idx >= 0) spinnerDept.setSelection(idx)
    }

    private fun highlightSyntax() {
        val text = etText.text?.toString() ?: return
        if (text.isEmpty()) return
        val spannable = SpannableString(text)
        Regex("\\{\\{[^}]+\\}\\}").findAll(text).forEach { match ->
            spannable.setSpan(BackgroundColorSpan(0xFFFFFF00.toInt()), match.range.first, match.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(0xFF0B1A2F.toInt()), match.range.first, match.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        isUpdatingText = true
        val sel = etText.selectionStart.coerceAtMost(spannable.length)
        etText.setText(spannable, TextView.BufferType.SPANNABLE)
        etText.setSelection(sel.coerceAtMost(etText.text.length))
        isUpdatingText = false
    }

    private fun setupAutoComplete() {
        etText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdatingText) return
                val text = s?.toString() ?: return
                val pos = etText.selectionStart
                if (pos < 2 || pos > text.length) return
                if (text.lastIndexOf("{{", pos) == pos - 2) showAutoCompleteDialog()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun showAutoCompleteDialog() {
        val allVars = mutableListOf("Подразделение", "Расчет", "Соисполнители")
        allVars.addAll(allVariables.filter { it.name != "Расчет" }.map { it.name }.distinct())
        AlertDialog.Builder(this)
            .setTitle("Выберите переменную")
            .setItems(allVars.toTypedArray()) { _, which ->
                val varName = allVars[which]
                val text = etText.text.toString()
                val pos = etText.selectionStart
                val newText = text.substring(0, pos - 2) + "{{$varName}}" + text.substring(pos)
                isUpdatingText = true
                etText.setText(newText)
                etText.setSelection((pos - 2 + "{{$varName}}".length).coerceAtMost(newText.length))
                isUpdatingText = false
                highlightSyntax()
            }.show()
    }

    private fun setupVariableButtons() {
        llVariableContainer.removeAllViews()

        // Системные всегда
        addGroup("Системные", listOf("Подразделение", "Расчет", "Соисполнители"), true)

        // Общие переменные (все, кроме Расчета)
        val commonVars = allVariables.filter { it.typeGlobal == "common" && it.name != "Расчет" }
        if (commonVars.isNotEmpty()) addGroup("Общие переменные", commonVars.map { it.name }.distinct(), false)

        // Переменные подразделений — группируем по dept
        val deptVars = allVariables.filter { it.typeGlobal == "dept" && it.name != "Расчет" }
        deptVars.groupBy { it.dept }.forEach { (deptName, vars) ->
            if (deptName.isNotEmpty() && vars.isNotEmpty())
                addGroup("Подразделение: $deptName", vars.map { it.name }.distinct(), false)
        }
    }

    private fun addGroup(title: String, items: List<String>, isOpen: Boolean) {
        if (items.isEmpty()) return
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 4, 0, 4) }
        val header = TextView(this).apply {
            text = if (isOpen) "▼ $title (${items.size})" else "▶ $title (${items.size})"
            textSize = 12f; setTextColor(0xFF0B1A2F.toInt()); typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(8, 8, 8, 8); setBackgroundResource(android.R.drawable.list_selector_background)
            isClickable = true; isFocusable = true; tag = if (isOpen) "open" else "closed"
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; visibility = if (isOpen) View.VISIBLE else View.GONE; setPadding(4, 2, 4, 2)
        }
        items.forEach { name ->
            content.addView(Button(this).apply {
                text = name; textSize = 10f; setPadding(8, 4, 8, 4); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 4, 4) }
                setOnClickListener { insertVariable(name) }
            })
        }
        val scroll = HorizontalScrollView(this).apply { addView(content); visibility = if (isOpen) View.VISIBLE else View.GONE }
        container.addView(header); container.addView(scroll)
        header.setOnClickListener {
            if (header.tag == "open") { header.text = "▶ $title (${items.size})"; scroll.visibility = View.GONE; header.tag = "closed" }
            else { header.text = "▼ $title (${items.size})"; scroll.visibility = View.VISIBLE; header.tag = "open" }
        }
        llVariableContainer.addView(container)
    }

    private fun insertVariable(name: String) {
        val pos = etText.selectionStart
        val txt = etText.text.toString()
        val placeholder = "{{$name}}"
        val newText = txt.substring(0, pos) + placeholder + txt.substring(pos)
        isUpdatingText = true
        etText.setText(newText)
        etText.setSelection((pos + placeholder.length).coerceAtMost(newText.length))
        isUpdatingText = false
        highlightSyntax()
        etText.requestFocus()
    }

    private fun saveTemplate() {
        val name = etName.text.toString().trim()
        val text = etText.text.toString().trim()
        if (name.isEmpty()) { etName.error = "Введите название"; return }
        if (text.isEmpty()) { etText.error = "Введите текст"; return }
        val type = if (cbCommon.isChecked) "common" else "own"
        val deptForTemplate = if (type == "common") "" else spinnerDept.selectedItem?.toString() ?: dept
        val templates = SharedPrefs.getTemplates(this).toMutableList()
        if (isEditMode && templateId != null) {
            val idx = templates.indexOfFirst { it.id == templateId }
            if (idx != -1) templates[idx] = templates[idx].copy(name = name, text = text, type = type, dept = deptForTemplate)
        } else {
            templates.add(Template(UUID.randomUUID().toString(), name, text, type, deptForTemplate))
        }
        SharedPrefs.saveTemplates(this, templates)
        Toast.makeText(this, "✅ Шаблон сохранен", Toast.LENGTH_SHORT).show()
        finish()
    }
}
