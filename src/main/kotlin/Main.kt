
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

@Serializable
data class PragmaTokens(val pragmaGameToken:String, val pragmaSocialToken: String)

@Serializable
data class AuthOrCreateResponse(val pragmaTokens: PragmaTokens)
var pragmaGameToken: String = ""

fun main(args: Array<String>) {
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        install(WebSockets)
    }
    runBlocking {
        val body = """
            {
                "providerId":1, 
                "providerToken": "{\"accountId\":\"test01\",\"displayName\":\"test01\"}",
                "gameShardId":"00000000-0000-0000-0000-000000000001",
                "loginQueuePassToken": ""
            }
        """.trimIndent()
        val contentType: ContentType = ContentType.Application.Json
        val host = "127.0.0.1"
        val response = client.request {
            url(scheme = "http", host = host, port = 11000, path = "/v1/account/authenticateorcreatev2")
            method = HttpMethod.Post
            setBody(TextContent(body, contentType))
            header(HttpHeaders.Accept, contentType)
        }
        val authOrCreate: AuthOrCreateResponse = response.body()
        pragmaGameToken = authOrCreate.pragmaTokens.pragmaGameToken

        client.webSocket(
            method = HttpMethod.Get,
            host = host,
            port = 10000,
            path = "/v1/rpc",
            prepareRequest()
        ) {
            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages() }

            userInputRoutine.join() // Wait for completion; either "exit" or error
            messageOutputRoutine.cancelAndJoin()
        }
    }
    client.close()
    println("Connection closed. Goodbye!")
}


fun prepareRequest(): HttpRequestBuilder.() -> Unit = {
    method = HttpMethod.Get
    url("ws", "127.0.0.1", 10000, "/v1/rpc")

    buildHeaders {
        header(HttpHeaders.Accept, "application/json")
        header(HttpHeaders.Authorization, "Bearer $pragmaGameToken")
//        header("pragma-reconnect", "")
    }

}

suspend fun DefaultClientWebSocketSession.outputMessages() {
    try {
        for (message in incoming) {
            message as? Frame.Text ?: continue
            println(message.readText())
        }
    } catch (e: Exception) {
        println("Error while receiving: " + e.localizedMessage)
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages() {
    while (true) {
        val message = readLine() ?: ""
        if (message.equals("exit", true)) return
        try {
            send(message)
        } catch (e: Exception) {
            println("Error while sending: " + e.localizedMessage)
            return
        }
    }
}

