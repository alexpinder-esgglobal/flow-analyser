package com.esgglobal.flowmo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.co.utilisoft.v8model.json.V8ModelV8JsonService;

@Configuration
public class MainConfig {

  @Value("${app.v8rest.url}")
  String v8RestUrl;

  @Bean
  public FlowAnalyserV8ModelV8JsonService v8ModelService() {
    return new FlowAnalyserV8ModelV8JsonService(v8RestUrl);
  }
}
