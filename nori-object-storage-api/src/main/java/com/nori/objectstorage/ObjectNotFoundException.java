package com.nori.objectstorage;

/**
 * 키 없음 전용 예외 — 소비자가 NOT_FOUND 로 매핑한다. 문제의 key 를 보존한다.
 */
public class ObjectNotFoundException extends ObjectStorageException {

    private final String key;

    public ObjectNotFoundException(String key) {
        super("object not found: " + key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
