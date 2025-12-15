# 🚀 다중 서버 테스트 가이드

## 📋 수정된 내용

docker-compose.yaml 파일이 다음과 같이 수정되었습니다:

### 추가된 서비스
- **backend-1**: 포트 5001 (HTTP), 5002 (WebSocket)
- **backend-2**: 포트 6001 (HTTP), 6002 (WebSocket)
- **backend-3**: 포트 7001 (HTTP), 7002 (WebSocket)
- **nginx**: 포트 8080 (HTTP LB), 8081 (WebSocket LB)

### 개선된 기능
- Redis에 비밀번호 인증 추가 (`requirepass ktb`)
- MongoDB와 Redis에 healthcheck 추가
- 모든 백엔드가 Redis를 통한 Pub/Sub 사용

## 🏃 실행 방법

### 1. 전체 환경 시작

```bash
# 이미지 빌드 및 모든 서비스 시작
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build
```

### 2. 서비스 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker-compose ps

# 로그 확인
docker-compose logs -f backend-1 backend-2 backend-3
```

## 🧪 테스트 시나리오

### 테스트 1: 직접 연결

1. **터미널 1** - 서버 1에 연결
```bash
curl http://localhost:5002/socket.io/?EIO=4&transport=polling
```

2. **터미널 2** - 서버 2에 연결
```bash
curl http://localhost:6002/socket.io/?EIO=4&transport=polling
```

3. **터미널 3** - 서버 3에 연결
```bash
curl http://localhost:7002/socket.io/?EIO=4&transport=polling
```

### 테스트 2: 로드 밸런서를 통한 연결

```bash
# Nginx를 통해 자동 분산
curl http://localhost:8081/socket.io/?EIO=4&transport=polling
```

### 테스트 3: Redis Pub/Sub 확인

```bash
# Redis 모니터링
docker-compose exec redis redis-cli -a ktb
> PSUBSCRIBE *
```

다른 터미널에서 메시지를 보내면 Redis를 통해 전파되는 것을 확인할 수 있습니다.

### 테스트 4: 서버 다운 시뮬레이션

```bash
# 한 서버 중지
docker-compose stop backend-2

# 다른 서버들이 계속 작동하는지 확인
curl http://localhost:5002/actuator/health
curl http://localhost:7002/actuator/health

# 서버 재시작
docker-compose start backend-2
```

## 📊 서비스 포트 구성

| 서비스 | HTTP | WebSocket | 설명 |
|--------|------|-----------|------|
| Backend-1 | 5001 | 5002 | 첫 번째 인스턴스 |
| Backend-2 | 6001 | 6002 | 두 번째 인스턴스 |
| Backend-3 | 7001 | 7002 | 세 번째 인스턴스 |
| Nginx | 8080 | 8081 | 로드 밸런서 |
| Redis | - | 6379 | 공유 저장소 |
| MongoDB | - | 27017 | 데이터베이스 |
| Prometheus | - | 9090 | 메트릭 수집 |
| Grafana | - | 9091 | 대시보드 |

## 🎯 브라우저에서 테스트

간단한 HTML 클라이언트를 만들어서 테스트할 수 있습니다:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Multi-Server Test</title>
    <script src="https://cdn.socket.io/4.7.2/socket.io.min.js"></script>
</head>
<body>
    <h1>Socket.IO Multi-Server Test</h1>

    <h2>Client 1 → Backend-1 (5002)</h2>
    <div id="client1"></div>

    <h2>Client 2 → Backend-2 (6002)</h2>
    <div id="client2"></div>

    <h2>Client 3 → Backend-3 (7002)</h2>
    <div id="client3"></div>

    <script>
        // 세 개의 다른 서버에 연결
        const socket1 = io('http://localhost:5002');
        const socket2 = io('http://localhost:6002');
        const socket3 = io('http://localhost:7002');

        socket1.on('connect', () => {
            document.getElementById('client1').innerHTML = '✅ Connected';
        });

        socket2.on('connect', () => {
            document.getElementById('client2').innerHTML = '✅ Connected';
        });

        socket3.on('connect', () => {
            document.getElementById('client3').innerHTML = '✅ Connected';
        });

        // 메시지 수신 테스트
        socket1.on('message', (data) => console.log('Client 1 received:', data));
        socket2.on('message', (data) => console.log('Client 2 received:', data));
        socket3.on('message', (data) => console.log('Client 3 received:', data));
    </script>
</body>
</html>
```

## 🔍 디버깅

### Redis 연결 확인
```bash
docker-compose exec redis redis-cli -a ktb ping
# 응답: PONG
```

### MongoDB 연결 확인
```bash
docker-compose exec mongo mongosh bootcamp-chat --eval "db.runCommand({ ping: 1 })"
```

### 백엔드 로그 확인
```bash
# 모든 백엔드 로그
docker-compose logs -f backend-1 backend-2 backend-3

# 특정 백엔드만
docker-compose logs -f backend-1
```

### Prometheus 타겟 확인
```bash
# 브라우저에서
open http://localhost:9090/targets
```

## 🛑 종료

```bash
# 서비스 중지
docker-compose down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose down -v
```

## ✅ 성공 기준

다음을 확인하면 다중 서버가 정상 작동하는 것입니다:

1. ✅ 3개의 백엔드 인스턴스가 모두 실행 중
2. ✅ 모든 인스턴스가 Redis에 연결됨
3. ✅ 서로 다른 서버에 연결된 클라이언트가 메시지 공유
4. ✅ Nginx를 통한 자동 로드 밸런싱
5. ✅ 한 서버 다운 시 다른 서버들이 계속 작동

## 🔧 문제 해결

### 포트 충돌
```bash
# 사용 중인 포트 확인
lsof -i :5002,6002,7002,8080,8081
```

### Redis 연결 실패
```bash
# Redis 컨테이너 재시작
docker-compose restart redis

# Redis 로그 확인
docker-compose logs redis
```

### 빌드 실패
```bash
# 캐시 없이 다시 빌드
docker-compose build --no-cache backend-1
```
