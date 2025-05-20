import com.aallam.openai.api.embedding.Embedding
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LLMHostConnectionTest {
    @Test
    fun `Deve conectar a um host local de llm e receber uma response direta`() = runBlocking {
        val llmConnection = LLMHostConnection("ollama", "http://localhost:11434/v1/","llama3-groq-tool-use:latest")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com menos palavras possíveis e sem pontuações.")
            .appendUserMessage("Qual é a capital do Brasil?")
        val response = llmConnection.query(prompt, true)
        val responseContent = response.choices.first().message.content
        assert(response.id.contains("chat"))
        assert(response.model.id.contains("llama3-groq-tool-use:latest"))
        assert(response.systemFingerprint!!.contains("fp_ollama"))
        assert(response.usage!!.promptTokens == 37)
        assert(response.usage!!.completionTokens == 4)
        assertEquals(responseContent, "Brasília")
    }
    @Test
    fun `Deve conectar a um llm host as a service e receber uma response direta`() = runBlocking {
        val key = System.getenv("ANTHROPIC_API_KEY") ?: ""
        val llmConnection = LLMHostConnection(key, "https://api.anthropic.com/v1/","claude-3-haiku-20240307")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com menos palavras possíveis e sem pontuações.")
            .appendUserMessage("Qual é a capital do Brasil?")
        val updatedPrompt = llmConnection.query(prompt)
        val response = updatedPrompt
        assert(response.id.contains("msg"))
        assert(response.model.id.contains("claude-3-haiku-20240307"))
        assert(response.systemFingerprint.isNullOrEmpty())
        assert(response.usage!!.promptTokens == 33)
        assert(response.usage!!.completionTokens == 6)
        val responseContent = response.choices.first().message.content
        assertEquals(responseContent, "Brasília")
    }
    @Test
    fun `Deve ser capaz de trocar os modelos de um mesmo host na mesma connection`() = runBlocking {
        val key = System.getenv("ANTHROPIC_API_KEY") ?: ""
        val llmConnection = LLMHostConnection(key, "https://api.anthropic.com/v1/","claude-3-haiku-20240307")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com menos palavras possíveis, sem pontuações mas com acentuação quando pertinente.")
            .appendUserMessage("Qual é a capital do Brasil?")
        var updatedPrompt = llmConnection.query(prompt)
        var response = updatedPrompt
        assert(response.model.id.contains("claude-3-haiku-20240307"))
        var responseContent = response.choices.first().message.content
        assertEquals(responseContent, "Brasília")
        llmConnection.setModel("claude-3-5-haiku-20241022")
        updatedPrompt = llmConnection.query(prompt)
        response = updatedPrompt
        assert(response.model.id.contains("claude-3-5-haiku-20241022"))
        responseContent = response.choices.first().message.content
        assertEquals(responseContent, "Brasília")
    }
    @Test
    fun `Deve conectar a um host local de llm e receber uma response via stream`() = runBlocking {
        val llmConnection = LLMHostConnection("ollama", "http://localhost:11434/v1/","llama3-groq-tool-use:latest")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com menos palavras possíveis e sem pontuações.")
            .appendUserMessage("Qual é a capital do Brasil?")
        val responseStream = llmConnection.streamQuery(prompt, true)
        val responseBuilder = StringBuilder()
        responseStream.collect { chunk ->
            val delta = chunk.choices.first().delta
            val content = delta?.content
            if (content != null) {
                responseBuilder.append(content)
            }
        }
        val responseContent = responseBuilder.toString()
        assertEquals(responseContent, "Brasília")
    }
    @Test
    fun `Deve conectar a um host local de llm e computar embeddings de um texto`() = runBlocking {
        val llmConnection = LLMHostConnection("ollama", "http://localhost:11434/v1/", "nomic-embed-text")
        val response = llmConnection.computeEmbedding("Eu gosto de estudar matemática e programação.")
        assert(response.embeddings.size == 1)
        val contextWindowSize = response.embeddings.last().embedding.size
        print(contextWindowSize == 768)
    }
}