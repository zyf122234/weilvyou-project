<template>
  <div class="page">
    <AppHeader />

    <main class="main">
      <section class="assistant-shell">
        <header class="assistant-header">
          <div class="assistant-title">
            <div class="assistant-icon">
              <el-icon><Service /></el-icon>
            </div>
            <div>
              <h1>智能客服</h1>
              <p>可咨询酒店推荐、订单查询、余额和账号相关问题。</p>
            </div>
          </div>

          <div class="assistant-actions">
            <span class="status-pill">
              <span class="status-dot"></span>
              在线
            </span>
          </div>
        </header>

        <div class="quick-strip">
          <button
            v-for="item in quickQuestions"
            :key="item"
            class="quick-chip"
            type="button"
            @click="sendQuickQuestion(item)"
          >
            <el-icon><ChatLineRound /></el-icon>
            <span>{{ item }}</span>
          </button>
        </div>

        <section class="chat-area">
          <div ref="messageBoxRef" class="message-list">
            <template v-for="message in messages" :key="message.id">
            <div
              v-if="shouldShowMessage(message)"
              class="message-row"
              :class="`is-${message.role}`"
            >
              <div class="message-avatar">
                <el-icon v-if="message.role === 'assistant'"><Service /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>

              <div class="message-main">
                <div class="message-meta">
                  <span>{{ message.role === 'assistant' ? '小微助手' : '我' }}</span>
                </div>
                <div class="message-bubble" :class="{ 'has-rich-content': hasRichContent(message) }">
                  <template
                    v-for="(segment, segmentIndex) in message.segments"
                    :key="`${message.id}-${segmentIndex}`"
                  >
                    <div v-if="segment.type === 'text'" class="message-text">
                      <p v-for="(paragraph, paragraphIndex) in segment.paragraphs" :key="paragraphIndex">
                        {{ paragraph }}
                      </p>
                    </div>

                    <div v-else-if="segment.type === 'orderTable'" class="order-result-list">
                      <article v-for="(order, orderIndex) in segment.orders" :key="orderIndex" class="order-result-card">
                        <div class="order-result-head">
                          <div class="order-title">
                            <strong>{{ order.hotelName || '酒店订单' }}</strong>
                            <span>订单号：{{ order.orderNo || '-' }}</span>
                          </div>
                          <span class="order-status-badge" :class="orderStatusClass(order.status)">
                            {{ order.status || '未知状态' }}
                          </span>
                        </div>

                        <div class="order-result-grid">
                          <div class="order-result-cell">
                            <span>入住时间</span>
                            <strong>{{ order.checkIn || '-' }}</strong>
                          </div>
                          <div class="order-result-cell">
                            <span>离店时间</span>
                            <strong>{{ order.checkOut || '-' }}</strong>
                          </div>
                          <div class="order-result-cell is-amount">
                            <span>订单金额</span>
                            <strong>{{ order.amount || '-' }}</strong>
                          </div>
                        </div>
                      </article>
                    </div>

                    <div v-else-if="segment.type === 'table'" class="generic-table-wrap">
                      <table class="generic-result-table">
                        <thead>
                          <tr>
                            <th v-for="header in segment.headers" :key="header">{{ header }}</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(row, rowIndex) in segment.rows" :key="rowIndex">
                            <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </template>
                </div>
              </div>
            </div>
            </template>

            <div v-if="loading && waitingForStream" class="message-row is-assistant">
              <div class="message-avatar">
                <el-icon><Service /></el-icon>
              </div>
              <div class="message-main">
                <div class="message-meta">
                  <span>小微助手</span>
                </div>
                <div class="message-bubble is-loading">
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <footer class="composer">
          <div class="composer-box">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              resize="none"
              maxlength="500"
              show-word-limit
              placeholder="输入问题，例如：帮我推荐上海300元左右的酒店"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <el-button
              class="send-button"
              type="primary"
              :icon="Promotion"
              :loading="loading"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </footer>
      </section>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatLineRound,
  Promotion,
  Service,
  User
} from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { chatWithAi, getCurrentAiConversation } from '@/api/ai'

