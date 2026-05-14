---
runbook_id: RB-001
title: 배포 직후 5xx 증가 대응 절차
symptom_types:
  - HTTP_5XX_INCREASE
  - DEPLOYMENT_RELATED_FAILURE
service_type: api-service
severity: high
owner: platform-team
last_updated: 2026-05-14
---

# 배포 직후 5xx 증가 대응 절차

## 1. 적용 조건

이 Runbook은 다음 조건에서 사용한다.

- 배포 이후 30분 이내 5xx 에러율 증가
- 특정 API endpoint에서 500 응답 증가
- 신규 버전 배포 직후 장애 발생 의심

## 2. 배포 영향 가능성이 높은 경우

- 배포 시점과 에러 증가 시점이 30분 이내로 근접
- 신규 버전에서만 exception 발생
- 이전 유사 장애에서 rollback 후 에러율 감소 이력 존재
- 에러율이 baseline 대비 5배 이상 증가

## 3. 배포 영향 가능성이 낮은 경우

- 배포 전부터 에러율 증가
- 여러 독립 서비스에서 동시에 장애 발생
- 외부 API 장애 알람이 먼저 발생
- DB connection pool 고갈이 먼저 관측됨

## 4. 확인 순서

1. 최근 배포 버전을 확인한다.
2. 배포 시점과 에러 증가 시점을 비교한다.
3. CloudWatch에서 공통 exception message를 확인한다.
4. 특정 endpoint에 에러가 집중되는지 확인한다.
5. 외부 API timeout 여부를 확인한다.

## 5. 1차 대응

- 즉시 rollback하지 않는다.
- 영향 범위와 에러율을 먼저 확인한다.
- rollback은 담당자 승인 후 진행한다.
- Agent는 rollback을 실행하지 않는다.

## 6. Escalation 기준

- 5xx rate가 10분 이상 baseline 대비 10배 이상 유지
- checkout / payment / order 핵심 플로우 실패
- 고객 결제 실패 발생
- 담당자 승인 없이는 변경 작업 금지

## 7. 참고 지표

- HTTP 5xx rate
- deployment timestamp
- exception message
- endpoint별 error count
- downstream API timeout rate
