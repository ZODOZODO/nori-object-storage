package com.nori.objectstorage.s3;

import com.nori.objectstorage.ObjectStorageClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@code nori.storage.s3.*} 가 설정되면 {@link ObjectStorageClient} 빈을 자동 와이어링한다.
 * 소비자가 직접 빈을 주면 {@link ConditionalOnMissingBean} 으로 그쪽이 우선한다.
 */
@AutoConfiguration
@EnableConfigurationProperties(NoriStorageS3Properties.class)
public class NoriObjectStorageS3AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    ObjectStorageClient objectStorageClient(NoriStorageS3Properties p) {
        return new S3ObjectStorageClient(p);
    }
}
