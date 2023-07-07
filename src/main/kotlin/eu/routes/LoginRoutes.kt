package eu.routes

import LoginType
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import eu.services.UserService
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.*

val env = dotenv()

@Serializable
data class LoginParameters(
    val idToken: String,
    val loginType: LoginType,
)

val transport = NetHttpTransport()
val jsonFactory = GsonFactory()

fun Route.authRoutes() {
    val userService by inject<UserService>()

    post("/login") {
        try {
            val loginParameters = call.receive<LoginParameters>()

            // Verify the access token against Google OAuth2 server asynchronously
            val response = async { verifyGoogleIdToken(loginParameters.idToken) }
            val googleIdToken = response.await()
            val payload = googleIdToken?.payload
            if (payload != null) {
                val user = userService.createUser(
                    payload.email,
                    payload.get("given_name").toString(),
                    null,
                    payload.get("family_name").toString(),
                )
                val accessToken = userService.createAccessToken(
                    loginParameters.idToken,
                    loginParameters.loginType,
                    user.id,
                    null,
                )
                call.respond(HttpStatusCode.OK, accessToken)
            } else {
                // Access token is invalid
                call.respond(HttpStatusCode.Unauthorized, "Invalid id token")
            }
        } catch (e: ContentTransformationException) {
            call.respond(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }
}

private suspend fun verifyGoogleIdToken(accessToken: String): GoogleIdToken? =
    withContext(Dispatchers.IO) {
        val verifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
            .setAudience(Collections.singletonList(env["GOOGLE_CLIENT_ID"]))
            .build()

        return@withContext try {
            verifier.verify(accessToken)
        } catch (e: Exception) {
            print(e.toString())
            null
        }
    }
