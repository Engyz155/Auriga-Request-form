package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ClientRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Cohesive Colors matching the Elegant Vibrant Palette (Indigo + Slate Light Theme) ---
private val DeepSlate = Color(0xFFF3F4F6)       // VibrantBg - light grayish slate background
private val CardSlate = Color(0xFFFFFFFF)       // White - clean cards background
private val MechanicalTeal = Color(0xFF4F46E5)  // IndigoPrimary - beautiful Indigo 600
private val LightTeal = Color(0xFFEEF2FF)       // IndigoLight - very light indigo for highlight cards
private val OnTeal = Color(0xFF312E81)          // IndigoOnLight - deep indigo text for alerts/buttons
private val AmberAccent = Color(0xFFD97706)     // Amber Accent - beautiful status colors
private val LightAmber = Color(0xFFFEF3C7)      // Light Amber for critical backgrounds
private val OnAmber = Color(0xFF92400E)         // Dark Amber text
private val SteelBlue = Color(0xFF6366F1)       // IndigoLabel - Slate indigo labels
private val SoftWhite = Color(0xFF0F172A)       // SlateDark - dark body text
private val GrayText = Color(0xFF64748B)        // SlateMuted - nice medium gray for helper texts

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("📝 Request Form", "🗂️ My Requests")

    val allRequests by viewModel.allRequests.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlate),
        containerColor = DeepSlate
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding, bottom = navBarPadding)
        ) {
            // --- Custom Hexagon Header ---
            HeaderSection()

            // --- Elegant Horizontal Navigation Tab ---
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepSlate,
                contentColor = SoftWhite,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = CardSlate, thickness = 1.dp) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = if (index == 1) "$title (${allRequests.size})" else title,
                                color = if (isSelected) MechanicalTeal else GrayText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // --- Selected Tab Content ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> IntakeFormTab(viewModel)
                    1 -> RequestsLogTab(allRequests, viewModel)
                }
            }
        }

        // --- Proposal Detail Dialog Overlay ---
        selectedRequest?.let { request ->
            ProposalDetailDialog(
                request = request,
                onClose = { viewModel.selectRequest(null) },
                onDelete = {
                    viewModel.selectRequest(null)
                    viewModel.deleteRequest(request)
                }
            )
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(DeepSlate, CardSlate)))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hexagon-like "A" Logo
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MechanicalTeal, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "AURIGA AUTOMATION",
                color = SoftWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "Reverse Engineering & Mechanical Design",
                color = MechanicalTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeFormTab(viewModel: MainViewModel) {
    val clientName by viewModel.clientName.collectAsState()
    val contactInfo by viewModel.contactInfo.collectAsState()
    val sector by viewModel.selectedSector.collectAsState()
    val challenge by viewModel.selectedChallenge.collectAsState()
    val description by viewModel.projectDescription.collectAsState()
    val formState by viewModel.formState.collectAsState()

    var sectorExpanded by remember { mutableStateOf(false) }
    var challengeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Form Introduction Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Submit Work Request",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Provide your details below to generate an ASME-standard design draft and request consulting on reverse-engineering, machine redesigns, or custom SPMs.",
                    color = GrayText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // --- Card 1: Client Information ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MechanicalTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "1. Client Identity", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                HorizontalDivider(color = DeepSlate, thickness = 1.dp)

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { viewModel.clientName.value = it },
                    label = { Text("Name / Representative") },
                    placeholder = { Text("Enter client or contact name") },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        focusedBorderColor = MechanicalTeal,
                        unfocusedBorderColor = GrayText,
                        focusedLabelColor = MechanicalTeal,
                        unfocusedLabelColor = GrayText
                    )
                )

                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { viewModel.contactInfo.value = it },
                    label = { Text("Email or Phone Number") },
                    placeholder = { Text("Enter contact info for proposal delivery") },
                    leadingIcon = { Icon(Icons.Default.ContactMail, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_contact_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        focusedBorderColor = MechanicalTeal,
                        unfocusedBorderColor = GrayText,
                        focusedLabelColor = MechanicalTeal,
                        unfocusedLabelColor = GrayText
                    )
                )
            }
        }

        // --- Card 2: Technical Specifications ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = AmberAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "2. Project Specifications", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                HorizontalDivider(color = DeepSlate, thickness = 1.dp)

                // --- Sector Selection Dropdown ---
                Text(text = "Industrial Sector Sector", color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                ExposedDropdownMenuBox(
                    expanded = sectorExpanded,
                    onExpandedChange = { sectorExpanded = !sectorExpanded }
                ) {
                    OutlinedTextField(
                        value = sector,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectorExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite,
                            focusedBorderColor = MechanicalTeal,
                            unfocusedBorderColor = GrayText
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = sectorExpanded,
                        onDismissRequest = { sectorExpanded = false },
                        modifier = Modifier.background(CardSlate)
                    ) {
                        SECTORS.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection, color = SoftWhite) },
                                onClick = {
                                    viewModel.onSectorSelected(selection)
                                    sectorExpanded = false
                                },
                                modifier = Modifier.background(CardSlate)
                            )
                        }
                    }
                }

                // --- Specific Challenge Dropdown ---
                Text(text = "Specific Sector Problem / Line Challenge", color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                ExposedDropdownMenuBox(
                    expanded = challengeExpanded,
                    onExpandedChange = { challengeExpanded = !challengeExpanded }
                ) {
                    OutlinedTextField(
                        value = challenge,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = challengeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite,
                            focusedBorderColor = MechanicalTeal,
                            unfocusedBorderColor = GrayText
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = challengeExpanded,
                        onDismissRequest = { challengeExpanded = false },
                        modifier = Modifier.background(CardSlate)
                    ) {
                        val challenges = SECTOR_CHALLENGES[sector] ?: emptyList()
                        challenges.forEach { selection ->
                            val isCritical = selection.contains("Critical", ignoreCase = true)
                            val isFrequent = selection.contains("Frequent", ignoreCase = true)
                            val tagColor = if (isCritical) OnAmber else if (isFrequent) OnTeal else GrayText
                            val tagBg = if (isCritical) LightAmber else if (isFrequent) LightTeal else CardSlate

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selection.substringBefore(" ("),
                                            color = SoftWhite,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selection.contains("(")) {
                                            Box(
                                                modifier = Modifier
                                                    .background(tagBg, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = selection.substringAfter("(").substringBefore(")"),
                                                    color = tagColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectedChallenge.value = selection
                                    challengeExpanded = false
                                },
                                modifier = Modifier.background(CardSlate)
                            )
                        }
                    }
                }

                // --- Detailed Project Description ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Project Specifications & Technical Constraints",
                        color = GrayText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val isAnalyzingVoice by viewModel.isAnalyzingVoice.collectAsState()
                    val context = LocalContext.current
                    val speechRecognizerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val data = result.data
                            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            val spokenText = results?.getOrNull(0) ?: ""
                            if (spokenText.isNotEmpty()) {
                                viewModel.analyzeAndAddVoiceInput(spokenText)
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell us your project specifications...")
                                }
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Voice recognition is not supported on this device.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("voice_input_button")
                    ) {
                        if (isAnalyzingVoice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MechanicalTeal,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MechanicalTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.projectDescription.value = it },
                    placeholder = { Text("Details about physical measurements, broken components, or standards needed...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("client_description_input"),
                    minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        focusedBorderColor = MechanicalTeal,
                        unfocusedBorderColor = GrayText
                    )
                )
            }
        }

        // --- Card 4: Photo/Attachment (Camera Section) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MechanicalTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "4. Attachment Photo (Optional)", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                HorizontalDivider(color = DeepSlate, thickness = 1.dp)

                val capturedImageState by viewModel.capturedImage.collectAsState()
                val context = LocalContext.current

                // Launcher for Camera intent (ACTION_IMAGE_CAPTURE)
                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val bitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
                        if (bitmap != null) {
                            viewModel.capturedImage.value = bitmap
                        } else {
                            Toast.makeText(context, "Failed to capture image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Launcher for Permission request
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        try {
                            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            cameraLauncher.launch(cameraIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Camera could not be opened: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Camera permission is required to capture photos of components.", Toast.LENGTH_LONG).show()
                    }
                }

                if (capturedImageState != null) {
                    // Show captured image preview with option to retake/remove
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = capturedImageState!!.asImageBitmap(),
                            contentDescription = "Captured Component Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.capturedImage.value = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    // Trigger camera directly since permission is already granted
                                    val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                                    cameraLauncher.launch(cameraIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MechanicalTeal),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Capture CTA
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add a photo of the mechanical system, broken spare part, or physical dimensions to enrich your AI engineering proposal.",
                            color = GrayText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                // Request CAMERA permission
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MechanicalTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capture Part Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // --- Custom Animated Submission Status Action ---
        SubmitActionButton(formState = formState, onSubmit = { viewModel.submitRequest() })

        // Error message if any
        if (formState is FormUiState.Error) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (formState as FormUiState.Error).message,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.resetFormState() }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SoftWhite)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SubmitActionButton(formState: FormUiState, onSubmit: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "Rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "sync"
    )

    when (formState) {
        FormUiState.Idle, is FormUiState.Error -> {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MechanicalTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Submit & Sync Request", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        FormUiState.Syncing -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = CardSlate),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Syncing Symbol",
                        tint = AmberAccent,
                        modifier = Modifier.rotate(rotation)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Syncing Local Request...", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        FormUiState.GeneratingProposal -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = CardSlate),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MechanicalTeal,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "AI Drafting ASME Proposal (Thinking Level High)...", color = MechanicalTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        is FormUiState.Success -> {
            Button(
                onClick = {},
                enabled = true, // Tap to dismiss / revert back
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OnTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.CloudDone, contentDescription = "Data Cloud Confirmation", tint = LightTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Synced & Confirmed!", color = LightTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun RequestsLogTab(requests: List<ClientRequest>, viewModel: MainViewModel) {
    if (requests.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = GrayText,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Proposals Found",
                color = SoftWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Once you submit a client request in the Form tab, the generated AI engineering proposal will instantly sync and display here in this dashboard.",
                color = GrayText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Dashboard Summary Header
            Text(
                text = "Engineering Proposals Dashboard",
                color = SoftWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // KPI Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Total
                Card(
                    modifier = Modifier.weight(1.1f),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Total Proposals", color = GrayText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, tint = MechanicalTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = requests.size.toString(), color = SoftWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Metric 2: Synced to Cloud
                val syncedCount = requests.count { it.isSynced }
                Card(
                    modifier = Modifier.weight(1.1f),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Cloud Synced", color = GrayText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = syncedCount.toString(), color = SoftWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Metric 3: Pending
                val pendingCount = requests.size - syncedCount
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(text = "Pending Sync", color = GrayText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = pendingCount.toString(), color = SoftWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = "Proposals List (${requests.size})",
                color = GrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(requests) { request ->
                    RequestCard(request = request, onClick = { viewModel.selectRequest(request) })
                }
            }
        }
    }
}

@Composable
fun RequestCard(request: ClientRequest, onClick: () -> Unit) {
    val date = remember(request.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(request.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("request_card_${request.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Sector Tag and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(MechanicalTeal.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = request.sector,
                        color = MechanicalTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = date,
                    color = GrayText,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Challenge Title
            Text(
                text = request.challenge.substringBefore(" ("),
                color = SoftWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Client Info
            Text(
                text = "Client: ${request.clientName} | ${request.contactInfo}",
                color = GrayText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dividers & Badges
            HorizontalDivider(color = DeepSlate, thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sync Status Badge
                    if (request.isSynced) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD1FAE5), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Cloud Synced",
                                color = Color(0xFF065F46),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(LightAmber, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Local Draft",
                                color = OnAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Proposal Status Badge
                    val hasProposal = !request.proposalDraft.isNullOrBlank()
                    Box(
                        modifier = Modifier
                            .background(if (hasProposal) LightTeal else Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (hasProposal) "Proposal Ready" else "Drafting...",
                            color = if (hasProposal) OnTeal else GrayText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // View Details Arrow Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View Details",
                        color = MechanicalTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View details",
                        tint = MechanicalTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProposalDetailDialog(request: ClientRequest, onClose: () -> Unit, onDelete: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            color = CardSlate
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(CardSlate, DeepSlate)))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Engineering Proposal Draft", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Target Standard: ASME Y14.2", color = MechanicalTeal, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SoftWhite)
                    }
                }

                // Proposal Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick stats
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepSlate),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Client Name: ${request.clientName}", color = SoftWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Contact: ${request.contactInfo}", color = SoftWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Sector: ${request.sector}", color = SoftWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Selected Problem Line: ${request.challenge}", color = SoftWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    HorizontalDivider(color = DeepSlate, thickness = 1.dp)

                    // Rich proposal text
                    Text(
                        text = request.proposalDraft ?: "Proposal draft is generating...",
                        color = SoftWhite,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.Monospace // Gives clean technical typewriter aesthetic
                    )
                }

                // Actions Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepSlate)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            request.proposalDraft?.let {
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MechanicalTeal),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Proposal", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}




@Composable
fun AiConsultantTab(viewModel: MainViewModel) {
    val aiResponse by viewModel.aiResponse.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    var chatInput by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        "What is ASME Y14.2 drafting standard?",
        "How do you measure broken parts with no documentation?",
        "What CAD software do you support?",
        "What is your fastest turnaround for high-precision parts?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chat Window Title
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MechanicalTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "AI Engineering Assistant", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Expert feedback on mechanical drafting, standards, & manufacturing rules", color = GrayText, fontSize = 11.sp)
                }
            }
        }

        // Quick Suggestion Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Frequently Asked Consulting Questions:", color = GrayText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickQuestions.forEach { question ->
                    Box(
                        modifier = Modifier
                            .background(CardSlate, RoundedCornerShape(20.dp))
                            .clickable {
                                chatInput = question
                                viewModel.askAiAssistant(question)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = question, color = MechanicalTeal, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Response Display Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CardSlate, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (aiLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MechanicalTeal)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Consulting Technical Database...", color = GrayText, fontSize = 12.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = aiResponse ?: "Ask our AI Assistant any engineering or standards question (e.g. details on SolidWorks modelling, casting parameters, or ASME Y14.2 templates).",
                        color = if (aiResponse != null) SoftWhite else GrayText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontFamily = if (aiResponse != null) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }

        // Chat Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Ask about materials, specs, reverse engineering...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite,
                    focusedBorderColor = MechanicalTeal,
                    unfocusedBorderColor = CardSlate
                ),
                singleLine = true
            )

            Button(
                onClick = {
                    viewModel.askAiAssistant(chatInput)
                    chatInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = MechanicalTeal),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}


@Composable
fun AboutAurigaTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Corporate Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(number = "15+", label = "Projects Delivered", modifier = Modifier.weight(1f))
            StatCard(number = "4", label = "Industries Served", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(number = "3", label = "Core Service Lines", modifier = Modifier.weight(1f))
            StatCard(number = "14 Days", label = "Fastest Turnaround", modifier = Modifier.weight(1f))
        }

        // About Us Description
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "About Auriga Automation", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Auriga Automation is a premier mechanical design and reverse-engineering consultancy based in Hooghly, West Bengal, serving fabrication units, foundries, and manufacturers across Kolkata and Howrah. We specialize in recreating production-ready designs for machines and parts that have no surviving documentation, redesigning equipment already in use, and developing special-purpose machines built around real-world workshop capacities.",
                    color = GrayText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MechanicalTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Hooghly, West Bengal, India", color = SoftWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Core Service Lines
        Text(text = "Core Service Lines", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        ServiceLineCard(
            icon = Icons.Default.Sync,
            title = "⚙ Reverse Engineering & Recreation",
            desc = "Rebuilding detailed 3D models, manufacturing drawings, and full Bills of Materials (BOM) for broken, worn out, or completely undocumented machine components."
        )

        ServiceLineCard(
            icon = Icons.Default.Settings,
            title = "🔄 Machine Redesign & Modification",
            desc = "Upgrading, automating, or fixing structural weaknesses in operational equipment, including customization of machines imported from abroad."
        )

        ServiceLineCard(
            icon = Icons.Default.Build,
            title = "📐 New Machine & SPM Design",
            desc = "Delivering complete, production-ready engineering packages for new Special Purpose Machines (SPM), precisely scoped to actual shop floor manufacturing capabilities."
        )

        // Quality & Standards Highlight
        Card(
            colors = CardDefaults.cardColors(containerColor = MechanicalTeal.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.border(1.dp, MechanicalTeal, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MechanicalTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "ASME Y14.2 Compliant Drafting", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Every design and 2D blueprint released by Auriga Automation strictly adheres to ASME Y14.2 standards with rigorous GD&T (Geometric Dimensioning & Tolerancing) control. This guarantees flawless manufacturing-readiness on the shop floor with zero dimensional ambiguity.",
                    color = SoftWhite.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Sample Catalog Outline
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Reference Case Catalog", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                HorizontalDivider(color = DeepSlate)

                ReferenceRow(title = "Roller Conveyor (MDR)", details = "ASME Y14.2 compliant, chassis & 3-point leg assembly, full BOM.")
                ReferenceRow(title = "STRIP-01 Gripper Mounting", details = "7075-T6 aluminium alloy. Parallelism held to 0.1 mm, hole tolerance ±0.10 mm.")
                ReferenceRow(title = "Robot Tool Adapter", details = "7075-O aluminium alloy. Twin bolt-circle interface, mating parallelism 0.05 mm.")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatCard(number: String, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = number, color = MechanicalTeal, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = GrayText, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ServiceLineCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = MechanicalTeal, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = desc, color = GrayText, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun ReferenceRow(title: String, details: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, color = MechanicalTeal, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(text = details, color = GrayText, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(modifier = Modifier.height(6.dp))
    }
}
