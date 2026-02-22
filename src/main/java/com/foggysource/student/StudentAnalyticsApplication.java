package com.foggysource.student;

import com.foggyframework.core.annotates.EnableFoggyFramework;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFoggyFramework(bundleName = "student-analytics")
public class StudentAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentAnalyticsApplication.class, args);
    }
}
