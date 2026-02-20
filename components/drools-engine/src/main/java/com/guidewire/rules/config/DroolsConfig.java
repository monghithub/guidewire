package com.guidewire.rules.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Configuration
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    private static final String RULES_PATH = "rules/";

    @Bean
    public KieServices kieServices() {
        return KieServices.Factory.get();
    }

    @Bean
    public KieContainer kieContainer(KieServices kieServices) throws IOException {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] ruleFiles = resolver.getResources("classpath:" + RULES_PATH + "**/*.drl");

        for (Resource ruleFile : ruleFiles) {
            String path = RULES_PATH + ruleFile.getFilename();
            log.info("Loading DRL rule file: {}", path);
            kieFileSystem.write(ResourceFactory.newClassPathResource(path, "UTF-8"));
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(
                    "Error building Drools rules: " + kieBuilder.getResults().getMessages(Message.Level.ERROR));
        }

        if (kieBuilder.getResults().hasMessages(Message.Level.WARNING)) {
            log.warn("Drools build warnings: {}", kieBuilder.getResults().getMessages(Message.Level.WARNING));
        }

        KieModule kieModule = kieBuilder.getKieModule();
        KieRepository kieRepository = kieServices.getRepository();
        kieRepository.addKieModule(kieModule);

        return kieServices.newKieContainer(kieRepository.getDefaultReleaseId());
    }

    // Note: KieSession is NOT a singleton bean. Each rule evaluation creates
    // a new session from kieContainer in RulesService to ensure thread safety.
}
