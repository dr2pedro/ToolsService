import com.aallam.openai.api.chat.FunctionTool
import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.chat.ToolType
import com.aallam.openai.api.core.Parameters
import com.anthropic.models.messages.ToolUnion
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class ToolAdapter {
    fun toolDefinitionToOpenAITool(toolDefinition: ToolDefinition): Tool {
        val params = buildToolParameters(toolDefinition.inputSchema)
        val functionTool = FunctionTool(
            name = toolDefinition.name,
            parameters = params,
            description = toolDefinition.description.toString()
        )
        return Tool(ToolType.Function, functionTool)
    }

    fun serverToolToToolDefinition(serverTool: ToolUnion): ToolDefinition {
        return object : ToolDefinition {
            override val name: String = serverTool.asTool().name()
            override val description = serverTool.asTool().description().toString()
            override val inputSchema: Map<String, Any> = mapOf(
                "type" to serverTool.asTool().inputSchema()._type(),
                "properties" to serverTool.asTool().inputSchema()._properties(),
                "required" to serverTool.asTool().inputSchema()._additionalProperties()["required"]!!
            )
        }
    }

    private fun buildToolParameters(inputSchema: Map<String, Any>): Parameters {
        return Parameters.buildJsonObject {
            put("type", "object")

            // Processa as propriedades
            val properties = inputSchema["properties"] as? Map<*, *>
            if (properties != null) {
                putJsonObject("properties") {
                    properties.forEach { (propertyName, propertyValue) ->
                        val property = propertyValue as? Map<*, *>
                        if (property != null) {
                            putJsonObject(propertyName.toString()) {
                                property.forEach { (key, value) ->
                                    when {
                                        value is List<*> && key.toString() == "enum" -> {
                                            putJsonArray(key.toString()) {
                                                value.forEach { enumValue ->
                                                    add(enumValue.toString())
                                                }
                                            }
                                        }
                                        else -> put(key.toString(), value.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Processa os campos obrigatórios
            @Suppress("UNCHECKED_CAST")
            val required = inputSchema["required"] as? List<String>
            if (required != null) {
                putJsonArray("required") {
                    required.forEach { add(it) }
                }
            }
        }
    }
}