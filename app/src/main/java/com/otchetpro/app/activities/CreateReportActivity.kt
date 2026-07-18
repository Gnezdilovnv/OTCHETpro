package com.otchetpro.app.activities

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.*
import com.otchetpro.app.utils.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CreateReportActivity : AppCompatActivity() {

    private lateinit var spinnerTemplate: Spinner
    private lateinit var spinnerDept: Spinner
    private lateinit var spinnerUnit: Spinner
    private lateinit var tvPreview: TextView
    private lateinit var btnSave: Button
    private lateinit var btnClose: Button
    private lateinit var linearVariables: LinearLayout
    private lateinit var tvVarCount: TextView
    private lateinit var rvSubDepts: RecyclerView
    private lateinit var tvSubDeptsHint: TextView
    private lateinit var progressBar: ProgressBar

    private var dept = ""
    private val variableValues = mutableMapOf<String, String>()
    private var selectedDept = ""
    private var selectedUnit = ""
    private var templates = listOf<Template>()
    private var allVariables = listOf<Variable>()
    private var allDepts = listOf<String>()
    private var selectedSubDepts = mutableListOf<String>()
    private var isDraftSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        spinnerTemplate = findViewById(R.id.spinner_template)
        spinnerDept = findViewById(R.id.spinner_dept)
        spinnerUnit = findViewById(R.id.spinner_unit)
        tvPreview = findViewById(R.id.tv_preview)
        btnSave = findViewById(R.id.btn_save_report)
        btnClose = findViewById(R.id.btn_close_create)
        linearVariables = findViewById(R.id.linear_variables)
        tvVarCount = findViewById(R.id.tv_var_count)
        rvSubDepts = findViewById(R.id.rv_subdepts)
        tvSubDeptsHint = findViewById(R.id.tv_subdepts_hint)
        progressBar = findViewById(R.id.progress_bar)

        dept = SharedPrefs.getDept(this)
        allDepts = SharedPrefs.getDepts(this)
        setupDeptSpinner(); setupUnitSpinner(); setupTemplateSpinner(); setupSubDepts()
        allVariables = SharedPrefs.getVariables(this).filter { it.dept == dept || it.typeGlobal == "common" || it.typeGlobal == "dept" }
        generateVariableFields()

        spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, _: View?, pos: Int, _: Long) { updatePreview() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        btnSave.setOnClickListener { saveReport() }
        btnClose.setOnClickListener {
            if (!isDraftSaved && variableValues.isNotEmpty())
                AlertDialog.Builder(this).setTitle("Черновик?").setMessage("Сохранить черновик?").setPositiveButton("Да") { _, _ -> saveDraft() }.setNegativeButton("Нет") { _, _ -> finish() }.show()
            else finish()
        }
        loadDraft(); updatePreview()
    }

    private fun setupDeptSpinner() {
        spinnerDept.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allDepts).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val idx = allDepts.indexOf(dept); if (idx >= 0) spinnerDept.setSelection(idx)
        spinnerDept.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, _: View?, pos: Int, _: Long) { selectedDept = p?.getItemAtPosition(pos).toString() ?: dept; setupUnitSpinner(); updatePreview() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupUnitSpinner() {
        val units = SharedPrefs.getVariables(this).filter { it.name == "Расчет" && it.dept == selectedDept && it.type == "select" }.flatMap { it.options }
        spinnerUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, if (units.isNotEmpty()) units else listOf("Нет расчетов")).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, _: View?, pos: Int, _: Long) { selectedUnit = p?.getItemAtPosition(pos).toString() ?: ""; updatePreview() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupTemplateSpinner() {
        templates = SharedPrefs.getTemplates(this).filter { it.dept == dept || it.type == "common" }
        spinnerTemplate.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, templates.map { it.name }).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun setupSubDepts() {
        val items = SharedPrefs.getAllUnits(this).groupBy { it.first }.flatMap { (d, u) -> u.map { "$d: ${it.second}" } }
        if (items.isNotEmpty()) {
            rvSubDepts.visibility = View.VISIBLE; tvSubDeptsHint.visibility = View.VISIBLE
            rvSubDepts.layoutManager = LinearLayoutManager(this)
            rvSubDepts.adapter = SubDeptAdapter(items, selectedSubDepts) { selectedSubDepts = it; tvSubDeptsHint.text = "Выбрано: ${it.size}"; updatePreview() }
        } else { rvSubDepts.visibility = View.GONE; tvSubDeptsHint.visibility = View.GONE }
    }

    private fun setupDateMask(et: EditText) {
        et.addTextChangedListener(object : TextWatcher {
            private var updating = false
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                val clean = s.toString().replace(Regex("[^0-9]"), "")
                if (clean.length >= 8) {
                    updating = true
                    et.setText("${clean.substring(0,2)}.${clean.substring(2,4)}.${clean.substring(4,8)}")
                    et.setSelection(10); updating = false
                } else if (clean.length >= 4) {
                    updating = true
                    et.setText("${clean.substring(0,2)}.${clean.substring(2)}")
                    et.setSelection(clean.length + 1); updating = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun generateVariableFields() {
        linearVariables.removeAllViews()
        // Группируем переменные по типу для удобства
        val common = allVariables.filter { it.typeGlobal == "common" && it.name != "Расчет" }
        val deptVars = allVariables.filter { it.typeGlobal == "dept" && it.dept == selectedDept && it.name != "Расчет" }
        val unitVars = allVariables.filter { it.typeGlobal == "unit" && it.dept == selectedDept && it.name != "Расчет" }

        if (common.isNotEmpty()) addSection("Общие переменные", common)
        if (deptVars.isNotEmpty()) addSection("Подразделение: $selectedDept", deptVars)
        if (unitVars.isNotEmpty()) addSection("Расчёт: ${if (selectedUnit != "Нет расчетов") selectedUnit else ""}", unitVars)
        if (common.isEmpty() && deptVars.isEmpty() && unitVars.isEmpty()) {
            linearVariables.addView(TextView(this).apply { text = "Нет переменных для заполнения. Добавьте в настройках."; setPadding(8, 16, 8, 16); setTextColor(0xFF6F85A5.toInt()) })
        }
    }

    private fun addSection(title: String, vars: List<Variable>) {
        linearVariables.addView(TextView(this).apply { text = title; textSize = 13f; setTextColor(0xFF0B1A2F.toInt()); typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, 16, 0, 8) })
        vars.forEach { v ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, 12) }
            row.addView(TextView(this).apply { text = v.name + if (v.required) " *" else ""; setTextColor(0xFF3A4F6E.toInt()); textSize = 13f })
            val input = createInputField(v)
            row.addView(input)
            linearVariables.addView(row)
        }
    }

    private fun createInputField(v: Variable): View = when (v.type) {
        "date" -> EditText(this).apply { hint = "ДД.ММ.ГГГГ"; setPadding(12, 12, 12, 12); setBackgroundResource(android.R.drawable.editbox_background); setupDateMask(this); addListener(v.name) }
        "number" -> EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; hint = "Число"; setPadding(12, 12, 12, 12); setBackgroundResource(android.R.drawable.editbox_background); addListener(v.name) }
        "select" -> Spinner(this).apply {
            adapter = ArrayAdapter(this@CreateReportActivity, android.R.layout.simple_spinner_item, v.options.ifEmpty { listOf("Нет вариантов") }).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setPadding(12, 12, 12, 12); setBackgroundResource(android.R.drawable.editbox_background)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, _: View?, pos: Int, _: Long) { variableValues[v.name] = (p?.adapter as? ArrayAdapter<String>)?.getItem(pos) ?: ""; updatePreview(); autoSaveDraft() }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        "multiselect" -> Spinner(this).apply {
            adapter = ArrayAdapter(this@CreateReportActivity, android.R.layout.simple_spinner_item, v.options.ifEmpty { listOf("Нет вариантов") }).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setPadding(12, 12, 12, 12); setBackgroundResource(android.R.drawable.editbox_background)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, _: View?, pos: Int, _: Long) { variableValues[v.name] = (p?.adapter as? ArrayAdapter<String>)?.getItem(pos) ?: ""; updatePreview(); autoSaveDraft() }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        else -> EditText(this).apply { hint = "Введите значение"; setPadding(12, 12, 12, 12); setBackgroundResource(android.R.drawable.editbox_background); addListener(v.name) }
    }

    private fun EditText.addListener(name: String) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { variableValues[name] = s.toString(); updatePreview(); autoSaveDraft() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun updatePreview() {
        val pos = spinnerTemplate.selectedItemPosition
        if (pos < 0 || pos >= templates.size) { tvPreview.text = "Выберите шаблон"; return }
        var text = templates[pos].text
        text = text.replace("{{Подразделение}}", spinnerDept.selectedItem.toString())
        text = text.replace("{{Расчет}}", spinnerUnit.selectedItem.toString())
        val subStr = if (selectedSubDepts.isNotEmpty()) selectedSubDepts.joinToString(", ") else ""
        text = text.replace("{{Соисполнители}}", subStr)
        if (selectedSubDepts.isEmpty()) text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}.*?\\{\\{/Соисполнители\\}\\}"), "")
        else text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}(.*?)\\{\\{Соисполнители\\}\\}(.*?)\\{\\{/Соисполнители\\}\\}"), "$1$subStr$2")
        val tvars = allVariables.filter { text.contains("{{${it.name}}}") }
        var filled = 0
        tvars.forEach { v ->
            val valStr = variableValues[v.name] ?: ""
            if (valStr.isNotEmpty()) { text = text.replace("{{${v.name}}}", valStr); filled++ }
            else if (!v.required) text = text.replace("{{${v.name}}}", "")
        }
        tvPreview.text = text; tvVarCount.text = "$filled из ${tvars.size} заполнено"
    }

    private fun autoSaveDraft() {
        getSharedPreferences("draft", MODE_PRIVATE).edit().putString("data", variableValues.toString()).putString("dept", selectedDept).putString("unit", selectedUnit).putInt("tpl", spinnerTemplate.selectedItemPosition).apply()
        isDraftSaved = true
    }

    private fun loadDraft() {
        val p = getSharedPreferences("draft", MODE_PRIVATE)
        val d = p.getString("data", "") ?: ""; if (d.isEmpty()) return
        try {
            d.replace("{", "").replace("}", "").split(", ").forEach { val parts = it.split("="); if (parts.size == 2) variableValues[parts[0]] = parts[1] }
            val di = allDepts.indexOf(p.getString("dept", "")); if (di >= 0) spinnerDept.setSelection(di)
            val ti = p.getInt("tpl", 0); if (ti < templates.size) spinnerTemplate.setSelection(ti)
            Toast.makeText(this, "💾 Черновик восстановлен", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    private fun saveDraft() { autoSaveDraft(); finish() }

    private fun saveReport() {
        val pos = spinnerTemplate.selectedItemPosition
        if (pos < 0 || pos >= templates.size) { Toast.makeText(this, "Выберите шаблон", Toast.LENGTH_SHORT).show(); return }
        progressBar.visibility = View.VISIBLE
        var text = templates[pos].text
        text = text.replace("{{Подразделение}}", spinnerDept.selectedItem.toString())
        text = text.replace("{{Расчет}}", spinnerUnit.selectedItem.toString())
        val subStr = selectedSubDepts.joinToString(", ")
        text = text.replace("{{Соисполнители}}", subStr)
        if (selectedSubDepts.isEmpty()) text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}.*?\\{\\{/Соисполнители\\}\\}"), "")
        else text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}(.*?)\\{\\{Соисполнители\\}\\}(.*?)\\{\\{/Соисполнители\\}\\}"), "$1$subStr$2")
        val missing = mutableListOf<String>()
        allVariables.filter { it.name != "Расчет" }.forEach { v ->
            val valStr = variableValues[v.name] ?: ""
            if (v.required && valStr.isEmpty()) { missing.add(v.name); return@forEach }
            if (valStr.isNotEmpty()) text = text.replace("{{${v.name}}}", valStr) else text = text.replace("{{${v.name}}}", "")
        }
        if (missing.isNotEmpty()) { progressBar.visibility = View.GONE; AlertDialog.Builder(this).setTitle("Ошибка").setMessage("Заполните: ${missing.joinToString(", ")}").setPositiveButton("OK", null).show(); return }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val id = AppDatabase.getInstance(this@CreateReportActivity).reportDao().insert(Report(dept = spinnerDept.selectedItem.toString(), templateName = templates[pos].name, text = text, variables = variableValues.toString(), subDepts = selectedSubDepts.joinToString(","), status = "saved"))
                val file = DocxGenerator.generateReport(this@CreateReportActivity, text, "Отчет_$id.docx")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE; getSharedPreferences("draft", MODE_PRIVATE).edit().clear().apply()
                    Toast.makeText(this@CreateReportActivity, if (file != null) "✅ Сохранено" else "❌ Ошибка файла", Toast.LENGTH_SHORT).show(); finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE; Toast.makeText(this@CreateReportActivity, "❌ ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    inner class SubDeptAdapter(
        private val items: List<String>,
        private val selected: MutableList<String>,
        private val onUpdate: (MutableList<String>) -> Unit
    ) : RecyclerView.Adapter<SubDeptAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { val checkBox: CheckBox = itemView.findViewById(R.id.cb_subdept) }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_subdept_check, p, false))
        override fun onBindViewHolder(h: ViewHolder, pos: Int) {
            h.checkBox.text = items[pos]; h.checkBox.isChecked = selected.contains(items[pos])
            h.checkBox.setOnCheckedChangeListener { _, ok -> if (ok) selected.add(items[pos]) else selected.remove(items[pos]); onUpdate(selected) }
        }
        override fun getItemCount() = items.size
    }
}
