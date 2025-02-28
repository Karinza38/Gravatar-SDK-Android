package com.gravatar.demoapp.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gravatar.AvatarQueryOptions
import com.gravatar.AvatarUrl
import com.gravatar.demoapp.BuildConfig
import com.gravatar.demoapp.R
import com.gravatar.demoapp.ui.activity.QuickEditorTestActivity
import com.gravatar.demoapp.ui.components.GravatarEmailInput
import com.gravatar.demoapp.ui.components.GravatarPasswordInput
import com.gravatar.quickeditor.GravatarQuickEditor
import com.gravatar.quickeditor.ui.editor.AuthenticationMethod
import com.gravatar.quickeditor.ui.editor.AvatarPickerContentLayout
import com.gravatar.quickeditor.ui.editor.GravatarQuickEditorParams
import com.gravatar.quickeditor.ui.editor.GravatarUiMode
import com.gravatar.quickeditor.ui.editor.bottomsheet.GravatarQuickEditorBottomSheet
import com.gravatar.quickeditor.ui.oauth.OAuthParams
import com.gravatar.types.Email
import com.gravatar.ui.GravatarTheme
import com.gravatar.ui.LocalGravatarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AvatarUpdateTab(modifier: Modifier = Modifier) {
    var userEmail by remember { mutableStateOf(BuildConfig.DEMO_EMAIL) }
    var userToken by remember { mutableStateOf(BuildConfig.DEMO_BEARER_TOKEN) }
    var useToken by remember { mutableStateOf(false) }
    var tokenVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var cacheBuster: String? by remember { mutableStateOf(null) }
    val scrollState: ScrollState = rememberScrollState()
    var pickerContentLayout: AvatarPickerContentLayout by rememberSaveable(
        stateSaver = AvatarPickerContentLayoutSaver,
    ) {
        mutableStateOf(AvatarPickerContentLayout.Horizontal)
    }
    var pickerUiMode: GravatarUiMode by rememberSaveable {
        mutableStateOf(GravatarUiMode.SYSTEM)
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GravatarEmailInput(email = userEmail, onValueChange = { userEmail = it }, Modifier.fillMaxWidth())
            Row {
                GravatarPasswordInput(
                    password = userToken,
                    passwordIsVisible = tokenVisible,
                    enabled = useToken,
                    onValueChange = { value -> userToken = value },
                    onVisibilityChange = { visible -> tokenVisible = visible },
                    label = { Text(text = "Bearer token") },
                    modifier = Modifier.weight(1f),
                )
                Checkbox(checked = useToken, onCheckedChange = { useToken = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ContentLayoutDropdown(
                    selectedContentLayout = pickerContentLayout,
                    onContentLayoutSelected = { pickerContentLayout = it },
                    modifier = Modifier.weight(1f),
                )
                UiModeDropdown(
                    selectedUiMode = pickerUiMode,
                    onUiModeSelected = { pickerUiMode = it },
                    modifier = Modifier.weight(1f),
                )
            }
            UpdateAvatarComposable(
                modifier = Modifier.clickable {
                    keyboardController?.hide()
                    showBottomSheet = true
                },
                isUploading = false,
                email = Email(userEmail),
                cacheBuster = cacheBuster,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    scope.launch {
                        GravatarQuickEditor.logout(Email(userEmail))
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            ) {
                Text(text = "Logout user")
            }
            if (BuildConfig.DEBUG) {
                Button(
                    onClick = {
                        context.startActivity(Intent(context, QuickEditorTestActivity::class.java))
                    },
                    modifier = Modifier.padding(bottom = 20.dp),
                ) {
                    Text(text = "Test with Activity without Compose")
                }
            }
        }
    }
    if (showBottomSheet) {
        val authenticationMethod = if (useToken) {
            AuthenticationMethod.Bearer(userToken)
        } else {
            AuthenticationMethod.OAuth(
                OAuthParams {
                    clientId = BuildConfig.DEMO_OAUTH_CLIENT_ID
                    redirectUri = BuildConfig.DEMO_OAUTH_REDIRECT_URI
                },
            )
        }
        // CompositionLocalProvider is not required, it's added here
        // to test that the QuickEditor theme can't be overridden
        CompositionLocalProvider(
            LocalGravatarTheme provides object : GravatarTheme {
                // Override theme colors
                override val colorScheme: ColorScheme
                    @Composable
                    get() = MaterialTheme.colorScheme.copy(surface = Color.Red)
            },
        ) {
            GravatarQuickEditorBottomSheet(
                gravatarQuickEditorParams = GravatarQuickEditorParams {
                    email = Email(userEmail)
                    avatarPickerContentLayout = pickerContentLayout
                    uiMode = pickerUiMode
                },
                authenticationMethod = authenticationMethod,
                onAvatarSelected = remember {
                    {
                        cacheBuster = System.currentTimeMillis().toString()
                    }
                },
                onDismiss = remember {
                    {
                        Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
                        showBottomSheet = false
                    }
                },
            )
        }
    }
}

@Composable
private fun UpdateAvatarComposable(
    isUploading: Boolean,
    email: Email,
    cacheBuster: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (isUploading) {
            CircularProgressIndicator()
        } else {
            val size = 128.dp
            val sizePx = with(LocalDensity.current) { size.roundToPx() }
            AsyncImage(
                model = AvatarUrl(
                    email,
                    AvatarQueryOptions {
                        preferredSize = sizePx
                    },
                ).url(cacheBuster).toString(),
                contentDescription = "Avatar Image",
                modifier = Modifier
                    .size(size)
                    .padding(8.dp)
                    .clip(CircleShape),
                placeholder = rememberVectorPainter(Icons.Rounded.AccountCircle),
            )
            Text(text = stringResource(R.string.update_avatar_button_label))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentLayoutDropdown(
    selectedContentLayout: AvatarPickerContentLayout,
    onContentLayoutSelected: (AvatarPickerContentLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val contentLayoutOptions = listOf(
        AvatarPickerContentLayout.Horizontal,
        AvatarPickerContentLayout.Vertical,
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        TextField(
            readOnly = true,
            value = selectedContentLayout.toString(),
            onValueChange = { },
            label = { Text("Content Layout") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            contentLayoutOptions.forEach { selectionOption ->
                DropdownMenuItem(text = { Text(text = selectionOption.toString()) }, onClick = {
                    onContentLayoutSelected(selectionOption)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UiModeDropdown(
    selectedUiMode: GravatarUiMode,
    onUiModeSelected: (GravatarUiMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val uiModeOptions = listOf(
        GravatarUiMode.LIGHT,
        GravatarUiMode.DARK,
        GravatarUiMode.SYSTEM,
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        TextField(
            readOnly = true,
            value = selectedUiMode.toString(),
            onValueChange = { },
            label = { Text("UI mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            uiModeOptions.forEach { selectionOption ->
                DropdownMenuItem(text = { Text(text = selectionOption.toString()) }, onClick = {
                    onUiModeSelected(selectionOption)
                    expanded = false
                })
            }
        }
    }
}

private val AvatarPickerContentLayoutSaver: Saver<AvatarPickerContentLayout, String> = run {
    val horizontalKey = "horizontal"
    val verticalKey = "vertical"
    Saver(
        save = { value ->
            when (value) {
                AvatarPickerContentLayout.Horizontal -> horizontalKey
                AvatarPickerContentLayout.Vertical -> verticalKey
            }
        },
        restore = { value ->
            when (value) {
                horizontalKey -> AvatarPickerContentLayout.Horizontal
                else -> AvatarPickerContentLayout.Vertical
            }
        },
    )
}

@Preview
@Composable
private fun UpdateAvatarComposablePreview() = UpdateAvatarComposable(false, Email("gravatar@automattic.com"), null)

@Preview
@Composable
private fun UpdateAvatarLoadingComposablePreview() =
    UpdateAvatarComposable(true, Email("gravatar@automattic.com"), null)

@Preview
@Composable
private fun AvatarUpdateTabPreview() = AvatarUpdateTab()
