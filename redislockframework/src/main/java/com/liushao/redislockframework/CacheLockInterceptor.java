package com.liushao.redislockframework;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class CacheLockInterceptor implements InvocationHandler{
	public static int ERROR_COUNT  = 0;
	private Object proxied;
	
	
	public CacheLockInterceptor(Object proxied) {
		this.proxied = proxied;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		CacheLock cacheLock = method.getAnnotation(CacheLock.class);
		//没有cacheLock注解，pass
		if(null == cacheLock){
			System.out.println("no cacheLock annotation");			
			return method.invoke(proxied, args);
		}
		
		//获得方法中参数的注解
		Annotation[][] annotations = method.getParameterAnnotations();
		//根据获取到的参数注解和参数列表获得加锁的参数
		Object lockedObject = getLockedObject(annotations,args);
		String objectValue = lockedObject.toString();
		RedisLock lock = new RedisLock(cacheLock.lockedPrefix(), objectValue);
		boolean result = lock.lock(cacheLock.timeOut(), cacheLock.expireTime());
		if(!result){//取锁失败
			ERROR_COUNT += 1;
			throw new CacheLockException("get lock fail");
			
		}
		try{
			//执行方法
			return method.invoke(proxied, args);
		}finally{
			System.out.println("intecepor 释放锁");
			lock.unlock();//释放锁
		}
		
	}

	
	/**
	 *  从方法参数中找出@lockedComplexOnbject的参数，在redis中取该参数对应的锁
	 * @param annotations
	 * @param args
	 * @return
	 * @throws CacheLockException
	 */
	private Object getLockedObject(Annotation[][] annotations,Object[] args) throws CacheLockException{
		if(null == args || args.length == 0){
			throw new CacheLockException("方法参数为空，没有被锁定的对象");
		}
		
		if(null == annotations || annotations.length == 0){
			throw new CacheLockException("没有被注解的参数");
		}
		//不支持多个参数加锁，只支持第一个注解为lockedObject或者lockedComplexObject的参数
		int index = -1;//标记参数的位置指针
		for(int i = 0;i < annotations.length;i++){
			for(int j = 0;j < annotations[i].length;j++){
				if(annotations[i][j] instanceof LockedComplexObject){//注解为LockedComplexObject
					index = i;
					try {
						return args[i].getClass().getField(((LockedComplexObject)annotations[i][j]).field());
					} catch (NoSuchFieldException | SecurityException e) {
						throw new CacheLockException("注解对象中没有该属性" + ((LockedComplexObject)annotations[i][j]).field());
					}
				}
				
				if(annotations[i][j] instanceof LockedObject){
					index = i;
					break;
				}
			}
			//找到第一个后直接break，不支持多参数加锁
			if(index != -1){
				break;
			}
		}
		
		if(index == -1){
			throw new CacheLockException("请指定被锁定参数");
		}
		
		return args[index];
	}
}
