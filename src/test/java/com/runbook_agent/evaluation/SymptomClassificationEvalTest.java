package com.runbook_agent.evaluation;

import com.runbook_agent.domain.SymptomType;
import com.runbook_agent.service.IncidentInputParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SymptomClassificationEvalTest {

    @Autowired
    private IncidentInputParser inputParser;

    // 섹션 20.1 평가 질문셋 — 20개
    private static final List<EvalQuestion> EVAL_QUESTIONS = List.of(
            new EvalQuestion("checkout-service에서 배포 후 500 에러가 증가했습니다", SymptomType.HTTP_5XX_INCREASE),
            new EvalQuestion("order-service에서 DB timeout 로그가 반복됩니다", SymptomType.DB_TIMEOUT),
            new EvalQuestion("payment-service 응답 시간이 3초 이상입니다", SymptomType.LATENCY_INCREASE),
            new EvalQuestion("user-service에서 5xx 에러가 발생했습니다", SymptomType.HTTP_5XX_INCREASE),
            new EvalQuestion("checkout-service에서 외부 결제 API timeout이 발생했습니다", SymptomType.EXTERNAL_API_TIMEOUT),
            new EvalQuestion("배포 이후 장애가 발생했습니다", SymptomType.DEPLOYMENT_RELATED_FAILURE),
            new EvalQuestion("서비스 응답이 느립니다", SymptomType.LATENCY_INCREASE),
            new EvalQuestion("데이터베이스 연결이 계속 실패합니다", SymptomType.DB_TIMEOUT),
            new EvalQuestion("checkout-service에서 500 에러가 갑자기 증가했습니다", SymptomType.HTTP_5XX_INCREASE),
            new EvalQuestion("payment-service의 외부 API 응답이 없습니다", SymptomType.EXTERNAL_API_TIMEOUT),
            new EvalQuestion("order-service 배포 후 오류가 발생하고 있습니다", SymptomType.DEPLOYMENT_RELATED_FAILURE),
            new EvalQuestion("API 응답 지연이 심각합니다", SymptomType.LATENCY_INCREASE),
            new EvalQuestion("DB timeout 에러가 로그에 쌓이고 있습니다", SymptomType.DB_TIMEOUT),
            new EvalQuestion("신규 버전 배포 직후 장애가 의심됩니다", SymptomType.DEPLOYMENT_RELATED_FAILURE),
            new EvalQuestion("서버 에러율이 급격히 증가했습니다", SymptomType.HTTP_5XX_INCREASE),
            new EvalQuestion("checkout-service가 매우 느립니다", SymptomType.LATENCY_INCREASE),
            new EvalQuestion("order-service에서 database 연결이 끊어집니다", SymptomType.DB_TIMEOUT),
            new EvalQuestion("결제 외부 API 연동이 timeout으로 실패합니다", SymptomType.EXTERNAL_API_TIMEOUT),
            new EvalQuestion("v1.24.3 릴리즈 이후 문제가 생겼습니다", SymptomType.DEPLOYMENT_RELATED_FAILURE),
            new EvalQuestion("HTTP 500 에러가 지속 발생합니다", SymptomType.HTTP_5XX_INCREASE)
    );

    record EvalQuestion(String input, SymptomType expected) {
    }

    static Stream<Arguments> evalQuestions() {
        return EVAL_QUESTIONS.stream()
                .map(q -> Arguments.of(q.input(), q.expected()));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("evalQuestions")
    void classifySymptom_eachQuestion(String input, SymptomType expected) {
        assertThat(inputParser.classifySymptom(input)).isEqualTo(expected);
    }

    // 완료 기준: 20개 질문 중 80% 이상 정확도
    @Test
    void classifySymptom_overallAccuracy_atLeast80Percent() {
        long correct = EVAL_QUESTIONS.stream()
                .filter(q -> inputParser.classifySymptom(q.input()) == q.expected())
                .count();

        double accuracy = (double) correct / EVAL_QUESTIONS.size() * 100;
        System.out.printf("분류 정확도: %.1f%% (%d/%d)%n", accuracy, correct, EVAL_QUESTIONS.size());

        assertThat(accuracy).isGreaterThanOrEqualTo(80.0);
    }
}
