package com.gravatar.quickeditor.ui.avatarpicker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gravatar.quickeditor.QuickEditorContainer
import com.gravatar.quickeditor.data.FileUtils
import com.gravatar.quickeditor.data.repository.AvatarRepository
import com.gravatar.restapi.models.Avatar
import com.gravatar.restapi.models.Profile
import com.gravatar.services.ProfileService
import com.gravatar.services.Result
import com.gravatar.types.Email
import com.gravatar.ui.components.ComponentState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

internal class AvatarPickerViewModel(
    private val email: Email,
    private val profileService: ProfileService,
    private val avatarRepository: AvatarRepository,
    private val fileUtils: FileUtils,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AvatarPickerUiState(email = email))
    val uiState: StateFlow<AvatarPickerUiState> = _uiState.asStateFlow()
    private val _actions = Channel<AvatarPickerAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        fetchAvatars()
        fetchProfile()
    }

    fun selectAvatar(avatar: Avatar) {
        viewModelScope.launch {
            val avatarId = avatar.imageId
            if (_uiState.value.identityAvatars?.selectedAvatarId != avatarId) {
                _uiState.update { currentState ->
                    currentState.copy(selectingAvatarId = avatarId)
                }
                when (avatarRepository.selectAvatar(email, avatarId)) {
                    is Result.Success -> {
                        _uiState.update { currentState ->
                            currentState.copy(
                                identityAvatars = currentState.identityAvatars?.copy(selectedAvatarId = avatarId),
                                selectingAvatarId = null,
                                profile = currentState.profile?.copy { copyAvatar(avatar) },
                            )
                        }
                        _actions.send(AvatarPickerAction.AvatarSelected(avatar))
                    }

                    is Result.Failure -> {
                        _uiState.update { currentState ->
                            currentState.copy(selectingAvatarId = null)
                        }
                        // display error snack
                    }
                }
            }
        }
    }

    fun localImageSelected(imageUri: Uri) {
        viewModelScope.launch {
            _actions.send(AvatarPickerAction.LaunchImageCropper(imageUri, fileUtils.createTempFile()))
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(uploadingAvatar = uri, scrollToIndex = 0)
            }
            when (avatarRepository.uploadAvatar(email, uri)) {
                is Result.Success -> {
                    fileUtils.deleteFile(uri)
                    fetchAvatars(showLoading = false, scrollToSelected = false)
                    _uiState.update { currentState ->
                        currentState.copy(uploadingAvatar = null)
                    }
                }

                is Result.Failure -> {
                    fileUtils.deleteFile(uri) // Once we have better UI for errors we will keep the file for retries
                    _uiState.update { currentState ->
                        currentState.copy(uploadingAvatar = null)
                    }
                }
            }
        }
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            _uiState.update { currentState -> currentState.copy(profile = ComponentState.Loading) }
            when (val result = profileService.retrieveCatching(email)) {
                is Result.Success -> {
                    _uiState.update { currentState ->
                        currentState.copy(profile = ComponentState.Loaded(result.value))
                    }
                }

                is Result.Failure -> {
                    _uiState.update { currentState ->
                        currentState.copy(profile = null)
                    }
                }
            }
        }
    }

    private fun fetchAvatars() {
        viewModelScope.launch {
            fetchAvatars(showLoading = true)
        }
    }

    private suspend fun fetchAvatars(showLoading: Boolean = true, scrollToSelected: Boolean = true) {
        if (showLoading) {
            _uiState.update { currentState -> currentState.copy(isLoading = true) }
        }
        when (val result = avatarRepository.getAvatars(email)) {
            is Result.Success -> {
                _uiState.update { currentState ->
                    val identityAvatars = result.value
                    currentState.copy(
                        identityAvatars = identityAvatars,
                        scrollToIndex = if (scrollToSelected && identityAvatars.avatars.isNotEmpty()) {
                            identityAvatars.avatars.indexOfFirst {
                                it.imageId == identityAvatars.selectedAvatarId
                            }.coerceAtLeast(0)
                        } else {
                            null
                        },
                        isLoading = false,
                        error = false,
                    )
                }
            }

            is Result.Failure -> {
                _uiState.update { currentState ->
                    currentState.copy(identityAvatars = null, isLoading = false, error = true)
                }
            }
        }
    }
}

internal class AvatarPickerViewModelFactory(
    private val email: Email,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return AvatarPickerViewModel(
            email = email,
            profileService = QuickEditorContainer.getInstance().profileService,
            avatarRepository = QuickEditorContainer.getInstance().avatarRepository,
            fileUtils = QuickEditorContainer.getInstance().fileUtils,
        ) as T
    }
}

internal fun Profile.copyAvatar(avatar: Avatar): Profile {
    return Profile {
        hash = this@copyAvatar.hash
        displayName = this@copyAvatar.displayName
        profileUrl = this@copyAvatar.profileUrl
        avatarUrl = URI.create(avatar.fullUrl)
        avatarAltText = avatar.altText
        location = this@copyAvatar.location
        description = this@copyAvatar.description
        jobTitle = this@copyAvatar.jobTitle
        description = this@copyAvatar.description
        jobTitle = this@copyAvatar.jobTitle
        company = this@copyAvatar.company
        verifiedAccounts = this@copyAvatar.verifiedAccounts
        pronunciation = this@copyAvatar.pronunciation
        pronouns = this@copyAvatar.pronouns
        timezone = this@copyAvatar.timezone
        languages = this@copyAvatar.languages
        firstName = this@copyAvatar.firstName
        lastName = this@copyAvatar.lastName
        isOrganization = this@copyAvatar.isOrganization
        links = this@copyAvatar.links
        interests = this@copyAvatar.interests
        payments = this@copyAvatar.payments
        contactInfo = this@copyAvatar.contactInfo
        gallery = this@copyAvatar.gallery
        numberVerifiedAccounts = this@copyAvatar.numberVerifiedAccounts
        lastProfileEdit = this@copyAvatar.lastProfileEdit
        registrationDate = this@copyAvatar.registrationDate
    }
}

private fun <T> ComponentState<T>.copy(transform: T.() -> T): ComponentState<T> = when (this) {
    is ComponentState.Loaded -> ComponentState.Loaded(loadedValue.transform())
    is ComponentState.Loading -> this
    is ComponentState.Empty -> this
}
