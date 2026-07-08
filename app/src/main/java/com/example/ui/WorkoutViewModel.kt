package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)

    // --- Core Database Flows ---
    val routines: StateFlow<List<WorkoutRoutine>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scans: StateFlow<List<CalorieScan>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressHistory: StateFlow<List<DailyProgress>> = repository.allProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI UI State variables ---
    private val _workoutGenerationState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val workoutGenerationState: StateFlow<UiState<String>> = _workoutGenerationState.asStateFlow()

    private val _calorieScanState = MutableStateFlow<UiState<CalorieScan>>(UiState.Idle)
    val calorieScanState: StateFlow<UiState<CalorieScan>> = _calorieScanState.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true) // Default to true for dark frosted glass look
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // --- Selected Image for Scanner ---
    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    // --- PDF Download Result ---
    private val _pdfDownloadState = MutableStateFlow<UiState<File>>(UiState.Idle)
    val pdfDownloadState: StateFlow<UiState<File>> = _pdfDownloadState.asStateFlow()

    // Initialize mock data on first launch to make the visual dashboard beautiful
    init {
        viewModelScope.launch {
            repository.allProgress.first().let { list ->
                if (list.isEmpty()) {
                    setupDemoProgressData()
                }
            }
        }
    }

    private suspend fun setupDemoProgressData() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)

        // Seed last 6 days of progress
        val weights = listOf(78.2, 78.1, 78.0, 78.0, 77.9, 77.8)
        val minutes = listOf(45, 0, 60, 30, 45, 50)
        val completions = listOf(1, 0, 1, 1, 1, 1)
        val cals = listOf(2200, 1950, 2400, 2100, 1850, 2300)

        for (i in 0 until 6) {
            val dateStr = sdf.format(cal.time)
            repository.addProgressLog(
                dateString = dateStr,
                weightKg = weights[i],
                activeMinutes = minutes[i],
                workoutsCompleted = completions[i],
                caloriesConsumed = cals[i]
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    // --- Actions ---

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun selectBitmap(bitmap: Bitmap?) {
        _selectedImageBitmap.value = bitmap
        _calorieScanState.value = UiState.Idle
    }

    fun loadBitmapFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateWorkout(goal: String, experience: String, daysPerWeek: Int, notes: String) {
        viewModelScope.launch {
            _workoutGenerationState.value = UiState.Loading
            val success = repository.generateAndSaveWorkout(goal, experience, daysPerWeek, notes)
            if (success) {
                _workoutGenerationState.value = UiState.Success("Workout generated successfully and added to your routines!")
            } else {
                _workoutGenerationState.value = UiState.Error("Failed to generate workout. Please verify your internet connection and Gemini API key.")
            }
        }
    }

    fun clearWorkoutGenerationState() {
        _workoutGenerationState.value = UiState.Idle
    }

    fun scanMeal(bitmap: Bitmap, notes: String) {
        viewModelScope.launch {
            _calorieScanState.value = UiState.Loading
            val scanResult = repository.analyzeAndSaveMeal(bitmap, notes, null)
            if (scanResult != null) {
                _calorieScanState.value = UiState.Success(scanResult)
            } else {
                _calorieScanState.value = UiState.Error("Failed to scan meal. Ensure your image clearly shows food and your Gemini API Key is configured in the Secrets panel.")
            }
        }
    }

    fun clearCalorieScanState() {
        _calorieScanState.value = UiState.Idle
    }

    fun addManualProgress(weight: Double?, activeMin: Int, workouts: Int, calories: Int) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.addProgressLog(
                dateString = today,
                weightKg = weight,
                activeMinutes = activeMin,
                workoutsCompleted = workouts,
                caloriesConsumed = calories
            )
        }
    }

    fun downloadPdf(routine: WorkoutRoutine) {
        viewModelScope.launch {
            _pdfDownloadState.value = UiState.Loading
            val pdfFile = repository.downloadRoutineAsPdf(routine)
            if (pdfFile != null && pdfFile.exists()) {
                _pdfDownloadState.value = UiState.Success(pdfFile)
            } else {
                _pdfDownloadState.value = UiState.Error("Failed to create PDF. Please check your storage.")
            }
        }
    }

    fun clearPdfDownloadState() {
        _pdfDownloadState.value = UiState.Idle
    }

    fun deleteRoutine(id: Int) {
        viewModelScope.launch {
            repository.deleteRoutine(id)
        }
    }

    fun deleteMealScan(id: Int) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }
}
