### Plan for today

1. Logback encoder
2. Logback config class
3. Make decorator around `StructuredLogEncoder`

1.3+
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>${CONSOLE_LOG_THRESHOLD}</level>
        </filter>
        <encoder class="org.springframework.boot.logging.logback.StructuredLogEncoder">
            <format>${CONSOLE_LOG_STRUCTURED_FORMAT}</format>
            <charset>${CONSOLE_LOG_CHARSET}</charset>
        </encoder>
    </appender>
</configuration>
```
