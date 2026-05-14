---
runbook_id: RB-004
title: DB timeout 대응 절차
symptom_types:
  - DB_TIMEOUT
service_type: api-service
severity: high
owner: platform-team
last_updated: 2026-05-14
---

# DB timeout 대응 절차

## 1. 적용 조건

이 Runbook은 다음 조건에서 사용한다.

- DB timeout 또는 connection pool exhausted 로그 반복 발생
- DB 연결 대기 시간이 급격히 증가
- 쿼리 실행 시간이 평소 대비 급증

## 2. DB timeout 가능성이 높은 경우

- connection pool exhausted 로그 발생
- 슬로우 쿼리 개수 급증
- 특정 쿼리 또는 트랜잭션에서만 timeout 집중
- 대량 데이터 처리 배치 작업과 동시에 발생

## 3. DB timeout 가능성이 낮은 경우

- DB 서버 CPU / memory가 정상 범위
- 특정 단일 쿼리에서만 간헐적으로 발생
- 외부 API 장애가 먼저 관측된 경우

## 4. 확인 순서

1. DB connection pool 현재 사용량과 최대치를 확인한다.
2. 슬로우 쿼리 로그에서 실행 시간이 긴 쿼리를 확인한다.
3. DB 서버 CPU, memory, I/O 상태를 확인한다.
4. 최근 배포에서 쿼리 변경이 있었는지 확인한다.
5. 배치 작업이나 대량 데이터 처리가 동시에 실행 중인지 확인한다.
6. Lock 경합(deadlock) 여부를 확인한다.

## 5. 1차 대응

- DB 직접 수정(쿼리 kill, 테이블 변경)은 즉시 실행하지 않는다.
- connection pool size 변경은 담당자 승인 후 진행한다.
- DB 서버 재시작은 담당자 승인 후 진행한다.
- Agent는 DB 수정이나 재시작을 실행하지 않는다.

## 6. Escalation 기준

- connection pool이 완전히 고갈되어 서비스 요청 전체 실패
- deadlock으로 인한 트랜잭션 전체 중단
- 핵심 주문 / 결제 플로우에서 DB 오류 지속
- 담당자 승인 없이는 DB 변경 작업 금지

## 7. 참고 지표

- active DB connections
- connection pool wait time
- slow query count
- DB CPU usage
- DB I/O throughput
- lock wait time
