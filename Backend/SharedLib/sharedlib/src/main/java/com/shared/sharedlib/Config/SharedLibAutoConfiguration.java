package com.shared.sharedlib.Config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "com.shared.sharedlib")
public class SharedLibAutoConfiguration {
    // This class enables autoconfiguration of shared library components
    // The @ComponentScan ensures that shared library components are automatically discovered
}