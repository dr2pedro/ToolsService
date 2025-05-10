import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

sealed class Transport {
    abstract fun init(): AbstractTransport
    abstract fun close()
    private var _transport: AbstractTransport? = null
    val transport: AbstractTransport
        get() = _transport ?: init()
    protected fun setTransport(value: AbstractTransport) {
        if (_transport != null) {
            throw IllegalStateException("Transport already initialized")
        }
        _transport = value
    }
    class Stdio(path: String) : Transport() {
        var command: List<String>
            private set
        private var process: Process? = null

        init {
            command = buildList {
                when (path.substringAfterLast(".")) {
                    "js" -> add("node")
                    "py" -> add(
                        if (System.getProperty("os.name").lowercase().contains("win")) {
                            "python"
                        } else {
                            "python3"
                        }
                    )
                    "jar" -> addAll(listOf("java", "-jar"))
                    else -> add("docker run -i --rm")
                }
                add(path)
            }
        }

        override fun init(): AbstractTransport {
            if (process == null) {
                process = ProcessBuilder(command).start()
                val input = process!!.inputStream.asSource().buffered()
                val output = process!!.outputStream.asSink().buffered()
                setTransport(StdioClientTransport(input, output))
            }
            return transport
        }

        override fun close() = runBlocking {
            process?.destroy()
            transport.close()
        }
    }
    class Sse(
        val url: String,
        val client: HttpClient = HttpClient { install(SSE); install(Logging) }
    ) : Transport() {
        override fun init(): AbstractTransport {
            setTransport(SseClientTransport(client = client, urlString = url))
            return transport
        }
        override fun close() = runBlocking {
            transport.close()
            client.close()
        }
    }
}