package com.patchself.codexmacro.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.patchself.codexmacro.R
import com.patchself.codexmacro.bluetooth.CommandKeycap
import com.patchself.codexmacro.bluetooth.CustomKeyBinding

@Composable
internal fun localizedKeycapLabel(keycap: CommandKeycap): String {
    val resource = keycap.stringResource
    return if (resource == null) keycap.label else stringResource(resource)
}

@Composable
internal fun localizedShortcutLabel(binding: CustomKeyBinding): String {
    val labels = buildList {
        if (binding.modifiers and 0x01 != 0) add("Ctrl")
        if (binding.modifiers and 0x04 != 0) add("Option")
        if (binding.modifiers and 0x02 != 0) add("Shift")
        if (binding.modifiers and 0x08 != 0) add("Command")
        add(binding.key.label)
    }
    return labels.joinToString(" + ")
}

private val CommandKeycap.stringResource: Int?
    @StringRes get() = when (this) {
        CommandKeycap.Bug -> R.string.keycap_bug
        CommandKeycap.Assistant -> R.string.keycap_assistant
        CommandKeycap.Terminal -> R.string.keycap_terminal
        CommandKeycap.Download -> R.string.keycap_download
        CommandKeycap.Trash -> R.string.keycap_trash
        CommandKeycap.Edit -> R.string.keycap_new
        CommandKeycap.Send -> R.string.keycap_browser
        CommandKeycap.Spark -> R.string.keycap_star
        CommandKeycap.NewChat -> R.string.keycap_diff
        CommandKeycap.Run -> R.string.keycap_play
        CommandKeycap.Branches -> R.string.keycap_git
        CommandKeycap.Redirect -> R.string.keycap_branch
        CommandKeycap.BranchAdd -> R.string.keycap_merge
        CommandKeycap.Merge -> R.string.keycap_pull_request
        CommandKeycap.Plug -> R.string.keycap_paint
        CommandKeycap.Experiment -> R.string.keycap_lab
        CommandKeycap.Review -> R.string.keycap_party
        CommandKeycap.History -> R.string.keycap_time
        CommandKeycap.Think -> R.string.keycap_mind_add
        CommandKeycap.Link -> R.string.keycap_mind_remove
        CommandKeycap.Fast -> R.string.keycap_fast
        CommandKeycap.Approve -> R.string.keycap_approve
        CommandKeycap.Decline -> R.string.keycap_reject
        CommandKeycap.Fork -> R.string.keycap_split
        CommandKeycap.Settings -> R.string.keycap_settings
        CommandKeycap.FolderAdd -> R.string.keycap_folder
        CommandKeycap.Upload -> R.string.keycap_upload
        CommandKeycap.Apps -> R.string.keycap_apps
        CommandKeycap.Mic -> R.string.keycap_mic
        CommandKeycap.Codex -> R.string.keycap_codex
        CommandKeycap.Yolo, CommandKeycap.Yeet -> null
    }
