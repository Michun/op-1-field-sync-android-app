package com.op1sync.app.core.audio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for OP-1 AIF/AIFC files.
 * Extracts JSON metadata from the APPL chunk with "op-1" signature.
 */
@Singleton
class OP1PatchParser @Inject constructor() {
    
    companion object {
        private const val TAG = "OP1PatchParser"
        private const val FORM_ID = "FORM"
        private const val APPL_ID = "APPL"
        private const val SSND_ID = "SSND"
        private const val OP1_SIGNATURE = "op-1"
    }
    
    /**
     * Parse an OP-1 AIF file and extract patch metadata.
     */
    suspend fun parsePatch(file: File): OP1PatchMetadata? = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.name.lowercase().endsWith(".aif")) {
            return@withContext null
        }
        
        try {
            RandomAccessFile(file, "r").use { raf ->
                // Read FORM header
                val formId = ByteArray(4)
                raf.readFully(formId)
                if (String(formId) != FORM_ID) {
                    Log.w(TAG, "Not a FORM file: ${file.name}")
                    return@withContext null
                }
                
                // Skip file size (4 bytes) and form type (4 bytes = AIFC/AIFF)
                raf.skipBytes(8)
                
                // Search for APPL chunk
                while (raf.filePointer < raf.length() - 8) {
                    val chunkId = ByteArray(4)
                    if (raf.read(chunkId) != 4) break
                    
                    val chunkSize = raf.readInt()
                    val chunkIdStr = String(chunkId)
                    
                    if (chunkIdStr == APPL_ID) {
                        // Read signature (4 bytes)
                        val signature = ByteArray(4)
                        raf.readFully(signature)
                        
                        if (String(signature) == OP1_SIGNATURE) {
                            // Read JSON data
                            val jsonBytes = ByteArray(chunkSize - 4)
                            raf.readFully(jsonBytes)
                            val jsonStr = String(jsonBytes).trim('\u0000', ' ')
                            
                            return@withContext parseJson(jsonStr, file.name)
                        }
                    }
                    
                    // Skip to next chunk (padding to even boundary)
                    val skipSize = if (chunkSize % 2 == 1) chunkSize + 1 else chunkSize
                    raf.skipBytes(skipSize - if (chunkIdStr == APPL_ID) 4 else 0)
                }
                
                Log.w(TAG, "No OP-1 APPL chunk found in ${file.name}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ${file.name}", e)
            null
        }
    }
    
    /**
     * Parse a drum patch file and extract sample data for playback.
     * Returns DrumSampleData with SSND sound data and sample boundaries.
     */
    suspend fun parseDrumSamples(file: File): DrumSampleData? = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.name.lowercase().endsWith(".aif")) {
            return@withContext null
        }
        
        try {
            RandomAccessFile(file, "r").use { raf ->
                var applJson: JSONObject? = null
                var ssndData: ByteArray? = null
                
                // Read FORM header
                val formId = ByteArray(4)
                raf.readFully(formId)
                if (String(formId) != FORM_ID) {
                    Log.w(TAG, "Not a FORM file: ${file.name}")
                    return@withContext null
                }
                
                // Skip file size (4 bytes) and form type (4 bytes = AIFC/AIFF)
                raf.skipBytes(8)
                
                // Search for APPL and SSND chunks
                while (raf.filePointer < raf.length() - 8) {
                    val chunkId = ByteArray(4)
                    if (raf.read(chunkId) != 4) break
                    
                    val chunkSize = raf.readInt()
                    val chunkIdStr = String(chunkId)
                    
                    when (chunkIdStr) {
                        APPL_ID -> {
                            // Read signature (4 bytes)
                            val signature = ByteArray(4)
                            raf.readFully(signature)
                            
                            if (String(signature) == OP1_SIGNATURE) {
                                val jsonBytes = ByteArray(chunkSize - 4)
                                raf.readFully(jsonBytes)
                                val jsonStr = String(jsonBytes).trim('\u0000', ' ')
                                applJson = JSONObject(jsonStr)
                            } else {
                                raf.skipBytes(chunkSize - 4)
                            }
                        }
                        SSND_ID -> {
                            // Read SSND chunk
                            val offset = raf.readInt()
                            val blockSize = raf.readInt()
                            val soundData = ByteArray(chunkSize - 8)
                            raf.readFully(soundData)
                            ssndData = soundData
                            Log.d(TAG, "SSND chunk: ${soundData.size} bytes, offset=$offset, blockSize=$blockSize")
                        }
                        else -> {
                            // Skip other chunks (with padding to even boundary)
                            val skipSize = if (chunkSize % 2 == 1) chunkSize + 1 else chunkSize
                            raf.skipBytes(skipSize)
                        }
                    }
                    
                    // Stop if we have both
                    if (applJson != null && ssndData != null) break
                }
                
                // Extract sample boundaries from JSON
                if (applJson != null && ssndData != null) {
                    val startArray = applJson.optJSONArray("start") ?: return@withContext null
                    val endArray = applJson.optJSONArray("end") ?: return@withContext null
                    
                    val startPositions = (0 until startArray.length()).map { startArray.getInt(it) }
                    val endPositions = (0 until endArray.length()).map { endArray.getInt(it) }
                    
                    return@withContext DrumSampleData(
                        name = applJson.optString("name", file.nameWithoutExtension),
                        type = applJson.optString("type", "drum"),
                        ssndData = ssndData,
                        startPositions = startPositions,
                        endPositions = endPositions
                    )
                }
                
                Log.w(TAG, "Missing APPL or SSND chunk in ${file.name}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing drum samples from ${file.name}", e)
            null
        }
    }
    
    private fun parseJson(jsonStr: String, fileName: String): OP1PatchMetadata? {
        return try {
            val json = JSONObject(jsonStr)
            val type = json.optString("type", "unknown")
            
            when {
                json.has("drum_version") || type == "drum" -> parseDrumPatch(json, fileName)
                json.has("synth_version") || type != "drum" -> parseSynthPatch(json, fileName)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: $jsonStr", e)
            null
        }
    }
    
    private fun parseDrumPatch(json: JSONObject, fileName: String): DrumPatchMetadata {
        return DrumPatchMetadata(
            name = json.optString("name", fileName.removeSuffix(".aif")),
            drumVersion = json.optInt("drum_version", 1),
            fxType = json.optString("fx_type", "none"),
            fxActive = json.optBoolean("fx_active", false),
            lfoType = json.optString("lfo_type", "none"),
            lfoActive = json.optBoolean("lfo_active", false),
            stereo = json.optBoolean("stereo", false),
            sampleCount = json.optJSONArray("start")?.length() ?: 0
        )
    }
    
    private fun parseSynthPatch(json: JSONObject, fileName: String): SynthPatchMetadata {
        return SynthPatchMetadata(
            name = json.optString("name", fileName.removeSuffix(".aif")),
            type = json.optString("type", "unknown"),
            synthVersion = json.optInt("synth_version", 1),
            octave = json.optInt("octave", 0),
            fxType = json.optString("fx_type", "none"),
            fxActive = json.optBoolean("fx_active", false),
            lfoType = json.optString("lfo_type", "none"),
            lfoActive = json.optBoolean("lfo_active", false)
        )
    }
}

