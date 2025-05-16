import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LLMHostConnectionTest {
    @Test
    fun `Deve conectar a um host local de llm`() = runBlocking {
        val llmConnection = LLMHostConnection("ollama", "http://localhost:11434/v1/","llama3-groq-tool-use:latest")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com o menos palavras possíveis e sem pontuações.")
            .appendUserMessage("Qual é a capital do Brasil?")
        val response = llmConnection.query(prompt, true)
        val responseContent = response.choices.first().message.content
        assert(response.id.contains("chat"))
        assert(response.model.id.contains("llama3-groq-tool-use:latest"))
        assert(response.systemFingerprint!!.contains("fp_ollama"))
        assert(response.usage!!.promptTokens == 38)
        assert(response.usage!!.completionTokens == 4)
        assertEquals(responseContent, "Brasília")
    }
    @Test
    fun `Deve conectar a um llm host as a service`() = runBlocking {
        val key = System.getenv("ANTHROPIC_API_KEY") ?: ""
        val llmConnection = LLMHostConnection(key, "https://api.anthropic.com/v1/","claude-3-haiku-20240307")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com o menos palavras possíveis e sem pontuações.")
            .appendUserMessage("Qual é a capital do Brasil?")
        val updatedPrompt = llmConnection.query(prompt)
        val response = updatedPrompt
        assert(response.id.contains("msg"))
        assert(response.model.id.contains("claude-3-haiku-20240307"))
        assert(response.systemFingerprint.isNullOrEmpty())
        assert(response.usage!!.promptTokens == 34)
        assert(response.usage!!.completionTokens == 6)
        val responseContent = response.choices.first().message.content
        assertEquals(responseContent, "Brasília")
    }
}