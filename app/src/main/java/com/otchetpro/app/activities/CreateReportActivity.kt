package com.otchetpro.app.activities

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
    
    private var dept = ""
    private val variableValues = mutableMapOf<String, String>()
    private var selectedDept = ""
    private var selectedUnit = ""
    private var templates = listOf<Template>()
    private var allVariables = listOf<Variable>()
    private var allDepts = listOf<String>()
    private var allVariableViews = mutableListOf<Pair<Variable, View>>()

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
        
        dept = SharedPrefs.getDept(this)
        allDepts = SharedPrefs.getDepts(this)

        setupDeptSpinner()
        setupUnitSpinner()
        setupTemplateSpinner()
        
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

    private fun generateVariableFields() {
        linearVariables.removeAllViews()
        allVariableViews.clear()
        
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
            allVariableViews.add(Pair(variable, inputField))
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
        
        // Собираем все переменные, которые есть в шаблоне
        val templateVars = mutableListOf<Variable>()
        allVariables.forEach { variable ->
            if (text.contains("{{${variable.name}}}")) {
                templateVars.add(variable)
            }
        }
        
        // Создаем Spannable для подсветки
        val spannable = SpannableString(text)
        var hasEmptyRequired = false
        var filledCount = 0
        
        // Подставляем значения переменных
        templateVars.forEach { variable ->
            val placeholder = "{{${variable.name}}}"
            val value = variableValues[variable.name] ?: ""
            
            if (value.isNotEmpty()) {
                // Заменяем placeholder на значение
                text = text.replace(placeholder, value)
                filledCount++
            } else if (variable.required) {
                // Обязательная, но не заполнена — подсвечиваем красным
                hasEmptyRequired = true
                val placeholderIndex = text.indexOf(placeholder)
                if (placeholderIndex >= 0) {
                    val endIndex = placeholderIndex + placeholder.length
                    spannable.setSpan(
                        ForegroundColorSpan(0xFFFF0000.toInt()),
                        placeholderIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else {
                // Необязательная и не заполнена — удаляем из текста
                text = text.replace(placeholder, "")
            }
        }
        
        // Обновляем TextView
        tvPreview.text = text
        
        // Обновляем счетчик
        val totalVars = templateVars.size
        val filled = filledCount
        tvVarCount.text = "$filled из $totalVars переменных заполнено" +
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
        
        // Проверяем обязательные поля
        var allFilled = true
        val missingFields = mutableListOf<String>()
        
        // Подставляем переменные
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
}
