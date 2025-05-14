import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LLMHostConnectionTest {
    @Test
    fun `Deve conectar a um host local de llm`() = runBlocking {
        val llmConnection = LLMHostConnection("ollama", "http://localhost:11434/v1/","llama3")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com o menos palavras possíveis e sem pontuações.")
            .appendUserMessage("Qual é a capital do Brasil?")
        val response = llmConnection.query(prompt, true).choices.first().message.content
        assertEquals(response, "Brasília")
    }
    @Test
    fun `Deve conectar a um llm host as a service`() = runBlocking {
        val key = System.getenv("ANTHROPIC_API_KEY") ?: ""
        val llmConnection = LLMHostConnection(key, "https://api.anthropic.com/v1/","claude-3-haiku-20240307")
        val prompt = PromptBuilder()
            .appendSystemMessage("Você responderá com o menos palavras possíveis e sem pontuações.")
            .appendUserMessage("Qual é a capital do Brasil?")
        // No modo de compatibilidade da anthropic eles não aceitam tools da openai
        val updatedPrompt = llmConnection.query(prompt)
        // TODO: Aqui precisa validar todo o retorno da LLM. Espera-se que para diferentes modelos ele esteja mais ou menos completo.
        val response = updatedPrompt.choices.first().message.content
        assertEquals(response, "Brasília")
    }
}