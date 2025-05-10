interface ToolDefinition {
    val name: String
    val description: String?
    val inputSchema: Map<String, Any>
}