#ToolsService

## Visão Geral
O `ToolsService` é uma implementação projetada para lidar com a comunicação entre modelos de linguagens (LLM) e servidores de ferramentas. Ele utiliza conexões com servidores para consultar ferramentas, prompts e recursos disponíveis, além de gerenciar chamadas de ferramentas com base em entradas do usuário.

## Recursos Principais
- Integração com servidores diversos via diferentes transportes (`Stdio`, `SSE`).
- Configuração dinâmica de conexões entre servidores de ferramentas e LLMs.
- Validação de parâmetros de ferramentas antes de executá-las.
- Suporte à execução de prompts configurados para o ambiente de ferramentas.
- Carregamento e consulta de recursos e prompts do servidor.

---

## Exemplo de Uso - `Main.kt`

Abaixo segue um exemplo funcional mostrando como inicializar o `ToolsService` e utilizar seus métodos principais para chamar ferramentas ou consultar prompts e recursos.

```kotlin
// Configuração da conexão com o LLM
    val llmConnection = LLMHostConnection(
        nameOrKey = "ollama",
        hostPath = "http://localhost:11434/v1/",
        modelName = "llama3-groq-tool-use"
    )

    // Inicializando o ToolsService com múltiplas conexões
    val toolsService = ToolsService(llmConnection, listOf(stdioConnection, sseConnection))

    // Construindo e utilizando um prompt
    val promptBuilder = PromptBuilder()
        .appendSystemMessage("You ALWAYS search for tools to get the results.")
        .appendUserMessage("Add 2 to 3 using available tools.")

    // Atualizando o prompt com chamadas de ferramentas
    val updatedPrompt = toolsService.updateWithToolCall(promptBuilder)

    // Realizando uma consulta ao LLM com o prompt atualizado
    val response = llmConnection.query(updatedPrompt, false).choices.firstOrNull()?.message?.content

    // Exibindo a resposta
    println("Resposta: $response")

    // Consultando prompts e recursos disponíveis
    val prompts = toolsService.loadPrompts()
    val resources = toolsService.fetchResources()

    println("Prompts disponíveis: ${prompts.map { it.description }}")
    println("Recursos disponíveis: ${resources.map { it.name }}")
```

---

## Aplicações

O `ToolsService` é altamente aplicável para cenários em que modelos de aprendizado de máquina necessitam:
1. **Integração com APIs externas**: como usar ferramentas hospedadas em servidores para buscar, processar ou transformar dados.
2. **Execução dinâmica de tarefas**: usando ferramentas dinâmicas para auxiliar no desenvolvimento de programas, processamento de linguagem natural, ou até mesmo cálculos matemáticos complexos.
3. **Automatização de processos interativos**: fornece compatibilidade entre ferramentas e prompts complexos, permitindo pipelines com interações automáticas.

---

## Limitações
- **Complexidade**: A estrutura atual pode ser complexa para usuários iniciantes em Kotlin devido ao uso intensivo de corrotinas e integrações de servidores.
- **Conexão aos servidores**: Se os servidores de ferramentas estiverem indisponíveis ou mal configurados, o serviço não ficará funcional.
- **Dependências externas**: O funcionamento depende de bibliotecas como `kotlinx.serialization`, `ktor` e integrações com LLMs, o que pode gerar dependências difíceis de gerenciar.
- **Validação de Parâmetros**: A validação atual dos parâmetros das ferramentas pode ser rígida, resultando em falhas frequentes quando os schemas não correspondem perfeitamente.

---

## Pontos de Melhoria Futura
1. **Flexibilidade na Validação de Parâmetros**: Implementar validações menos rigorosas ou possibilidade de configuração customizada para lidar com situações onde os parâmetros não se alinham perfeitamente.
2. **Documentação e Exemplos**: Melhorar os exemplos e incluir documentação detalhada para desenvolvedores iniciantes.
3. **Relatório de Logs**: Adicionar relatórios estruturados e configuráveis para melhor rastrear erros e manipular eventos do servidor.
4. **Cache de Ferramentas, Prompts e Recursos**: Implementar níveis de cache local para evitar múltiplas chamadas ao servidor e melhorar a performance.
5. **Testes Automatizados**: Embora existam testes básicos, expandi-los para cenários mais complexos com mocks seria ideal.
6. **Interface Gráfica (GUI)**: Criar uma interface de usuário para configurar conexões, consultar ferramentas disponíveis e visualizar prompts e recursos.
7. **Suporte a Configurações Dinâmicas**: Permitir que transportes e conexões sejam configurados em tempo de execução.

---

## Estrutura do Projeto

### Arquivos principais
1. **`ToolsService.kt`**: O núcleo do serviço, gerencia conexões com servidores e interações com ferramentas.
2. **`LLMHostConnection.kt`**: Gerencia a conexão com o LLM e realiza consultas.
3. **`PromptBuilder.kt`**: Cria e organiza mensagens baseadas no estado dos prompts.
4. **`ToolsServerConnection.kt`**: Abstração para conexão com servidores de ferramentas e seus recursos.

### Testes de unidade
Os testes fornecidos em `ToolsServiceTest` cobrem:
- Configuração de ferramentas com múltiplos transportes (Stdio & SSE).
- Chamadas de ferramentas em servidores.
- Validação de parâmetros e respostas.
- Consulta de prompts e recursos disponíveis.

Execute os testes para entender melhor os comportamentos esperados e validar a implementação antes do uso em produção.

---

Sinta-se à vontade para contribuir ou ampliar as funcionalidades conforme necessário!
