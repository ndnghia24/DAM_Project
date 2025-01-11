package com.example.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để đánh dấu các lớp hoặc interface cần sinh TableGateway.
 */
@Target(ElementType.TYPE) // Áp dụng trên lớp hoặc interface
@Retention(RetentionPolicy.CLASS) // Lưu trữ trong bytecode và có thể truy cập trong Annotation Processor
public @interface TableGateWay {
    /**
     * Tùy chọn: Repository mà TableGateway sẽ sử dụng.
     * Nếu không cung cấp, Annotation Processor sẽ tự động suy luận.
     */
    String repository() default "com.example.repositories.EntityRepository";
}