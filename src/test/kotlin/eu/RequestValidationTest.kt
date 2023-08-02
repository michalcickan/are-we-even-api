// import eu.helpers.MockTransactionHandler
// import eu.models.parameters.expense.AddExpenseParameters
// import eu.models.parameters.expense.AddExpenseParametersPayer
// import eu.models.parameters.LoginParameters
// import eu.models.responses.AccessToken
// import eu.models.responses.GenericResponse
// import eu.module
// import eu.modules.ITransactionHandler
// import eu.services.*
// import eu.execeptions.APIException
// import eu.validation.IAuthRequestValidation
// import io.ktor.client.*
// import io.ktor.client.call.*
// import io.ktor.client.plugins.contentnegotiation.*
// import io.ktor.client.request.*
// import io.ktor.http.*
// import io.ktor.serialization.kotlinx.json.*
// import io.ktor.server.config.*
// import io.ktor.server.testing.*
// import io.ktor.util.*
// import io.mockk.coEvery
// import io.mockk.every
// import io.mockk.mockk
// import kotlinx.serialization.json.Json
// import org.junit.After
// import org.junit.Before
// import org.junit.Test
// import org.koin.core.context.startKoin
// import org.koin.core.context.stopKoin
// import org.koin.dsl.module
// import org.koin.test.KoinTest
// import org.springframework.security.crypto.password.PasswordEncoder
// import java.util.*
// import kotlin.test.assertEquals
// import kotlin.test.assertNotNull
// import kotlin.test.fail
//
// class RequestValidationTest : KoinTest {
//    private val mockTransactionHandler: ITransactionHandler = MockTransactionHandler()
//    private val passwordEncoder: PasswordEncoder = mockk()
//    private val mockValidationService: IAuthRequestValidation = mockk()
//    private val mockAuthService: IAuthService = mockk()
//
//    @Before
//    fun setup() {
//        startKoin {
//            modules(
//                module {
//                    single<IAuthRequestValidation> { mockValidationService }
//                    single<ITransactionHandler> { mockTransactionHandler }
//                    single<IJWTService> { JWTService(get(), "", "", "") }
//                    single<PasswordEncoder> { passwordEncoder }
//                    single<IUserService> { UserService(get(), get()) }
//                    single<IAuthService> { mockAuthService }
//                },
//            )
//        }
//    }
//
//    @After
//    fun tearDown() {
//        stopKoin()
//    }
//
//    @OptIn(InternalAPI::class)
//    @Test
//    fun `should return 200 OK if email is null`() = testApplication {
//        val client = setupAndGetHttpClient()
//        val response = client.post(
//            "/login",
//        ) {
//            contentType(ContentType.Application.Json)
//            setBody(
//                LoginParameters(
//                    null,
//                    null,
//                    null,
//                ),
//            )
//        }
//
//        val apiException = APIException.IncorrectLoginValues
//        assertEquals(apiException.statusCode, response.status)
//
//        try {
//            val response: GenericResponse<Unit> = response.body()
//            assertNotNull(apiException.message, response.error?.message)
//        } catch (e: Exception) {
//            fail("Couldn't parse error")
//        }
//    }
//
//    @Test
//    fun `should return 400 Bad Request if email is invalid`() = testApplication {
//        val client = setupAndGetHttpClient()
//        every { mockValidationService.validateEmail(any()) } returns false
//        val call = client.post("/login") {
//            contentType(ContentType.Application.Json)
//            setBody("""{"email": "invalid_email"}""")
//        }
//        val apiException = APIException.InvalidEmailFormat
//        val response: GenericResponse<Unit> = call.body()
//        assertEquals(apiException.statusCode, call.status)
//        assertEquals(apiException.message, response.error?.message)
//    }
//
//    @Test
//    fun `should return 200 OK if email is valid`() = testApplication {
//        // Mock the behavior of validationService.validateEmail()
//        every { mockValidationService.validateEmail(any()) } returns true
//        coEvery { mockAuthService.loginWith(any(), any()) } returns AccessToken(
//            "fdfads",
//            "fdafdafa",
//            Date(),
//        )
//        val client = setupAndGetHttpClient()
//        val call = client.post("/login") {
//            contentType(ContentType.Application.Json)
//            setBody(
//                LoginParameters(
//                    null,
//                    "Qwerty123!",
//                    "test@email.eu",
//                ),
//            )
//        }
//        val body: GenericResponse<Unit> = call.body()
//        print(body)
//        assertEquals(HttpStatusCode.OK, call.status)
//    }
//
//    @Test
//    fun `should throw the PaidOrDueAmountCannotBeNegative when some of sums are negative`() = testApplication {
//        // Mock the behavior of validationService.validateEmail()
//        every { mockValidationService.validateEmail(any()) } returns true
//        coEvery { mockAuthService.loginWith(any(), any()) } returns AccessToken(
//            "fdfads",
//            "fdafdafa",
//            Date(),
//        )
//        val client = setupAndGetHttpClient()
//        val paidAmounts = listOf(20f, 30f, -40f)
//        val dueAmounts = listOf(30f, 30f, 30f)
//        val result = client.post("/groups/2/expense") {
//            contentType(ContentType.Application.Json)
//            bearerAuth("test")
//            setBody(
//                AddExpenseParameters(
//                    users = paidAmounts.mapIndexed { index, amount ->
//                        AddExpenseParametersPayer(
//                            2,
//                            amount,
//                            dueAmounts[index],
//                        )
//                    },
//                    "something",
//                ),
//            )
//        }
//        val exception = APIException.PaidOrDueAmountCannotBeNegative
//        val response: GenericResponse<Unit> = result.body()
//        assertEquals(exception.message, response.error?.message)
//    }
// }
//
// private fun ApplicationTestBuilder.setupAndGetHttpClient(): HttpClient {
//    application {
//        module(testing = true)
//    }
//    environment {
//        config = MapApplicationConfig()
//    }
//
//    return createClient {
//        install(ContentNegotiation) {
//            json(
//                Json {
//                    explicitNulls = false
//                    ignoreUnknownKeys = true
//                },
//            )
//        }
//    }
// }