/**
 * Data class containing drum sample data for playback.
 */
data class DrumSampleData(
    val name: String,
    val type: String,
    val ssndData: ByteArray,
    val startPositions: List<Int>,
    val endPositions: List<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DrumSampleData
        return name == other.name && ssndData.contentEquals(other.ssndData)
    }
    
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + ssndData.contentHashCode()
        return result
    }
}

/**
 * Base class for OP-1 patch metadata.
 */
sealed class OP1PatchMetadata {
    abstract val name: String
    abstract val fxType: String
    abstract val fxActive: Boolean
    abstract val lfoType: String
    abstract val lfoActive: Boolean
    
    /**
     * Get the synth/drum engine type for icon display.
     */
    abstract val engineType: String
}

data class DrumPatchMetadata(
    override val name: String,
    val drumVersion: Int,
    override val fxType: String,
    override val fxActive: Boolean,
    override val lfoType: String,
    override val lfoActive: Boolean,
    val stereo: Boolean,
    val sampleCount: Int
) : OP1PatchMetadata() {
    override val engineType: String = "drum"
}

data class SynthPatchMetadata(
    override val name: String,
    val type: String,  // drwave, dbox, string, cluster, sampler, etc.
    val synthVersion: Int,
    val octave: Int,
    override val fxType: String,
    override val fxActive: Boolean,
    override val lfoType: String,
    override val lfoActive: Boolean
) : OP1PatchMetadata() {
    override val engineType: String = type
}

/**
 * Map OP-1 engine/effect types to display names.
 */
object OP1TypeNames {
    val engines = mapOf(
        "drum" to "Drum Kit",
        "drwave" to "DR Wave",
        "dbox" to "D-Box",
        "string" to "String",
        "cluster" to "Cluster",
        "sampler" to "Sampler",
        "digital" to "Digital",
        "fm" to "FM",
        "phase" to "Phase",
        "pulse" to "Pulse",
        "dsynth" to "DSynth",
        "voltage" to "Voltage"
    )
    
    val effects = mapOf(
        "delay" to "Delay",
        "punch" to "Punch",
        "cwo" to "CWO",
        "grid" to "Grid",
        "nitro" to "Nitro",
        "phone" to "Phone",
        "spring" to "Spring"
    )
    
    val lfos = mapOf(
        "tremolo" to "Tremolo",
        "value" to "Value",
        "crank" to "Crank",
        "random" to "Random",
        "element" to "Element",
        "midi" to "MIDI"
    )
    
    fun getEngineName(type: String) = engines[type.lowercase()] ?: type.replaceFirstChar { it.uppercase() }
    fun getEffectName(type: String) = effects[type.lowercase()] ?: type.replaceFirstChar { it.uppercase() }
    fun getLfoName(type: String) = lfos[type.lowercase()] ?: type.replaceFirstChar { it.uppercase() }
}
