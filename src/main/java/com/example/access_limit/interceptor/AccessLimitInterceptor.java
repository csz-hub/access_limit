package com.example.access_limit.interceptor;

import com.example.access_limit.annotation.AccessLimit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {
    @Resource
    private RedisTemplate<String, Integer> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // Handler 是否为 HandlerMethod 实例
        if (handler instanceof HandlerMethod) {
            // 强转
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 获取方法
            Method method = handlerMethod.getMethod();
            // 是否有AccessLimit注解
            if (!method.isAnnotationPresent(AccessLimit.class)) {
                return true;
            }
            // 获取注解内容信息
            AccessLimit accessLimit = method.getAnnotation(AccessLimit.class);
            if (accessLimit == null) {
                return true;
            }
            int times = accessLimit.times();//请求次数
            int second = accessLimit.second();//请求时间范围
            //根据 API 限流
            String key = request.getRequestURI();
            //刷新限流时间
            String key2 = "k2";
            //根据key获取已请求次数
            Integer maxTimes = redisTemplate.opsForValue().get(key);
            Integer value = redisTemplate.opsForValue().get(key2);
            if (maxTimes == null || value == null) {
                //set时一定要加过期时间
                redisTemplate.opsForValue().set(key, 1, second, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(key2, 0, second, TimeUnit.SECONDS);
            } else if (maxTimes < times) {
                redisTemplate.opsForValue().set(key, maxTimes + 1, second, TimeUnit.SECONDS);
            } else {
                //超出访问次数
                render(response);
                return false;
            }
        }

        return true;
    }

    private void render(HttpServletResponse response)throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream out = response.getOutputStream();
        String str = "系统繁忙,请稍后再试。";
        out.write(str.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}
