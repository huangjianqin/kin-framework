package org.kin.framework.asyncdb.dao;

import org.kin.framework.asyncdb.HibernateSynchronizer;
import org.kin.framework.asyncdb.entity.Person;
import org.springframework.stereotype.Component;

/**
 * @author huangjianqin
 * @date 2021/1/2
 */
@Component
public class PersonSynchronizer extends HibernateSynchronizer<Integer, Person, PersonDao> {
}
