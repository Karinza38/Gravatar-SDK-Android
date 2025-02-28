package com.gravatar.quickeditor.ui.oauth

import com.gravatar.restapi.models.Profile
import com.gravatar.ui.components.ComponentState

internal data class OAuthUiState(
    val status: OAuthStatus = OAuthStatus.LoginRequired,
    val profile: ComponentState<Profile>? = null,
)

internal sealed class OAuthStatus {
    internal data object Authorizing : OAuthStatus()

    internal data object LoginRequired : OAuthStatus()

    internal data object WrongEmailAuthorized : OAuthStatus()

    internal data class EmailAssociatedCheckError(val token: String) : OAuthStatus()
}

internal sealed class OAuthAction {
    internal data object StartOAuth : OAuthAction()

    internal data object AuthorizationSuccess : OAuthAction()

    internal data object AuthorizationFailure : OAuthAction()
}
