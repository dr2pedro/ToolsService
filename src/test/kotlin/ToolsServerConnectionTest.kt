import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ToolsServerConnectionTest {
    @Test
    fun `Deve conectar a um host local de Tools via STDIO`() = runBlocking {
        val serverPathString = "C:\\Users\\dr2p\\WebstormProjects\\simple-mcp-server\\dist\\stdio_server.js"
        val transport = Transport.Stdio(serverPathString)
        val toolsConnection = ToolsServerConnection(transport)
        toolsConnection.connect()
        val tools = toolsConnection.tools
        assertEquals(1, tools?.size)
        toolsConnection.disconnect()
    }
    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `Deve conectar a um host local de Tools via HTTP`() = runBlocking {
        val severURL = "http://localhost:8080"
        val httpClient = HttpClient() {
            install(SSE)
            install(Logging)

            // Aqui é onde se configura o token de acesso ao servidor.
            // TODO: Não precisa se preocupar em abstrair isso pq está fácil de usar.
            /*
                defaultRequest {
                    headers.append("Authorization", "Bearer ${< O token aqui >}")
                }
            */
        }
        val transport = Transport.Sse(severURL, httpClient)
        val toolsConnection = ToolsServerConnection(transport)
        toolsConnection.connect()
        val tools = toolsConnection.tools
        assertEquals(1, tools?.size)
        toolsConnection.disconnect()
    }
    @Test
    fun `Deve testar a chamada de tools para um servidor via STDIO`() = runBlocking {
        val serverPathString = "C:\\Users\\dr2p\\WebstormProjects\\simple-mcp-server\\dist\\stdio_server.js"
        val transport = Transport.Stdio(serverPathString)
        val toolsConnection = ToolsServerConnection(transport)
        toolsConnection.connect()
        val params = mapOf("a" to 2, "b" to 3)
        val callToolResult = toolsConnection.callTool("add", params)
        val result = ToolsServerConnection.extractText(callToolResult)
        assertEquals("5", result)
        toolsConnection.disconnect()
    }
    @Test
    fun `Deve testar a chamada de tools para um servidor via HTTP`() = runBlocking {
        val serverURL = "http://localhost:8080"
        val httpClient = HttpClient() {
            install(SSE)
            install(Logging)
        }
        val transport = Transport.Sse(serverURL, httpClient)
        val toolsConnection = ToolsServerConnection(transport)
        toolsConnection.connect()
        val toolName = "add"
        val params = mapOf("a" to 2, "b" to 3)
        val callToolResult = toolsConnection.callTool(toolName, params)
        val result = ToolsServerConnection.extractText(callToolResult)
        assertEquals("5", result)
        val resultForLLM = ToolsServerConnection.extractTextForLLM(toolName,callToolResult)
        val resultedForLLM = "{\"type\": \"tool_result\",\"tool_name\": \"$toolName\",\"result\": \"5\"}"
        assertEquals(resultedForLLM, resultForLLM)
        toolsConnection.disconnect()
    }
}