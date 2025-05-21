import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ToolsServiceTest {
    @Test
    fun `A LLM deve reconhecer a tool que esta em um server STDIO, enviar o pedido de chamar essa tool corretamente e obter o resultado final esperado`() = runBlocking {
        val serverPathString = "C:\\Users\\dr2p\\WebstormProjects\\simple-mcp-server\\dist\\stdio_server.js"
        val transport = Transport.Stdio(serverPathString)
        val toolsConnection = ToolsServerConnection(transport)
        val llmConnection = LLMChatConnection("ollama", "http://localhost:11434/v1/", "llama3-groq-tool-use")
        val service = ToolsService(llmConnection, toolsConnection)
        val prompt = PromptBuilder()
            .appendSystemMessage("You ALWAYS search for tools to get the results.")
            .appendUserMessage("Add 2 to 3 using available tools.")
        toolsConnection.getToolDefinitions()?.map { tool ->
            prompt.addTool(tool)
        }
        val updatedPrompt = service.updateWithToolCall(prompt)
        val params = LLMChatConnection.QueryParams(updatedPrompt)
        val response = llmConnection.query(params).choices.first().message.content as String
        print(response)
        assert(response.contains("5"))
    }
    @Test
    fun `A LLM deve reconhecer a tool que esta em um server SSE, enviar o pedido de chamar essa tool corretamente e obter o resultado final esperado`() = runBlocking {
        val severURL = "http://localhost:8080"
        val httpClient = HttpClient() { install(SSE); install(Logging) }
        val transport = Transport.Sse(severURL, httpClient)
        val toolsConnection = ToolsServerConnection(transport)
        val llmConnection = LLMChatConnection("ollama","http://localhost:11434/v1/", "llama3-groq-tool-use")
        val service = ToolsService(llmConnection, toolsConnection)
        val prompt = PromptBuilder()
            .appendSystemMessage("You ALWAYS search for tools to get the results.")
            .appendUserMessage("Add 2 to 3 using available tools.")
        val updatedPrompt = service.updateWithToolCall(prompt)
        val params = LLMChatConnection.QueryParams(updatedPrompt)
        val response = llmConnection.query(params).choices.first().message.content as String
        assert(response.contains("5"))
    }
    @Test
    fun `Deve ser capaz de consultar os prompts do servidor`() = runBlocking {
        val severURL = "http://localhost:8080"
        val httpClient = HttpClient() { install(SSE); install(Logging) }
        val transport = Transport.Sse(severURL, httpClient)
        val toolsConnection = ToolsServerConnection(transport)
        val llmConnection = LLMChatConnection("ollama","http://localhost:11434/v1/", "llama3-groq-tool-use")
        val service = ToolsService(llmConnection, toolsConnection)
        val prompts = service.loadPrompts()
        assert(prompts.isNotEmpty())
        assert(prompts.size == 1)
        assert(prompts.first().description == "Develop small kotlin applications")
    }
    @Test
    fun `Deve ser capaz de buscar os recursos do servidor`() = runBlocking {
        val severURL = "http://localhost:8080"
        val httpClient = HttpClient() { install(SSE); install(Logging) }
        val transport = Transport.Sse(severURL, httpClient)
        val toolsConnection = ToolsServerConnection(transport)
        val llmConnection = LLMChatConnection("ollama","http://localhost:11434/v1/", "llama3-groq-tool-use")
        val service = ToolsService(llmConnection, toolsConnection)
        val resources = service.fetchResources()
        assert(resources.isNotEmpty())
        assert(resources.size == 1)
        assert(resources.first().name == "Web Search")
    }
    @Test
    fun `Deve ser capaz de escolher uma tool dentre duas com o mesmo nome`() = runBlocking {
        val serverPathString = "C:\\Users\\dr2p\\WebstormProjects\\simple-mcp-server\\dist\\stdio_server.js"
        val transport1 = Transport.Stdio(serverPathString)
        val toolsConnection1 = ToolsServerConnection(transport1)
        val severURL = "http://localhost:8080"
        val httpClient = HttpClient() { install(SSE); install(Logging) }
        val transport2 = Transport.Sse(severURL, httpClient)
        val toolsConnection2 = ToolsServerConnection(transport2)
        val llmConnection = LLMChatConnection("ollama","http://localhost:11434/v1/", "llama3-groq-tool-use")
        val service = ToolsService(llmConnection, listOf<ToolsServerConnection>(toolsConnection1, toolsConnection2))
        val prompt = PromptBuilder()
            .appendSystemMessage("You ALWAYS search for tools to get the results.")
            .appendUserMessage("Add 2 to 3 using available tools.")
        val updatedPrompt = service.updateWithToolCall(prompt)
        val params = LLMChatConnection.QueryParams(updatedPrompt)
        val response = llmConnection.query(params).choices.first().message.content as String
        assert(response.contains("5"))
    }
}