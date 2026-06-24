package com.nori.objectstorage;

import java.io.InputStream;

/**
 * 설정된 버킷 1개를 대상으로 객체를 넣고/빼고/지운다. 백엔드(S3 등)·SDK 는 은닉된다.
 *
 * <p>키는 소비자가 생성해 넘기는 불투명 문자열이다 — 라이브러리는 prefix 규칙·MIME·권한을
 * 모른다(단일 책임). 백엔드 오류는 {@link ObjectStorageException} 계열로 던진다.
 */
public interface ObjectStorageClient {

    /**
     * 스트림을 객체로 저장한다. 동일 key 재저장은 덮어쓴다(S3 PUT 표준).
     *
     * @param size        Content-Length — 호출자가 책임지는 정확한 바이트 수
     * @param contentType null·blank 이면 {@code application/octet-stream} 으로 저장
     * @throws ObjectStorageException key·content 가 null·blank 이거나 백엔드 오류 시
     */
    StoredObject put(String key, InputStream content, long size, String contentType);

    /**
     * 바이트 배열을 객체로 저장한다. size 는 {@code bytes.length}.
     *
     * @throws ObjectStorageException key·bytes 가 null·blank 이거나 백엔드 오류 시
     */
    StoredObject putBytes(String key, byte[] bytes, String contentType);

    /**
     * 객체를 다운로드한다. 스트림 + 메타(크기·타입)를 동봉해 Content-Length 를 보존한다
     * (추가 왕복 0). 반환 스트림 close 는 소비자 책임(try-with-resources).
     *
     * @throws ObjectNotFoundException 키가 없을 때
     * @throws ObjectStorageException  그 외 백엔드 오류
     */
    ObjectContent get(String key);

    /**
     * 객체 존재 여부.
     *
     * @return 존재하면 true, 없으면 false
     * @throws ObjectStorageException 키 없음 외의 백엔드 오류(false 로 삼키지 않는다)
     */
    boolean exists(String key);

    /**
     * 객체를 삭제한다. 멱등 — 없는 키도 성공으로 본다.
     *
     * @throws ObjectStorageException 키 없음 외의 백엔드 오류
     */
    void delete(String key);
}
