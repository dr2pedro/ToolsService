import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.SystemMessageBuilder
import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.chat.ToolId
import com.aallam.openai.api.chat.ToolMessageBuilder
import com.aallam.openai.api.chat.UserMessageBuilder

/**
 * Builder para criar prompts para a API de chat.
 *
 * @constructor Cria uma instância da classe [PromptBuilder].
 */
class PromptBuilder {
    var messages: MutableList<ChatMessage> = mutableListOf()
        private set
    var toolsDefinitions: MutableList<ToolDefinition> = mutableListOf()
        private set
    private val toolAdapter = ToolAdapter()
    fun addTool(toolDefinition: ToolDefinition): PromptBuilder {
        toolsDefinitions.add(toolDefinition)
        return this
    }
    fun appendUserMessage(content: String): PromptBuilder {
        val userMsg = UserMessageBuilder()
        userMsg.content = content
        messages.add(userMsg.build())
        return this
    }
    fun appendSystemMessage(content: String): PromptBuilder {
        val systemMsg = SystemMessageBuilder()
        systemMsg.content = content
        messages.add(systemMsg.build())
        return this
    }
    fun appendToolMessage(toolId: ToolId, content: String? = null): PromptBuilder {
        val toolMsg = ToolMessageBuilder()
        toolMsg.content = content
        toolMsg.toolCallId = toolId
        messages.add(toolMsg.build())
        return this
    }
    fun build(): Pair<List<ChatMessage>, List<Tool>> {
        val resultedMessages = messages.toList()
        val clientTools = toolsDefinitions.map { toolAdapter.toolDefinitionToOpenAITool(it)  }
        return Pair(resultedMessages, clientTools)
    }
}