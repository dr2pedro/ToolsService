import com.anthropic.core.JsonValue
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolUnion
import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Prompt
import io.modelcontextprotocol.kotlin.sdk.Resource
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import kotlinx.coroutines.runBlocking

/**
 * A classe responsável por se comunicar com o servidor de ferramentas.
 *
 * @property transport A lista de conexões com os servidores de ferramentas.
 * @property clientName O nome do cliente.
 * @property clientVersion A versão do cliente.
 *
 * @constructor Cria uma instância da classe [ToolsServerConnection].
*/
class ToolsServerConnection(val transport: Transport, val clientName: String = "default-client", val clientVersion: String = "1.0.0") {
    companion object {
        fun extractText(result: CallToolResultBase?): String? {
            val result = result?.content?.joinToString("\n") { (it as TextContent).text ?: "" }
            return result
        }

        fun extractTextForLLM(toolName: String, result: CallToolResultBase?): String? {
            val resultContent = result?.content?.joinToString("\n") { (it as TextContent).text ?: "" }
            val json = "{\"type\": \"tool_result\",\"tool_name\": \"$toolName\",\"result\": \"$resultContent\"}"
            return json
        }
    }
    var connection: Client
        private set
    var tools: List<ToolUnion>? = null
        private set
    var prompts: List<Prompt>? = null
        private set
    var resources: List<Resource>? = null
        private set
    private val toolAdapter = ToolAdapter()

    init {
        connection = Client(Implementation(name = clientName, version = clientVersion))
    }

    fun getToolDefinitions(): List<ToolDefinition>? {
        return tools?.map { toolAdapter.mcpServerToolToToolDefinition(it) }
    }
    private suspend fun setServerTools() {
        try {
            val toolsListResult = connection.listTools()
            val toolsMutableList = mutableListOf<ToolUnion>()
            toolsListResult?.tools?.forEach { tool ->
                val type = JsonValue.from(tool.inputSchema.type)
                val properties = JsonValue.from(tool.inputSchema.properties)
                val required = JsonValue.from(tool.inputSchema.required)
                val input = Tool.InputSchema.builder().type(type).properties(properties).putAdditionalProperty("required", required).build()
                val description = tool.description ?: "The mcp server didn't provided a description for this tool."
                val name = tool.name
                val toolBuilder = Tool.builder().name(name).description(description).inputSchema(input).build()
                val union = ToolUnion.ofTool(toolBuilder)
                toolsMutableList.add(union)
                println("Connected to server with < $name > tool.")
            }
            tools = toolsMutableList.toList()
        } catch (e: Exception) {
            tools = null
            return
        }
    }
    private suspend fun setServerPrompts() {
        try {
            val promptListResult = connection.listPrompts()
            val promptMutableList = mutableListOf<Prompt>()
            promptListResult?.prompts?.forEach { prompt -> promptMutableList.add(prompt) }
            val prompt = promptMutableList.toList()
            if (prompt.isEmpty()) {
                println("No prompts found on the server.")
            }
            prompts = prompt
        } catch (e: Exception) {
            prompts = null
            return
        }
    }
    private suspend fun setServerResources() {
        try {
            val resourceListResult = connection.listResources()
            val resourceMutableList = mutableListOf<Resource>()
            resourceListResult?.resources?.forEach { resource -> resourceMutableList.add(resource) }
            val resource = resourceMutableList.toList()
            if (resource.isEmpty()) {
                println("No resources found on the server.")
            }
            resources = resource
        } catch (e: Exception) {
            resources = null
            return
        }

    }
    fun connect() = runBlocking {
        connection.connect(transport.transport)
        setServerTools()
        setServerPrompts()
        setServerResources()

        if(tools.isNullOrEmpty() && prompts.isNullOrEmpty() && resources.isNullOrEmpty()) {
            throw Exception("The server is empty. Connect to a server that has prompts, resources or tools.")
        }
    }
    fun disconnect() = runBlocking {
        transport.close()
    }
    suspend fun callTool(toolName: String, toolArgs: Map<String, Any?>? = null): CallToolResultBase? {
        val arguments = toolArgs ?: emptyMap()
        return connection.callTool(name = toolName, arguments = arguments)
    }
}
