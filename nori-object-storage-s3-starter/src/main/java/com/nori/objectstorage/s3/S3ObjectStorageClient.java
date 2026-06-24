package com.nori.objectstorage.s3;

import java.io.InputStream;
import java.net.URI;

import com.nori.objectstorage.ObjectContent;
import com.nori.objectstorage.ObjectNotFoundException;
import com.nori.objectstorage.ObjectStorageClient;
import com.nori.objectstorage.ObjectStorageException;
import com.nori.objectstorage.StoredObject;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * {@link ObjectStorageClient} 의 AWS SDK v2 기반 S3 구현. {@link AutoCloseable} 이라
 * AutoConfiguration 빈 소멸 시 Spring 이 {@link #close()} 를 자동 호출한다.
 *
 * <p>생성자에서 properties 로 {@link S3Client} 를 1회 구성해 보관한다 (path-style 강제).
 */
public class S3ObjectStorageClient implements ObjectStorageClient, AutoCloseable {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final S3Client s3;
    private final String bucket;

    public S3ObjectStorageClient(NoriStorageS3Properties p) {
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(p.getEndpoint()))
                .region(Region.of(p.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.getAccessKey(), p.getSecretKey())))
                .forcePathStyle(p.isPathStyle())
                .build();
        this.bucket = p.getBucket();
    }

    @Override
    public StoredObject put(String key, InputStream content, long size, String contentType) {
        requireText(key, "key");
        if (content == null) {
            throw new ObjectStorageException("content must not be null");
        }
        String ct = normalizeContentType(contentType);
        try {
            PutObjectResponse resp = s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket).key(key)
                            .contentType(ct).contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(content, size));
            return new StoredObject(key, size, ct, resp.eTag());
        } catch (SdkException e) {
            throw new ObjectStorageException("put failed: " + key, e);
        }
    }

    @Override
    public StoredObject putBytes(String key, byte[] bytes, String contentType) {
        requireText(key, "key");
        if (bytes == null) {
            throw new ObjectStorageException("bytes must not be null");
        }
        String ct = normalizeContentType(contentType);
        try {
            PutObjectResponse resp = s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket).key(key)
                            .contentType(ct).contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes));
            return new StoredObject(key, bytes.length, ct, resp.eTag());
        } catch (SdkException e) {
            throw new ObjectStorageException("put failed: " + key, e);
        }
    }

    @Override
    public ObjectContent get(String key) {
        requireText(key, "key");
        try {
            ResponseInputStream<GetObjectResponse> ris = s3.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            GetObjectResponse r = ris.response();
            return new ObjectContent(ris, r.contentLength(), r.contentType(), r.eTag());
        } catch (SdkException e) {
            if (isNotFound(e)) {
                throw new ObjectNotFoundException(key);
            }
            throw new ObjectStorageException("get failed: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        requireText(key, "key");
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (SdkException e) {
            if (isNotFound(e)) {
                return false;
            }
            throw new ObjectStorageException("exists failed: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        requireText(key, "key");
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            if (isNotFound(e)) {
                return;   // 멱등 — 없는 키 무시
            }
            throw new ObjectStorageException("delete failed: " + key, e);
        }
    }

    @Override
    public void close() {
        s3.close();
    }

    /** NoSuchKey 또는 HTTP 404 → "키 없음"으로 본다 (백엔드가 표준코드/단순404 무엇이든 안전). */
    private static boolean isNotFound(SdkException e) {
        if (e instanceof NoSuchKeyException) {
            return true;
        }
        return e instanceof S3Exception s3e && s3e.statusCode() == 404;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ObjectStorageException(name + " must not be blank");
        }
    }

    private static String normalizeContentType(String contentType) {
        return (contentType == null || contentType.isBlank()) ? DEFAULT_CONTENT_TYPE : contentType;
    }
}