const quickQuestions = [
  '帮我推荐上海的高性价比酒店',
  '查询我的订单',
  '我的账户余额是多少',
  '北京有哪些五星级酒店'
]

const inputMessage = ref('')
const loading = ref(false)
const waitingForStream = ref(false)
const sessionLoading = ref(false)
const messageBoxRef = ref(null)
const conversationId = ref(sessionStorage.getItem('aiConversationId') || '')
const messages = ref([
  createAssistantMessage('你好，我是小微。请直接输入你要咨询的内容。')
])

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function createAssistantMessage(content) {
  return {
    id: createMessageId(),
    role: 'assistant',
    content,
    segments: parseAssistantContent(content)
  }
}

function createUserMessage(content) {
  return {
    id: createMessageId(),
    role: 'user',
    content,
    segments: [createTextSegment(content)]
  }
}

function sendQuickQuestion(question) {
  inputMessage.value = question
  sendMessage()
}

onMounted(loadConversation)

async function loadConversation() {
  sessionLoading.value = true
  try {
    const res = await getCurrentAiConversation()
    const data = res.data || res || {}
    conversationId.value = data.conversationId || ''
    if (conversationId.value) {
      sessionStorage.setItem('aiConversationId', conversationId.value)
    }

    const history = Array.isArray(data.messages) ? data.messages : []
    messages.value = history.length
      ? history.map(createHistoryMessage)
      : [createAssistantMessage('你好，我是小微。请直接输入你要咨询的内容。')]
    scrollToBottom()
  } catch (error) {
    ElMessage.error(error.message || '会话加载失败')
  } finally {
    sessionLoading.value = false
  }
}

function createHistoryMessage(message) {
  const role = String(message.role || '').toUpperCase() === 'USER' ? 'user' : 'assistant'
  const content = message.content || ''
  return role === 'user' ? createUserMessage(content) : createAssistantMessage(content)
}

async function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || loading.value || sessionLoading.value) return

  if (!conversationId.value) {
    await loadConversation()
  }

  if (!conversationId.value) {
    ElMessage.error('会话创建失败')
    return
  }

  messages.value.push(createUserMessage(content))
  inputMessage.value = ''
  loading.value = true
  waitingForStream.value = true
  scrollToBottom()

  const assistantMessage = createAssistantMessage('')
  messages.value.push(assistantMessage)

  try {
    await chatWithAi(
      {
        message: content,
        conversationId: conversationId.value
      },
      (_, fullText) => {
        waitingForStream.value = false
        assistantMessage.content = fullText
        assistantMessage.segments = parseAssistantContent(fullText)
        scrollToBottom()
      }
    )
    if (!assistantMessage.content) {
      assistantMessage.content = '暂未获取到有效回复。'
      assistantMessage.segments = parseAssistantContent(assistantMessage.content)
    }
  } catch (error) {
    assistantMessage.content = error.message || 'AI 服务暂时不可用，请稍后再试。'
    assistantMessage.segments = parseAssistantContent(assistantMessage.content)
    ElMessage.error(error.message || 'AI 服务暂时不可用')
  } finally {
    loading.value = false
    waitingForStream.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messageBoxRef.value) {
      messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
    }
  })
}

function hasRichContent(message) {
  return message.segments?.some((segment) => segment.type !== 'text')
}

function shouldShowMessage(message) {
  return message.role !== 'assistant' || Boolean(message.content)
}

function parseAssistantContent(content) {
  const source = String(content || '').replace(/\r\n/g, '\n').trim()
  if (!source) {
    return [createTextSegment('')]
  }

  const lines = source.split('\n')
  const segments = []
  const textBuffer = []

  const flushText = () => {
    const text = textBuffer.join('\n').trim()
    textBuffer.length = 0
    if (text) {
      segments.push(createTextSegment(text))
    }
  }

  for (let index = 0; index < lines.length;) {
    if (isMarkdownTableStart(lines, index)) {
      const tableLines = []
      while (index < lines.length && isMarkdownTableLine(lines[index])) {
        tableLines.push(lines[index])
        index += 1
      }

      const table = parseMarkdownTable(tableLines)
      if (table) {
        flushText()
        segments.push(createTableSegment(table))
        continue
      }

      textBuffer.push(...tableLines)
      continue
    }

    textBuffer.push(lines[index])
    index += 1
  }

  flushText()
  return segments.length ? segments : [createTextSegment(source)]
}

