import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class LLMEmbeddingConnectionTest {
    @Test
    fun `Deve conectar a um host local de llm e computar embeddings de um texto`() = runBlocking {
        val llmConnection = LLMEmbeddingConnection("ollama", "http://localhost:11434/v1/", "nomic-embed-text")
        val response = llmConnection.query(listOf("Eu gosto de estudar matemática e programação."))
        assert(response.embeddings.size == 1)
        val contextWindowSize = response.embeddings.last().embedding.size
        print(contextWindowSize == 768)
    }
}