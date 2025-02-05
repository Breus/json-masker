package notdefaultpackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        log.atInfo().setMessage("test message").addKeyValue("iban", "1234").addKeyValue("alex", 420).addKeyValue("artur", "hello").log();
    }
}
