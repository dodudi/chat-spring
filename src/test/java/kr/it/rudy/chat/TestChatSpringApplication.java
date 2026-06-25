package kr.it.rudy.chat;

import org.springframework.boot.SpringApplication;

public class TestChatSpringApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChatSpringApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
