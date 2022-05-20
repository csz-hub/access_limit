### 使用Java 注解+Redis实现对接口访问限流控制功能

#### 代码说明:

###### 1. 自定义接口限流注解:

```java
@Inherited
@Documented
@Target({ElementType.FIELD,ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessLimit {

    //指定second 时间内 API请求次数
    int times() default 10;

    // 请求次数的指定时间范围  秒数(redis数据过期时间)
    int second() default 10;
}
```

###### 2. 自定义限流拦截器:

```java
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
```

###### 3. 注册拦截器：

```java
@Configuration
public class ApplicationConfig implements WebMvcConfigurer {
    @Autowired
    private AccessLimitInterceptor accessLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //API限流拦截
        registry.addInterceptor(accessLimitInterceptor).addPathPatterns("/**");
    }
}
```

###### 4. 测试接口：

```java
@RestController
public class AccessLimitController {

    @RequestMapping("/limit")
    @AccessLimit(times = 5)
    public String limit() {
        return "success";
    }
}
```

测试地址：http://120.24.38.128:8080/limit

限制了10秒内处理5个请求。
