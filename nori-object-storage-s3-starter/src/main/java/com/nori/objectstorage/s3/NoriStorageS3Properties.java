package com.nori.objectstorage.s3;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code nori.storage.s3.*} 바인딩. 필수 4종은 {@link NotBlank} 로 바인딩 시점 검증 —
 * 누락 시 속성명을 메시지에 담아 부팅 즉시 실패한다.
 */
@Validated
@ConfigurationProperties("nori.storage.s3")
public class NoriStorageS3Properties {

    /** SeaweedFS S3 엔드포인트 (예: http://192.168.0.203:8333). */
    @NotBlank
    private String endpoint;

    /** 앱별 버킷 (nori-hrm / nori-tc / nori-mes). */
    @NotBlank
    private String bucket;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    /** S3 호환 더미 리전. 기본 us-east-1. */
    private String region = "us-east-1";

    /** S3 호환 엔드포인트 필수. 기본 true. */
    private boolean pathStyle = true;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(boolean pathStyle) {
        this.pathStyle = pathStyle;
    }
}
