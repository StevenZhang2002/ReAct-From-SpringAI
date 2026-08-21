#!/bin/bash
# 批量创建剩余教程分支 (06-15)
# 每个分支基于上一节创建

cd /Users/bowen/Desktop/OpenSourceProject/spring-ai-agentx

PREV="tutorial/05-streaming"

declare -A SECTIONS
SECTIONS[06]="session-persistence"
SECTIONS[07]="params-concurrency"
SECTIONS[08]="thinking-mode"
SECTIONS[09]="hitl"
SECTIONS[10]="hook-system"
SECTIONS[11]="context-compression"
SECTIONS[12]="long-term-memory"
SECTIONS[13]="trace-structured"
SECTIONS[14]="advanced-tools"
SECTIONS[15]="advanced-features"

for num in 06 07 08 09 10 11 12 13 14 15; do
    name=${SECTIONS[$num]}
    branch="tutorial/${num}-${name}"
    git checkout -b "$branch" "$PREV" 2>/dev/null || git checkout "$branch"
    mkdir -p "tutorial/${num}-${name}"
    PREV="$branch"
    echo "Created branch: $branch"
done

echo "All branches created!"
