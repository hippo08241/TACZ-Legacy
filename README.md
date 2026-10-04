# TACZ-Legacy

`TACZ-Legacy`는 `TACZ`의 **Minecraft 1.12.2 Forge 포팅 프로젝트**이며, **Kotlin + RetroFuturaGradle** 기술 스택을 사용합니다.

프로젝트 목표:

- TACZ의 핵심 게임플레이, 상호작용, 기능을 이식하고 일관성을 유지
- 기존 아트 리소스와 리소스 명명 체계를 최대한 재사용
- 1.20 시대의 "모드 데이터팩 / 총기 팩(건팩)"을 가능한 한 매끄럽게 이식하고 호환성을 유지
- 1.12.2 렌더링 측 아키텍처를 재구성하여 확장 가능한 렌더링 파이프라인 제공

---

## 현재 단계

현재 저장소에서 완료된 항목:

- Kotlin 1.12.2 프로젝트 기본 설정 초기화
- 모드 메인 엔트리 / 프록시 골격 구축
- Mixin 기본 환경 초기화(`mixins.tacz.json`)
- 프로젝트 수준 Copilot 가이드 파일 초기화(`.github/copilot-instructions.md`)
- 마이그레이션 청사진 문서 초기화(`docs/MIGRATION_PLAN.md`)

---

## 기술 스택

- Minecraft Forge `1.12.2`(`14.23.5.2847`)
- Kotlin(Forgelin-Continuous)
- RetroFuturaGradle
- Sponge Mixin(MixinBooter를 통해 연동)

---

## 빠른 시작

1. 디컴파일 작업 공간 초기화: `gradlew setupDecompWorkspace`
2. Gradle 프로젝트 가져오기/새로고침(IDEA)
3. 개발 환경 실행: `gradlew runClient` / `gradlew runServer`

> `gradle.properties`의 `use_mixins/use_coremod/use_access_transformer` 등의 스위치를 변경했다면, setup을 다시 실행하고 Gradle을 새로고침하는 것을 권장합니다.

### 자동화 스모크 테스트

- 순수 컴파일 검사: `./gradlew classes`
- 클라이언트 로딩 스모크 테스트: `bash scripts/runclient_smoke.sh`
- 사용자 지정 타임아웃(초): `bash scripts/runclient_smoke.sh 120`

이 스크립트는 로그를 `build/smoke-tests/`에 출력하며, 다음 조건 중 하나를 만족하면 통과로 판정합니다:

- 타임아웃 전에 `FoundationSmoke` + `Forge Mod Loader has successfully loaded` 시작 완료 마커에 도달
- 마커에 도달하면 스크립트가 자동으로 클라이언트를 종료하고 반환하므로, 콘솔이 계속 멈춰 있지 않습니다

이는 "실제 MC 환경까지 정상적으로 시작되는지"에 대한 회귀 검증에 적합합니다. 구체적인 게임플레이 흐름(사격, 재장전, 작업대 등)을 검증하려면, 이를 바탕으로 더 세밀한 상호작용 스크립트나 통합 테스트를 추가하는 것을 권장합니다.

---

## 문서 안내

- 프로젝트 개요: `PROJECT_OVERVIEW.md`
- 마이그레이션 청사진: `docs/MIGRATION_PLAN.md`
- 아키텍처 및 렌더링 청사진: `docs/ARCHITECTURE_BOUNDARY_AND_RENDER_PIPELINE.md`
- Copilot 협업 가이드: `.github/copilot-instructions.md`

---

## 설계 원칙(마이그레이션 기간)

1. **호환성 우선**: 먼저 동작과 데이터 호환성을 보장한 뒤 구조를 개선합니다.
2. **계층별 마이그레이션**: 리소스/데이터 계층, 로직 계층, 렌더링 계층을 단계적으로 진행합니다.
3. **관측 가능성**: 핵심 시스템은 마이그레이션 기간 동안 반드시 로그와 진단 스위치를 갖춰야 합니다.
4. **롤백 가능**: 위험도가 높은 변경(특히 렌더링)은 점진적 적용 스위치를 지원해야 합니다.
5. **분리하되 MC와 단절하지 않기**: 단위 테스트를 위해 핵심 로직은 가능한 한 MC 의존성을 제거하되, 명확한 MC 어댑터 계층은 유지합니다.

---

## 라이선스

상위 프로젝트 TACZ의 저작권 및 라이선스 제약을 따릅니다. 이 저장소는 포팅 개발에 필요한 코드 골격과 문서만 포함합니다.
