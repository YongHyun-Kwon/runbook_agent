---
runbook_id: RB-002
title: 응답 지연 대응 절차
symptom_types:
  - LATENCY_INCREASE
service_type: api-service
severity: medium
owner: platform-team
last_updated: 2026-05-14
---

# 응답 지연 대응 절차

## 1. 적용 조건

이 Runbook은 다음 조건에서 사용한다.

- API 응답 시간이 평소 대비 2배 이상 증가
- P99 latency가 3초를 초과
- 특정 서비스의 응답이 전반적으로 느려지는 경우

## 2. 응답 지연 가능성이 높은 경우

- 외부 의존 API의 응답 시간이 동시에 증가
- DB connection pool 대기 시간 증가
- 배포 이후 발생하고 특정 로직에서만 느려짐
- JVM heap 사용률이 급격히 증가

## 3. 응답 지연 가능성이 낮은 경우

- 에러율이 정상 범위이고 latency만 증가
- 특정 단일 사용자 또는 IP에서만 발생
- 인프라 메트릭(CPU, memory)이 정상

## 4. 확인 순서

1. 서비스 health check 상태를 확인한다.
2. 에러율과 latency 추이를 함께 확인한다.
3. 외부 의존 서비스(결제 API, 배송 API 등)의 응답 시간을 확인한다.
4. DB connection pool 대기 시간과 슬로우 쿼리를 확인한다.
5. 최근 배포 이력을 확인한다.
6. JVM 메모리 및 GC 로그를 확인한다.

## 5. 1차 대응

- 즉시 scale-out하지 않는다.
- 원인을 파악하지 않은 scale-out은 근본 원인을 숨길 수 있다.
- scale-out은 담당자 승인 후 진행한다.
- Agent는 scale-out을 실행하지 않는다.

## 6. Escalation 기준

- P99 latency가 5초를 10분 이상 초과
- 결제 또는 주문 핵심 플로우에 영향
- 외부 API timeout으로 사용자 오류 발생
- 담당자 승인 없이는 인프라 변경 금지

## 7. 참고 지표

- P95 / P99 latency
- external API response time
- DB connection pool wait time
- slow query count
- JVM heap usage
- GC pause time
