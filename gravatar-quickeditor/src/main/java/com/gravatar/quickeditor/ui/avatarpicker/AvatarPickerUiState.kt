package com.gravatar.quickeditor.ui.avatarpicker

import android.net.Uri
import com.gravatar.quickeditor.data.repository.IdentityAvatars
import com.gravatar.quickeditor.ui.editor.ContentLayout
import com.gravatar.restapi.models.Avatar
import com.gravatar.restapi.models.Profile
import com.gravatar.types.Email
import com.gravatar.ui.components.ComponentState

internal data class AvatarPickerUiState(
    val email: Email,
    val contentLayout: ContentLayout,
    val isLoading: Boolean = false,
    val error: SectionError? = null,
    val profile: ComponentState<Profile>? = null,
    val identityAvatars: IdentityAvatars? = null,
    val selectingAvatarId: String? = null,
    val uploadingAvatar: Uri? = null,
    val scrollToIndex: Int? = null,
) {
    val avatarsSectionUiState: AvatarsSectionUiState? = identityAvatars?.mapToUiModel()?.let {
        AvatarsSectionUiState(
            avatars = it,
            scrollToIndex = scrollToIndex,
            uploadButtonEnabled = uploadingAvatar == null,
            contentLayout = contentLayout,
        )
    }

    private fun IdentityAvatars.mapToUiModel(): List<AvatarUi> {
        return mutableListOf<AvatarUi>().apply {
            if (uploadingAvatar != null) add(AvatarUi.Local(uploadingAvatar))
            addAll(
                this@mapToUiModel.avatars.map { avatar ->
                    AvatarUi.Uploaded(
                        avatar = avatar,
                        isSelected = avatar.imageId == (selectingAvatarId ?: selectedAvatarId),
                        isLoading = avatar.imageId == selectingAvatarId,
                    )
                },
            )
        }.toList()
    }
}

internal sealed class SectionError {
    data object ServerError : SectionError()

    data class InvalidToken(val showLogin: Boolean) : SectionError()

    data object Unknown : SectionError()

    data object NoInternetConnection : SectionError()
}

internal data class AvatarsSectionUiState(
    val contentLayout: ContentLayout,
    val avatars: List<AvatarUi>,
    val scrollToIndex: Int?,
    val uploadButtonEnabled: Boolean,
)

internal sealed class AvatarUi(val avatarId: String) {
    data class Uploaded(
        val avatar: Avatar,
        val isSelected: Boolean,
        val isLoading: Boolean,
    ) : AvatarUi(avatar.imageId)

    data class Local(
        val uri: Uri,
    ) : AvatarUi(uri.toString())
}
