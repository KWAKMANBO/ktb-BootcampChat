# 다중 서버 로그 예시

## 서버 시작 시

각 서버가 시작될 때 다음과 같은 로그가 출력됩니다:

```log
🚀 [backend-1] Socket.IO server configured on 0.0.0.0:5002 with Redis store for multi-server support
🚀 [backend-2] Socket.IO server configured on 0.0.0.0:6002 with Redis store for multi-server support
🚀 [backend-3] Socket.IO server configured on 0.0.0.0:7002 with Redis store for multi-server support
```

## 웹소켓 연결 시

클라이언트가 연결할 때마다 **어떤 서버에 연결되었는지** 명확히 표시됩니다:

### Backend-1에 연결된 경우
```log
🔌 [backend-1] New Socket.IO connection attempt | SocketID: 8f3e4a2b-1234-5678-abcd-ef1234567890 | IP: /127.0.0.1:54321
✅ [backend-1] Socket.IO user connected: 홍길동 (user123) | SocketID: 8f3e4a2b-1234-5678-abcd-ef1234567890 | IP: /127.0.0.1:54321 | Concurrent users: 1
```

### Backend-2에 연결된 경우
```log
🔌 [backend-2] New Socket.IO connection attempt | SocketID: 9a4f5b3c-2345-6789-bcde-ef2345678901 | IP: /127.0.0.1:54322
✅ [backend-2] Socket.IO user connected: 김철수 (user456) | SocketID: 9a4f5b3c-2345-6789-bcde-ef2345678901 | IP: /127.0.0.1:54322 | Concurrent users: 2
```

### Backend-3에 연결된 경우
```log
🔌 [backend-3] New Socket.IO connection attempt | SocketID: ab5g6c4d-3456-7890-cdef-ef3456789012 | IP: /127.0.0.1:54323
✅ [backend-3] Socket.IO user connected: 이영희 (user789) | SocketID: ab5g6c4d-3456-7890-cdef-ef3456789012 | IP: /127.0.0.1:54323 | Concurrent users: 3
```

## 웹소켓 연결 해제 시

```log
❌ [backend-1] Socket.IO user disconnected: 홍길동 (user123) | SocketID: 8f3e4a2b-1234-5678-abcd-ef1234567890 | Concurrent users: 2
❌ [backend-2] Socket.IO user disconnected: 김철수 (user456) | SocketID: 9a4f5b3c-2345-6789-bcde-ef2345678901 | Concurrent users: 1
❌ [backend-3] Socket.IO user disconnected: 이영희 (user789) | SocketID: ab5g6c4d-3456-7890-cdef-ef3456789012 | Concurrent users: 0
```

## 로그에서 확인할 수 있는 정보

1. **서버 식별**: `[backend-1]`, `[backend-2]`, `[backend-3]`
2. **사용자 정보**: 이름과 사용자 ID
3. **Socket ID**: 각 연결의 고유 ID
4. **IP 주소**: 클라이언트의 IP
5. **동시 접속자 수**: 현재 연결된 총 사용자 수

## Docker Compose 로그 확인 방법

### 모든 백엔드 로그 확인
```bash
docker-compose logs -f backend-1 backend-2 backend-3
```

### 특정 서버만 확인
```bash
# Backend-1만
docker-compose logs -f backend-1

# Backend-2만
docker-compose logs -f backend-2

# Backend-3만
docker-compose logs -f backend-3
```

### 연결/해제 로그만 필터링
```bash
# 연결 로그만
docker-compose logs -f backend-1 backend-2 backend-3 | grep "Socket.IO user connected"

# 해제 로그만
docker-compose logs -f backend-1 backend-2 backend-3 | grep "Socket.IO user disconnected"

# 둘 다
docker-compose logs -f backend-1 backend-2 backend-3 | grep -E "connected|disconnected"
```

### 실시간 로그 모니터링 (색상 구분)
```bash
# 각 서버별로 터미널 분할하여 실시간 모니터링
# 터미널 1
docker-compose logs -f backend-1 | grep --color=always "backend-1"

# 터미널 2
docker-compose logs -f backend-2 | grep --color=always "backend-2"

# 터미널 3
docker-compose logs -f backend-3 | grep --color=always "backend-3"
```

## 다중 서버 동작 확인 예시

3명의 사용자가 각각 다른 서버에 연결된 경우:

```log
2025-12-14 10:23:15 ✅ [backend-1] Socket.IO user connected: Alice (user001) | Concurrent users: 1
2025-12-14 10:23:18 ✅ [backend-2] Socket.IO user connected: Bob (user002) | Concurrent users: 2
2025-12-14 10:23:21 ✅ [backend-3] Socket.IO user connected: Charlie (user003) | Concurrent users: 3
```

이 경우 세 사용자는 서로 다른 서버에 연결되어 있지만, **Redis Pub/Sub 덕분에** 서로 메시지를 주고받을 수 있습니다!

## 로그 레벨 조정

더 자세한 로그를 보고 싶다면 `application.properties`에서:

```properties
# 상세 로그
logging.level.com.ktb.chatapp=DEBUG
logging.level.com.corundumstudio.socketio=DEBUG

# 기본 로그
logging.level.com.ktb.chatapp=INFO
logging.level.com.corundumstudio.socketio=INFO
```
