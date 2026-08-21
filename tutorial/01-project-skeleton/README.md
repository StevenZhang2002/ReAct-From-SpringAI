# 第 01 节：项目骨架搭建

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 01 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [上一节](#) | [下一节 →](../02-core-models/README.md)

## 本节目标

从零搭建 Spring AI AgentX 的 Maven 多模块项目结构，建立基础包目录。

## 本节新增

- ✅ Maven 多模块项目结构（parent + core）
- ✅ 核心依赖配置（Spring AI、Reactor、FastJSON2）
- ✅ 基础包结构（agent、model、tools、utils、exception）

## 上节回顾

这是第一节，无上节内容。

## 架构设计

### 为什么选择 Maven 多模块？

```
spring-ai-agentx/
├── pom.xml                          # 父 POM（依赖管理）
├── spring-ai-agentx-core/           # 核心模块
│   ├── pom.xml
│   └── src/main/java/...
└── spring-ai-agentx-samples/        # 示例模块（后续添加）
    ├── pom.xml
    └── src/main/java/...
```

**优势：**
1. **模块化**：core 独立发布，samples 可选依赖
2. **依赖统一管理**：父 POM 集中管理 Spring AI 版本
3. **清晰边界**：核心逻辑 vs 示例代码分离

### 核心依赖选型

#### Spring AI 1.1.0

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

**为什么选 Spring AI？**
- 官方支持多模型（OpenAI、DeepSeek、智谱等）
- 统一的 ChatModel/ChatClient API
- 内置工具调用支持（ToolCallback）
- 流式响应支持（Flux）

#### Reactor Core

```xml
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
</dependency>
```

**为什么需要 Reactor？**
- Agent 流式输出基于 Flux
- 工具并发执行需要响应式编程
- Sink 事件发射机制

#### FastJSON2

```xml
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
</dependency>
```

**用途：**
- 工具参数 JSON 解析
- 结构化输出修复

## 代码实现

### 1. 父 POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>com.agentx.ai</groupId>
    <artifactId>spring-ai-agentx</artifactId>
    <version>1.0.1-M1</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>21</java.version>
        <spring-ai.version>1.1.0</spring-ai.version>
        <fastjson2.version>2.0.60</fastjson2.version>
    </properties>

    <modules>
        <module>spring-ai-agentx-core</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

**关键点：**
- `packaging=pom`：父项目不打包 jar
- `dependencyManagement`：统一管理 Spring AI 版本
- 继承 `spring-boot-starter-parent`：自动管理 Spring 依赖版本

### 2. Core 模块 POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.agentx.ai</groupId>
        <artifactId>spring-ai-agentx</artifactId>
        <version>1.0.1-M1</version>
    </parent>

    <artifactId>spring-ai-agentx-core</artifactId>
    <name>Spring AI AgentX Core</name>

    <dependencies>
        <!-- Spring AI ChatModel -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>

        <!-- Reactor Core -->
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
        </dependency>

        <!-- FastJSON2 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
    </dependencies>
</project>
```

**关键点：**
- 只引入最基础的依赖
- 后续章节按需添加（JDBC、VectorStore、Sandbox 等）

### 3. 包结构

```
com.agentx.ai.core/
├── agent/
│   ├── internal/          # 内部实现（不对外暴露）
│   │   ├── AgentLoopExecutor.java
│   │   ├── ToolCallExecutor.java
│   │   └── LoopMessageBuilder.java
│   └── ReactAgent.java    # 对外主入口
├── model/                 # 数据模型
│   ├── AgentResult.java
│   ├── AgentStreamEvent.java
│   └── RunnableParams.java
├── tools/                 # 工具相关
├── utils/                 # 工具类
└── exception/             # 异常定义
```

**设计原则：**
- `agent/internal`：内部实现细节，用户不应直接调用
- `agent/ReactAgent`：对外唯一入口，Builder 模式
- `model`：纯数据类，无业务逻辑

## 编译验证

```bash
mvn clean compile
```

预期输出：
```
[INFO] BUILD SUCCESS
```

## 本节小结

✅ 完成 Maven 多模块项目搭建
✅ 配置核心依赖（Spring AI、Reactor、FastJSON2）
✅ 建立基础包结构

## 下节预告

**第 02 节：核心模型定义**

将实现：
- `AgentResult`：Agent 执行结果（Completed/Failed）
- `AgentStreamEvent`：流式事件（Text/ToolStart/ToolEnd/Complete）
- `RunnableParams`：运行时参数
- `ThinkingMode`：思考模式枚举

这些模型是后续 Agent 实现的基础数据结构。
