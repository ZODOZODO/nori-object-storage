package com.nori.objectstorage.s3;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.nori.objectstorage.ObjectContent;
import com.nori.objectstorage.ObjectNotFoundException;
import com.nori.objectstorage.ObjectStorageException;
import com.nori.objectstorage.StoredObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** spec §4.3/§5 의 수용기준(AC)을 운영 동일 엔진(SeaweedFS Testcontainers)으로 검증한다. */
class S3ObjectStorageClientTest extends SeaweedFsTestSupport {

    private S3ObjectStorageClient client;

    @BeforeEach
    void setUp() {
        client = new S3ObjectStorageClient(props());
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    private static String key() {
        return "it/" + UUID.randomUUID();
    }

    private static byte[] readAndClose(ObjectContent oc) throws IOException {
        try (InputStream in = oc.content()) {
            return in.readAllBytes();
        }
    }

    @Test
    void put_storesAndReturnsMeta() {
        String k = key();
        byte[] data = "hello".getBytes(UTF_8);
        StoredObject so = client.put(k, new ByteArrayInputStream(data), data.length, "text/plain");
        assertEquals(k, so.key());
        assertEquals(data.length, so.size());
        assertEquals("text/plain", so.contentType());
        assertNotNull(so.eTag());
    }

    @Test
    void putBytes_usesByteLengthAsSize() throws IOException {
        String k = key();
        byte[] data = "world!".getBytes(UTF_8);
        StoredObject so = client.putBytes(k, data, "text/plain");
        assertEquals(data.length, so.size());
        assertArrayEquals(data, readAndClose(client.get(k)));
    }

    @Test
    void get_returnsStreamAndContentLength() throws IOException {
        String k = key();
        byte[] data = "content-length-check".getBytes(UTF_8);
        client.putBytes(k, data, "text/plain");
        ObjectContent oc = client.get(k);
        assertEquals(data.length, oc.size());
        assertArrayEquals(data, readAndClose(oc));
    }

    @Test
    void get_missingKey_throwsObjectNotFound() {
        String missing = key();
        ObjectNotFoundException ex = assertThrows(ObjectNotFoundException.class, () -> client.get(missing));
        assertEquals(missing, ex.getKey());
    }

    @Test
    void exists_true_whenPresent() {
        String k = key();
        client.putBytes(k, "x".getBytes(UTF_8), "text/plain");
        assertTrue(client.exists(k));
    }

    @Test
    void exists_false_whenAbsent() {
        assertFalse(client.exists(key()));
    }

    @Test
    void delete_isIdempotent() {
        String absent = key();
        assertDoesNotThrow(() -> client.delete(absent));   // 없는 키 → 성공

        String k = key();
        client.putBytes(k, "x".getBytes(UTF_8), "text/plain");
        client.delete(k);
        assertFalse(client.exists(k));
    }

    @Test
    void put_overwritesSameKey() throws IOException {
        String k = key();
        client.putBytes(k, "v1".getBytes(UTF_8), "text/plain");
        client.putBytes(k, "v2".getBytes(UTF_8), "text/plain");
        assertArrayEquals("v2".getBytes(UTF_8), readAndClose(client.get(k)));
    }

    @Test
    void contentType_roundTrips() {
        String k = key();
        client.putBytes(k, "png".getBytes(UTF_8), "image/png");
        assertEquals("image/png", client.get(k).contentType());
    }

    @Test
    void contentType_blank_defaultsToOctetStream() {
        String k = key();
        StoredObject so = client.putBytes(k, "x".getBytes(UTF_8), "   ");
        assertEquals("application/octet-stream", so.contentType());
        assertEquals("application/octet-stream", client.get(k).contentType());
    }

    @Test
    void put_blankKey_throwsObjectStorageException() {
        byte[] data = "x".getBytes(UTF_8);
        ObjectStorageException ex = assertThrows(ObjectStorageException.class,
                () -> client.put("   ", new ByteArrayInputStream(data), data.length, "text/plain"));
        assertTrue(ex.getMessage().contains("key"));
    }

    @Test
    void put_nullContent_throwsObjectStorageException() {
        ObjectStorageException ex = assertThrows(ObjectStorageException.class,
                () -> client.put(key(), null, 0, "text/plain"));
        assertTrue(ex.getMessage().contains("content"));
    }
}
