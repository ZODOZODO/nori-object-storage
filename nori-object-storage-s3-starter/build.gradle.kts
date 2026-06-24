// S3 구현 + 자동 설정 모듈 (spec §4.2 스코프 확정).
// java-library·toolchain·publish 는 루트 subprojects {} 가 일괄 적용.
dependencies {
    api(project(":nori-object-storage-api"))                 // 계약 재노출 — 소비자가 ObjectStorageClient 직접 참조
    implementation(platform(libs.awssdk.bom))                // AWS BOM — starter 한정 (api 엔 불필요)
    implementation(libs.awssdk.s3)                           // SDK 은닉 (전이 차단). 버전은 BOM 위임
    compileOnly(libs.spring.boot.autoconfigure)              // 소비자 Boot 가 런타임 지배
    compileOnly(libs.jakarta.validation.api)                 // 소비자 Boot validation 제공
    annotationProcessor(platform(libs.spring.boot.dependencies))  // annotationProcessor 는 implementation BOM 미상속 → 별도 위임
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.spring.boot.test)            // ApplicationContextRunner (AutoConfiguration 와이어링 검증)
    testImplementation(libs.spring.boot.autoconfigure)   // AutoConfigurations — main 은 compileOnly 라 test 에 별도 필요
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // awssdk-s3 는 implementation 이라 test classpath 에 이미 포함 (S3Client 직접 사용 가능)
}
