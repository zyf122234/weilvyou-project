import request from '@/utils/request'

export function getCurrentAiConversation() {
  return request.get('/ai/customer-service/session/current')
}

export function invalidateCurrentAiConversation() {
  return request.post('/ai/customer-service/session/invalidate-current')
}

export async function chatWithAi(data, onChunk) {
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream, application/json'
  }
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch('/api/ai/customer-service/chat', {
    method: 'POST',
    headers,
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }

  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const isSse = response.headers.get('content-type')?.includes('text/event-stream')
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let fullText = ''

  const appendContent = (content) => {
    if (!content) return
    fullText += content
    onChunk?.(content, fullText)
  }

  const flushSseEvents = (finished = false) => {
    buffer = buffer.replace(/\r\n/g, '\n')

    let eventEndIndex = buffer.indexOf('\n\n')
    while (eventEndIndex !== -1) {
      const eventBlock = buffer.slice(0, eventEndIndex)
      buffer = buffer.slice(eventEndIndex + 2)
      appendContent(parseSseEvent(eventBlock))
      eventEndIndex = buffer.indexOf('\n\n')
    }

    if (finished && buffer.trim()) {
      appendContent(parseSseEvent(buffer))
      buffer = ''
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, { stream: true })
    if (isSse) {
      buffer += chunk
      flushSseEvents()
    } else {
      appendContent(chunk)
    }
  }

  const tail = decoder.decode()
  if (tail) {
    if (isSse) {
      buffer += tail
      flushSseEvents(true)
    } else {
      appendContent(tail)
    }
  } else if (isSse) {
    flushSseEvents(true)
  }

  return { data: { answer: fullText } }
}

async function parseErrorMessage(response) {
  const contentType = response.headers.get('content-type') || ''
  const fallback = 'AI 服务调用失败'

  if (contentType.includes('application/json')) {
    const data = await response.json().catch(() => null)
    return data?.message || fallback
  }

  const errorText = await response.text().catch(() => '')
  return errorText || fallback
}

function parseSseEvent(eventBlock) {
  const lines = String(eventBlock || '').split('\n')
  const dataLines = []

  for (const line of lines) {
    if (!line || line.startsWith(':')) continue
    if (!line.startsWith('data:')) continue

    let data = line.slice(5)
    if (data.startsWith(' ')) {
      data = data.slice(1)
    }
    dataLines.push(data)
  }

  const content = dataLines.join('\n')
  return content === '[DONE]' ? '' : content
}
