#!/bin/bash
# 测试火山引擎API响应速度
# 使用实际的prompt生成1张数学卡片

curl -X POST "https://ark.cn-beijing.volces.com/api/v3/chat/completions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer bd747896-e89b-46f4-a5ab-0a232d086845" \
  -d '{
    "model": "ep-20251015101857-wc8xz",
    "messages": [
      {
        "role": "system",
        "content": "你是一位资深教育专家,擅长将复杂知识转化为简单易懂的学习卡片。"
      },
      {
        "role": "user",
        "content": "🎯 生成1张'数学'主题学习卡片(难度:medium)\n\n📋 输出JSON数组格式:\n[{\n  \"question\": \"问题内容(简洁明了)\",\n  \"answer\": \"答案解析(逻辑清晰)\"\n}]\n\n✨ 要求:\n1️⃣ 内容准确、有趣、实用\n2️⃣ 问题引发思考,答案逻辑清晰\n3️⃣ 严格JSON格式,无额外内容\n4️⃣ 中文输出\n\n🚀 开始生成:"
      }
    ],
    "stream": false
  }' \
  -w "\n\n⏱️  总耗时: %{time_total}秒\n⏱️  连接耗时: %{time_connect}秒\n⏱️  首字节耗时: %{time_starttransfer}秒\n"