// 순수 계약 모듈 — Spring·AWS 의존 0 (spec §4.2).
// java-library·toolchain·publish 는 루트 subprojects {} 가 일괄 적용.
dependencies {
    compileOnly(libs.slf4j.api)   // 로깅 인터페이스만, 전이 없음
}
