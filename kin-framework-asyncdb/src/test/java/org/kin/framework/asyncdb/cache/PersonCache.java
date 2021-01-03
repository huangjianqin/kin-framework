package org.kin.framework.asyncdb.cache;

import org.kin.framework.asyncdb.SoftValueEntityCache;
import org.kin.framework.asyncdb.dao.PersonSynchronizer;
import org.kin.framework.asyncdb.entity.Person;
import org.springframework.stereotype.Component;

/**
 * @author huangjianqin
 * @date 2021/1/2
 */
@Component
public class PersonCache extends SoftValueEntityCache<Integer, Person, PersonSynchronizer> {
    @Override
    protected Person initEntity(Integer pk, Object... args) {
        Person person = new Person();
        person.setId(pk);
        return person;
    }
}
