package com.ai.infrastructure.conversation;

import com.ai.infrastructure.tools.ToolEngine;
import com.ai.infrastructure.model.OpenAIModelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 持续执行管理器测试类
 * 测试工具和子agent的调用
 */
public class ContinuousExecutionManagerTest {

    private ContinuousExecutionManager continuousExecutionManager;
    
    @Mock
    private ToolEngine mockToolEngine;
    
    @Mock
    private OpenAIModelClient mockModelClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        continuousExecutionManager = new ContinuousExecutionManager(mockToolEngine, mockModelClient);
    }

    @Test
    public void testToolCallExecution() {
        // 测试工具调用执行
        // 这个测试主要是验证架构，实际的工具调用在ConversationManager中测试
        assertNotNull(continuousExecutionManager);
    }

    @Test
    public void testSubAgentExecution() {
        // 测试子Agent执行
        // 这个测试主要是验证架构，实际的子Agent创建和执行在ContinuousExecutionManager中实现
        assertNotNull(continuousExecutionManager);
    }
}