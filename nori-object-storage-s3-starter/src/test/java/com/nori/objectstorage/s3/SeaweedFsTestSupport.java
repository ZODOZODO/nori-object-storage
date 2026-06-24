package com.nori.objectstorage.s3;

import java.net.URI;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * 운영과 동일 엔진(SeaweedFS)을 Testcontainers 로 띄워 S3 8333 만 노출한다.
 * 컨테이너는 클래스 공유(static), {@link BeforeAll} 에서 테스트 버킷 생성을 재시도로 확정한다.
 */
@Testcontainers
abstract class SeaweedFsTestSupport {

    static final String ACCESS_KEY = "admin";
    static final String SECRET_KEY = "norisys911!";
    static final String BUCKET = "nori-test";
    static final String REGION = "us-east-1";
    static final int S3_PORT = 8333;

    @Container
    static final GenericContainer<?> SEAWEED =
            new GenericContainer<>(DockerImageName.parse("chrislusf/seaweedfs:4.35"))
                    .withCommand("mini", "-dir=/data")
                    .withEnv("AWS_ACCESS_KEY_ID", ACCESS_KEY)
                    .withEnv("AWS_SECRET_ACCESS_KEY", SECRET_KEY)
                    .withExposedPorts(S3_PORT)
                    .waitingFor(Wait.forHttp("/").forPort(S3_PORT)
                            .forStatusCodeMatching(c -> c == 200 || c == 403));

    static String endpoint() {
        return "http://" + SEAWEED.getHost() + ":" + SEAWEED.getMappedPort(S3_PORT);
    }

    /** 테스트용 properties — 컨테이너 매핑 포트 기준. */
    static NoriStorageS3Properties props() {
        NoriStorageS3Properties p = new NoriStorageS3Properties();
        p.setEndpoint(endpoint());
        p.setBucket(BUCKET);
        p.setAccessKey(ACCESS_KEY);
        p.setSecretKey(SECRET_KEY);
        p.setRegion(REGION);
        p.setPathStyle(true);
        return p;
    }

    /** SeaweedFS 는 PUT 전 버킷 존재가 필요 — 컨테이너 readiness 후에도 짧은 재시도로 확정. */
    @BeforeAll
    static void ensureBucket() {
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint()))
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .forcePathStyle(true)
                .build()) {
            SdkException last = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                try {
                    s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
                    return;
                } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
                    return;   // 이미 존재 = OK
                } catch (SdkException e) {
                    last = e;
                    sleep(500);
                }
            }
            throw new IllegalStateException("테스트 버킷 생성 실패: " + BUCKET, last);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
