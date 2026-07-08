package com.example.data

import android.content.Context
import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

// --- Data Models ---

@Entity(tableName = "workout_routines")
data class WorkoutRoutine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val fitnessGoal: String,
    val exercisesJson: String, // JSON Array of Exercise objects
    val createdAt: Long = System.currentTimeMillis()
)

data class Exercise(
    val name: String,
    val sets: Int,
    val repsOrDuration: String,
    val notes: String = ""
)

@Entity(tableName = "calorie_scans")
data class CalorieScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val foodName: String,
    val calories: Int,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val analysisText: String = "",
    val imagePath: String? = null // local file path to the scanned image
)

@Entity(tableName = "daily_progress", indices = [Index(value = ["dateString"], unique = true)])
data class DailyProgress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // Format: YYYY-MM-DD
    val weightKg: Double? = null,
    val activeMinutes: Int = 0,
    val workoutsCompleted: Int = 0,
    val caloriesConsumed: Int = 0
)

// --- Converters ---

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Exercise::class.java)
    private val adapter = moshi.adapter<List<Exercise>>(listType)

    @TypeConverter
    fun fromExercisesList(exercises: List<Exercise>?): String {
        return exercises?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toExercisesList(json: String?): List<Exercise> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// --- DAOs ---

@Dao
interface WorkoutDao {
    // Workout Routines
    @Query("SELECT * FROM workout_routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<WorkoutRoutine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: WorkoutRoutine): Long

    @Query("DELETE FROM workout_routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Int)

    // Calorie Scans
    @Query("SELECT * FROM calorie_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<CalorieScan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: CalorieScan): Long

    @Query("DELETE FROM calorie_scans WHERE id = :id")
    suspend fun deleteScanById(id: Int)

    // Daily Progress
    @Query("SELECT * FROM daily_progress ORDER BY dateString ASC")
    fun getAllProgress(): Flow<List<DailyProgress>>

    @Query("SELECT * FROM daily_progress WHERE dateString = :dateLimit LIMIT 1")
    suspend fun getProgressForDate(dateLimit: String): DailyProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: DailyProgress): Long
}

// --- Database ---

@Database(
    entities = [WorkoutRoutine::class, CalorieScan::class, DailyProgress::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_fitness_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
