package com.gravatar.quickeditor.ui.avatarpicker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gravatar.quickeditor.QuickEditorContainer
import com.gravatar.quickeditor.data.DownloadManagerError
import com.gravatar.quickeditor.data.FileUtils
import com.gravatar.quickeditor.data.ImageDownloader
import com.gravatar.quickeditor.data.models.QuickEditorError
import com.gravatar.quickeditor.data.repository.AvatarRepository
import com.gravatar.quickeditor.ui.editor.AvatarPickerContentLayout
import com.gravatar.quickeditor.ui.editor.GravatarQuickEditorParams
import com.gravatar.restapi.models.Avatar
import com.gravatar.services.ErrorType
import com.gravatar.services.GravatarResult
import com.gravatar.services.ProfileService
import com.gravatar.types.Email
import com.gravatar.ui.components.ComponentState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class AvatarPickerViewModel(
    private val email: Email,
    private val handleExpiredSession: Boolean,
    private val avatarPickerContentLayout: AvatarPickerContentLayout,
    private val profileService: ProfileService,
    private val avatarRepository: AvatarRepository,
    private val imageDownloader: ImageDownloader,
    private val fileUtils: FileUtils,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(AvatarPickerUiState(email = email, avatarPickerContentLayout = avatarPickerContentLayout))
    val uiState: StateFlow<AvatarPickerUiState> = _uiState.asStateFlow()
    private val _actions = Channel<AvatarPickerAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        refresh()
        nonAvatarSelectedAlertObserver()
    }

    fun onEvent(event: AvatarPickerEvent) {
        when (event) {
            is AvatarPickerEvent.LocalImageSelected -> localImageSelected(event.uri)
            AvatarPickerEvent.Refresh -> refresh()
            is AvatarPickerEvent.AvatarSelected -> selectAvatar(event.avatar)
            is AvatarPickerEvent.ImageCropped -> uploadAvatar(event.uri)
            AvatarPickerEvent.HandleAuthFailureTapped -> {
                viewModelScope.launch {
                    _actions.send(AvatarPickerAction.InvokeAuthFailed)
                }
            }

            AvatarPickerEvent.FailedAvatarDialogDismissed -> dismissFailedUploadDialog()
            is AvatarPickerEvent.FailedAvatarTapped -> showFailedUploadDialog(event.uri)
            is AvatarPickerEvent.FailedAvatarDismissed -> removedFailedUpload(event.uri)
            is AvatarPickerEvent.DownloadAvatarTapped -> downloadAvatar(event.avatar)
            AvatarPickerEvent.DownloadManagerDisabledDialogDismissed -> hideDownloadManagerAlert()
            is AvatarPickerEvent.AvatarDeleteSelected -> deleteAvatar(event.avatarId)
            AvatarPickerEvent.AvatarDeleteAlertDismissed -> hideNonSelectedAvatarAlert()
            is AvatarPickerEvent.AvatarRatingSelected -> updateAvatar(event.avatarId, event.rating)
        }
    }

    private fun updateAvatar(avatarId: String, rating: Avatar.Rating? = null, altText: String? = null) {
        viewModelScope.launch {
            val oldAvatar: Avatar? = _uiState.value.emailAvatars?.avatars?.find { it.imageId == avatarId }
            if (!oldAvatar.shouldUpdateRating(rating) && !oldAvatar.shouldUpdateAltText(altText)) {
                return@launch
            }
            val updateType = AvatarUpdateType.RATING
            _uiState.update { currentState ->
                val emailAvatars = currentState.emailAvatars?.copy(
                    avatars = currentState.emailAvatars.avatars.map { avatar ->
                        if (avatar.imageId == avatarId) {
                            avatar.copy(rating, altText)
                        } else {
                            avatar
                        }
                    },
                )
                currentState.copy(emailAvatars = emailAvatars)
            }
            when (avatarRepository.updateAvatar(email, avatarId, rating, altText)) {
                is GravatarResult.Success -> {
                    _actions.send(AvatarPickerAction.AvatarUpdated(updateType))
                }

                is GravatarResult.Failure -> {
                    _uiState.update { currentState ->
                        val emailAvatars = currentState.emailAvatars?.copy(
                            avatars = currentState.emailAvatars.avatars.map { avatar ->
                                if (avatar.imageId == avatarId) {
                                    avatar.copy(oldAvatar?.rating, oldAvatar?.altText)
                                } else {
                                    avatar
                                }
                            },
                        )
                        currentState.copy(emailAvatars = emailAvatars)
                    }
                    _actions.send(AvatarPickerAction.AvatarUpdateFailed(updateType))
                }
            }
        }
    }

    private fun hideDownloadManagerAlert() {
        _uiState.update { currentState ->
            currentState.copy(downloadManagerDisabled = false)
        }
    }

    private fun downloadAvatar(avatar: Avatar) {
        viewModelScope.launch {
            when (val result = imageDownloader.downloadImage(avatar.imageUrl)) {
                is GravatarResult.Failure -> {
                    when (result.error) {
                        DownloadManagerError.DOWNLOAD_MANAGER_NOT_AVAILABLE -> {
                            _actions.send(AvatarPickerAction.DownloadManagerNotAvailable)
                        }

                        DownloadManagerError.DOWNLOAD_MANAGER_DISABLED -> {
                            _uiState.update { currentState ->
                                currentState.copy(
                                    downloadManagerDisabled = true,
                                )
                            }
                        }
                    }
                }

                is GravatarResult.Success -> _actions.send(AvatarPickerAction.AvatarDownloadStarted)
            }
        }
    }

    private fun showFailedUploadDialog(uri: Uri) {
        _uiState.update { currentState ->
            currentState.copy(failedUploadDialog = currentState.failedUploads.firstOrNull { it.uri == uri })
        }
    }

    private fun dismissFailedUploadDialog() {
        _uiState.update { currentState ->
            currentState.copy(failedUploadDialog = null)
        }
    }

    private fun removedFailedUpload(uri: Uri) {
        _uiState.update { currentState ->
            currentState.copy(
                failedUploads = currentState.failedUploads.filter { it.uri != uri }.toSet(),
                failedUploadDialog = null,
            )
        }
    }

    private fun refresh() {
        fetchAvatars()
        if (uiState.value.profile !is ComponentState.Loaded) {
            fetchProfile()
        }
    }

    private fun selectAvatar(avatar: Avatar) {
        viewModelScope.launch {
            val avatarId = avatar.imageId
            if (_uiState.value.emailAvatars?.selectedAvatarId != avatarId) {
                _uiState.update { currentState ->
                    currentState.copy(selectingAvatarId = avatarId)
                }
                when (avatarRepository.selectAvatar(email, avatarId)) {
                    is GravatarResult.Success -> {
                        _uiState.update { currentState ->
                            val emailAvatars = currentState.emailAvatars?.copy(selectedAvatarId = avatarId)
                            currentState.copy(
                                emailAvatars = emailAvatars,
                                selectingAvatarId = null,
                                avatarUpdates = currentState.avatarUpdates.inc(),
                            )
                        }
                        _actions.send(AvatarPickerAction.AvatarSelected)
                    }

                    is GravatarResult.Failure -> {
                        _uiState.update { currentState ->
                            currentState.copy(selectingAvatarId = null)
                        }
                        _actions.send(AvatarPickerAction.AvatarSelectionFailed)
                    }
                }
            }
        }
    }

    private fun localImageSelected(imageUri: Uri) {
        viewModelScope.launch {
            _actions.send(AvatarPickerAction.LaunchImageCropper(imageUri, fileUtils.createCroppedAvatarFile()))
        }
    }

    @Suppress("LongMethod")
    private fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    uploadingAvatar = uri,
                    failedUploads = currentState.failedUploads.filter { it.uri != uri }.toSet(),
                    scrollToIndex = 0,
                    failedUploadDialog = null,
                )
            }
            when (val result = avatarRepository.uploadAvatar(email, uri)) {
                is GravatarResult.Success -> {
                    fileUtils.deleteFile(uri)
                    val avatar = result.value
                    if (avatar.selected == true) {
                        _actions.send(AvatarPickerAction.AvatarSelected)
                    }
                    _uiState.update { currentState ->
                        val emailAvatars = currentState.emailAvatars?.copy(
                            avatars = buildList {
                                add(avatar)
                                addAll(
                                    currentState.emailAvatars.avatars.filter { it.imageId != avatar.imageId },
                                )
                            },
                            selectedAvatarId = if (avatar.selected == true) {
                                avatar.imageId
                            } else {
                                currentState.emailAvatars.selectedAvatarId
                            },
                        )
                        currentState.copy(
                            uploadingAvatar = null,
                            emailAvatars = emailAvatars,
                            scrollToIndex = null,
                            avatarUpdates = if (avatar.selected == true) {
                                currentState.avatarUpdates.inc()
                            } else {
                                currentState.avatarUpdates
                            },
                        )
                    }
                }

                is GravatarResult.Failure -> {
                    _uiState.update { currentState ->
                        currentState.copy(
                            uploadingAvatar = null,
                            scrollToIndex = null,
                            failedUploads = currentState.failedUploads + AvatarUploadFailure(
                                uri,
                                error = (result.error as? QuickEditorError.Request)?.type,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            _uiState.update { currentState -> currentState.copy(profile = ComponentState.Loading) }
            when (val result = profileService.retrieveCatching(email)) {
                is GravatarResult.Success -> {
                    _uiState.update { currentState ->
                        currentState.copy(profile = ComponentState.Loaded(result.value))
                    }
                }

                is GravatarResult.Failure -> {
                    _uiState.update { currentState ->
                        currentState.copy(profile = null)
                    }
                }
            }
        }
    }

    private fun fetchAvatars() {
        viewModelScope.launch {
            fetchAvatars(
                showLoading = true,
                scrollToSelected = avatarPickerContentLayout == AvatarPickerContentLayout.Horizontal,
            )
        }
    }

    private suspend fun fetchAvatars(showLoading: Boolean = true, scrollToSelected: Boolean = true) {
        if (showLoading) {
            _uiState.update { currentState -> currentState.copy(isLoading = true) }
        }
        when (val result = avatarRepository.getAvatars(email)) {
            is GravatarResult.Success -> {
                _uiState.update { currentState ->
                    val emailAvatars = result.value
                    currentState.copy(
                        emailAvatars = emailAvatars,
                        scrollToIndex = if (scrollToSelected && emailAvatars.avatars.isNotEmpty()) {
                            emailAvatars.avatars.indexOfFirstOrNull {
                                it.imageId == emailAvatars.selectedAvatarId
                            }
                        } else {
                            null
                        },
                        isLoading = false,
                        error = null,
                    )
                }
            }

            is GravatarResult.Failure -> {
                _uiState.update { currentState ->
                    currentState.copy(emailAvatars = null, isLoading = false, error = result.error.asSectionError)
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun deleteAvatar(avatarId: String) {
        viewModelScope.launch {
            val avatarIndex = _uiState.value.emailAvatars?.avatars?.indexOfFirstOrNull { it.imageId == avatarId }
            val isSelectedAvatar = avatarId == _uiState.value.emailAvatars?.selectedAvatarId
            val avatar = avatarIndex?.let { _uiState.value.emailAvatars?.avatars?.get(avatarIndex) }
            if (avatar != null) {
                _uiState.update { currentState ->
                    val emailAvatars = currentState.emailAvatars?.copy(
                        avatars = currentState.emailAvatars.avatars.filter { it.imageId != avatarId },
                        selectedAvatarId = if (currentState.emailAvatars.selectedAvatarId == avatarId) {
                            null
                        } else {
                            currentState.emailAvatars.selectedAvatarId
                        },
                    )
                    currentState.copy(
                        emailAvatars = emailAvatars,
                    )
                }
                when (val result = avatarRepository.deleteAvatar(email, avatarId)) {
                    is GravatarResult.Success -> {
                        notifyAvatarDeletedSuccessfully(isSelectedAvatar)
                    }

                    is GravatarResult.Failure -> {
                        if ((result.error as? QuickEditorError.Request)?.type == ErrorType.NotFound) {
                            notifyAvatarDeletedSuccessfully(isSelectedAvatar)
                        } else {
                            notifyAvatarDeletionFailure(avatarId, avatarIndex, avatar, isSelectedAvatar)
                        }
                    }
                }
            }
        }
    }

    private suspend fun notifyAvatarDeletionFailure(
        avatarId: String,
        avatarIndex: Int,
        avatar: Avatar,
        isSelectedAvatar: Boolean,
    ) {
        _actions.send(AvatarPickerAction.AvatarDeletionFailed(avatarId))
        _uiState.update { currentState ->
            val emailAvatars = currentState.emailAvatars?.copy(
                avatars = currentState.emailAvatars.avatars.toMutableList().apply {
                    add(avatarIndex, avatar)
                },
                selectedAvatarId = if (isSelectedAvatar) {
                    avatarId
                } else {
                    currentState.emailAvatars.selectedAvatarId
                },
            )
            currentState.copy(
                emailAvatars = emailAvatars,
            )
        }
    }

    private suspend fun notifyAvatarDeletedSuccessfully(isSelectedAvatar: Boolean) {
        if (isSelectedAvatar) _actions.send(AvatarPickerAction.AvatarSelected)
        _uiState.update { currentState ->
            currentState.copy(
                avatarUpdates = if (isSelectedAvatar) {
                    currentState.avatarUpdates.inc()
                } else {
                    currentState.avatarUpdates
                },
            )
        }
    }

    private fun hideNonSelectedAvatarAlert() {
        _uiState.update { currentState ->
            currentState.copy(
                nonSelectedAvatarAlertVisible = false,
            )
        }
    }

    private fun nonAvatarSelectedAlertObserver() {
        _uiState
            .filter { it.emailAvatars != null }
            .map { it.emailAvatars?.selectedAvatarId != null }
            .distinctUntilChanged()
            .onEach { isAvatarSelected ->
                if (isAvatarSelected) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            nonSelectedAvatarAlertVisible = false,
                        )
                    }
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            nonSelectedAvatarAlertVisible = true,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private val QuickEditorError.asSectionError: SectionError
        get() = when (this) {
            QuickEditorError.TokenNotFound -> SectionError.InvalidToken(handleExpiredSession)
            QuickEditorError.Unknown -> SectionError.Unknown
            is QuickEditorError.Request -> when (type) {
                ErrorType.Server -> SectionError.ServerError
                ErrorType.Network -> SectionError.NoInternetConnection
                ErrorType.Unauthorized -> SectionError.InvalidToken(handleExpiredSession)
                else -> SectionError.Unknown
            }
        }

    private fun Avatar?.shouldUpdateRating(newRating: Avatar.Rating?): Boolean {
        return newRating != null && this?.rating != newRating
    }

    private fun Avatar?.shouldUpdateAltText(newAltText: String?): Boolean {
        return newAltText != null && this?.altText != newAltText
    }
}

internal class AvatarPickerViewModelFactory(
    private val gravatarQuickEditorParams: GravatarQuickEditorParams,
    private val handleExpiredSession: Boolean,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return AvatarPickerViewModel(
            handleExpiredSession = handleExpiredSession,
            email = gravatarQuickEditorParams.email,
            avatarPickerContentLayout = gravatarQuickEditorParams.avatarPickerContentLayout,
            profileService = QuickEditorContainer.getInstance().profileService,
            avatarRepository = QuickEditorContainer.getInstance().avatarRepository,
            imageDownloader = QuickEditorContainer.getInstance().imageDownloader,
            fileUtils = QuickEditorContainer.getInstance().fileUtils,
        ) as T
    }
}

private inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    val index = indexOfFirst { predicate(it) }
    return if (index == -1) null else index
}

internal fun Avatar.copy(rating: Avatar.Rating? = null, altText: String? = null): Avatar {
    return Avatar {
        imageId = this@copy.imageId
        imageUrl = this@copy.imageUrl
        updatedDate = this@copy.updatedDate
        selected = this@copy.selected
        this.altText = altText ?: this@copy.altText
        this.rating = rating ?: this@copy.rating
    }
}
