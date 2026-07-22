package com.patchself.codexmacro.bluetooth

data class ControllerSettings(
    val stableConnection: Boolean = false,
    val autoResume: Boolean = false,
    val showKeyLabels: Boolean = true,
    val bluetoothDataLogging: Boolean = false,
    val activeLayer: Int = 0,
    val layerKeycaps: List<List<CommandKeycap>> = CommandKeycap.defaultLayers,
)

enum class CommandKeycap(
    val storageId: String,
    val label: String,
    val glyph: String,
) {
    Bug("bug", "Bug", "BUG"),
    Assistant("assistant", "OpenAI", "OAI"),
    Terminal("terminal", "Terminal", ">_"),
    Download("download", "Download", "↓"),
    Trash("trash", "Trash", "DEL"),
    Edit("edit", "New", "NEW"),
    Send("send", "Browser", "NAV"),
    Spark("spark", "Star", "MAGIC"),
    NewChat("new_chat", "Diff", "DIFF"),
    Run("run", "Play", "PLAY"),
    Branches("branches", "Git", "GIT"),
    Redirect("redirect", "Branch", "BRCH"),
    BranchAdd("branch_add", "Merge", "MRG"),
    Merge("merge", "Pull request", "PR"),
    Plug("plug", "Paint", "PAINT"),
    Experiment("experiment", "Lab", "LAB"),
    Review("review", "Party", "PARTY"),
    History("history", "Time", "TIME"),
    Think("think", "Mind +", "MIND+"),
    Link("link", "Mind -", "MIND-"),
    Fast("fast", "Fast", "ϟ"),
    Approve("approve", "Approve", "✓"),
    Decline("decline", "Reject", "REJ"),
    Fork("fork", "Split", "SPLIT"),
    Settings("settings", "Settings", "⚙"),
    FolderAdd("folder_add", "Folder", "FOLD"),
    Upload("upload", "Upload", "↑"),
    Apps("apps", "Apps", "••"),
    Yolo("yolo", "yolo", ":yolo:"),
    Yeet("yeet", "yeet", ":yeet:"),
    Mic("mic", "Mic", "MIC"),
    Codex("codex", "Codex", "CODEX"),
    ;

    companion object {
        const val commandKeyCount = 6
        const val layerCount = 6

        val defaultLayout = listOf(Fast, Approve, Decline, Fork, Mic, Codex)
        val defaultLayers = List(layerCount) { defaultLayout }

        /** encodeLayout serializes the six visible command-key legends for preferences. */
        fun encodeLayout(layout: List<CommandKeycap>): String =
            normalizeLayout(layout).joinToString(",") { it.storageId }

        /** decodeLayout restores a complete command-key layout or the hardware defaults. */
        fun decodeLayout(encoded: String?): List<CommandKeycap> {
            val keycapsById = entries.associateBy(CommandKeycap::storageId)
            val decoded = encoded
                ?.split(',')
                ?.mapNotNull(keycapsById::get)
            return normalizeLayout(decoded)
        }

        /** normalizeLayout guarantees the controller always has six command-key legends. */
        fun normalizeLayout(layout: List<CommandKeycap>?): List<CommandKeycap> =
            layout?.takeIf { it.size == commandKeyCount } ?: defaultLayout

        /** encodeLayers serializes all six command-key layers for preferences. */
        fun encodeLayers(layers: List<List<CommandKeycap>>): String =
            normalizeLayers(layers).joinToString(";") { encodeLayout(it) }

        /** decodeLayers restores six complete command-key layers or the hardware defaults. */
        fun decodeLayers(encoded: String?): List<List<CommandKeycap>> {
            val decoded = encoded?.split(';')?.map(::decodeLayout)
            return normalizeLayers(decoded)
        }

        /** normalizeLayers guarantees six layers containing six command-key legends each. */
        fun normalizeLayers(layers: List<List<CommandKeycap>>?): List<List<CommandKeycap>> =
            layers
                ?.takeIf { it.size == layerCount }
                ?.map(::normalizeLayout)
                ?: defaultLayers
    }
}
