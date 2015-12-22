package com.liushao.redislockframework;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheLock {
	String lockedPrefix() default "";//redis 锁key的前缀
	long timeOut() default 2000;//锁时间
	int expireTime() default 100000;//key在redis里存在的时间，1000S
}
