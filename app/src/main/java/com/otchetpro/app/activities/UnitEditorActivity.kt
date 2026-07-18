package com.otchetpro.app.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.otchetpro.app.R
import com.otchetpro.app.data.Variable
import com.otchetpro.app.data.VariableTypes
import com.otchetpro.app.utils.SharedPrefs
import java.util.*

class UnitEditorActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvDeptInfo: TextView
    private lateinit var etNewUnit: EditText
    private lateinit var btnAdd: Button
    private lateinit var rvUnits: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClose: Button

    private var deptName = ""
    private var units = mutableListOf<String>()
    private var variableId = ""
    private lateinit var adapter: UnitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unit_editor)

        tvTitle = findViewById(R.id.tv_unit_editor_title)
        tvDeptInfo = findViewById(R.id.tv_unit_dept_info)
        etNewUnit = findViewById(R.id.et_new_unit)
        btnAdd = findViewById(R.id.btn_add_unit)
        rvUnits = findViewById(R.id.rv_units)
        tvEmpty = findViewById(R.id.tv_units_empty)
        btnClose = findViewById(R.id.btn_unit_editor_close)

        deptName = intent.getStringExtra("dept_name") ?: ""
        tvTitle.text = "Расчёты: $deptName"
        tvDeptInfo.text = "Подразделение: $deptName"

        btnClose.setOnClickListener { finish() }
        btnAdd.setOnClickListener { addUnit() }

        adapter = UnitAdapter(
            items = units,
            onEdit = { pos, newName -> updateUnit(pos, newName) },
            onDelete = { pos -> deleteUnit(pos) }
        )
        rvUnits.layoutManager = LinearLayoutManager(this)
        rvUnits.adapter = adapter

        loadUnits()
    }

    override fun onResume() {
        super.onResume()
        loadUnits()
    }

    private fun loadUnits() {
        val allVars = SharedPrefs.getVariables(this)
        val unitVar = allVars.find { it.name == "Расчет" && it.dept == deptName && it.type == "select" && it.typeGlobal == "unit" }

        if (unitVar != null) {
            variableId = unitVar.id
            units.clear()
            units.addAll(unitVar.options)
        } else {
            variableId = ""
            units.clear()
        }

        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (units.isEmpty()) {
            rvUnits.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvUnits.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun saveUnits() {
        val allVars = SharedPrefs.getVariables(this).toMutableList()

        if (variableId.isNotEmpty()) {
            val idx = allVars.indexOfFirst { it.id == variableId }
            if (idx != -1) {
                allVars[idx] = allVars[idx].copy(options = units.toList())
            }
        } else if (units.isNotEmpty()) {
            allVars.add(
                Variable(
                    id = UUID.randomUUID().toString(),
                    name = "Расчет",
                    type = VariableTypes.SELECT,
                    required = false,
                    typeGlobal = "unit",
                    dept = deptName,
                    options = units.toList()
                )
            )
        }

        SharedPrefs.saveVariables(this, allVars)
    }

    private fun addUnit() {
        val name = etNewUnit.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название расчёта", Toast.LENGTH_SHORT).show()
            return
        }
        if (units.contains(name)) {
            Toast.makeText(this, "❌ Такой расчёт уже существует", Toast.LENGTH_SHORT).show()
            return
        }
        units.add(name)
        saveUnits()
        etNewUnit.text.clear()
        adapter.notifyItemInserted(units.size - 1)
        updateEmptyState()
        Toast.makeText(this, "✅ Расчёт добавлен", Toast.LENGTH_SHORT).show()
    }

    private fun updateUnit(pos: Int, newName: String) {
        if (newName.isEmpty()) return
        if (units.contains(newName) && units[pos] != newName) {
            Toast.makeText(this, "❌ Такой расчёт уже существует", Toast.LENGTH_SHORT).show()
            return
        }
        units[pos] = newName
        saveUnits()
        adapter.notifyItemChanged(pos)
        Toast.makeText(this, "✅ Обновлено", Toast.LENGTH_SHORT).show()
    }

    private fun deleteUnit(pos: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить расчёт?")
            .setMessage("Удалить \"${units[pos]}\"?")
            .setPositiveButton("Удалить") { d, w ->
                units.removeAt(pos)
                saveUnits()
                adapter.notifyItemRemoved(pos)
                updateEmptyState()
                Toast.makeText(this, "✅ Удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

class UnitAdapter(
    private var items: MutableList<String>,
    private val onEdit: (Int, String) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<UnitAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_unit_name)
        val btnEdit: Button = v.findViewById(R.id.btn_unit_edit)
        val btnDelete: Button = v.findViewById(R.id.btn_unit_delete)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_unit, p, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val name = items[pos]
        h.tvName.text = name
        h.btnEdit.setOnClickListener {
            val input = EditText(h.itemView.context).apply { setText(name) }
            AlertDialog.Builder(h.itemView.context)
                .setTitle("Редактировать расчёт")
                .setView(input)
                .setPositiveButton("Сохранить") { d, w -> onEdit(pos, input.text.toString().trim()) }
                .setNegativeButton("Отмена", null)
                .show()
        }
        h.btnDelete.setOnClickListener { onDelete(pos) }
    }

    override fun getItemCount() = items.size
}
