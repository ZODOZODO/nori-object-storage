// 루트 공통 빌드 — nori-tc 의 build-logic 컨벤션 플러그인 대신, 모듈 2개 규모에 맞춰
// 컨벤션을 subprojects {} 에 인라인한다 (spec §4.6 "과설계 배제").
// java-library·maven-publish 는 core 플러그인이라 루트 plugins 블록 없이 subprojects 에서 apply 한다.

allprojects {
    group = "com.nori"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    // 루트 subprojects {} 블록에서는 type-safe `libs` 접근자를 쓸 수 없어
    // VersionCatalogsExtension 으로 카탈로그를 직접 조회한다 (Gradle 9.2.1).
    val libs = rootProject.the<VersionCatalogsExtension>().named("libs")

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }

    // Spring Boot BOM 만 전 모듈 일괄 (api 의 slf4j 버전 관리에도 필요).
    // AWS SDK BOM·Testcontainers BOM 은 starter 모듈에서만 (api 는 AWS·test 불필요, spec §4.2).
    dependencies {
        add("implementation", platform(libs.findLibrary("spring-boot-dependencies").get()))
    }

    afterEvaluate {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    // BOM 위임 버전을 발행 POM 에 고정 → 소비자 재현성.
                    versionMapping {
                        usage("java-api") { fromResolutionResult() }
                        usage("java-runtime") { fromResolutionResult() }
                    }
                }
            }
            // 로컬 mavenLocal 은 projectId 불필요 — 원격 GitLab 발행 시에만 repo 등록.
            val projectId = providers.environmentVariable("CI_PROJECT_ID")
                .orElse(providers.gradleProperty("ciProjectId")).orNull
            if (projectId != null) {
                repositories.maven {
                    name = "GitLab"
                    url = uri("https://gitlab.com/api/v4/projects/$projectId/packages/maven")
                    credentials(HttpHeaderCredentials::class) {
                        val job = providers.environmentVariable("CI_JOB_TOKEN").orNull
                        if (job != null) {
                            name = "Job-Token"
                            value = job
                        } else {
                            name = "Private-Token"
                            value = providers.gradleProperty("gitlabToken").orNull
                                ?: providers.environmentVariable("GITLAB_TOKEN").orNull
                                ?: error("gitlabToken/GITLAB_TOKEN 필요")
                        }
                    }
                    authentication { create<HttpHeaderAuthentication>("header") }
                }
            }
        }
    }
}
