package com.patchself.codexmacro.bluetooth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class ControllerSettings(
    val stableConnection: Boolean = false,
    val autoResume: Boolean = false,
    val showKeyLabels: Boolean = true,
    val bluetoothDataLogging: Boolean = false,
    val activeLayer: Int = 0,
    val codexKeycaps: List<CommandKeycap> = CommandKeycap.defaultLayout,
    val customLayers: List<List<CustomKeyBinding>> = CustomKeyBinding.defaultLayers,
)

data class CustomKeyBinding(
    val keycap: CommandKeycap = CommandKeycap.Terminal,
    val customIconUri: String? = null,
    val key: KeyboardKey = KeyboardKey.Space,
    val modifiers: Int = 0,
) {
    val shortcutLabel: String
        get() = buildList {
            if (modifiers and KeyboardModifier.Control.mask != 0) add("Ctrl")
            if (modifiers and KeyboardModifier.Option.mask != 0) add("Option")
            if (modifiers and KeyboardModifier.Shift.mask != 0) add("Shift")
            if (modifiers and KeyboardModifier.Command.mask != 0) add("Command")
            add(key.label)
        }.joinToString(" + ")

    companion object {
        const val keyCount = 12
        const val customLayerCount = 5

        val defaultLayout: List<CustomKeyBinding> by lazy {
            List(keyCount) { index ->
                CustomKeyBinding(
                    keycap = defaultKeycaps[index],
                    key = defaultKeys[index],
                )
            }
        }
        val defaultLayers: List<List<CustomKeyBinding>> by lazy { List(customLayerCount) { defaultLayout } }

        /** encodeLayers serializes all custom key icons and shortcuts. */
        fun encodeLayers(layers: List<List<CustomKeyBinding>>): String = buildJsonArray {
            normalizeLayers(layers).forEach { layer ->
                add(buildJsonArray {
                    layer.forEach { binding ->
                        add(buildJsonObject {
                            put("icon", binding.keycap.storageId)
                            binding.customIconUri?.let { put("uri", it) }
                            put("key", binding.key.storageId)
                            put("mod", binding.modifiers)
                        })
                    }
                })
            }
        }.toString()

        /** decodeLayers restores valid custom key layers or the defaults. */
        fun decodeLayers(encoded: String?): List<List<CustomKeyBinding>> {
            if (encoded.isNullOrBlank()) return defaultLayers
            return runCatching {
                val keycaps = CommandKeycap.entries.associateBy(CommandKeycap::storageId)
                val keys = KeyboardKey.entries.associateBy(KeyboardKey::storageId)
                Json.parseToJsonElement(encoded).jsonArray.map { layerElement ->
                    layerElement.jsonArray.map { bindingElement ->
                        val value = bindingElement.jsonObject
                        CustomKeyBinding(
                            keycap = value["icon"]?.jsonPrimitive?.contentOrNull?.let(keycaps::get)
                                ?: CommandKeycap.Terminal,
                            customIconUri = value["uri"]?.jsonPrimitive?.contentOrNull,
                            key = value["key"]?.jsonPrimitive?.contentOrNull?.let(keys::get)
                                ?: KeyboardKey.Space,
                            modifiers = value["mod"]?.jsonPrimitive?.intOrNull
                                ?.and(KeyboardModifier.allMask) ?: 0,
                        )
                    }
                }
            }.getOrNull()?.let(::normalizeLayers) ?: defaultLayers
        }

        /** normalizeLayers guarantees five custom layers with twelve bindings each. */
        fun normalizeLayers(layers: List<List<CustomKeyBinding>>?): List<List<CustomKeyBinding>> =
            layers
                ?.takeIf { it.size == customLayerCount && it.all { layer -> layer.size == keyCount } }
                ?.map { it.toList() }
                ?: defaultLayers

        private val defaultKeycaps = listOf(
            CommandKeycap.Edit,
            CommandKeycap.Send,
            CommandKeycap.Settings,
            CommandKeycap.History,
            CommandKeycap.Terminal,
            CommandKeycap.Apps,
            CommandKeycap.Branches,
            CommandKeycap.Run,
            CommandKeycap.Fork,
            CommandKeycap.Merge,
            CommandKeycap.Download,
            CommandKeycap.Upload,
        )
        private val defaultKeys = listOf(
            KeyboardKey.One,
            KeyboardKey.Two,
            KeyboardKey.Three,
            KeyboardKey.Four,
            KeyboardKey.Five,
            KeyboardKey.Six,
            KeyboardKey.Left,
            KeyboardKey.Right,
            KeyboardKey.C,
            KeyboardKey.V,
            KeyboardKey.PageDown,
            KeyboardKey.PageUp,
        )
    }
}

