interface LLMConnection<ParamsType, ReturnType> : AutoCloseable {
    suspend fun query(params: ParamsType): ReturnType
}