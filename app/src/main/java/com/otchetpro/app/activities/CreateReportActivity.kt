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
import com.otchetpro.app.utils.*
import kotlinx.coroutines.*

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
    private lateinit var subDeptAdapter: SubDeptAdapter
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

        setupDeptSpinner()
        setupUnitSpinner()
        setupTemplateSpinner()
        setupSubDepts()
        
        allVariables = SharedPrefs.getVariables(this).filter { 
            it.dept == dept || it.typeGlobal == "common" || it.typeGlobal == "dept"
        }
        generateVariableFields()

        spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { 
                updatePreview() 
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveReport() }
        btnClose.setOnClickListener { 
            if (!isDraftSaved && variableValues.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Сохранить черновик?")
                    .setMessage("У вас есть незаполненные данные. Сохранить их как черновик?")
                    .setPositiveButton("Сохранить") { _, _ -> saveDraft() }
                    .setNegativeButton("Не сохранять") { _, _ -> finish() }
                    .show()
            } else {
                finish()
            }
        }
        
        loadDraft()
        updatePreview()
    }

    private fun setupDeptSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allDepts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDept.adapter = adapter
        
        val currentIndex = allDepts.indexOf(dept)
        if (currentIndex >= 0) spinnerDept.setSelection(currentIndex)
        
        spinnerDept.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDept = parent?.getItemAtPosition(position).toString() ?: dept
                setupUnitSpinner()
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupUnitSpinner() {
        val unitVars = SharedPrefs.getVariables(this).filter { 
            it.name == "Расчет" && it.dept == selectedDept && it.type == "select"
        }
        val options = if (unitVars.isNotEmpty()) {
            unitVars.first().options
        } else {
            listOf("Нет расчетов")
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = adapter
        
        spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedUnit = parent?.getItemAtPosition(position).toString() ?: ""
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTemplateSpinner() {
        val allTemplates = SharedPrefs.getTemplates(this)
        templates = allTemplates.filter { it.dept == dept || it.type == "common" }
        val templateNames = templates.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, templateNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTemplate.adapter = adapter
    }

    private fun setupSubDepts() {
        val allUnits = SharedPrefs.getAllUnits(this)
        val grouped = allUnits.groupBy { it.first }
        val items = grouped.flatMap { (deptName, units) ->
            units.map { "${deptName}: ${it.second}" }
        }
        
        if (items.isNotEmpty()) {
            rvSubDepts.visibility = View.VISIBLE
            tvSubDeptsHint.visibility = View.VISIBLE
            subDeptAdapter = SubDeptAdapter(items, selectedSubDepts) { updated ->
                selectedSubDepts = updated
                tvSubDeptsHint.text = "Выбрано: ${selectedSubDepts.size} соисполнителей"
                updatePreview()
            }
            rvSubDepts.layoutManager = LinearLayoutManager(this)
            rvSubDepts.adapter = subDeptAdapter
        } else {
            rvSubDepts.visibility = View.GONE
            tvSubDeptsHint.visibility = View.GONE
        }
    }

    // ============================================================
    // МАСКА ВВОДА ДАТЫ
    // ============================================================
    private fun setupDateMask(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                val text = s.toString()
                val clean = text.replace(Regex("[^0-9]"), "")
                
                if (clean.length >= 8) {
                    isUpdating = true
                    val formatted = "${clean.substring(0,2)}.${clean.substring(2,4)}.${clean.substring(4,8)}"
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                    isUpdating = false
                } else if (clean.length >= 4 && clean.length < 6) {
                    isUpdating = true
                    val formatted = "${clean.substring(0,2)}.${clean.substring(2)}"
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                    isUpdating = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun generateVariableFields() {
        linearVariables.removeAllViews()
        
        allVariables.forEach { variable ->
            if (variable.name == "Расчет") return@forEach
            
            val row = LinearLayout(this).apply { 
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 16)
            }
            
            val label = TextView(this).apply {
                text = variable.name + if (variable.required) " *" else ""
                setTextColor(0xFF0B1A2F.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            row.addView(label)
            
            val inputField = when (variable.type) {
                "date" -> EditText(this).apply {
                    hint = "ДД.ММ.ГГГГ"
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    setupDateMask(this)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(text: android.text.Editable?) { 
                            variableValues[variable.name] = text.toString()
                            updatePreview()
                            autoSaveDraft()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
                "number" -> EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    hint = "Введите число"
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(text: android.text.Editable?) { 
                            variableValues[variable.name] = text.toString()
                            updatePreview()
                            autoSaveDraft()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
                "select" -> Spinner(this).apply {
                    val options = variable.options.toTypedArray()
                    adapter = ArrayAdapter(this@CreateReportActivity, android.R.layout.simple_spinner_item, 
                        if (options.isEmpty()) arrayOf("Нет вариантов") else options)
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selected = (parent?.adapter as? ArrayAdapter<String>)?.getItem(position) ?: ""
                            variableValues[variable.name] = selected
                            updatePreview()
                            autoSaveDraft()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    })
                }
                "multiselect" -> Spinner(this).apply {
                    val options = variable.options.toTypedArray()
                    adapter = ArrayAdapter(this@CreateReportActivity, android.R.layout.simple_spinner_item, 
                        if (options.isEmpty()) arrayOf("Нет вариантов") else options)
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selected = (parent?.adapter as? ArrayAdapter<String>)?.getItem(position) ?: ""
                            variableValues[variable.name] = selected
                            updatePreview()
                            autoSaveDraft()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    })
                }
                else -> EditText(this).apply {
                    hint = "Введите значение"
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(text: android.text.Editable?) { 
                            variableValues[variable.name] = text.toString()
                            updatePreview()
                            autoSaveDraft()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
            }
            row.addView(inputField)
            
            val hint = TextView(this).apply {
                text = if (variable.required) "Обязательное поле" else "Необязательное поле"
                textSize = 11f
                setTextColor(0xFF6F85A5.toInt())
            }
            row.addView(hint)
            linearVariables.addView(row)
        }
    }

    private fun updatePreview() {
        val position = spinnerTemplate.selectedItemPosition
        if (position < 0 || position >= templates.size) {
            tvPreview.text = "Выберите шаблон"
            return
        }
        
        var text = templates[position].text
        
        // Системные переменные
        val deptName = spinnerDept.selectedItem.toString()
        val unitName = spinnerUnit.selectedItem.toString()
        text = text.replace("{{Подразделение}}", deptName)
        text = text.replace("{{Расчет}}", unitName)
        
        // Соисполнители
        val subStr = if (selectedSubDepts.isNotEmpty()) selectedSubDepts.joinToString(", ") else ""
        text = text.replace("{{Соисполнители}}", subStr)
        
        if (selectedSubDepts.isEmpty()) {
            text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}.*?\\{\\{/Соисполнители\\}\\}"), "")
        } else {
            text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}(.*?)\\{\\{Соисполнители\\}\\}(.*?)\\{\\{/Соисполнители\\}\\}"), "$1$subStr$2")
        }
        
        // Подставляем переменные
        val templateVars = allVariables.filter { text.contains("{{${it.name}}}") }
        var filledCount = 0
        
        templateVars.forEach { variable ->
            val placeholder = "{{${variable.name}}}"
            val value = variableValues[variable.name] ?: ""
            
            if (value.isNotEmpty()) {
                text = text.replace(placeholder, value)
                filledCount++
            } else if (variable.required) {
                // Оставляем placeholder для подсветки
            } else {
                text = text.replace(placeholder, "")
            }
        }
        
        tvPreview.text = text
        tvVarCount.text = "$filledCount из ${templateVars.size} переменных заполнено"
    }

    // ============================================================
    // АВТОСОХРАНЕНИЕ ЧЕРНОВИКА
    // ============================================================
    private fun autoSaveDraft() {
        val prefs = getSharedPreferences("draft", MODE_PRIVATE)
        prefs.edit()
            .putString("draft_data", variableValues.toString())
            .putString("draft_dept", selectedDept)
            .putString("draft_unit", selectedUnit)
            .putInt("draft_template", spinnerTemplate.selectedItemPosition)
            .apply()
        isDraftSaved = true
    }

    private fun loadDraft() {
        val prefs = getSharedPreferences("draft", MODE_PRIVATE)
        val draftData = prefs.getString("draft_data", "")
        if (draftData.isNullOrEmpty()) return
        
        try {
            // Восстанавливаем значения
            val data = draftData.replace("{", "").replace("}", "").split(", ")
            data.forEach { pair ->
                val parts = pair.split("=")
                if (parts.size == 2) {
                    variableValues[parts[0]] = parts[1]
                }
            }
            
            val deptPos = allDepts.indexOf(prefs.getString("draft_dept", ""))
            if (deptPos >= 0) spinnerDept.setSelection(deptPos)
            
            val templatePos = prefs.getInt("draft_template", 0)
            if (templatePos < templates.size) spinnerTemplate.setSelection(templatePos)
            
            Toast.makeText(this, "💾 Черновик восстановлен", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Ошибка восстановления
        }
    }

    private fun saveDraft() {
        autoSaveDraft()
        Toast.makeText(this, "💾 Черновик сохранен", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveReport() {
        val position = spinnerTemplate.selectedItemPosition
        if (position < 0 || position >= templates.size) {
            Toast.makeText(this, "Выберите шаблон", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        
        var text = templates[position].text
        
        // Системные переменные
        val deptName = spinnerDept.selectedItem.toString()
        val unitName = spinnerUnit.selectedItem.toString()
        text = text.replace("{{Подразделение}}", deptName)
        text = text.replace("{{Расчет}}", unitName)
        
        // Соисполнители
        val subStr = if (selectedSubDepts.isNotEmpty()) selectedSubDepts.joinToString(", ") else ""
        text = text.replace("{{Соисполнители}}", subStr)
        if (selectedSubDepts.isEmpty()) {
            text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}.*?\\{\\{/Соисполнители\\}\\}"), "")
        } else {
            text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}(.*?)\\{\\{Соисполнители\\}\\}(.*?)\\{\\{/Соисполнители\\}\\}"), "$1$subStr$2")
        }
        
        // Проверяем обязательные поля
        var allFilled = true
        val missingFields = mutableListOf<String>()
        
        allVariables.forEach { variable ->
            if (variable.name == "Расчет") return@forEach
            val value = variableValues[variable.name] ?: ""
            
            if (variable.required && value.isEmpty()) {
                allFilled = false
                missingFields.add(variable.name)
            } else if (value.isNotEmpty()) {
                text = text.replace("{{${variable.name}}}", value)
            } else {
                text = text.replace("{{${variable.name}}}", "")
            }
        }
        
        if (!allFilled) {
            progressBar.visibility = View.GONE
            val message = "Заполните обязательные поля:\n" + missingFields.joinToString(", ")
            AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        val report = Report(
            dept = deptName,
            templateName = templates[position].name,
            text = text,
            variables = variableValues.toString(),
            subDepts = selectedSubDepts.joinToString(","),
            status = "saved"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val id = AppDatabase.getInstance(this@CreateReportActivity).reportDao().insert(report)
                val fileName = "Отчет_${id}.docx"
                val file = DocxGenerator.generateReport(this@CreateReportActivity, text, fileName)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // Очищаем черновик
                    getSharedPreferences("draft", MODE_PRIVATE).edit().clear().apply()
                    if (file != null) {
                        Toast.makeText(this@CreateReportActivity, "✅ Сохранено: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@CreateReportActivity, "❌ Ошибка сохранения файла", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@CreateReportActivity, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    inner class SubDeptAdapter(
        private val items: List<String>,
        private val selected: MutableList<String>,
        private val onUpdate: (MutableList<String>) -> Unit
    ) : RecyclerView.Adapter<SubDeptAdapter.ViewHolder>() {
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkBox: CheckBox = itemView.findViewById(R.id.cb_subdept)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subdept_check, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.checkBox.text = item
            holder.checkBox.isChecked = selected.contains(item)
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (!selected.contains(item)) selected.add(item)
                } else {
                    selected.remove(item)
                }
                onUpdate(selected)
            }
        }
        
        override fun getItemCount(): Int = items.size
    }
}

    // Валидация числового поля
    private fun validateNumberField(editText: EditText): Boolean {
        val text = editText.text.toString()
        if (text.isEmpty()) return true // Пустое поле допустимо (если не обязательное)
        return try {
            text.toDouble()
            true
        } catch (e: NumberFormatException) {
            editText.error = "Введите число"
            false
        }
    }
