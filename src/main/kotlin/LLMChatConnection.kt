import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import kotlinx.coroutines.flow.Flow

/**
 * Conexão para se comunicar com um llm via chat.
 *
 * @property nameOrKey O nome ou chave da API.
 * @property hostPath O caminho do host.
 * @property modelName O nome do modelo.
 *
 * @constructor Cria uma instância da classe [LLMChatConnection].
*/
class LLMChatConnection(
    val nameOrKey: String,
    val hostPath: String,
    val modelName: String
) : LLMConnection<LLMChatConnection.QueryParams, ChatCompletion> {
    private var llmConnection: OpenAI
    var model: String? = null
        private set
    init {
        val token = nameOrKey
        // Não precisa de adaptador, os fornecedores fazem camada de compatibilidade com esse client.
        val host = OpenAIHost(hostPath)
        val config = OpenAIConfig(token = token, host = host)
        this.llmConnection = OpenAI(config)
        this.model = modelName
    }
    fun setModel(name: String) {
        this.model = name
    }
    private fun makeChatCompletionRequest(prompt: PromptBuilder, withTools: Boolean = false): ChatCompletionRequest {
        val model = model ?: ""
        val prompt = prompt.build()
        val tools = if(withTools) prompt.component2() else null
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = prompt.component1(),
            tools = tools
        )
        return chatCompletionRequest
    }
    override suspend fun query(params: QueryParams): ChatCompletion {
        val prompt = params.prompt
        val withTools = params.withTools
        val chatCompletionRequest = makeChatCompletionRequest(prompt, withTools)
        val completion = llmConnection.chatCompletion(chatCompletionRequest)
        return completion
    }
    fun streamQuery(prompt: PromptBuilder, withTools: Boolean = false): Flow<ChatCompletionChunk> {
        val chatCompletionRequest = makeChatCompletionRequest(prompt, withTools)
        val completion = llmConnection.chatCompletions(chatCompletionRequest)
        return completion
    }
    override fun close() {
        llmConnection.close()
    }

    data class QueryParams(
       val prompt: PromptBuilder,
       val withTools: Boolean = false
    )
}