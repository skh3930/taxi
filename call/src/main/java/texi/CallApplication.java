package texi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import texi.config.KafkaProcessor;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableBinding(KafkaProcessor.class)
@EnableFeignClients
public class CallApplication {
    public static ApplicationContext applicationContext;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(CallApplication.class, args);
    }
}
