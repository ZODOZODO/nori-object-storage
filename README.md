# nori-object-storage

사내 공통 오브젝트 스토리지 라이브러리. 소비 시스템(hrm·tc·mes)이 SeaweedFS(S3 호환)에
붙어 객체를 넣고/빼고/지우는 동작을 단일 인터페이스로 추상화한다. AWS SDK·엔드포인트는
라이브러리 내부에 은닉된다.

## 모듈

| 좌표 | 역할 |
|---|---|
| `com.nori:nori-object-storage-api` | 순수 계약 — `ObjectStorageClient` + 값 타입 + 예외. Spring·AWS 의존 0. |
| `com.nori:nori-object-storage-s3-starter` | S3 구현 + `@AutoConfiguration`. 소비자는 이 모듈 1개만 의존. |

버전 `0.0.1` / group `com.nori` / JVM 21 / Java.

## 소비 (요약)

`build.gradle.kts`:

```kotlin
implementation("com.nori:nori-object-storage-s3-starter:0.0.1")
```

`application.yml`:

```yaml
nori.storage.s3:
  endpoint: http://192.168.0.203:8333
  bucket: nori-hrm          # tc → nori-tc, mes → nori-mes
  access-key: nori-hrm
  secret-key: ${NORI_STORAGE_SECRET}
  region: us-east-1         # 선택 (기본 us-east-1)
  path-style: true          # 선택 (기본 true — S3 호환 필수)
```

`ObjectStorageClient` 빈이 자동 와이어링된다. 소비자는 이 빈을 주입받아 자기 파일
서비스 내부에서만 위임한다 — 키 생성·MIME·권한·audit 검증은 소비자 책임.
