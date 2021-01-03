package org.kin.framework.asyncdb;

import org.kin.framework.asyncdb.cache.PersonCache;
import org.kin.framework.asyncdb.entity.Person;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author huangjianqin
 * @date 2021/1/2
 */
@SpringBootApplication
@EnableAsyncDb
public class AsyncDbTest {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AsyncDbTest.class, args);
        run(context);
        context.close();
    }

    private static void run(ConfigurableApplicationContext context) {
        PersonCache personCache = context.getBean(PersonCache.class);

        Person person = personCache.load(1);
        System.out.println(person);

        person.setName("嘻嘻");
        person.setGender((byte) 1);
        person.update();

        personCache.invalidAndDelete(1);
        System.out.println(person);
        person = personCache.load(1);
        System.out.println(person);
    }
}
