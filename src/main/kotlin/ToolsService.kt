import com.aallam.openai.api.chat.ToolCall
import io.modelcontextprotocol.kotlin.sdk.Prompt
import io.modelcontextprotocol.kotlin.sdk.Resource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/**
 * A classe responsável por atualizar o prompt com as informações de chamadas de ferramentas.
 *
 * @property toolsConnection A lista de conexões com os servidores de ferramentas.
 * @property llmConnection A conexão com o servidor de linguagem natural (LLM).
 * @property retry O número de tentativas para chamar uma ferramenta.
 *
 * @constructor Cria uma instância da classe [ToolsService].
*/
class ToolsService {
    val toolsConnection: MutableMap<String, ToolsServerConnection> = mutableMapOf()
    val llmConnection: LLMHostConnection
    val retry: Int
    constructor(llmConnection: LLMHostConnection,toolsConnection: List<ToolsServerConnection>, retry: Int = 5) {
        this.llmConnection = llmConnection
        toolsConnection.forEach { tools ->
            this.toolsConnection.put("${tools.clientName}/v${tools.clientVersion}", tools)
        }
        this.retry = retry
        this.toolsConnection.forEach { (id, toolsConnection) ->
            toolsConnection.connect()
        }
    }
    constructor(llmConnection: LLMHostConnection,toolsConnection: ToolsServerConnection, retry: Int = 5) {
        this.llmConnection = llmConnection
        this.toolsConnection.put("${toolsConnection.clientName}/v${toolsConnection.clientVersion}", toolsConnection)
        this.retry = retry
        // Inicia todos os servidores de tools.
        this.toolsConnection.forEach { (id, toolsConnection) ->
            toolsConnection.connect()
        }
    }

    private fun findToolsConnection(tollName: String): Pair<ToolsServerConnection?, Map<String, Any?>?> {
        var connection: ToolsServerConnection? = null
        var params: Map<String, Any?>? = null
        toolsConnection.forEach { (_, toolsConnection) ->
            toolsConnection.getToolDefinitions()?.forEach { tool ->
                if(tool.name == tollName) {
                    connection = toolsConnection
                    params = tool.inputSchema
                }
            }
        }
        return Pair(connection, params)
    }
    private fun compareParams(toolParams: Map<String, Any?>?, callToolParams: Map<String, JsonElement>): Pair<Boolean, List<String>> {
        val toolCallParamsKeys = listOf(callToolParams.keys).toString()
        val toolParamsKeys = listOf(toolParams?.get("required")).toString()
        val isEqual = toolCallParamsKeys == toolParamsKeys
        return Pair(isEqual, listOf(toolCallParamsKeys, toolParamsKeys))
    }
    private suspend fun callTool(toolCall: ToolCall.Function, prompt: PromptBuilder): Pair<PromptBuilder, Boolean> {
        val toolName = toolCall.function.name
        val toolCallId = toolCall.id
        val toolCallParams = Json.parseToJsonElement(toolCall.function.arguments).jsonObject.toMap()
        val (toolConnection, toolParams) = findToolsConnection(toolName)
        if (toolParams == null) {
            println("[ERROR] The tool < $toolName > did not return any params. ")
            return Pair(prompt, false)
        }
        val (isEqual, keysCompared) = compareParams(toolParams, toolCallParams)
        if (!isEqual) {
            println("[ERROR] Incorrect keys: expected ${keysCompared.first()}, got ${keysCompared.last()}")
            return Pair(prompt, false)
        }
        val toolCallRequestMessage = "[ Calling tool $toolName with args $toolCallParams ]"
        prompt.appendToolMessage(toolCallId, toolCallRequestMessage)
        val callToolResult = toolConnection?.callTool(toolName, toolCallParams)
        val response = ToolsServerConnection.extractTextForLLM(toolName, callToolResult)
        prompt.appendToolMessage(toolCallId, response)
        return Pair(prompt, true)
    }
    private fun addToolsToPrompt(prompt: PromptBuilder) {
        toolsConnection.forEach { (id, toolsConnection) ->
            toolsConnection.getToolDefinitions()?.forEach { tool ->
                println("From Client $id adding tool ${tool.name} with params ${tool.inputSchema}")
                prompt.addTool(tool)
            }
        }
    }
    suspend fun updateWithToolCall(prompt: PromptBuilder): PromptBuilder {
        addToolsToPrompt(prompt)
        var attempts = 0
        try {
            while (attempts < retry) {
                println("\n[INFO] Tentativa $attempts \n")
                var queryResponse = llmConnection.query(prompt, true).choices.first().message
                val toolCall = queryResponse.toolCalls?.firstOrNull() as? ToolCall.Function
                if (toolCall == null) {
                    println("\n[INFO] Não existe uma chamada de tools nesta tentativa.\n")
                    attempts++
                    continue
                }
                println("\n[INFO] Foi encontrada uma chamada de tool: ${toolCall.function.name}\n")
                val (updatedPrompt, isParamsCorrect) = callTool(toolCall, prompt)
                if (isParamsCorrect) {
                    return updatedPrompt
                } else {
                    println("\n[WARNING] Parâmetros incorretos na chamada da Tool. Reiniciando tentativas...\n")
                    attempts = 0 // Reinicia tentativas caso os parâmetros estejam errados
                }
            }
            println("\n[WARNING] Não foi possível identificar ou chamar a Tool corretamente após $retry tentativas.\n")
            return prompt
        } catch (e: Exception) {
            println("\n[ERROR] Um erro ocorreu durante o processamento: ${e.message}\n")
            throw e
        }
    }
    fun loadPrompts(): List<Prompt> {
        val availablePrompts = mutableListOf<Prompt>()
        toolsConnection.forEach { (_, toolsConnection) ->
            toolsConnection.prompts?.forEach { prompt ->
                availablePrompts.add(prompt)
            }
        }
        return availablePrompts.toList()
    }
    fun fetchResources(): List<Resource> {
        val resources = mutableListOf<Resource>()
        toolsConnection.forEach { (_, toolsConnection) ->
            toolsConnection.resources?.forEach { resource ->
                resources.add(resource)
            }
        }
        return resources.toList()
    }
}