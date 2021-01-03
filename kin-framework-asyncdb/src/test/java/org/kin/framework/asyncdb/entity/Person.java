package org.kin.framework.asyncdb.entity;

import org.kin.framework.asyncdb.AsyncDbEntity;
import org.kin.framework.asyncdb.DbSynchronzierClass;
import org.kin.framework.asyncdb.dao.PersonSynchronizer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author huangjianqin
 * @date 2021/1/2
 */
@Entity
@DbSynchronzierClass(type = PersonSynchronizer.class)
public class Person extends AsyncDbEntity<Integer> {
    /** id */
    @Id
    private int id;
    /** 名字 */
    private String name;
    /** 性别, 1男2女 */
    @Column(columnDefinition = "tinyInt")
    private byte gender;

    public static Person male(int id, String name) {
        return of(id, name, (byte) 1);
    }

    public static Person female(int id, String name) {
        return of(id, name, (byte) 2);
    }

    public static Person of(int id, String name, byte gender) {
        Person inst = new Person();
        inst.id = id;
        inst.name = name;
        inst.gender = gender;
        return inst;
    }

    @Override
    public Integer getPrimaryKey() {
        return id;
    }

    //setter && getter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender=" + gender +
                '}';
    }
}
