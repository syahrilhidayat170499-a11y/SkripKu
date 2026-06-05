package com.example.domain.model

import org.json.JSONArray
import org.json.JSONObject

enum class ElementType {
    SCENE_HEADING,  // INT. RUANG TAMU - MALAM
    ACTION,         // Present tense, visual paragraph
    CHARACTER,      // CAPS LOCK, Centered
    DIALOGUE,       // Under character name, centered width
    PARENTHETICAL,  // (tersenyum), under character name
    TRANSITION      // CUT TO:, FADE IN: aligned to right
}

data class ScriptElement(
    val type: ElementType,
    val text: String
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("type", type.name)
        obj.put("text", text)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): ScriptElement {
            val typeStr = obj.optString("type", ElementType.ACTION.name)
            val type = try {
                ElementType.valueOf(typeStr)
            } catch (e: Exception) {
                ElementType.ACTION
            }
            val text = obj.optString("text", "")
            return ScriptElement(type, text)
        }

        fun listToJsonString(elements: List<ScriptElement>): String {
            val array = JSONArray()
            for (element in elements) {
                array.put(element.toJsonObject())
            }
            return array.toString()
        }

        fun listFromJsonString(jsonStr: String): List<ScriptElement> {
            val list = mutableListOf<ScriptElement>()
            if (jsonStr.isEmpty()) return list
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(fromJsonObject(obj))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
    }
}
