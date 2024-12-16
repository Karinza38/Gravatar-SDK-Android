package com.gravatar.services

import com.gravatar.GravatarSdkContainerRule
import com.gravatar.restapi.models.Avatar
import com.gravatar.restapi.models.Rating
import com.gravatar.types.Hash
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.net.URI
import kotlin.test.assertNull

class AvatarServiceTest {
    @get:Rule
    var containerRule = GravatarSdkContainerRule()

    private lateinit var avatarService: AvatarService
    private val oauthToken = "oauthToken"

    private val avatar = Avatar {
        imageId = "id"
        imageUrl = URI.create("https://gravatar.com/avatar/test")
        selected = false
        altText = ""
        rating = Avatar.Rating.G
        updatedDate = ""
    }

    @Before
    fun setUp() {
        avatarService = AvatarService()
    }

    // V3 Methods
    @Test
    fun `given a file when uploading avatar then Gravatar service is invoked`() = runTest {
        val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
            every { isSuccessful } returns true
            every { body() } returns avatar
        }
        coEvery { containerRule.gravatarApiMock.uploadAvatar(any(), any(), any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns true

        avatarService.upload(
            File("avatarFile"),
            oauthToken,
            Hash("hash"),
            selectAvatar = false,
        )

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.uploadAvatar(
                withArg {
                    assertTrue(
                        with(it.headers?.values("Content-Disposition").toString()) {
                            contains("image") && contains("avatarFile")
                        },
                    )
                },
                withArg { },
                withArg { },
            )
        }
    }

    @Test
    fun `given null optional params when uploading avatar then null values passed`() = runTest {
        val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
            every { isSuccessful } returns true
            every { body() } returns avatar
        }
        coEvery { containerRule.gravatarApiMock.uploadAvatar(any(), any(), any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns true

        avatarService.upload(
            File("avatarFile"),
            oauthToken,
            null,
            null,
        )

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.uploadAvatar(
                withArg {
                    assertTrue(
                        with(it.headers?.values("Content-Disposition").toString()) {
                            contains("image") && contains("avatarFile")
                        },
                    )
                },
                withNullableArg { assertNull(it) },
                withNullableArg { assertNull(it) },
            )
        }
    }

    @Test
    fun `given an avatar upload when an error occurs then an exception is thrown`() =
        runTestExpectingGravatarException(ErrorType.Server, HttpException::class.java) {
            val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
                every { isSuccessful } returns false
                every { code() } returns 500
            }
            coEvery { containerRule.gravatarApiMock.uploadAvatar(any(), any(), any()) } returns mockResponse