function createTextSegment(text) {
  return {
    type: 'text',
    paragraphs: String(text || '')
      .split(/\n{2,}/)
      .map((paragraph) => paragraph
        .split('\n')
        .map((line) => cleanMarkdown(line).replace(/^[-*]\s+/, '• '))
        .join('\n')
        .trim())
      .filter(Boolean)
  }
}

function isMarkdownTableStart(lines, index) {
  return isMarkdownTableLine(lines[index]) && isMarkdownSeparatorLine(lines[index + 1])
}

function isMarkdownTableLine(line = '') {
  return /^\s*\|.*\|\s*$/.test(line)
}

function isMarkdownSeparatorLine(line = '') {
  return /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line)
}

function parseMarkdownTable(lines) {
  if (lines.length < 3) return null

  const headers = splitTableRow(lines[0])
  const rows = lines.slice(2)
    .filter(isMarkdownTableLine)
    .map(splitTableRow)
    .filter((row) => row.some(Boolean))

  if (!headers.length || !rows.length) return null
  return { headers, rows }
}

function splitTableRow(line) {
  return line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((cell) => cleanMarkdown(cell))
}

function createTableSegment(table) {
  const isOrderTable = table.headers.some((header) => ['订单编号', '订单号', '酒店名称', '入住时间', '离店时间', '金额', '状态'].includes(header))

  if (isOrderTable) {
    return {
      type: 'orderTable',
      orders: table.rows.map((row) => normalizeOrderRow(table.headers, row))
    }
  }

  return {
    type: 'table',
    headers: table.headers,
    rows: table.rows
  }
}

function normalizeOrderRow(headers, row) {
  const record = {}
  headers.forEach((header, index) => {
    record[header] = row[index] || ''
  })

  const pick = (...keys) => keys.map((key) => record[key]).find(Boolean) || ''

  return {
    orderNo: pick('订单编号', '订单号'),
    hotelName: pick('酒店名称', '商品名称', '酒店'),
    checkIn: pick('入住时间', '入住日期'),
    checkOut: pick('离店时间', '离店日期'),
    amount: pick('金额', '订单金额', '总金额'),
    status: normalizeStatus(pick('状态', '订单状态'))
  }
}

function cleanMarkdown(value) {
  return String(value || '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/<br\s*\/?>/gi, ' ')
    .trim()
}

function normalizeStatus(status) {
  const cleaned = cleanMarkdown(status).replace(/[✅✔☑❌✖⏳]/g, '').trim()
  return cleaned || cleanMarkdown(status)
}

function orderStatusClass(status) {
  const text = normalizeStatus(status)
  if (/已支付|已完成|支付成功/.test(text)) return 'is-paid'
  if (/待支付|未支付|待付款/.test(text)) return 'is-pending'
  if (/取消|已关闭|退款/.test(text)) return 'is-canceled'
  return 'is-default'
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--bg-body);
  color: var(--text-primary);
}

.main {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 28px 72px;
}

