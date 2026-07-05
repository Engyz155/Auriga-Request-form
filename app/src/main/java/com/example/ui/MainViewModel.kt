package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.ThinkingConfig
import com.example.api.GoogleSheetsClient
import com.example.api.CreateSpreadsheetRequest
import com.example.api.SpreadsheetProperties
import com.example.api.ValueRange
import com.example.data.AppDatabase
import com.example.data.ClientRequest
import com.example.data.ClientRequestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FormUiState {
    object Idle : FormUiState
    object Syncing : FormUiState
    object GeneratingProposal : FormUiState
    data class Success(val requestId: Long) : FormUiState
    data class Error(val message: String) : FormUiState
}

class MainViewModel(
    private val repository: ClientRequestRepository,
    private val sharedPreferences: SharedPreferences? = null
) : ViewModel() {

    // --- Form Inputs ---
    val clientName = MutableStateFlow("")
    val contactInfo = MutableStateFlow("")
    val selectedSector = MutableStateFlow("FMCG & Chemicals")
    val selectedChallenge = MutableStateFlow("Packaging line breakdown — no spare drawing (Critical)")
    val projectDescription = MutableStateFlow("")

    // --- Voice Assistant State ---
    val isAnalyzingVoice = MutableStateFlow(false)

    init {
        // Load draft if sharedPreferences is provided
        sharedPreferences?.let { prefs ->
            val savedName = prefs.getString("draft_client_name", "") ?: ""
            val savedContact = prefs.getString("draft_contact_info", "") ?: ""
            val savedSector = prefs.getString("draft_selected_sector", "FMCG & Chemicals") ?: "FMCG & Chemicals"
            val savedChallenge = prefs.getString("draft_selected_challenge", "") ?: ""
            val savedDescription = prefs.getString("draft_project_description", "") ?: ""

            if (savedName.isNotEmpty()) clientName.value = savedName
            if (savedContact.isNotEmpty()) contactInfo.value = savedContact
            if (savedSector.isNotEmpty()) selectedSector.value = savedSector
            if (savedChallenge.isNotEmpty()) selectedChallenge.value = savedChallenge
            if (savedDescription.isNotEmpty()) projectDescription.value = savedDescription
        }

        // Auto-save form inputs as the user types
        sharedPreferences?.let { prefs ->
            viewModelScope.launch {
                clientName.collect { prefs.edit().putString("draft_client_name", it).apply() }
            }
            viewModelScope.launch {
                contactInfo.collect { prefs.edit().putString("draft_contact_info", it).apply() }
            }
            viewModelScope.launch {
                selectedSector.collect { prefs.edit().putString("draft_selected_sector", it).apply() }
            }
            viewModelScope.launch {
                selectedChallenge.collect { prefs.edit().putString("draft_selected_challenge", it).apply() }
            }
            viewModelScope.launch {
                projectDescription.collect { prefs.edit().putString("draft_project_description", it).apply() }
            }
        }
    }

    // --- List of Submissions ---
    val allRequests: StateFlow<List<ClientRequest>> = repository.allRequests
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Selected Request for Proposal Detail ---
    private val _selectedRequest = MutableStateFlow<ClientRequest?>(null)
    val selectedRequest = _selectedRequest.asStateFlow()

    // --- Form Submission State ---
    private val _formState = MutableStateFlow<FormUiState>(FormUiState.Idle)
    val formState = _formState.asStateFlow()

    // --- General AI Chat / Question State ---
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse = _aiResponse.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    fun selectRequest(request: ClientRequest?) {
        _selectedRequest.value = request
    }

    fun updatePrefilledDescription() {
        // No-op to respect user intent: "Project Specification will be filled by customer only I dont want it to be pre filled"
    }

    fun onSectorSelected(sector: String) {
        selectedSector.value = sector
        val challenges = SECTOR_CHALLENGES[sector] ?: emptyList()
        if (challenges.isNotEmpty()) {
            selectedChallenge.value = challenges.first()
        } else {
            selectedChallenge.value = "Custom solution request (General)"
        }
    }

    fun submitRequest() {
        val name = clientName.value.trim()
        val contact = contactInfo.value.trim()
        val sector = selectedSector.value
        val challenge = selectedChallenge.value
        val description = projectDescription.value.trim()

        if (name.isEmpty() || contact.isEmpty() || description.isEmpty()) {
            _formState.value = FormUiState.Error("Please fill out all required fields (Name, Contact, Description).")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Enter Syncing Mode (Spin sync icon)
                _formState.value = FormUiState.Syncing
                
                // Pre-save request as unsynced draft
                val initialRequest = ClientRequest(
                    clientName = name,
                    contactInfo = contact,
                    sector = sector,
                    challenge = challenge,
                    description = description,
                    isSynced = false
                )
                val requestId = repository.insert(initialRequest)
                
                // Upload to Google Sheets
                var isSheetsSynced = false
                var syncError: String? = null
                try {
                    syncToGoogleSheets(name, contact, sector, challenge, description)
                    isSheetsSynced = true
                } catch (e: Exception) {
                    syncError = e.localizedMessage ?: "Failed to upload to Google Sheets."
                }

                // Transition to AI Proposal Generation
                _formState.value = FormUiState.GeneratingProposal
                
                val proposal = generateProposalViaGemini(name, contact, sector, challenge, description)

                // Update Request with synced status and the proposal draft
                val updatedRequest = initialRequest.copy(
                    id = requestId,
                    proposalDraft = proposal,
                    isSynced = isSheetsSynced
                )
                repository.update(updatedRequest)

                // 3. Confirm submission and show "Data Cloud" checkmark
                if (isSheetsSynced) {
                    _formState.value = FormUiState.Success(requestId)
                } else {
                    _formState.value = FormUiState.Error("Proposal generated locally, but failed to sync with Google Sheets: $syncError")
                }
                
                // Reset form inputs (Project Specification starts completely blank!)
                clientName.value = ""
                contactInfo.value = ""
                projectDescription.value = ""

            } catch (e: Exception) {
                _formState.value = FormUiState.Error(e.localizedMessage ?: "Failed to save or sync request.")
            }
        }
    }

    private fun getAccessToken(): String {
        val envToken = System.getenv("GOOGLE_ACCESS_TOKEN")
        if (!envToken.isNullOrEmpty()) {
            return envToken
        }
        val configToken = BuildConfig.GOOGLE_ACCESS_TOKEN
        if (configToken.isNotEmpty() && configToken != "GOOGLE_ACCESS_TOKEN_PLACEHOLDER") {
            return configToken
        }
        return ""
    }

    private suspend fun syncToGoogleSheets(
        name: String,
        contact: String,
        sector: String,
        challenge: String,
        description: String
    ) {
        val token = getAccessToken()
        if (token.isEmpty()) {
            throw Exception("Google Workspace authorization not found. Please click 'Accept' on the OAuth prompt to sync with Google Sheets.")
        }

        val authHeader = "Bearer $token"

        // Search for existing spreadsheet in Drive
        val query = "name = 'Auriga Client Requests' and mimeType = 'application/vnd.google-apps.spreadsheet' and trashed = false"
        val searchResponse = GoogleSheetsClient.service.searchFiles(authHeader, query)
        var spreadsheetId = searchResponse.files.firstOrNull()?.id

        // Create a new spreadsheet if not found
        if (spreadsheetId == null) {
            val createRequest = CreateSpreadsheetRequest(
                properties = SpreadsheetProperties(title = "Auriga Client Requests")
            )
            val createResponse = GoogleSheetsClient.service.createSpreadsheet(authHeader, createRequest)
            spreadsheetId = createResponse.spreadsheetId

            // Add Header row
            val headers = listOf(
                listOf("Timestamp", "Client Name", "Contact Info", "Industrial Sector", "Challenge Selected", "Project Specifications")
            )
            try {
                GoogleSheetsClient.service.appendValues(
                    authHeader = authHeader,
                    spreadsheetId = spreadsheetId,
                    range = "Sheet1!A1",
                    valueInputOption = "USER_ENTERED",
                    body = ValueRange(values = headers)
                )
            } catch (e: Exception) {
                // Non-blocking, continue to write row
            }
        }

        // Format and append current client request row
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val row = listOf(
            listOf(timestamp, name, contact, sector, challenge, description)
        )
        GoogleSheetsClient.service.appendValues(
            authHeader = authHeader,
            spreadsheetId = spreadsheetId,
            range = "Sheet1!A1",
            valueInputOption = "USER_ENTERED",
            body = ValueRange(values = row)
        )
    }

    fun analyzeAndAddVoiceInput(spokenText: String) {
        if (spokenText.isBlank()) return
        viewModelScope.launch {
            isAnalyzingVoice.value = true
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI_API_KEY")) {
                    projectDescription.value = spokenText
                    return@launch
                }

                val systemPrompt = """
                    You are Aditya Maity's AI Assistant for Auriga Automation (mechanical design consultancy).
                    The user has dictated their project specification or engineering problem via voice.
                    Your task is to analyze the raw spoken text and transform it into a highly professional, structured, detailed, and technically precise Project Specification block.
                    Utilize appropriate mechanical design terminology (e.g. reverse engineering, SolidWorks drawings, ASME Y14.2 drafting standards, material selection, fabrication parameters).
                    Do not include any conversational pleasantries, preambles, or chat text. Just return the refined Project Specification directly.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = "Raw spoken voice input: $spokenText")))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    projectDescription.value = responseText.trim()
                } else {
                    projectDescription.value = spokenText
                }
            } catch (e: Exception) {
                projectDescription.value = spokenText
            } finally {
                isAnalyzingVoice.value = false
            }
        }
    }

    fun resetFormState() {
        _formState.value = FormUiState.Idle
    }

    fun deleteRequest(request: ClientRequest) {
        viewModelScope.launch {
            repository.delete(request)
        }
    }

    // Direct general AI assistance chat using gemini-3.5-flash (fast, general tasks)
    fun askAiAssistant(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResponse.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val systemPrompt = """
                    You are Aditya Maity's AI Assistant for Auriga Automation (mechanical design consultancy in Hooghly).
                    You are knowledgeable about reverse engineering, ASME Y14.2 drawing standards, SolidWorks, AutoCAD, 
                    3D modelling, material handling, Jute mills machinery, and local workshop fabrication capabilities in Kolkata/Howrah.
                    Answer the user's technical or consulting query in a friendly, helpful, and highly professional manner.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = query)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = request
                )
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _aiResponse.value = responseText ?: "I'm sorry, I couldn't process that query."
            } catch (e: Exception) {
                _aiResponse.value = "Consulting Assistant is currently offline. Here is a localized tip: Please review standard engineering parameters or try submitting your request to sync with the main database."
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private suspend fun generateProposalViaGemini(
        name: String,
        contact: String,
        sector: String,
        challenge: String,
        description: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI_API_KEY")) {
            return getFallbackProposal(name, contact, sector, challenge, description)
        }

        return try {
            val systemPrompt = """
                You are the AI Engineering Consultant for Auriga Automation, a prestigious mechanical design and reverse-engineering consultancy based in Hooghly, West Bengal (serving Kolkata, Howrah, and surrounding industrial regions).
                Your role is to analyze a client's work request (their name, contact info, sector, specific challenge, and details) and generate a highly professional, detailed, structured, standards-compliant (ASME Y14.2) engineering proposal draft.
                
                AURIGA AUTOMATION COMPANY PROFILE & CREDENTIALS:
                - Core Services:
                  1. Reverse Engineering & Recreation: Rebuilding 3D models, manufacturing drawings, and BOMs for broken, worn, or undocumented machines and parts.
                  2. Machine Redesign & Modification: Upgrading, automating, or fixing weaknesses in equipment already in operation—including imported machines.
                  3. New Machine & SPM (Special Purpose Machine) Design: Production-ready design packages scoped to a workshop's real manufacturing capability.
                - Key Achievements: 15+ projects delivered, 4 major industries served, 14 days fastest turnaround.
                - Engineering Philosophy: Every drawing we hand over is built for what a shop floor can actually execute (ASME Y14.2 drafting standards, GD&T tolerancing, full BOM).
                - Team: Hands-on experience across material handling, process equipment, automation, structural systems, piping, fabrication, and maintenance.
                - Lead Consultant: Aditya Maity (adityamaity35@gmail.com, +91 80137 64162).
                
                PROPOSAL STRUCTURE REQUIREMENTS:
                The generated proposal draft MUST be elegant, authoritative, and structured with the following sections (use clear Markdown formatting):
                1. PROPOSAL TITLE: [A professional project title, e.g., "Engineering Proposal: Reverse Engineering of Packaging Line Components for FMCG Plant"]
                2. CLIENT DETAILS: [Summarize Client Name, Contact Info, Sector]
                3. PROBLEM ANALYSIS: [Analyze the specific challenge and why it is critical, e.g., downtime cost of lack of documentation, audit non-conformance, etc.]
                4. PROPOSED SOLUTION & TECHNICAL METHODOLOGY: [Detail a robust 3-stage methodology based on Auriga's workflow: 1. Visit & Measure (precise on-site inspection), 2. Design & Tolerance (modeling in SolidWorks/AutoCAD with ASME standards and GD&T), 3. Deliver (manufacturing drawings, complete BOM, or validated prototype)]
                5. KEY DELIVERABLES: [Specific outputs like 3D CAD files, ASME Y14.2-compliant 2D fabrication drawings, Bill of Materials (BOM), etc.]
                6. TIMELINE & REASSURANCE: [Suggest a realistic timeline, referencing Auriga's rapid turnaround, e.g., 10-14 days, and encourage starting small with a single part if they want to evaluate quality first]
                7. CALL TO ACTION / SIGN-OFF: [Standard professional closing from Aditya Maity, Lead Mechanical Design Consultant, Auriga Automation]
                
                Maintain a highly professional, technically precise, encouraging, and collaborative tone. Keep it specific to the engineering details provided.
            """.trimIndent()

            val prompt = """
                Generate a custom engineering proposal for:
                Client Name: $name
                Contact Info: $contact
                Industrial Sector: $sector
                Selected Challenge: $challenge
                Detailed Description of Need: $description
            """.trimIndent()

            // Using gemini-3.1-pro-preview with ThinkingLevel.HIGH as mandated by system instructions
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    thinkingConfig = ThinkingConfig(thinkingLevel = "high")
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )

            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: getFallbackProposal(name, contact, sector, challenge, description)

        } catch (e: Exception) {
            getFallbackProposal(name, contact, sector, challenge, description, errorDetails = e.localizedMessage)
        }
    }

    private fun getFallbackProposal(
        name: String,
        contact: String,
        sector: String,
        challenge: String,
        description: String,
        errorDetails: String? = null
    ): String {
        return """
            # ENGINEERING PROPOSAL DRAFT
            *Prepared by Auriga Automation — Hooghly, West Bengal*
            
            **Project Title:** Mechanical Engineering Support & Solutions: $challenge
            
            ## 1. CLIENT & PROJECT OVERVIEW
            - **Client Name:** $name
            - **Contact Info:** $contact
            - **Industry Sector:** $sector
            - **Project Requirement:** $challenge
            
            ## 2. PROBLEM ANALYSIS
            The client has requested engineering assistance regarding: **$challenge**. 
            Our analysis indicates that addressing this issue is critical to minimize production downtime, restore operational capacity, or satisfy standard compliance parameters. Lack of drawing documentation or surviving baselines often results in prolonged breakdown delays and high replacement overheads.
            
            ## 3. PROPOSED METHODOLOGY BY AURIGA AUTOMATION
            Auriga Automation proposes our proven 3-stage workshop-aligned workflow to address this requirement:
            
            1. **Visit & Measure (On-Site Audit):**
               Our engineering team will visit your facility to inspect, measure, and analyze the broken or undocumented machinery on-site to extract the precise baseline constraints.
            
            2. **Design & Tolerance (High-Fidelity CAD Modeling):**
               Reconstruct high-precision 3D models in professional CAD suites (SolidWorks/AutoCAD/Inventor). We apply robust engineering judgment and datums, implementing **ASME Y14.2 drafting standards** and GD&T tolerancing to ensure manufacturing-readiness.
            
            3. **Deliver (Fabrication Handover):**
               Deliver fully detailed 2D fabrication drawing packages, clean assemblies, and a structured Bill of Materials (BOM) for smooth shop floor execution.
            
            ## 4. KEY DELIVERABLES
            - Complete ASME-standard 2D manufacturing drawings (PDF & DXF formats)
            - 3D CAD Assembly files (STEP or native SolidWorks files)
            - Structured Bill of Materials (BOM) with material grades and hardware specs
            
            ## 5. TIMELINE & STARTING SMALL
            - **Estimated Timeline:** 10 to 14 business days from on-site measurement.
            - **Engagement Option:** If you haven't worked with us before, you can start small! Send across a single worn part or a single machine drawing — we'll deliver it on a fixed turnaround so you can judge the quality before scaling up.
            
            ---
            *For further details or to authorize this project, please contact:*
            **Aditya Maity**  
            Lead Mechanical Design Consultant  
            Auriga Automation  
            Email: adityamaity35@gmail.com  
            Phone: +91 80137 64162  
            
            ${if (errorDetails != null) "*(Note: Direct AI model is currently offline. Fallback proposal generated locally. Internal log: $errorDetails)*" else ""}
        """.trimIndent()
    }
}

