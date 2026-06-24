package com.nori.objectstorage.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import com.nori.objectstorage.ObjectContent;
import com.nori.objectstorage.ObjectStorageClient;
import com.nori.objectstorage.StoredObject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** enabled 토글·필수 검증·@ConditionalOnMissingBean 의 와이어링 동작을 검증한다 (Docker 불필요). */
class NoriObjectStorageS3AutoConfigurationTest {

    private static final String[] REQUIRED = {
            "nori.storage.s3.endpoint=http://localhost:8333",
            "nori.storage.s3.bucket=nori-test",
            "nori.storage.s3.access-key=admin",
            "nori.storage.s3.secret-key=secret",
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NoriObjectStorageS3AutoConfiguration.class));

    @Test
    void disabled_byDefault_noBean() {
        runner.withPropertyValues(REQUIRED)   // 설정은 있어도 enabled 미설정 → 비활성
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ObjectStorageClient.class));
    }

    @Test
    void enabled_withRequiredProps_createsBean() {
        runner.withPropertyValues("nori.storage.s3.enabled=true")
                .withPropertyValues(REQUIRED)
                .run(ctx -> assertThat(ctx).hasSingleBean(ObjectStorageClient.class));
    }

    @Test
    void enabled_missingRequiredProps_contextFails() {
        runner.withPropertyValues("nori.storage.s3.enabled=true")   // 필수 4종 누락
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void customBean_wins_overAutoConfig() {
        runner.withPropertyValues("nori.storage.s3.enabled=true")
                .withPropertyValues(REQUIRED)
                .withUserConfiguration(CustomConfig.class)
                .run(ctx -> assertThat(ctx).getBean(ObjectStorageClient.class)
                        .isSameAs(ctx.getBean("customClient")));
    }

    @Configuration
    static class CustomConfig {
        @Bean
        ObjectStorageClient customClient() {
            return new NoopObjectStorageClient();
        }
    }

    /** 커스텀 빈 우선 검증용 stub. */
    static class NoopObjectStorageClient implements ObjectStorageClient {
        @Override
        public StoredObject put(String key, InputStream content, long size, String contentType) {
            return null;
        }

        @Override
        public StoredObject putBytes(String key, byte[] bytes, String contentType) {
            return null;
        }

        @Override
        public ObjectContent get(String key) {
            return null;
        }

        @Override
        public boolean exists(String key) {
            return false;
        }

        @Override
        public void delete(String key) {
        }
    }
}
