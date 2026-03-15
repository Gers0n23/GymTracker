package com.gcordero.gymtracker.data.util

import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.Routine
import org.json.JSONArray
import org.json.JSONObject

object RoutineExportUtil {

    private const val TAG_START = "[GYMTRACKER_ROUTINE]"
    private const val TAG_END = "[/GYMTRACKER_ROUTINE]"

    /**
     * Serializes a routine + exercises into a shareable text string with embedded JSON.
     */
    fun serialize(routine: Routine, exercises: List<Exercise>): String {
        val json = buildJson(routine, exercises)
        return buildString {
            appendLine("📋 Rutina GymTracker: ${routine.name}")
            if (routine.description.isNotEmpty()) appendLine(routine.description)
            appendLine()
            appendLine(TAG_START)
            appendLine(json)
            appendLine(TAG_END)
        }
    }

    /**
     * Returns a compact JSON string suitable for sending to Gemini for analysis.
     */
    fun toAnalysisJson(routine: Routine, exercises: List<Exercise>): String =
        buildJson(routine, exercises)

    private fun buildJson(routine: Routine, exercises: List<Exercise>): String {
        val obj = JSONObject().apply {
            put("name", routine.name)
            put("description", routine.description)
            put("muscleGroups", JSONArray(routine.muscleGroups))
            put("daysOfWeek", JSONArray(routine.daysOfWeek))
            put("exercises", JSONArray(exercises.map { ex ->
                JSONObject().apply {
                    put("name", ex.name)
                    put("muscleGroup", ex.muscleGroup)
                    put("equipment", ex.equipment)
                    put("exerciseType", ex.exerciseType)
                    put("targetSets", ex.targetSets)
                    put("initialWeight", ex.initialWeight)
                    put("notes", ex.notes)
                    put("order", ex.order)
                }
            }))
        }
        return obj.toString()
    }

    data class ParsedRoutine(val routine: Routine, val exercises: List<Exercise>)

    /**
     * Parses a string that contains [GYMTRACKER_ROUTINE]...[/GYMTRACKER_ROUTINE] tags.
     * Returns null if parsing fails or tags are not found.
     */
    fun parse(text: String): ParsedRoutine? {
        val start = text.indexOf(TAG_START).takeIf { it >= 0 } ?: return null
        val end = text.indexOf(TAG_END).takeIf { it > start } ?: return null
        val json = text.substring(start + TAG_START.length, end).trim()
        return try {
            val obj = JSONObject(json)
            val muscleGroups = obj.optJSONArray("muscleGroups")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            val daysOfWeek = obj.optJSONArray("daysOfWeek")?.let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }
            } ?: emptyList()
            val routine = Routine(
                name = obj.optString("name", "Rutina importada"),
                description = obj.optString("description", ""),
                muscleGroups = muscleGroups,
                daysOfWeek = daysOfWeek
            )
            val exercisesArr = obj.optJSONArray("exercises")
            val exercises = (0 until (exercisesArr?.length() ?: 0)).map { i ->
                val ex = exercisesArr!!.getJSONObject(i)
                Exercise(
                    name = ex.optString("name", ""),
                    muscleGroup = ex.optString("muscleGroup", ""),
                    equipment = ex.optString("equipment", ""),
                    exerciseType = ex.optString("exerciseType", "STRENGTH"),
                    targetSets = ex.optInt("targetSets", 3),
                    initialWeight = ex.optDouble("initialWeight", 0.0),
                    notes = ex.optString("notes", ""),
                    order = ex.optInt("order", i)
                )
            }
            ParsedRoutine(routine, exercises)
        } catch (e: Exception) {
            null
        }
    }
}
