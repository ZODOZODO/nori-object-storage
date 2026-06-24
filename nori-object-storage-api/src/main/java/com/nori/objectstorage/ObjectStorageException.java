package com.nori.objectstorage;

/**
 * 라이브러리 전용 런타임 예외. 백엔드 SDK 예외는 이 타입(또는 하위)으로 매핑되며 cause 를 보존한다.
 */
public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message) {
        super(message);
    }

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
