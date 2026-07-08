package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.network.GeminiClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class WorkoutRepository(private val context: Context) {
    private val db = WorkoutDatabase.getDatabase(context)
    private val dao = db.workoutDao()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Exercise::class.java)
    private val exerciseAdapter = moshi.adapter<List<Exercise>>(listType)

    // --- Flows for UI Consumption ---
    val allRoutines: Flow<List<WorkoutRoutine>> = dao.getAllRoutines()
    val allScans: Flow<List<CalorieScan>> = dao.getAllScans()
    val allProgress: Flow<List<DailyProgress>> = dao.getAllProgress()

    // --- DB Mutations ---
    suspend fun insertRoutine(routine: WorkoutRoutine) = dao.insertRoutine(routine)
    suspend fun deleteRoutine(id: Int) = dao.deleteRoutineById(id)
    suspend fun insertScan(scan: CalorieScan) = dao.insertScan(scan)
    suspend fun deleteScan(id: Int) = dao.deleteScanById(id)

    suspend fun addProgressLog(
        dateString: String,
        weightKg: Double?,
        activeMinutes: Int,
        workoutsCompleted: Int,
        caloriesConsumed: Int
    ) {
        val existing = dao.getProgressForDate(dateString)
        if (existing != null) {
            val updated = existing.copy(
                weightKg = weightKg ?: existing.weightKg,
                activeMinutes = existing.activeMinutes + activeMinutes,
                workoutsCompleted = existing.workoutsCompleted + workoutsCompleted,
                caloriesConsumed = existing.caloriesConsumed + caloriesConsumed
            )
            dao.insertProgress(updated)
        } else {
            dao.insertProgress(
                DailyProgress(
                    dateString = dateString,
                    weightKg = weightKg,
                    activeMinutes = activeMinutes,
                    workoutsCompleted = workoutsCompleted,
                    caloriesConsumed = caloriesConsumed
                )
            )
        }
    }

    // --- Gemini Actions ---

    suspend fun generateAndSaveWorkout(
        goal: String,
        experience: String,
        daysPerWeek: Int,
        notes: String
    ): Boolean {
        val jsonResult = GeminiClient.generateWorkoutRoutine(goal, experience, daysPerWeek, notes) ?: return false
        return try {
            val moshiObject = moshi.adapter(Map::class.java).fromJson(jsonResult)
            val name = moshiObject?.get("name") as? String ?: "Custom $goal Routine"
            val fitnessGoal = moshiObject?.get("fitnessGoal") as? String ?: goal
            
            // Map the parsed exercises to the JSON string we need
            val exercisesRaw = moshiObject?.get("exercises") as? List<*>
            val exercises = mutableListOf<Exercise>()
            exercisesRaw?.forEach { item ->
                val map = item as? Map<*, *>
                val exName = map?.get("name") as? String ?: "Exercise"
                val sets = (map?.get("sets") as? Double)?.toInt() ?: 3
                val reps = map?.get("repsOrDuration") as? String ?: "10 reps"
                val exNotes = map?.get("notes") as? String ?: ""
                exercises.add(Exercise(exName, sets, reps, exNotes))
            }

            val exercisesJson = exerciseAdapter.toJson(exercises)
            val routine = WorkoutRoutine(
                name = name,
                fitnessGoal = fitnessGoal,
                exercisesJson = exercisesJson
            )
            dao.insertRoutine(routine)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun analyzeAndSaveMeal(
        bitmap: Bitmap,
        contextPrompt: String,
        localImagePath: String?
    ): CalorieScan? {
        val jsonResult = GeminiClient.analyzeMealImage(bitmap, contextPrompt) ?: return null
        return try {
            val moshiObject = moshi.adapter(Map::class.java).fromJson(jsonResult)
            val foodName = moshiObject?.get("foodName") as? String ?: "Scanned Meal"
            val calories = (moshiObject?.get("calories") as? Double)?.toInt() ?: 0
            val protein = moshiObject?.get("proteinGrams") as? Double ?: 0.0
            val carbs = moshiObject?.get("carbsGrams") as? Double ?: 0.0
            val fat = moshiObject?.get("fatGrams") as? Double ?: 0.0
            val analysisText = moshiObject?.get("analysisText") as? String ?: "Successfully scanned and calculated."

            val scan = CalorieScan(
                foodName = foodName,
                calories = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                analysisText = analysisText,
                imagePath = localImagePath
            )
            val id = dao.insertScan(scan)
            
            // Auto log calories to today's progress!
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            addProgressLog(
                dateString = today,
                weightKg = null,
                activeMinutes = 0,
                workoutsCompleted = 0,
                caloriesConsumed = calories
            )

            scan.copy(id = id.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- PDF Generator ---

    fun downloadRoutineAsPdf(routine: WorkoutRoutine): File? {
        val exercises = try {
            exerciseAdapter.fromJson(routine.exercisesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // 1. Draw modern styling header / logo background area
        paint.color = Color.parseColor("#0F172A") // Deep Slate Blue
        canvas.drawRect(0f, 0f, 595f, 180f, paint)

        // Draw geometric accent decoration
        paint.color = Color.parseColor("#10B981") // Mint Green Accent
        canvas.drawRect(0f, 175f, 595f, 180f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("GLASSFIT WORKOUTS", 45f, 65f, paint)

        // Routine Name
        paint.color = Color.parseColor("#34D399") // Light Emerald
        paint.textSize = 18f
        canvas.drawText(routine.name, 45f, 105f, paint)

        // Meta (Date + Goal)
        paint.color = Color.parseColor("#94A3B8") // Light slate
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(routine.createdAt))
        canvas.drawText("Goal: ${routine.fitnessGoal}   |   Created: $dateStr", 45f, 135f, paint)

        // Content Title
        paint.color = Color.parseColor("#1E293B") // Dark Slate
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("EXERCISE SCHEDULE & DETAILS", 45f, 220f, paint)

        // Line spacer
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1.5f
        canvas.drawLine(45f, 230f, 550f, 230f, paint)

        var yPos = 265f
        exercises.forEachIndexed { index, exercise ->
            if (yPos > 760f) {
                // If single page overflows, simple safeguard
                return@forEachIndexed
            }

            // Exercise number + name
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("${index + 1}. ${exercise.name}", 45f, yPos, paint)

            // Reps/Sets summary on right
            paint.color = Color.parseColor("#059669") // Darker emerald for contrast
            paint.textSize = 13f
            paint.isFakeBoldText = true
            val setsStr = "${exercise.sets} Sets x ${exercise.repsOrDuration}"
            canvas.drawText(setsStr, 400f, yPos, paint)

            // Instruction Note underneath
            if (exercise.notes.isNotEmpty()) {
                paint.color = Color.parseColor("#475569") // Slate Gray
                paint.textSize = 11f
                paint.isFakeBoldText = false
                canvas.drawText("Tip: ${exercise.notes}", 60f, yPos + 18f, paint)
                yPos += 50f
            } else {
                yPos += 35f
            }

            // Item separator line
            paint.color = Color.parseColor("#F1F5F9")
            paint.strokeWidth = 1.0f
            canvas.drawLine(45f, yPos - 12f, 550f, yPos - 12f, paint)
            yPos += 10f
        }

        // Draw Footer
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Generated exclusively via GlassFit. Your personalized digital fitness card.", 45f, 800f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to Public Documents or app external downloads to easily share
        val downloadsDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val safeFileName = "GlassFit_${routine.name.replace("\\s+".toRegex(), "_")}.pdf"
        val file = File(downloadsDir, safeFileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