            avatarService.upload(
                File("avatarFile"),
                oauthToken,
                Hash("hash"),
                selectAvatar = false,
            )
        }

    @Test
    fun `given a file when uploadCatching avatar then Gravatar service is invoked`() = runTest {
        val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
            every { isSuccessful } returns true
            every { body() } returns avatar
        }
        coEvery { containerRule.gravatarApiMock.uploadAvatar(any(), any(), any()) } returns mockResponse

        val response = avatarService.uploadCatching(
            File("avatarFile"),
            oauthToken,
            Hash("hash"),
            selectAvatar = false,
        )

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.uploadAvatar(
                withArg {
                    assertTrue(
                        with(it.headers?.values("Content-Disposition").toString()) {
                            contains("image") && contains("avatarFile")
                        },
                    )
                },
                withArg { },
                withArg { },
            )
        }

        assertTrue(response is GravatarResult.Success)
    }

    @Test
    fun `given an avatar uploadCatching when an error occurs then a Result Failure is returned`() = runTest {
        val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
            every { isSuccessful } returns false
            every { code() } returns 500
        }
        coEvery { containerRule.gravatarApiMock.uploadAvatar(any(), any(), any()) } returns mockResponse

        val response = avatarService.uploadCatching(
            File("avatarFile"),
            oauthToken,
            Hash("hash"),
            selectAvatar = false,
        )

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.uploadAvatar(
                withArg {
                    assertTrue(
                        with(it.headers?.values("Content-Disposition").toString()) {
                            contains("image") && contains("avatarFile")
                        },
                    )
                },
                withArg { },
                withArg { },
            )
        }

        assertEquals(ErrorType.Server, (response as GravatarResult.Failure).error)
    }

    @Test
    fun `given a hash and an avatarId when setting avatar then Gravatar service is invoked`() = runTest {
        val hash = "hash"
        val avatarId = "avatarId"
        val mockResponse = mockk<Response<Unit>>(relaxed = true) {
            every { isSuccessful } returns true
        }

        coEvery { containerRule.gravatarApiMock.setEmailAvatar(avatarId, any()) } returns mockResponse

        avatarService.setAvatar(hash, avatarId, oauthToken)
    }

    @Test
    fun `given a hash and an avatarId when setting an avatar and an error occurs then an exception is thrown`() =
        runTestExpectingGravatarException(ErrorType.Server, HttpException::class.java) {
            val hash = "hash"
            val avatarId = "avatarId"
            val mockResponse = mockk<Response<Unit>>(relaxed = true) {
                every { isSuccessful } returns false
                every { code() } returns 500
            }

            coEvery { containerRule.gravatarApiMock.setEmailAvatar(avatarId, any()) } returns mockResponse

            avatarService.setAvatar(hash, avatarId, oauthToken)
        }

    @Test
    fun `given a hash and an avatarId when setAvatarCatching then Gravatar service is invoked`() = runTest {
        val hash = "hash"
        val avatarId = "avatarId"
        val mockResponse = mockk<Response<Unit>>(relaxed = true) {
            every { isSuccessful } returns true
        }

        coEvery { containerRule.gravatarApiMock.setEmailAvatar(avatarId, any()) } returns mockResponse

        val response = avatarService.setAvatarCatching(hash, avatarId, oauthToken)

        assertEquals(Unit, (response as GravatarResult.Success).value)
    }

    @Test
    fun `given a hash and an avatarId when setAvatarCatching and an error occurs then a Result Failure is returned`() =
        runTest {
            val hash = "hash"
            val avatarId = "avatarId"
            val mockResponse = mockk<Response<Unit>>(relaxed = true) {
                every { isSuccessful } returns false
                every { code() } returns 500
            }

            coEvery { containerRule.gravatarApiMock.setEmailAvatar(avatarId, any()) } returns mockResponse

            val response = avatarService.setAvatarCatching(hash, avatarId, oauthToken)

            assertEquals(ErrorType.Server, (response as GravatarResult.Failure).error)
        }

    @Test
    fun `given an imageId when deleting avatar then Gravatar service is invoked`() = runTest {
        val imageId = "imageId"
        val mockResponse = mockk<Response<Unit>>(relaxed = true) {
            every { isSuccessful } returns true
        }

        coEvery { containerRule.gravatarApiMock.deleteAvatar(imageId) } returns mockResponse

        avatarService.deleteAvatar(imageId, oauthToken)

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.deleteAvatar(imageId)
        }
    }

    @Test
    fun `given an imageId when deleting avatar and an error occurs then an exception is thrown`() =
        runTestExpectingGravatarException(ErrorType.Server, HttpException::class.java) {
            val imageId = "imageId"
            val mockResponse = mockk<Response<Unit>>(relaxed = true) {
                every { isSuccessful } returns false
                every { code() } returns 500
            }

            coEvery { containerRule.gravatarApiMock.deleteAvatar(imageId) } returns mockResponse

            avatarService.deleteAvatar(imageId, oauthToken)
        }

    @Test
    fun `given an imageId when deleteAvatarCatching then Gravatar service is invoked`() = runTest {
        val imageId = "imageId"
        val mockResponse = mockk<Response<Unit>>(relaxed = true) {
            every { isSuccessful } returns true
        }

        coEvery { containerRule.gravatarApiMock.deleteAvatar(imageId) } returns mockResponse

        val response = avatarService.deleteAvatarCatching(imageId, oauthToken)

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.deleteAvatar(imageId)
        }

        assertEquals(Unit, (response as GravatarResult.Success).value)
    }

    @Test
    fun `given an imageId when deleteAvatarCatching and an error occurs then a Result Failure is returned`() = runTest {
        val imageId = "imageId"
        val mockResponse = mockk<Response<Unit>>(relaxed = true) {
            every { isSuccessful } returns false
            every { code() } returns 500
        }

        coEvery { containerRule.gravatarApiMock.deleteAvatar(imageId) } returns mockResponse

        val response = avatarService.deleteAvatarCatching(imageId, oauthToken)

        assertEquals(ErrorType.Server, (response as GravatarResult.Failure).error)
    }

    @Test
    fun `given an avatarId when updating avatar then Gravatar service is invoked`() = runTest {
        val avatarId = "avatarId"
        val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
            every { isSuccessful } returns true
            every { body() } returns avatar
        }

        coEvery { containerRule.gravatarApiMock.updateAvatar(avatarId, any()) } returns mockResponse

        val updatedAvatar = avatarService.updateAvatar(
            avatarId,
            oauthToken,
            Avatar.Rating.PG,
            "New Alt Text",
        )

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.updateAvatar(
                avatarId,
                withArg {
                    assertEquals(Rating.PG, it.rating)
                    assertEquals("New Alt Text", it.altText)
                },
            )
        }

        assertEquals(avatar, updatedAvatar)
    }

    @Test
    fun `given an avatarId when updating avatar and an error occurs then an exception is thrown`() =
        runTestExpectingGravatarException(ErrorType.Server, HttpException::class.java) {
            val avatarId = "avatarId"
            val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
                every { isSuccessful } returns false
                every { code() } returns 500
            }

            coEvery { containerRule.gravatarApiMock.updateAvatar(avatarId, any()) } returns mockResponse

            avatarService.updateAvatar(
                avatarId,
                oauthToken,
                Avatar.Rating.PG,
                "New Alt Text",
            )
        }

    @Test
    fun `given an avatarId when updateAvatarCatching then Gravatar service is invoked`() = runTest {
        val avatarId = "avatarId"
        val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
            every { isSuccessful } returns true
            every { body() } returns avatar
        }

        coEvery { containerRule.gravatarApiMock.updateAvatar(avatarId, any()) } returns mockResponse

        val response = avatarService.updateAvatarCatching(
            avatarId,
            oauthToken,
            Avatar.Rating.PG,
            "New Alt Text",
        )

        coVerify(exactly = 1) {
            containerRule.gravatarApiMock.updateAvatar(
                avatarId,
                withArg {
                    assertEquals(Rating.PG, it.rating)
                    assertEquals("New Alt Text", it.altText)
                },
            )
        }

        assertEquals(avatar, (response as GravatarResult.Success).value)
    }

    @Test
    fun `given an avatarId when updateAvatarCatching and an error occurs then a Result Failure is returned`() =
        runTest {
            val avatarId = "avatarId"
            val mockResponse = mockk<Response<Avatar>>(relaxed = true) {
                every { isSuccessful } returns false
                every { code() } returns 500
            }

            coEvery { containerRule.gravatarApiMock.updateAvatar(avatarId, any()) } returns mockResponse

            val response = avatarService.updateAvatarCatching(
                avatarId,
                oauthToken,
                Avatar.Rating.PG,
                "New Alt Text",
            )

            assertEquals(ErrorType.Server, (response as GravatarResult.Failure).error)
        }
}