enum class KeyboardModifier(val mask: Int, val label: String) {
    Control(0x01, "Ctrl"),
    Shift(0x02, "Shift"),
    Option(0x04, "Option"),
    Command(0x08, "Command"),
    ;

    companion object {
        val allMask = entries.fold(0) { mask, modifier -> mask or modifier.mask }
    }
}

enum class KeyboardKey(
    val storageId: String,
    val label: String,
    val usage: Int,
) {
    A("a", "A", 0x04), B("b", "B", 0x05), C("c", "C", 0x06), D("d", "D", 0x07),
    E("e", "E", 0x08), F("f", "F", 0x09), G("g", "G", 0x0A), H("h", "H", 0x0B),
    I("i", "I", 0x0C), J("j", "J", 0x0D), K("k", "K", 0x0E), L("l", "L", 0x0F),
    M("m", "M", 0x10), N("n", "N", 0x11), O("o", "O", 0x12), P("p", "P", 0x13),
    Q("q", "Q", 0x14), R("r", "R", 0x15), S("s", "S", 0x16), T("t", "T", 0x17),
    U("u", "U", 0x18), V("v", "V", 0x19), W("w", "W", 0x1A), X("x", "X", 0x1B),
    Y("y", "Y", 0x1C), Z("z", "Z", 0x1D),
    One("1", "1", 0x1E), Two("2", "2", 0x1F), Three("3", "3", 0x20),
    Four("4", "4", 0x21), Five("5", "5", 0x22), Six("6", "6", 0x23),
    Seven("7", "7", 0x24), Eight("8", "8", 0x25), Nine("9", "9", 0x26), Zero("0", "0", 0x27),
    Enter("enter", "Enter", 0x28), Escape("escape", "Esc", 0x29), Backspace("backspace", "Backspace", 0x2A),
    Tab("tab", "Tab", 0x2B), Space("space", "Space", 0x2C), Minus("minus", "-", 0x2D),
    Equal("equal", "=", 0x2E), LeftBracket("left_bracket", "[", 0x2F),
    RightBracket("right_bracket", "]", 0x30), Backslash("backslash", "\\", 0x31),
    Semicolon("semicolon", ";", 0x33), Quote("quote", "'", 0x34), Grave("grave", "`", 0x35),
    Comma("comma", ",", 0x36), Period("period", ".", 0x37), Slash("slash", "/", 0x38),
    F1("f1", "F1", 0x3A), F2("f2", "F2", 0x3B), F3("f3", "F3", 0x3C), F4("f4", "F4", 0x3D),
    F5("f5", "F5", 0x3E), F6("f6", "F6", 0x3F), F7("f7", "F7", 0x40), F8("f8", "F8", 0x41),
    F9("f9", "F9", 0x42), F10("f10", "F10", 0x43), F11("f11", "F11", 0x44), F12("f12", "F12", 0x45),
    Home("home", "Home", 0x4A), PageUp("page_up", "Page Up", 0x4B), Delete("delete", "Delete", 0x4C),
    End("end", "End", 0x4D), PageDown("page_down", "Page Down", 0x4E),
    Right("right", "→", 0x4F), Left("left", "←", 0x50), Down("down", "↓", 0x51), Up("up", "↑", 0x52),
}

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

        /** encodeLayout serializes the six Codex command-key legends. */
        fun encodeLayout(layout: List<CommandKeycap>): String =
            normalizeLayout(layout).joinToString(",") { it.storageId }

        /** decodeLayout restores a complete Codex keycap layout or the hardware defaults. */
        fun decodeLayout(encoded: String?): List<CommandKeycap> {
            val keycapsById = entries.associateBy(CommandKeycap::storageId)
            val decoded = encoded?.split(',')?.mapNotNull(keycapsById::get)
            return normalizeLayout(decoded)
        }

        /** normalizeLayout guarantees the Codex layer has six command-key legends. */
        fun normalizeLayout(layout: List<CommandKeycap>?): List<CommandKeycap> =
            layout?.takeIf { it.size == commandKeyCount } ?: defaultLayout
    }
}
