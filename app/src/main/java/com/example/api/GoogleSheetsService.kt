package com.example.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Moshi Models for Google Drive and Google Sheets APIs ---
data class DriveFile(
    val id: String,
    val name: String
)

data class DriveFileSearchResponse(
    val files: List<DriveFile>
)

data class SpreadsheetProperties(
    val title: String
)

data class CreateSpreadsheetRequest(
    val properties: SpreadsheetProperties
)

data class CreateSpreadsheetResponse(
    val spreadsheetId: String
)

data class ValueRange(
    val values: List<List<String>>
)

data class AppendValuesResponse(
    val spreadsheetId: String,
    val tableRange: String?
)

interface GoogleSheetsApiService {
    @GET("drive/v3/files")
    suspend fun searchFiles(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String
    ): DriveFileSearchResponse

    @POST("sheets/v4/spreadsheets")
    suspend fun createSpreadsheet(
        @Header("Authorization") authHeader: String,
        @Body request: CreateSpreadsheetRequest
    ): CreateSpreadsheetResponse

    @POST("sheets/v4/spreadsheets/{spreadsheetId}/values/{range}:append")
    suspend fun appendValues(
        @Header("Authorization") authHeader: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String,
        @Body body: ValueRange
    ): AppendValuesResponse
}

object GoogleSheetsClient {
    private const val BASE_URL = "https://www.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GoogleSheetsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleSheetsApiService::class.java)
    }
}
