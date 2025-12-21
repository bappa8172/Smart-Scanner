package com.smartprivacy.scanner.analyzer

import com.google.gson.annotations.SerializedName

data class VTResponse(
    val data: VTData?
)

data class VTData(
    val attributes: VTAttributes?
)

data class VTAttributes(
    @SerializedName("last_analysis_stats")
    val lastAnalysisStats: VTAnalysisStats?,
    @SerializedName("last_analysis_results")
    val lastAnalysisResults: Map<String, VTAnalysisResult>?
)

data class VTAnalysisStats(
    val harmless: Int,
    val typeUnsupported: Int,
    val suspicious: Int,
    val confirmedTimeout: Int,
    val timeout: Int,
    val failure: Int,
    val malicious: Int,
    val undetected: Int
)

data class VTAnalysisResult(
    val method: String?,
    val engineName: String?,
    val category: String?,
    val result: String?
)
