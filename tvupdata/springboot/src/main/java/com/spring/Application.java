package com.spring;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

@SpringBootApplication
public class Application implements CommandLineRunner, WebMvcConfigurer {

//    @Autowired
//    DataSource dataSource;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>服务启动执行");
//        showConnection();
//        Locker.getInstance(jdbcTemplate, transactionManager);
//        bootStrap.init();
        System.out.println(">>>>>>>>>>>>>>>>>服务启动完成");
    }

//    private void showConnection() throws SQLException {
//        log.info(">>>>>>>>>>>>>>>>>dataSource:{}", dataSource.getClass().getName());
//        Connection connection = dataSource.getConnection();
//        log.info(">>>>>>>>>>>>>>>>>connection:{}", connection.toString());
//    }

    /**
     * 修改默认JSON转换器为FastJson
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 需要定义一个convert转换消息的对象;
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        // 添加fastJson的配置信息;
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
        // 全局时间配置
        fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
//      fastJsonConfig.setCharset(Charset.forName("UTF-8"));
//      //处理中文乱码问题
//      List<MediaType> fastMediaTypes = new ArrayList<>();
//      fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
//      //在convert中添加配置信息.
//      fastConverter.setSupportedMediaTypes(fastMediaTypes);
        fastConverter.setFastJsonConfig(fastJsonConfig);

        converters.add(fastConverter);
    }
}
