package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.CatalogExercise
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ExerciseCatalogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val catalogCollection = firestore.collection("exercise_catalog")

    // Cache en memoria para no releer Firestore en cada búsqueda de la misma sesión
    private var cachedCatalog: List<CatalogExercise>? = null

    /**
     * Obtiene todos los ejercicios del catálogo global.
     * El resultado se almacena en caché para la sesión actual de la app.
     */
    suspend fun getAllExercises(): List<CatalogExercise> {
        cachedCatalog?.let { return it }
        val snapshot = catalogCollection.orderBy("name").get().await()
        val exercises = snapshot.documents.mapNotNull { doc ->
            doc.toObject(CatalogExercise::class.java)?.copy(id = doc.id)
        }
        cachedCatalog = exercises
        return exercises
    }

    /**
     * Busca ejercicios en el catálogo filtrando por texto (en nombre y aliases)
     * y opcionalmente por grupo muscular.
     * La búsqueda es client-side sobre la lista cacheada para admitir búsquedas parciales y flexibles.
     */
    suspend fun searchExercises(query: String, muscleGroup: String? = null): List<CatalogExercise> {
        val all = getAllExercises()
        val normalizedQuery = query.trim().lowercase()

        return all.filter { exercise ->
            // Filtro por grupo muscular si se especificó
            val matchesMuscle = muscleGroup == null
                || exercise.muscleGroup.equals(muscleGroup, ignoreCase = true)

            // Filtro por texto: busca en nombre y en los aliases
            val matchesQuery = normalizedQuery.isEmpty()
                || exercise.name.lowercase().contains(normalizedQuery)
                || exercise.aliases.any { it.lowercase().contains(normalizedQuery) }
                || exercise.muscleGroup.lowercase().contains(normalizedQuery)
                || exercise.equipment.lowercase().contains(normalizedQuery)

            matchesMuscle && matchesQuery
        }.sortedWith(
            // Prioriza coincidencias en el nombre por encima de aliases
            compareBy { exercise ->
                if (normalizedQuery.isEmpty()) exercise.name
                else if (exercise.name.lowercase().contains(normalizedQuery)) "0_${exercise.name}"
                else "1_${exercise.name}"
            }
        )
    }

    /**
     * Retorna la lista de grupos musculares únicos disponibles en el catálogo.
     */
    suspend fun getMuscleGroups(): List<String> {
        return getAllExercises()
            .map { it.muscleGroup }
            .distinct()
            .sorted()
    }

    /**
     * Verifica si el catálogo ya fue poblado (tiene al menos un documento).
     */
    suspend fun isCatalogPopulated(): Boolean {
        val snapshot = catalogCollection.limit(1).get().await()
        return !snapshot.isEmpty
    }

    /**
     * Añade un ejercicio al catálogo (solo para administradores / seeding).
     */
    suspend fun addExerciseToCatalog(exercise: CatalogExercise) {
        val docRef = catalogCollection.document()
        val saved = exercise.copy(id = docRef.id)
        docRef.set(saved).await()
    }

    /** Invalida la caché (útil si se actualizó el catálogo remotamente). */
    fun invalidateCache() {
        cachedCatalog = null
    }
}
