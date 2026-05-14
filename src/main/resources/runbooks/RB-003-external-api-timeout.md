---
runbook_id: RB-003
title: 외부 API timeout 대응 절차
symptom_types:
  - EXTERNAL_API_TIMEOUT
  - HTTP_5XX_INCREASE
service_type: api-service
severity: medium
owner: platform-team
last_updated: 2026-05-14
---

# 외부 API timeout 대응 절차

## 1. 적용 조건

이 Runbook은 다음 조건에서 사용한다.

- 외부 API 호출에서 timeout 로그가 반복 발생
- 특정 downstream 서비스와의 연동에서만 오류 발생
- 외부 결제, 배송, 인증 API 연동 실패

## 2. 외부 API 원인 가능성이 높은 경우

- 외부 서비스 공식 장애 알람 또는 status page 이상
- 내부 서비스는 정상이나 특정 외부 API endpoint만 실패
- timeout 오류 발생 시각에 외부 API 응답 시간 급증
- 이전에도 동일 외부 API 관련 장애 이력 존재

## 3. 외부 API 원인 가능성이 낮은 경우

- 여러 독립적인 외부 서비스가 동시에 실패 (내부 네트워크 문제 의심)
- 내부 서비스 자체 에러율도 동시에 증가
- timeout 설정값이 비정상적으로 낮게 설정된 경우

## 4. 확인 순서

1. 외부 API 공식 status page를 확인한다.
2. 내부 로그에서 timeout 발생 시각과 빈도를 확인한다.
3. circuit breaker 상태를 확인한다. (OPEN / HALF_OPEN / CLOSED)
4. timeout 설정값이 적절한지 확인한다.
5. retry 로그를 확인한다.
6. 외부 API 담당 팀에 장애 여부를 확인한다.

## 5. 1차 대응

- 임의로 retry를 수동 실행하지 않는다.
- traffic 차단은 담당자 승인 후 진행한다.
- timeout 설정 변경은 담당자 승인 후 진행한다.
- Agent는 외부 API 호출이나 설정 변경을 실행하지 않는다.

## 6. Escalation 기준

- 핵심 결제 플로우에서 외부 API timeout 지속
- circuit breaker OPEN 상태가 5분 이상 지속
- 외부 API 담당 팀과 연락이 되지 않는 경우
- 담당자 승인 없이는 설정 변경 금지

## 7. 참고 지표

- external API response time
- timeout error count per endpoint
- circuit breaker state (OPEN / HALF_OPEN / CLOSED)
- retry success rate
- downstream error rate
