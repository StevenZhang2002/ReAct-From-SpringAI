#!/bin/zsh
set -e

REPO_ROOT="/Users/bowen/Desktop/OpenSourceProject/spring-ai-agentx"
cd "$REPO_ROOT"

typeset -A NUM_TO_DIR
NUM_TO_DIR=(
    [01]="01-project-skeleton"  [02]="02-core-models"
    [03]="03-minimal-agent"     [04]="04-react-loop"
    [05]="05-streaming"         [06]="06-session-persistence"
    [07]="07-params-concurrency" [08]="08-thinking-mode"
    [09]="09-hitl"              [10]="10-hook-system"
    [11]="11-context-compression" [12]="12-long-term-memory"
    [13]="13-trace-audit"       [14]="14-advanced-tools"
)

BRANCHES=(
    "tutorial/01-project-skeleton"   "tutorial/02-core-models"
    "tutorial/03-minimal-agent"      "tutorial/04-react-loop"
    "tutorial/05-streaming"          "tutorial/06-session-persistence"
    "tutorial/07-params-concurrency" "tutorial/08-thinking-mode"
    "tutorial/09-hitl"               "tutorial/10-hook-system"
    "tutorial/11-context-compression" "tutorial/12-long-term-memory"
    "tutorial/13-trace-audit"        "tutorial/14-advanced-tools"
)

