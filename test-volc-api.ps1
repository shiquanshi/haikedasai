# 测试火山引擎API响应速度
# 使用实际的prompt生成1张数学卡片

$startTime = Get-Date

$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer bd747896-e89b-46f4-a5ab-0a232d086845"
}

$body = @"
{
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
}
"@

Write-Host "🔗 正在调用火山引擎API..." -ForegroundColor Cyan
Write-Host "📝 生成1张数学卡片(难度:medium)" -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "https://ark.cn-beijing.volces.com/api/v3/chat/completions" `
        -Method Post `
        -Headers $headers `
        -Body $body `
        -ContentType "application/json"
    
    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalSeconds
    
    Write-Host "✅ API调用成功!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 响应内容:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
    Write-Host ""
    Write-Host "⏱️  总耗时: $([math]::Round($duration, 2))秒" -ForegroundColor Yellow
    
    if ($response.usage) {
        Write-Host "📈 Token统计:" -ForegroundColor Magenta
        Write-Host "   - 输入tokens: $($response.usage.prompt_tokens)"
        Write-Host "   - 输出tokens: $($response.usage.completion_tokens)"
        Write-Host "   - 总tokens: $($response.usage.total_tokens)"
        
        if ($response.usage.completion_tokens -gt 0) {
            $tokensPerSecond = [math]::Round($response.usage.completion_tokens / $duration, 2)
            Write-Host "   - 生成速度: $tokensPerSecond tokens/秒" -ForegroundColor Green
        }
    }
    
} catch {
    $endTime = Get-Date
    $duration = ($endTime - $startTime).TotalSeconds
    
    Write-Host "❌ API调用失败!" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "⏱️  失败前耗时: $([math]::Round($duration, 2))秒" -ForegroundColor Yellow
}