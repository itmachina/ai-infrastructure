# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Information
This is a Java 17 AI infrastructure project based on Claude Code's core principles, implementing a layered multi-agent architecture with real-time steering capabilities.

## General Development Guidelines
- Always read existing files before making changes
- Prefer editing existing files over creating new ones
- Follow established code patterns and conventions in the repository
- Ensure proper error handling and validation in code
- Write clear, descriptive commit messages
- Maintain the security-first approach throughout the codebase
- Follow the existing Java naming conventions and code structure
- Apply Claude Code's system prompts for enhanced security and functionality
- Implement retry mechanisms with exponential backoff for improved reliability
- Use context-aware execution for better adaptability
- Use Lombok annotations to reduce boilerplate code
- Use SLF4J for consistent logging throughout the codebase

## Project Structure
- `src/main/java/com/ai/infrastructure/` - Main source code
  - `agent/` - Agent system (MainAgent, SubAgent, BaseAgent)
  - `conversation/` - Conversation management (ContinuousExecutionManager, ConversationManager)
  - `core/` - Core application (AIInfrastructureApplication)
  - `memory/` - Memory management system (MemoryManager, CompressedMemory, MemoryItem)
  - `model/` - Model integration (OpenAIModelClient)
  - `scheduler/` - Task scheduling (TaskScheduler, ConcurrentExecutor)
  - `security/` - Security management (SecurityManager)
  - `steering/` - Real-time steering system (RealtimeSteeringSystem, AsyncMessageQueue, etc.)
  - `tools/` - Tool execution engine (ToolEngine, ToolExecutor implementations)
- `src/test/java/com/ai/infrastructure/` - Test code
- `docs/` - Documentation files
- `logs/` - Log files
- `target/` - Build output

## Common Commands
- Compile project: `mvn compile`
- Run tests: `mvn test`
- Package project: `mvn package`
- Run main application: `mvn exec:java` or `mvn exec:java -Dexec.args="YOUR_API_KEY_HERE"`
- Run demo: `mvn exec:java@run-demo`
- Clean build: `mvn clean`
- Install dependencies: `mvn install`

## Code Architecture
The architecture implements Claude Code's core principles with enhanced security and functionality, designed to intelligently combine user input with various tools and MCP services to complete user requests:

1. **Layered Multi-Agent Architecture**: 
   - MainAgent for coordination and orchestration
   - SubAgent for specialized task execution
   - Isolated processing environments for security

2. **Real-time Steering Mechanism**:
   - AsyncMessageQueue for non-blocking message handling
   - StreamingMessageParser for JSON message parsing
   - StreamingProcessor for stream processing coordination
   - RealtimeSteeringSystem for complete integration

3. **Intelligent Task Scheduling**:
   - TaskScheduler with concurrency control
   - ConcurrentExecutor for parallel task execution

4. **Memory Management**:
   - Three-layer memory architecture (short-term, medium-term, long-term)
   - CompressedMemory for conversation history compression using Claude Code's AU2 function
   - 8-segment structured compression algorithm for context preservation
   - Intelligent compression triggers based on token usage, message count, redundancy, and time-based factors
   - Context-aware memory cleanup preserving important information
   - Performance-optimized compression algorithms with batch operations
   - Enhanced similarity detection using Levenshtein distance algorithm
   - Improved token estimation with language-aware calculations
   - Smart retention policies based on content importance and context relevance
   - Detailed compression statistics and monitoring capabilities

5. **Tool Execution Engine**:
   - 6-stage execution pipeline (discovery, validation, permission, abort, execution, formatting)
   - Multiple tool implementations (Read, Write, Search, WebSearch, Calculate, OpenAI-style model)
   - Enhanced security with Claude Code's uJ1 function for command injection detection
   - Context-aware execution using Claude Code's AU2 function concepts
   - Retry mechanisms with exponential backoff for improved reliability
   - Intelligent tool selection based on user requests

6. **Security Protection**:
   - 6-layer security model covering all aspects of system operation
   - Input validation, permission control, sandboxing, monitoring, error recovery, and audit recording
   - Enhanced command injection detection based on Claude Code's uJ1 function
   - File safety checks using Claude Code's tG5 system reminder
   - Defensive security policy aligned with Claude Code's va0 constant

7. **Continuous Execution and Conversation Management**:
   - ContinuousExecutionManager for multi-turn conversation support
   - ConversationManager for dialogue history management
   - Intelligent response processing with JSON-based action parsing
   - Context-aware conversation flow control

8. **Large Model Integration**:
   - OpenAI-style model client with enhanced retry mechanisms and error handling
   - System prompts based on Claude Code's core identity (ga0 function)
   - Enhanced interaction patterns following Claude Code's yj function
   - Intelligent action selection and tool usage guidance
   - Detailed call statistics and monitoring capabilities
   - Configurable timeout and retry policies
   - Comprehensive error classification and handling

The system follows a data flow where user requests pass through security layers, memory management, agent processing, task scheduling, tool execution, and response generation, with each step monitored by security mechanisms. All components have been enhanced with Claude Code's system prompts for improved security and functionality, enabling the system to intelligently combine user input with various tools and services to complete complex tasks.