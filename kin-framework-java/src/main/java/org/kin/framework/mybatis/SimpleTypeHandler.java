package org.kin.framework.mybatis;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 一个简单的mybatis TypeHandler
 *
 * @param <CT> column value db java type, 即列字段值的数据库类型对应的java类型
 * @param <O>  column value java type
 * @author huangjianqin
 * @date 2021/1/14
 */
@SuppressWarnings("unchecked")
public abstract class SimpleTypeHandler<CT, O> implements TypeHandler<O> {
    @Override
    public void setParameter(PreparedStatement ps, int i, O parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public O getResult(ResultSet rs, String columnName) throws SQLException {
        return convertToJavaValue((CT) rs.getObject(columnName));
    }

    @Override
    public O getResult(ResultSet rs, int columnIndex) throws SQLException {
        return convertToJavaValue((CT) rs.getObject(columnIndex));
    }

    @Override
    public O getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convertToJavaValue((CT) cs.getObject(columnIndex));
    }

    /**
     * column java value => column db value
     */
    protected abstract CT convertToDbValue(O obj);

    /**
     * column db value => column java value
     */
    protected abstract O convertToJavaValue(CT columnValue);
}
