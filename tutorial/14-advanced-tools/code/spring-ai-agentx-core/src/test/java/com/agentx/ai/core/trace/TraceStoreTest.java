package com.agentx.ai.core.trace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceStore 单元测试（第13节）。
 *
 * <p>测试构造、初始化、保存方法。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TraceStore 测试")
class TraceStoreTest {

    @Mock
    private DataSource dataSource;

    @Nested
    @DisplayName("构造测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 DataSource 创建 TraceStore")
        void shouldCreateWithDataSource() {
            // When
            TraceStore traceStore = new TraceStore(dataSource);

            // Then
            assertNotNull(traceStore);
        }
    }

    @Nested
    @DisplayName("initialize() 方法测试")
    class InitializeTests {

        @Test
        @DisplayName("初始化不抛异常")
        void shouldNotThrowOnInitialize() {
            // Given
            TraceStore traceStore = new TraceStore(dataSource);

            // When & Then
            assertDoesNotThrow(traceStore::initialize);
        }

        @Test
        @DisplayName("多次初始化不抛异常")
        void shouldNotThrowOnMultipleInitialize() {
            // Given
            TraceStore traceStore = new TraceStore(dataSource);

            // When & Then
            assertDoesNotThrow(() -> {
                traceStore.initialize();
                traceStore.initialize();
            });
        }
    }

    @Nested
    @DisplayName("save() 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存成功记录")
        void shouldSaveSuccessRecord() {
            // Given
            TraceStore traceStore = new TraceStore(dataSource);

            // When & Then - 由于没有真实数据库，这里只验证方法签名
            // 实际测试需要集成测试环境
            assertDoesNotThrow(() ->
                    traceStore.save(
                            12345L,
                            "conv-123",
                            1,
                            "用户输入",
                            "助手输出",
                            "思考内容",
                            100,
                            50,
                            1000L,
                            true,
                            null
                    ));
        }

        @Test
        @DisplayName("保存失败记录")
        void shouldSaveFailedRecord() {
            // Given
            TraceStore traceStore = new TraceStore(dataSource);

            // When & Then
            assertDoesNotThrow(() ->
                    traceStore.save(
                            12345L,
                            "conv-123",
                            1,
                            "用户输入",
                            null,
                            null,
                            100,
                            0,
                            500L,
                            false,
                            "执行失败"
                    ));
        }
    }
}
