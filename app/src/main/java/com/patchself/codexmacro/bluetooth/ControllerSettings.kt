package com.patchself.codexmacro.bluetooth

data class ControllerSettings(
    val stableConnection: Boolean = false,
    val autoResume: Boolean = false,
    val activeLayer: Int = 0,
    val layerKeycaps: List<List<CommandKeycap>> = CommandKeycap.defaultLayers,
)

enum class CommandKeycap(
    val storageId: String,
    val label: String,
    val glyph: String,
) {
    Bug("bug", "Bug", "BUG"),
    Codex("codex", "Codex", "◎"),
    Terminal("terminal", "Terminal", ">_"),
    Download("download", "Download", "↓"),
    Trash("trash", "Trash", "DEL"),
    Edit("edit", "Edit", "✎"),
    Send("send", "Send", "➤"),
    Spark("spark", "Spark", "✦"),
    NewChat("new_chat", "New chat", "+"),
    Run("run", "Run", "▶"),
    Branches("branches", "Branches", "⑂"),
    Fork("fork", "Fork", "⑂"),
    BranchAdd("branch_add", "Branch +", "⑂+"),
    Merge("merge", "Merge", "⑂"),
    Plug("plug", "Plug", "PLG"),
    Experiment("experiment", "Experiment", "LAB"),
    Review("review", "Review", "REV"),
    History("history", "History", "◷"),
    Think("think", "Think", "THK"),
    Link("link", "Link", "∞"),
    Fast("fast", "Fast", "ϟ"),
    Approve("approve", "Approve", "✓"),
    Decline("decline", "Decline", "×"),
    Redirect("redirect", "Redirect", "↗"),
    Settings("settings", "Settings", "⚙"),
    FolderAdd("folder_add", "Folder +", "DIR+"),
    Upload("upload", "Upload", "↑"),
    Apps("apps", "Apps", "••"),
    Yolo("yolo", "yolo", "yolo"),
    Yeet("yeet", "yeet", "yeet"),
    Mic("mic", "Mic", "MIC"),
    Assistant("assistant", "Assistant", "AI"),
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
