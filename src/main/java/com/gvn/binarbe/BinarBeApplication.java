package com.gvn.binarbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cache.annotation.EnableCaching
public class BinarBeApplication {

  public static void main(String[] args) {
    SpringApplication.run(BinarBeApplication.class, args);
  }
}
