package com.company.autoconfigure.exceptionhandleryoutrack;

import com.company.exceptionhandleryoutrack.JrcConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({JrcConfiguration.class})
public class JrcAutoConfiguration {
}

