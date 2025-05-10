import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost

class LLMHostConnection(val nameOrKey: String, val hostPath: String, val modelName: String) {
    var llmConnection: OpenAI
        private set
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

    suspend fun query(prompt: PromptBuilder, withTools: Boolean = false): ChatCompletion {
        val model = model ?: ""
        val prompt = prompt.build()
        val tools = if(withTools) prompt.component2() else null
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = prompt.component1(),
            tools = tools
        )
        val completion = llmConnection.chatCompletion(chatCompletionRequest)
        return completion
    }
}