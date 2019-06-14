package com.example.currentlimiter.aop;

import com.example.currentlimiter.annotation.Limit;
import com.example.currentlimiter.enums.LimitTypeEnum;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;



/**
 * @ClassName : LimitInterceptor
 * @Description : //TODO
 * @author : chenling
 * @Date : 2019/6/12 18:53
 * @since : v1.0.0
 **/

@Configuration
@Aspect
@Slf4j
public class LimitInterceptor {

    private static final String UNKNOWN = "unknown";


    private  RedisTemplate<String, Serializable> limitRedisTemplate;

    @Autowired
    public LimitInterceptor(RedisTemplate<String, Serializable> limitRedisTemplate) {
        this.limitRedisTemplate = limitRedisTemplate;
    }


    @Around(value = "execution(public * *(..)) && @annotation(com.example.currentlimiter.annotation.Limit)")
    public Object interceptor(ProceedingJoinPoint pjp) {

        MethodSignature signature =(MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Limit methodAnnotation = method.getAnnotation(Limit.class);
        LimitTypeEnum limitType = methodAnnotation.limitType();
        String name = methodAnnotation.name();
        String key;
        int limitPeriod = methodAnnotation.period();
        int limitCount = methodAnnotation.count();

        switch (limitType){
            case IP:
                key =  getIpAddress();
                break;
            case CUSTOMER:
                // TODO 如果此处想根据表达式或者一些规则生成
                key=methodAnnotation.key();
                break;
            default:
                key = StringUtils.upperCase(method.getName());
        }

        ImmutableList<String> keys = ImmutableList.of(StringUtils.join(methodAnnotation.prefix(), key));

        try {

            String luaScript = buildLuaScript();
            DefaultRedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
            Number count = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);
            log.info("Access try count is {} for name={} and key = {}", count, name, key);

            if(count != null && count.intValue()<=limitCount ){
                return pjp.proceed();
            }else{
                throw  new RuntimeException("You have been dragged into the blacklist");
            }

        } catch (Throwable e) {
            throw  new RuntimeException(e.getLocalizedMessage());
        }
    }



    /**
     * 限流 脚本
     *
     * @return lua脚本
     */
    public String buildLuaScript() {
        StringBuilder lua = new StringBuilder();
        lua.append("local c");
        lua.append("\nc = redis.call('get',KEYS[1])");
        // 调用不超过最大值，则直接返回
        lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then");
        lua.append("\nreturn c;");
        lua.append("\nend");
        // 执行计算器自加
        lua.append("\nc = redis.call('incr',KEYS[1])");
        lua.append("\nif tonumber(c) == 1 then");
        // 从第一次调用开始限流，设置对应键值的过期
        lua.append("\nredis.call('expire',KEYS[1],ARGV[2])");
        lua.append("\nend");
        lua.append("\nreturn c;");
        return lua.toString();
    }



    public String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }







}