.assistant-shell {
  min-height: calc(100vh - 124px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.assistant-header {
  min-height: 82px;
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid var(--border-light);
}

.assistant-title {
  min-width: 0;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.assistant-icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #ffffff;
  font-size: 20px;
}

.assistant-title h1 {
  margin: 0 0 5px;
  font-size: 22px;
  line-height: 1.25;
}

.assistant-title p {
  color: var(--text-muted);
  font-size: 13px;
}

.assistant-actions {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.status-pill {
  height: 32px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--success);
}

.quick-strip {
  padding: 12px 22px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-subtle);
}

.quick-chip {
  height: 34px;
  padding: 0 12px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: #ffffff;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  transition: border-color var(--transition-fast), color var(--transition-fast), background var(--transition-fast);
}

.quick-chip:hover {
  border-color: rgba(37, 99, 235, 0.34);
  background: rgba(37, 99, 235, 0.06);
  color: var(--primary);
}

.chat-area {
  min-height: 0;
  background: #ffffff;
}

.message-list {
  height: 100%;
  min-height: 0;
  padding: 28px;
  overflow-y: auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-row + .message-row {
  margin-top: 18px;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-light);
  border-radius: 50%;
  background: #ffffff;
  color: var(--primary);
}

.message-row.is-user .message-avatar {
  border-color: var(--primary);
  background: var(--primary);
  color: #ffffff;
}

.message-main {
  max-width: min(720px, 74%);
}

.message-row.is-user .message-main {
  display: grid;
  justify-items: end;
}

.message-meta {
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.message-bubble {
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-subtle);
  color: var(--text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-bubble.has-rich-content {
  width: min(760px, 100%);
  padding: 0;
  border: 0;
  background: transparent;
  white-space: normal;
}

.message-row.is-user .message-bubble {
  border-color: rgba(37, 99, 235, 0.22);
  background: rgba(37, 99, 235, 0.08);
}

.message-text {
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-subtle);
  white-space: pre-wrap;
}

.message-text p {
  margin: 0;
}

.message-text p + p {
  margin-top: 8px;
}

.message-text + .order-result-list,
.order-result-list + .message-text,
.message-text + .generic-table-wrap,
.generic-table-wrap + .message-text {
  margin-top: 12px;
}

.order-result-list {
  display: grid;
  gap: 10px;
}

.order-result-card {
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.order-result-head {
  padding: 12px 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-subtle);
}

.order-title {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.order-title strong {
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.35;
}

.order-title span {
  color: var(--text-muted);
  font-size: 12px;
}

.order-status-badge {
  height: 26px;
  padding: 0 10px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: #ffffff;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.order-status-badge.is-paid {
  border-color: #a7f3d0;
  background: #ecfdf5;
  color: #047857;
}

.order-status-badge.is-pending {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.order-status-badge.is-canceled {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.order-result-grid {
  padding: 12px 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.order-result-cell {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.order-result-cell span {
  color: var(--text-muted);
  font-size: 12px;
}

.order-result-cell strong {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.35;
  word-break: break-word;
}

.order-result-cell.is-amount strong {
  color: var(--primary);
  font-size: 16px;
}

.generic-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
}

.generic-result-table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
  font-size: 13px;
}

.generic-result-table th,
.generic-result-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-light);
  text-align: left;
  vertical-align: top;
}

.generic-result-table th {
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-weight: 700;
}

.generic-result-table tr:last-child td {
  border-bottom: 0;
}

.message-bubble.is-loading {
  min-width: 70px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: dotPulse 1.2s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.32s;
}

.composer {
  padding: 14px 18px 18px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-subtle);
}

.composer-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  align-items: end;
  gap: 12px;
}

.composer-box :deep(.el-textarea__inner) {
  min-height: 86px !important;
  background: #ffffff;
}

.send-button {
  height: 40px;
}

@keyframes dotPulse {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

@media (max-width: 820px) {
  .main {
    padding: 18px 16px 48px;
  }

  .assistant-shell {
    min-height: calc(100vh - 104px);
  }

  .assistant-header {
    align-items: stretch;
    flex-direction: column;
  }

  .assistant-actions {
    justify-content: space-between;
  }

  .message-list {
    padding: 18px;
  }

  .message-main {
    max-width: 84%;
  }

  .message-bubble.has-rich-content {
    width: 100%;
  }

  .order-result-head {
    align-items: stretch;
    flex-direction: column;
  }

  .order-status-badge {
    width: fit-content;
  }

  .order-result-grid {
    grid-template-columns: 1fr;
  }

  .composer-box {
    grid-template-columns: 1fr;
  }
}
</style>
