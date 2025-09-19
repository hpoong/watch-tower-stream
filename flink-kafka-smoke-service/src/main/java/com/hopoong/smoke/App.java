package com.hopoong.smoke;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App implements CommandLineRunner {
	public static void main(String[] args) { SpringApplication.run(App.class, args); }
	@Override public void run(String... args) throws Exception {
		FlinkKafkaSmokeService.main(args); // 여기서 Flink 잡 실행
	}
}