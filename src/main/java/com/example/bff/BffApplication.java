package com.example.bff;

import com.example.bff.config.AppConfig;
import com.example.bff.config.RSAKeyRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties({RSAKeyRecord.class, AppConfig.class})
@SpringBootApplication
@EnableScheduling
public class BffApplication {

	public static void main(String[] args) {SpringApplication.run(BffApplication.class, args);
	}

}