get_deps_level() {
    local n=$((10#$1))
    if [ $n -le 5 ]; then echo "basic"
    elif [ $n -le 10 ]; then echo "jdbc"
    elif [ $n -le 12 ]; then echo "vector"
    else echo "full"
    fi
}

write_root_pom() {
    cat > "$1/pom.xml" << 'ROOTEOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
    <name>Spring AI AgentX</name>
    <properties>
        <spring-ai.version>1.1.0</spring-ai.version>
        <java.version>21</java.version>
        <fastjson2.version>2.0.60</fastjson2.version>
        <mybatis-plus.version>3.5.12</mybatis-plus.version>
    </properties>
    <modules><module>spring-ai-agentx-core</module></modules>
    <dependencyManagement><dependencies>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-bom</artifactId><version>${spring-ai.version}</version><type>pom</type><scope>import</scope></dependency>
    </dependencies></dependencyManagement>
    <dependencies><dependency><groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId></dependency></dependencies>
    <build><pluginManagement><plugins><plugin>
        <groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId>
        <configuration><source>${java.version}</source><target>${java.version}</target><compilerArgs><arg>-parameters</arg></compilerArgs></configuration>
    </plugin></plugins></pluginManagement></build>
    <repositories><repository><id>spring-milestones</id><name>Spring Milestones</name><url>https://repo.spring.io/milestone</url><snapshots><enabled>false</enabled></snapshots></repository></repositories>
</project>
ROOTEOF
}

write_core_pom() {
    local dir="$1" level="$2" extra=""
    if [[ "$level" == "jdbc" || "$level" == "vector" || "$level" == "full" ]]; then
        extra='
        <dependency><groupId>org.springframework</groupId><artifactId>spring-jdbc</artifactId><optional>true</optional></dependency>
        <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId><version>${mybatis-plus.version}</version></dependency>'
    fi
    if [[ "$level" == "vector" || "$level" == "full" ]]; then
        extra="$extra"'
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-vector-store</artifactId><optional>true</optional></dependency>'
    fi
    cat > "$dir/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.agentx.ai</groupId><artifactId>spring-ai-agentx</artifactId><version>1.0.1-M1</version></parent>
    <artifactId>spring-ai-agentx-core</artifactId>
    <name>Spring AI AgentX Core</name>
    <dependencies>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-model-openai</artifactId></dependency>
        <dependency><groupId>io.projectreactor</groupId><artifactId>reactor-core</artifactId></dependency>
        <dependency><groupId>com.alibaba.fastjson2</groupId><artifactId>fastjson2</artifactId><version>\${fastjson2.version}</version></dependency>${extra}
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
</project>
EOF
}

# 从 git 分支读取文件列表
list_files_on_branch() {
    local branch="$1" path="$2"
    git ls-tree -r --name-only "$branch" 2>/dev/null | grep "^tutorial/${path}/code/" | grep "\.java$"
}

fix_branch() {
    local branch="$1"
    local num="${branch:9:2}"
    local current_dir="${NUM_TO_DIR[$num]}"

    echo "=========================================="
    echo "修复: $branch (第 ${num} 节)"
    echo "=========================================="

    git checkout "$branch" 2>&1 | tail -1

    if [ "$num" = "01" ]; then
        rm -rf "tutorial/01-project-skeleton/code"
        mkdir -p "tutorial/01-project-skeleton/code/spring-ai-agentx-core"
        write_root_pom "tutorial/01-project-skeleton/code"
        write_core_pom "tutorial/01-project-skeleton/code/spring-ai-agentx-core" "basic"
        git add -A
        git commit -m "fix: 重建第01节累积代码（项目骨架 + pom.xml）" 2>/dev/null || echo "  无变更"
        echo "  结果: pom.xml only"
        return
    fi

    local tmp_main=$(mktemp -d)
    local tmp_test=$(mktemp -d)

    # 收集 main 源码：从当前分支的 tutorial/02..N 目录
    for i in $(seq 2 $((10#$num))); do
        local nn=$(printf "%02d" $i)
        local tdir="${NUM_TO_DIR[$nn]}"
        local src="tutorial/$tdir/code/spring-ai-agentx-core"

        if [ -d "$src/src/main/java" ]; then
            (cd "$src/src/main/java" && find . -name "*.java" | tar cf - -T - | tar xf - -C "$tmp_main") 2>/dev/null || true
        fi
    done

    # 收集 test 源码：从各教程自己的分支读取
    for i in $(seq 2 $((10#$num))); do
        local nn=$(printf "%02d" $i)
        local tname="${NUM_TO_DIR[$nn]}"
        local test_branch="tutorial/${nn}-${tname}"

        # 用 git ls-tree 找该分支上的 test 文件
        local test_files=$(git ls-tree -r --name-only "$test_branch" 2>/dev/null | grep "^tutorial/${nn}-${tname}/code/.*/src/test/" | grep "\.java$")

        if [ -n "$test_files" ]; then
            echo "$test_files" | while read f; do
                local rel="${f#tutorial/${nn}-${tname}/code/spring-ai-agentx-core/src/test/java/}"
                local dest_dir="$tmp_test/$(dirname "$rel")"
                mkdir -p "$dest_dir"
                git show "$test_branch:$f" > "$dest_dir/$(basename "$rel")" 2>/dev/null || true
            done
        fi
    done

    # 清理并重建
    rm -rf "tutorial/$current_dir/code"
    local target="tutorial/$current_dir/code"
    mkdir -p "$target/spring-ai-agentx-core/src/main/java"
    mkdir -p "$target/spring-ai-agentx-core/src/test/java"

    local main_count=$(find "$tmp_main" -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
    local test_count=$(find "$tmp_test" -name "*.java" 2>/dev/null | wc -l | tr -d ' ')

    if [ "$main_count" -gt 0 ]; then
        (cd "$tmp_main" && find . -name "*.java" | tar cf - -T - | tar xf - -C "$target/spring-ai-agentx-core/src/main/java") 2>/dev/null || true
    fi
    if [ "$test_count" -gt 0 ]; then
        (cd "$tmp_test" && find . -name "*.java" | tar cf - -T - | tar xf - -C "$target/spring-ai-agentx-core/src/test/java") 2>/dev/null || true
    fi

    write_root_pom "$target"
    write_core_pom "$target/spring-ai-agentx-core" "$(get_deps_level $num)"

    rm -rf "$tmp_main" "$tmp_test"

    echo "  累积: main=$main_count, test=$test_count, deps=$(get_deps_level $num)"

    git add -A
    git commit -m "fix: 重建第${num}节累积代码（${main_count} 源文件 + ${test_count} 测试文件）" 2>/dev/null || echo "  无变更"
}

echo "开始修复教程分支..."
echo ""

for branch in "${BRANCHES[@]}"; do
    fix_branch "$branch"
    echo ""
done

echo "=========================================="
echo "所有分支修复完成！"
echo "=========================================="
