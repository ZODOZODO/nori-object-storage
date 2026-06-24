package com.nori.objectstorage;

/**
 * put 결과 메타. {@code key} 는 내구 핸들(로컬 FS 의 storedPath 대체).
 */
public record StoredObject(String key, long size, String contentType, String eTag) {}
