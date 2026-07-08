@file:OptIn(ExperimentalLayoutApi::class)
package com.example.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalorieScan
import com.example.data.DailyProgress
import com.example.data.Exercise
import com.example.data.WorkoutRoutine
import com.example.ui.components.*
import com.example.ui.theme.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab(val title: String, val iconFilled: ImageVector, val iconOutlined: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    WORKOUTS("Workouts", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    SCANNER("Calorie Scan", Icons.Filled.CameraEnhance, Icons.Outlined.CameraEnhance),
    ANALYTICS("Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    PROFILE("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutApp(viewModel: WorkoutViewModel) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentTab = remember { mutableStateOf(AppTab.DASHBOARD) }
    val context = LocalContext.current

    // Observe PDF download success
    val pdfState by viewModel.pdfDownloadState.collectAsStateWithLifecycle()
    LaunchedEffect(pdfState) {
        when (val state = pdfState) {
            is UiState.Success -> {
                Toast.makeText(context, "PDF saved: ${state.data.name}", Toast.LENGTH_LONG).show()
                openPdfFile(context, state.data)
                viewModel.clearPdfDownloadState()
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.clearPdfDownloadState()
            }
            else -> {}
        }
    }

    MyApplicationTheme(darkTheme = isDark) {
        Scaffold(
            bottomBar = {
                GlassBottomNavigationBar(
                    currentTab = currentTab.value,
                    onTabSelected = { currentTab.value = it },
                    isDark = isDark
                )
            },
            containerColor = Color.Transparent, // Let GlassBackground handle background drawing
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            GlassBackground(isDark = isDark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(
                        targetState = currentTab.value,
                        animationSpec = tween(300),
                        label = "tab_crossfade"
                    ) { tab ->
                        when (tab) {
                            AppTab.DASHBOARD -> DashboardView(viewModel = viewModel, isDark = isDark)
                            AppTab.WORKOUTS -> WorkoutsView(viewModel = viewModel, isDark = isDark)
                            AppTab.SCANNER -> ScannerView(viewModel = viewModel, isDark = isDark)
                            AppTab.ANALYTICS -> AnalyticsView(viewModel = viewModel, isDark = isDark)
                            AppTab.PROFILE -> ProfileView(viewModel = viewModel, isDark = isDark)
                        }
                    }
                }
            }
        }
    }
}

// --- Dashboard Screen ---

@Composable
fun DashboardView(viewModel: WorkoutViewModel, isDark: Boolean) {
    val progressList by viewModel.progressHistory.collectAsStateWithLifecycle()
    val scans by viewModel.scans.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()

    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayProgress = progressList.find { it.dateString == todayDateStr } ?: DailyProgress(dateString = todayDateStr)

    val calorieTarget = 2300
    val activeMinutesTarget = 45

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Hello, Champion!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) TextPrimaryDark else TextPrimaryLight
                    )
                    Text(
                        text = "Track your fitness journey in sleek style.",
                        fontSize = 14.sp,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleDarkMode() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = if (isDark) PrimaryEmerald else SecondaryTeal
                    )
                }
            }
        }

        // Frosted Ring Progress Panel
        item {
            GlassCard(isDark = isDark) {
                Text(
                    text = "TODAY'S SUMMARY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEmerald,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Calories ring
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (todayProgress.caloriesConsumed.toFloat() / calorieTarget).coerceIn(0f, 1f) },
                                modifier = Modifier.size(90.dp),
                                color = PrimaryEmerald,
                                strokeWidth = 8.dp,
                                trackColor = if (isDark) Color(0x1EFFFFFF) else Color(0x1F000000),
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${todayProgress.caloriesConsumed}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                                )
                                Text(
                                    text = "kcal",
                                    fontSize = 10.sp,
                                    color = if (isDark) TextSecondaryDark else TextSecondaryLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Calorie Log",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                    }

                    // Active minutes ring
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (todayProgress.activeMinutes.toFloat() / activeMinutesTarget).coerceIn(0f, 1f) },
                                modifier = Modifier.size(90.dp),
                                color = SecondaryTeal,
                                strokeWidth = 8.dp,
                                trackColor = if (isDark) Color(0x1EFFFFFF) else Color(0x1F000000),
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${todayProgress.activeMinutes}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                                )
                                Text(
                                    text = "min",
                                    fontSize = 10.sp,
                                    color = if (isDark) TextSecondaryDark else TextSecondaryLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active Time",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                    }
                }
            }
        }

        // Today's Workouts Card
        item {
            GlassCard(isDark = isDark) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "WORKOUT TARGETS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal
                        )
                        Text(
                            text = "Routine: ${routines.firstOrNull()?.name ?: "No Active Routine"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                    }
                    IconButton(
                        onClick = { viewModel.addManualProgress(null, 0, 1, 0) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryEmerald.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Complete Workout",
                            tint = PrimaryEmerald
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Workouts Completed Today: ${todayProgress.workoutsCompleted}",
                    fontSize = 13.sp,
                    color = if (isDark) TextSecondaryDark else TextSecondaryLight
                )
            }
        }

        // Nutrition Insights & Scans History
        item {
            Text(
                text = "LATEST FOOD SCANS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (scans.isEmpty()) {
            item {
                GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No meals logged yet today.\nGo to Calorie Scan to scan your meal photo!",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
            }
        } else {
            items(scans.take(3)) { scan ->
                GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scan.foodName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextPrimaryDark else TextPrimaryLight
                            )
                            Text(
                                text = "Protein: ${scan.proteinGrams}g | Carbs: ${scan.carbsGrams}g | Fat: ${scan.fatGrams}g",
                                fontSize = 12.sp,
                                color = if (isDark) TextSecondaryDark else TextSecondaryLight
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${scan.calories} kcal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                            IconButton(
                                onClick = { viewModel.deleteMealScan(scan.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Scan",
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Workout Generator Screen ---

@Composable
fun WorkoutsView(viewModel: WorkoutViewModel, isDark: Boolean) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val genState by viewModel.workoutGenerationState.collectAsStateWithLifecycle()

    var goal by remember { mutableStateOf("Build Muscle") }
    var experience by remember { mutableStateOf("Intermediate") }
    var frequency by remember { mutableStateOf("4") }
    var extraNotes by remember { mutableStateOf("") }

    val goalsList = listOf("Build Muscle", "Weight Loss", "General Fitness", "Endurance")
    val expLevels = listOf("Beginner", "Intermediate", "Advanced")

    var showGoalsDropdown by remember { mutableStateOf(false) }
    var showExpDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "AI WORKOUT GENERATOR",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimaryDark else TextPrimaryLight
            )
            Text(
                text = "Create customized workout plans powered by Gemini.",
                fontSize = 13.sp,
                color = if (isDark) TextSecondaryDark else TextSecondaryLight
            )
        }

        // Generator Options Panel
        item {
            GlassCard(isDark = isDark) {
                Text(
                    text = "TARGETS & PARAMETERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEmerald,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Goal Picker
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    GlassTextField(
                        value = goal,
                        onValueChange = {},
                        label = "Fitness Goal",
                        isDark = isDark,
                        trailingIcon = {
                            IconButton(onClick = { showGoalsDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryEmerald)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showGoalsDropdown,
                        onDismissRequest = { showGoalsDropdown = false },
                        modifier = Modifier.background(if (isDark) DarkBackground else LightBackground)
                    ) {
                        goalsList.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g, color = if (isDark) TextPrimaryDark else TextPrimaryLight) },
                                onClick = {
                                    goal = g
                                    showGoalsDropdown = false
                                }
                            )
                        }
                    }
                }

                // Experience Level Picker
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    GlassTextField(
                        value = experience,
                        onValueChange = {},
                        label = "Experience Level",
                        isDark = isDark,
                        trailingIcon = {
                            IconButton(onClick = { showExpDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryEmerald)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showExpDropdown,
                        onDismissRequest = { showExpDropdown = false },
                        modifier = Modifier.background(if (isDark) DarkBackground else LightBackground)
                    ) {
                        expLevels.forEach { e ->
                            DropdownMenuItem(
                                text = { Text(e, color = if (isDark) TextPrimaryDark else TextPrimaryLight) },
                                onClick = {
                                    experience = e
                                    showExpDropdown = false
                                }
                            )
                        }
                    }
                }

                // Frequency Picker
                GlassTextField(
                    value = frequency,
                    onValueChange = { frequency = it.filter { c -> c.isDigit() } },
                    label = "Workout Days Per Week (2-6)",
                    isDark = isDark,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Coach notes
                GlassTextField(
                    value = extraNotes,
                    onValueChange = { extraNotes = it },
                    label = "Coach Notes (e.g., Focus on back & legs, no squats)",
                    isDark = isDark,
                    singleLine = false,
                    modifier = Modifier.height(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Generate button
                when (val state = genState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            color = PrimaryEmerald,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp)
                        )
                    }
                    is UiState.Success -> {
                        Text(
                            text = state.data,
                            color = PrimaryEmerald,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        GlassButton(
                            onClick = { viewModel.clearWorkoutGenerationState() },
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Acknowledge", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        GlassButton(
                            onClick = { viewModel.clearWorkoutGenerationState() },
                            isDark = isDark,
                            colors = listOf(Color.Red, Color.Red.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        GlassButton(
                            onClick = {
                                val freqInt = frequency.toIntOrNull()?.coerceIn(2, 6) ?: 4
                                viewModel.generateWorkout(goal, experience, freqInt, extraNotes)
                            },
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth().testTag("generate_routine_btn")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Custom Plan", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Generated Routines Header
        item {
            Text(
                text = "SAVED ROUTINES",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (routines.isEmpty()) {
            item {
                GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No custom workout routines yet.\nFill the form above to generate your first routine!",
                        fontSize = 13.sp,
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
            }
        } else {
            items(routines) { routine ->
                val exercises = remember(routine.exercisesJson) {
                    try {
                        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                        val listType = Types.newParameterizedType(List::class.java, Exercise::class.java)
                        moshi.adapter<List<Exercise>>(listType).fromJson(routine.exercisesJson) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextPrimaryDark else TextPrimaryLight
                            )
                            Text(
                                text = "Goal: ${routine.fitnessGoal} (${exercises.size} Exercises)",
                                fontSize = 12.sp,
                                color = if (isDark) TextSecondaryDark else TextSecondaryLight
                            )
                        }
                        Row {
                            IconButton(
                                onClick = { viewModel.downloadPdf(routine) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PrimaryEmerald.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Download PDF", tint = PrimaryEmerald)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.deleteRoutine(routine.id) }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Routine", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }

                    // Display exercises list
                    Spacer(modifier = Modifier.height(10.dp))
                    exercises.forEach { ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ex.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) TextPrimaryDark else TextPrimaryLight,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${ex.sets}x ${ex.repsOrDuration}",
                                fontSize = 12.sp,
                                color = SecondaryTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (ex.notes.isNotEmpty()) {
                            Text(
                                text = "Note: ${ex.notes}",
                                fontSize = 11.sp,
                                color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Calorie Scanner Screen ---

@Composable
fun ScannerView(viewModel: WorkoutViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val imageBitmap by viewModel.selectedImageBitmap.collectAsStateWithLifecycle()
    val scanState by viewModel.calorieScanState.collectAsStateWithLifecycle()

    var customMealPrompt by remember { mutableStateOf("") }

    // Intent launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadBitmapFromUri(it) }
    }

    // Demo meal templates to easily test without uploading a file
    val demoMeals = listOf(
        Pair("🥑 Avocado Toast", "Avocado bacon toast with two fried eggs"),
        Pair("🥗 Salmon Poke Bowl", "Salmon Poke Bowl with white sushi rice, cucumber, and spicy mayo"),
        Pair("🥞 Protein Pancakes", "Berry Protein Pancakes with maple syrup syrup and banana slices"),
        Pair("🥩 Ribeye Steak", "12oz Ribeye steak with sweet potato fries and asparagus")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "CALORIE SCANNER",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextPrimaryDark else TextPrimaryLight
                )
                Text(
                    text = "Snap or choose a photo of your food to auto-measure calories using Gemini.",
                    fontSize = 13.sp,
                    color = if (isDark) TextSecondaryDark else TextSecondaryLight
                )
            }
        }

        // Image selector box
        item {
            GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x11FFFFFF) else Color(0x0F000000))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Meal",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Upload",
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select meal photo from gallery",
                                fontSize = 14.sp,
                                color = if (isDark) TextSecondaryDark else TextSecondaryLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        border = BorderStroke(1.dp, PrimaryEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pick Image", color = PrimaryEmerald)
                    }
                    if (imageBitmap != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.selectBitmap(null) },
                            border = BorderStroke(1.dp, Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear", color = Color.Red)
                        }
                    }
                }
            }
        }

        // Demo Sandbox picker
        item {
            GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SANDBOX: DEMO MEAL SELECTOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryTeal,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "No photos? Click a demo meal template to simulate scanning:",
                    fontSize = 12.sp,
                    color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    demoMeals.forEach { (name, prompt) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryEmerald.copy(alpha = 0.15f))
                                .clickable {
                                    customMealPrompt = prompt
                                    // Generate a simulated mock bitmap so the Gemini API caller works
                                    val size = 200
                                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bitmap)
                                    val paint = android.graphics.Paint()
                                    paint.color = android.graphics.Color.rgb(16, 185, 129)
                                    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                                    viewModel.selectBitmap(bitmap)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(name, fontSize = 12.sp, color = PrimaryEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Custom Prompt input
        item {
            GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                GlassTextField(
                    value = customMealPrompt,
                    onValueChange = { customMealPrompt = it },
                    label = "Optional meal details / description",
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Perform scan action
                when (val state = scanState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            color = PrimaryEmerald,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp)
                        )
                    }
                    is UiState.Success -> {
                        val scan = state.data
                        Text(
                            text = "SUCCESSFULLY SCANNED!",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scan.foodName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                        Text(
                            text = "Calories: ${scan.calories} kcal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                        Text(
                            text = "Protein: ${scan.proteinGrams}g | Carbs: ${scan.carbsGrams}g | Fat: ${scan.fatGrams}g",
                            fontSize = 13.sp,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                        Text(
                            text = scan.analysisText,
                            fontSize = 13.sp,
                            color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassButton(
                            onClick = {
                                viewModel.clearCalorieScanState()
                                customMealPrompt = ""
                            },
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan Another Meal", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        GlassButton(
                            onClick = { viewModel.clearCalorieScanState() },
                            isDark = isDark,
                            colors = listOf(Color.Red, Color.Red.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        GlassButton(
                            onClick = {
                                val bitmap = imageBitmap
                                if (bitmap != null) {
                                    viewModel.scanMeal(bitmap, customMealPrompt)
                                    keyboardController?.hide()
                                } else {
                                    Toast.makeText(context, "Please select/upload an image or choose a demo meal first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth().testTag("scan_meal_btn")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze & Log Meal", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Interactive Progress Analytics Screen ---

@Composable
fun AnalyticsView(viewModel: WorkoutViewModel, isDark: Boolean) {
    val progressList by viewModel.progressHistory.collectAsStateWithLifecycle()

    val calorieData = remember(progressList) {
        progressList.map { Pair(it.dateString.takeLast(5), it.caloriesConsumed.toFloat()) }
    }
    val activeMinutesData = remember(progressList) {
        progressList.map { Pair(it.dateString.takeLast(5), it.activeMinutes.toFloat()) }
    }
    val weightData = remember(progressList) {
        progressList.filter { it.weightKg != null }.map { Pair(it.dateString.takeLast(5), it.weightKg!!.toFloat()) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "FITNESS ANALYTICS",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimaryDark else TextPrimaryLight
            )
            Text(
                text = "Interactive visual charts showing progress over time.",
                fontSize = 13.sp,
                color = if (isDark) TextSecondaryDark else TextSecondaryLight
            )
        }

        // Calorie intake over time (Line Chart)
        item {
            GlassCard(isDark = isDark) {
                Text(
                    text = "DAILY CALORIE INTAKE (kcal)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (calorieData.isEmpty()) {
                    Text("Insufficient progress data.", color = if (isDark) TextSecondaryDark else TextSecondaryLight)
                } else {
                    InteractiveLineGraph(
                        data = calorieData,
                        label = "kcal",
                        lineColor = AccentOrange,
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Active minutes over time (Bar Chart)
        item {
            GlassCard(isDark = isDark) {
                Text(
                    text = "DAILY ACTIVE TIME (minutes)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEmerald,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (activeMinutesData.isEmpty()) {
                    Text("Insufficient progress data.", color = if (isDark) TextSecondaryDark else TextSecondaryLight)
                } else {
                    InteractiveBarChart(
                        data = activeMinutesData,
                        barColor = PrimaryEmerald,
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Weight tracking (Line Chart)
        item {
            GlassCard(isDark = isDark) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BODY WEIGHT TRACKING (kg)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryTeal
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (weightData.isEmpty()) {
                    Text(
                        "No body weight logs registered. Update your current weight in Settings!",
                        color = if (isDark) TextSecondaryDark else TextSecondaryLight,
                        fontSize = 12.sp
                    )
                } else {
                    InteractiveLineGraph(
                        data = weightData,
                        label = "kg",
                        lineColor = SecondaryTeal,
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }
    }
}

// --- Settings/Profile Screen ---

@Composable
fun ProfileView(viewModel: WorkoutViewModel, isDark: Boolean) {
    var inputWeight by remember { mutableStateOf("") }
    var inputMinutes by remember { mutableStateOf("") }
    var inputCalories by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "LOG DAILY PROGRESS",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextPrimaryDark else TextPrimaryLight
            )
            Text(
                text = "Log weights and manual targets to feed the analytics dashboard.",
                fontSize = 13.sp,
                color = if (isDark) TextSecondaryDark else TextSecondaryLight
            )
        }

        // Log parameters
        item {
            GlassCard(isDark = isDark) {
                Text(
                    text = "MANUAL LOGGING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEmerald,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                GlassTextField(
                    value = inputWeight,
                    onValueChange = { inputWeight = it },
                    label = "Today's Weight (kg)",
                    isDark = isDark,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassTextField(
                    value = inputMinutes,
                    onValueChange = { inputMinutes = it },
                    label = "Add Active Minutes",
                    isDark = isDark,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlassTextField(
                    value = inputCalories,
                    onValueChange = { inputCalories = it },
                    label = "Add Manual Calories Consumed",
                    isDark = isDark,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassButton(
                    onClick = {
                        val weight = inputWeight.toDoubleOrNull()
                        val minutes = inputMinutes.toIntOrNull() ?: 0
                        val calories = inputCalories.toIntOrNull() ?: 0

                        if (weight != null || minutes > 0 || calories > 0) {
                            viewModel.addManualProgress(weight, minutes, 0, calories)
                            inputWeight = ""
                            inputMinutes = ""
                            inputCalories = ""
                            Toast.makeText(context, "Progress updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter at least one metric to log progress!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth().testTag("log_metrics_btn")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Metrics", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Appearance settings
        item {
            GlassCard(isDark = isDark) {
                Text(
                    text = "THEMING OPTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryTeal,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Dark Frosted Theme",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) TextPrimaryDark else TextPrimaryLight
                        )
                        Text(
                            text = "Switch between Dark and Light glass visuals",
                            fontSize = 12.sp,
                            color = if (isDark) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryEmerald,
                            checkedTrackColor = PrimaryEmerald.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

// --- Custom Interactive Graph Drawing via Canvas ---

@Composable
fun InteractiveLineGraph(
    data: List<Pair<String, Float>>,
    label: String,
    lineColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val textPaintColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    val gridColor = if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000)

    Canvas(modifier = modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
        val width = size.width
        val height = size.height

        val maxVal = (data.maxOfOrNull { it.second } ?: 100f).coerceAtLeast(1f)
        val minVal = (data.minOfOrNull { it.second } ?: 0f).coerceAtMost(maxVal - 1f)
        val valueRange = maxVal - minVal

        val pointsCount = data.size
        val xInterval = width / (pointsCount - 1).coerceAtLeast(1)

        val strokePath = Path()
        val fillPath = Path()

        // Horizontal dashed grid lines
        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        for (i in 0..4) {
            val gridY = height * (i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1f,
                pathEffect = dashPathEffect
            )
        }

        val mappedPoints = data.mapIndexed { idx, item ->
            val x = idx * xInterval
            // Map value dynamically so the range fits elegantly
            val normalized = (item.second - minVal) / valueRange
            val y = height - (normalized * height * 0.8f) - (height * 0.1f)
            Offset(x, y)
        }

        if (mappedPoints.isNotEmpty()) {
            strokePath.moveTo(mappedPoints[0].x, mappedPoints[0].y)
            fillPath.moveTo(mappedPoints[0].x, height)
            fillPath.lineTo(mappedPoints[0].x, mappedPoints[0].y)

            for (i in 1 until mappedPoints.size) {
                val prev = mappedPoints[i - 1]
                val curr = mappedPoints[i]
                // Draw elegant smooth bezier curves
                val controlX1 = prev.x + (curr.x - prev.x) / 2
                val controlY1 = prev.y
                val controlX2 = prev.x + (curr.x - prev.x) / 2
                val controlY2 = curr.y

                strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
            }

            fillPath.lineTo(mappedPoints.last().x, height)
            fillPath.close()

            // 1. Draw glowing frosted gradient region
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            // 2. Draw thick sleek neon core line
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 4.dp.toPx(), pathEffect = null)
            )

            // 3. Draw key coordinate points
            mappedPoints.forEachIndexed { i, pt ->
                // Outer glow ring
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

@Composable
fun InteractiveBarChart(
    data: List<Pair<String, Float>>,
    barColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val gridColor = if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000)

    Canvas(modifier = modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
        val width = size.width
        val height = size.height

        val maxVal = (data.maxOfOrNull { it.second } ?: 100f).coerceAtLeast(1f)
        val barsCount = data.size
        val barGroupWidth = width / barsCount
        val barWidth = barGroupWidth * 0.6f

        // Grid lines
        for (i in 0..4) {
            val gridY = height * (i / 4f)
            drawLine(
                color = gridColor,
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1f
            )
        }

        data.forEachIndexed { idx, item ->
            val left = idx * barGroupWidth + (barGroupWidth - barWidth) / 2
            val barHeightVal = (item.second / maxVal) * height * 0.85f
            val top = height - barHeightVal

            // Draw clean rounded bars using glowing gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(barColor, barColor.copy(alpha = 0.4f))
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeightVal),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

// --- Glass Bottom Navigation Component ---

@Composable
fun GlassBottomNavigationBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    isDark: Boolean
) {
    val glassBg = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderCol = if (isDark) DarkGlassBorder else LightGlassBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, borderCol, RoundedCornerShape(24.dp)),
        color = glassBg,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                val activeScale by animateFloatAsState(if (isSelected) 1.15f else 0.95f, label = "icon_scale")
                val activeColor = if (isDark) PrimaryEmerald else SecondaryTeal
                val defaultColor = if (isDark) TextSecondaryDark else TextSecondaryLight

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.iconFilled else tab.iconOutlined,
                        contentDescription = tab.title,
                        tint = if (isSelected) activeColor else defaultColor,
                        modifier = Modifier
                            .size(24.dp)
                            .animateContentSize()
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) activeColor else defaultColor
                    )
                }
            }
        }
    }
}

// --- Helper: Securely Open PDF ---
fun openPdfFile(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No PDF viewer found. File downloaded to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
