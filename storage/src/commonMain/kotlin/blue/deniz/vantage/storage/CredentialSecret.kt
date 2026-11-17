package blue.deniz.vantage.storage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface CredentialSecret {
    @Serializable
    @SerialName("login")
    data class Login(val username: String, val password: String) : CredentialSecret

    @Serializable
    @SerialName("token")
    data class Token(val token: String) : CredentialSecret
}