// --- ViewModel Factory ---
class MainViewModelFactory(
    private val repository: ClientRequestRepository,
    private val sharedPreferences: SharedPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- Dictionary Data ---
val SECTORS = listOf(
    "FMCG & Chemicals",
    "Machine Builders & Automation",
    "Material Handling, Cranes & Lifting",
    "Jute, Textile & Packaging",
    "Engineering Works & Fabrication"
)

val SECTOR_CHALLENGES = mapOf(
    "FMCG & Chemicals" to listOf(
        "Packaging line breakdown — no spare drawing (Critical)",
        "Machine bought 15 years ago — zero documentation (Frequent)",
        "Automation upgrade needed — no drawing to start from (Frequent)",
        "ISO audit found drawing gap — non-compliant (Critical)",
        "Custom solution request (General)"
    ),
    "Machine Builders & Automation" to listOf(
        "In-house designer left — knowledge is gone (Critical)",
        "Design backlog — can't take new orders (Frequent)",
        "Customer asks for variant — no baseline drawing (Frequent)",
        "Can't bid on export or large OEM orders (Growth)",
        "Custom solution request (General)"
    ),
    "Material Handling, Cranes & Lifting" to listOf(
        "Custom crane built without drawings — can't replicate (Critical)",
        "Conveyor layout for new plant — no one to design it (Frequent)",
        "CE or BIS certification needs drawing documentation (Growth)",
        "Client wants modified capacity — no design to work from (Frequent)",
        "Custom solution request (General)"
    ),
    "Jute, Textile & Packaging" to listOf(
        "50-year-old loom or jute machine breaks — no drawing exists (Critical)",
        "Machine runs but needs a speed or layout change (Frequent)",
        "Packaging line needs a new cutting or printing SPM (Frequent)",
        "Custom solution request (General)"
    ),
    "Engineering Works & Fabrication" to listOf(
        "Parts vary batch to batch — no fixed drawing (Critical)",
        "Government tender requires drawing package — none exist (Growth)",
        "Maintenance team guessing dimensions on repairs (Critical)",
        "Export buyer asks for ASME drawings — none exist (Growth)",
        "Custom solution request (General)"
    )
)

fun getPrefilledDescription(sector: String, challenge: String): String {
    return when (challenge) {
        "Packaging line breakdown — no spare drawing (Critical)" ->
            "Our sealing/filling machine has broken down. The original manufacturer is overseas, with a lead time of 6 to 12 weeks for spare parts. Production is stopped. We need Auriga to visit our site in Hooghly, measure the broken part, build a 3D model, and release a manufacturing drawing for a local workshop to fabricate."
        "Machine bought 15 years ago — zero documentation (Frequent)" ->
            "We have an imported processing machine bought 15 years ago. Every modification or repair is currently done by guesswork as there is no surviving technical documentation or drawings. We need Auriga to reverse engineer the full machine, hand over a 3D assembly, drawing set, and Bill of Materials (BOM) so we have a reliable baseline."
        "Automation upgrade needed — no drawing to start from (Frequent)" ->
            "We want to automate a manual filling or labelling step in our production line. The vendor asks for existing machine drawings, but we have none. We need Auriga to reverse engineer the current machine, design the modifications (sensor mounts, actuator brackets, modified frame), and deliver a complete engineering drawing package."
        "ISO audit found drawing gap — non-compliant (Critical)" ->
            "Our recent quality audit raised a major non-conformance: several of our key production machines have no as-built drawings. This puts our ISO certification renewal at risk. We need an as-built drawing set compliant with ASME Y14.2 standards, including proper tolerances and BOM, to close the audit gap."

        "In-house designer left — knowledge is gone (Critical)" ->
            "Our sole machine designer has left the company, and no drawings were kept. We have new orders coming in but have no in-house design capacity. We need Auriga as our design partner to reverse engineer our existing machines, rebuild a drawing library, and handle new design requests on demand."
        "Design backlog — can't take new orders (Frequent)" ->
            "Our order book is strong, but our design queue is 3 months deep. Delivery dates are slipping and customers are complaining. We need to offload overflow design work—including full 3D modelling, GD&T drawings, and BOMs—to Auriga on a fixed timeline so production keeps moving."
        "Customer asks for variant — no baseline drawing (Frequent)" ->
            "A client wants a modified version of our existing machine, but we never documented the original baseline. We cannot scope the variant properly. We need Auriga to reverse engineer our base machine first, then design the variant from that baseline."
        "Can't bid on export or large OEM orders (Growth)" ->
            "A major potential export buyer asks for ASME-standard drawing packages, formal GD&T tolerancing, and structured BOMs. Our internal shop drawings aren't sufficient. We need Auriga to upgrade our standard designs to full ASME Y14.2 compliant drawing sets so we can win this bid."

        "Custom crane built without drawings — can't replicate (Critical)" ->
            "We fabricated a custom crane years ago based purely on experience. A client wants to purchase the exact same unit, but we have no drawings to quote or build from. We need Auriga to reverse engineer the existing crane and produce a full 3D model, structural drawings, and BOM so we can replicate and quote confidently."
        "Conveyor layout for new plant — no one to design it (Frequent)" ->
            "We need a complete material flow layout for a new facility—including conveyor routing, chute positions, and transfer points. We can fabricate but don't have design capability. We need Auriga to provide a full general-arrangement layout, conveyor frame designs, and Bill of Quantities (BOQ)."
        "CE or BIS certification needs drawing documentation (Growth)" ->
            "We are looking to export our material handling machinery to Europe/abroad, which requires a full technical file (drawings, load calculations, material specs) for CE or BIS certification. We have none of this. We need Auriga to prepare a complete technical drawing package conforming to ASME Y14.2."
        "Client wants modified capacity — no design to work from (Frequent)" ->
            "A customer wants to upgrade their existing 5-ton conveyor system to 10-ton capacity. We know how to weld but need engineering guidance on which structural beams, end-carriages, and mounts require modification or reinforcement. We need Auriga to run capacity checks and design the modification package."

        "50-year-old loom or jute machine breaks — no drawing exists (Critical)" ->
            "Our 1960s loom has broken down. The original manufacturer is long out of business, and there are no spare parts or drawings in existence. Production is lost by the hour. We need Auriga to visit our plant, extract the broken casting part, reverse engineer it into a 3D model/manufacturing drawing, and coordinate with a local foundry to cast a replacement."
        "Machine runs but needs a speed or layout change (Frequent)" ->
            "We are introducing a new product requiring a change in the speed, drive ratios, or roller layouts of our existing machinery. We need Auriga to measure our current configuration, calculate and design the drive modifications, and supply detailed drawings for fabrication."
        "Packaging line needs a new cutting or printing SPM (Frequent)" ->
            "A new product SKU requires a custom cutting jig, sealing head, or printing registration fixture on our packaging line. We need Auriga to design this Special Purpose Machine (SPM) from scratch, delivering a full manufacturing drawing set for local fabrication."

        "Parts vary batch to batch — no fixed drawing (Critical)" ->
            "Our fabricated parts vary widely from batch to batch, leading to assembly issues and high customer rejection rates. We have no drawings with proper tolerances. We need Auriga to establish GD&T-controlled drawings with clear dimensional tolerances so our fabricators hold consistent dimensions."
        "Government tender requires drawing package — they don't have one (Growth)" ->
            "We are bidding on a government tender (e.g., GeM, CPWD) that requires a technical drawing submission, but we only have basic sketches. We need Auriga to prepare a highly professional, ASME-compliant drawing set with title blocks, tolerances, and revision history."
        "Maintenance team guessing dimensions on repairs (Critical)" ->
            "A critical shaft, bearing housing, or coupling has failed. Our maintenance team is measuring with calipers and sketching by hand, but after three attempts, the replacement part still doesn't fit properly. We need Auriga's precise reverse engineering to establish correct datums, tolerances, and stack-up analysis."
        "Export buyer asks for ASME drawings — none exist (Growth)" ->
            "An international buyer in the Middle East/Europe asks for a complete set of ASME-standard drawings for our fabrication products. Our current hand-drawn shop sheets won't qualify. We need Auriga to upgrade our entire catalog to standard digital formats."

        else -> "We would like to request Auriga Automation's engineering services for..."
    }
}
