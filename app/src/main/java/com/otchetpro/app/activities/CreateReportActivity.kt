package com.otchetpro.app.activities

import android.os.Bundle
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
    
    private var dept = ""
    private val variableValues = mutableMapOf<String, String>()
    private var selectedDept = ""
    private var selectedUnit = ""
    private var templates = listOf<Template>()
    private var allVariables = listOf<Variable>()
    private var allDepts = listOf<String>()
    private var selectedSubDepts = mutableListOf<String>()
    private lateinit var subDeptAdapter: SubDeptAdapter

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
        btnClose.setOnClickListener { finish() }
        
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

    // ============================================================
    // СОИСПОЛНИТЕЛИ — ПОДТЯГИВАЮТСЯ ИЗ ВСЕХ ПОДРАЗДЕЛЕНИЙ
    // ============================================================
    private fun setupSubDepts() {
        val allUnits = SharedPrefs.getAllUnits(this)
        // Группируем по подразделению
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
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(text: android.text.Editable?) { 
                            variableValues[variable.name] = text.toString()
                            updatePreview()
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
        
        // Условный блок для соисполнителей
        if (selectedSubDepts.isEmpty()) {
            text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}.*?\\{\\{/Соисполнители\\}\\}"), "")
        } else {
            text = text.replace(Regex("\\{\\{#Соисполнители\\}\\}(.*?)\\{\\{Соисполнители\\}\\}(.*?)\\{\\{/Соисполнители\\}\\}"), "$1$subStr$2")
        }
        
        // Собираем все переменные, которые есть в шаблоне
        val templateVars = mutableListOf<Variable>()
        allVariables.forEach { variable ->
            if (text.contains("{{${variable.name}}}")) {
                templateVars.add(variable)
            }
        }
        
        var filledCount = 0
        var hasEmptyRequired = false
        
        // Подставляем значения переменных
        templateVars.forEach { variable ->
            val placeholder = "{{${variable.name}}}"
            val value = variableValues[variable.name] ?: ""
            
            if (value.isNotEmpty()) {
                text = text.replace(placeholder, value)
                filledCount++
            } else if (variable.required) {
                hasEmptyRequired = true
                // Оставляем placeholder, он будет подсвечен красным
            } else {
                text = text.replace(placeholder, "")
            }
        }
        
        tvPreview.text = text
        tvVarCount.text = "$filledCount из ${templateVars.size} переменных заполнено" +
                          if (hasEmptyRequired) " ⚠️" else ""
    }

    private fun saveReport() {
        val position = spinnerTemplate.selectedItemPosition
        if (position < 0 || position >= templates.size) {
            Toast.makeText(this, "Выберите шаблон", Toast.LENGTH_SHORT).show()
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
        
        // Проверяем обязательные поля
        var allFilled = true
        val missingFields = mutableListOf<String>()
        
        allVariables.forEach { variable ->
            if (variable.name == "Расчет") return@forEach
            val value = variableValues[variable.name] ?: ""
            
            if (variable.required && value.isEmpty()) {
                allFilled = false
                missingFields.add(variable.name)
                text = text.replace("{{${variable.name}}}", "[${variable.name} не заполнено]")
            } else if (value.isNotEmpty()) {
                text = text.replace("{{${variable.name}}}", value)
            } else {
                text = text.replace("{{${variable.name}}}", "")
            }
        }
        
        if (!allFilled) {
            val message = "Заполните обязательные поля:\n" + missingFields.joinToString(", ")
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
            val id = AppDatabase.getInstance(this@CreateReportActivity).reportDao().insert(report)
            val fileName = "Отчет_${id}.docx"
            val file = DocxGenerator.generateReport(this@CreateReportActivity, text, fileName)
            withContext(Dispatchers.Main) {
                if (file != null) {
                    Toast.makeText(this@CreateReportActivity, "✅ Сохранено: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@CreateReportActivity, "❌ Ошибка сохранения файла", Toast.LENGTH_SHORT).show()
                }
                finish()
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
