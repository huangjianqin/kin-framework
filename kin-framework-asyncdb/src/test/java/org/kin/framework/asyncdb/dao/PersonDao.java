package org.kin.framework.asyncdb.dao;

import org.kin.framework.asyncdb.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author huangjianqin
 * @date 2021/1/2
 */
@Repository
public interface PersonDao extends JpaRepository<Person, Integer> {
}
