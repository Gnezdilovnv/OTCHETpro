package com.otchetpro.app.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.otchetpro.app.data.*

object SharedPrefs {
    private val gson = Gson()
    private const val P = "otchetpro_prefs"
    private val lock = Any()

    fun getDept(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("dept", "БпЛА") ?: "БпЛА"
    fun saveDept(c: Context, d: String) {
        synchronized(lock) {
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("dept", d).apply()
        }
    }

    fun getTemplates(c: Context): List<Template> {
        synchronized(lock) {
            val j = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("templates", "[]") ?: "[]"
            return try { gson.fromJson(j, object : TypeToken<List<Template>>() {}.type) } catch (e: Exception) { emptyList() }
        }
    }
    fun saveTemplates(c: Context, l: List<Template>) {
        synchronized(lock) {
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("templates", gson.toJson(l)).apply()
        }
    }

    fun getVariables(c: Context): List<Variable> {
        synchronized(lock) {
            val j = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("vars", "[]") ?: "[]"
            return try { gson.fromJson(j, object : TypeToken<List<Variable>>() {}.type) } catch (e: Exception) { emptyList() }
        }
    }
    fun saveVariables(c: Context, l: List<Variable>) {
        synchronized(lock) {
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("vars", gson.toJson(l)).apply()
        }
    }

    fun getRecipients(c: Context): List<Recipient> {
        synchronized(lock) {
            val j = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("recips", "[]") ?: "[]"
            return try { gson.fromJson(j, object : TypeToken<List<Recipient>>() {}.type) } catch (e: Exception) { emptyList() }
        }
    }
    fun saveRecipients(c: Context, l: List<Recipient>) {
        synchronized(lock) {
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("recips", gson.toJson(l)).apply()
        }
    }

    fun getSubDepts(c: Context): List<String> {
        synchronized(lock) {
            val j = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("subs", "[]") ?: "[]"
            return try { gson.fromJson(j, object : TypeToken<List<String>>() {}.type) } catch (e: Exception) { emptyList() }
        }
    }
    fun saveSubDepts(c: Context, l: List<String>) {
        synchronized(lock) {
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("subs", gson.toJson(l)).apply()
        }
    }

    fun getDepts(c: Context): List<String> {
        synchronized(lock) {
            val j = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("depts", "[]") ?: "[]"
            val parsed: List<String> = try {
                gson.fromJson(j, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
            return if (parsed.isEmpty()) {
                val defaults = listOf("БпЛА", "Миномет", "Артиллерия", "Танки")
                saveDepts(c, defaults)
                defaults
            } else {
                parsed
            }
        }
    }
    fun saveDepts(c: Context, l: List<String>) {
        synchronized(lock) {
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("depts", gson.toJson(l)).apply()
        }
    }

    fun saveDeptUnit(c: Context, dept: String, unit: String) {
        synchronized(lock) {
            c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("dept_unit_$dept", unit).apply()
        }
    }
    fun getDeptUnit(c: Context, dept: String): String? {
        synchronized(lock) {
            return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString("dept_unit_$dept", null)
        }
    }

    fun getAllUnits(c: Context): List<Pair<String, String>> {
        synchronized(lock) {
            val allVars = getVariables(c)
            val unitVars = allVars.filter { it.name == "Расчет" && it.type == "select" }
            val result = mutableListOf<Pair<String, String>>()
            unitVars.forEach { v ->
                v.options.forEach { option ->
                    result.add(Pair(v.dept, option))
                }
            }
            return result
        }
    }
}
