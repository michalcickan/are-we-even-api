import eu.models.responses.GenericResponse
import eu.models.responses.User
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class HandleRequestTest {
    private val mockCall: ApplicationCall = mockk()

    @Test
    fun `handleRequestWithExceptions should respond with NoContent for Unit result`() = runBlocking {
        val slot = slot<HttpStatusCode>()
        coEvery { mockCall.respond(capture(slot)) } answers {
            assertEquals(slot.captured, HttpStatusCode.NoContent)
        }
        handleRequestWithExceptions(mockCall) {}
    }

    @Test
    fun `handleRequestWithExceptions should respond with Status ok and object for User result`() = runBlocking {
        val statusSlot = slot<HttpStatusCode>()
        val userSlot = slot<GenericResponse<User>>()
        val user = User(
            2,
            null,
            null,
            null,
            emptyList(),
            "fdsafd@fdfa.sk",
        )
        coEvery { mockCall.respond(capture(statusSlot), capture(userSlot)) } answers {
            assertEquals(statusSlot.captured, HttpStatusCode.OK)
            assertEquals(userSlot.captured.data, user)
        }
        handleRequestWithExceptions(mockCall) {
            user
        }
    }
}
