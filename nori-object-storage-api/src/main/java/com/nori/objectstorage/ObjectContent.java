package com.nori.objectstorage;

import java.io.InputStream;

/**
 * 다운로드 결과 = 스트림 + 크기·타입. {@code content} 의 close 는 소비자 책임
 * (try-with-resources). {@code size} 로 Content-Length 를 보존한다.
 */
public record ObjectContent(InputStream content, long size, String contentType, String eTag) {}
