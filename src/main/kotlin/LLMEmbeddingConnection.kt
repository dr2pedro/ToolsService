import com.aallam.openai.api.embedding.EmbeddingRequest
import com.aallam.openai.api.embedding.EmbeddingResponse
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost

// Essa classe não precisa estar aqui. Ela deve estar em uma biblioteca separada.
// Só quem chama as tools (que é o objetivo desse serviço) é a conexão por chat, o restante
// faz mais sentido estar em um serviço de Agents.
class LLMEmbeddingConnection(nameOrKey: String, hostPath: String, modelName: String) : LLMConnection<List<String>, EmbeddingResponse> {
    private var llmConnection: OpenAI
    var model: String? = null
        private set
    init {
        val token = nameOrKey
        val host = OpenAIHost(hostPath)
        val config = OpenAIConfig(token = token, host = host)
        this.llmConnection = OpenAI(config)
        this.model = modelName
    }
    override suspend fun query(params: List<String>): EmbeddingResponse {
        val model = model ?: "nomic-embed-text"
        val request = EmbeddingRequest(model = ModelId(model), input = params)
        val result = llmConnection.embeddings(request)
        return result
    }
    override fun close() {
        llmConnection.close()
    }
}