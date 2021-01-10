package org.kin.framework.mybatis;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * entity dao 具体实现需要继承该类
 *
 * @author huangjianqin
 * @date 2021/1/10
 */
public abstract class DaoSupport<E, BM extends BaseMapper<E>> implements BaseDao<E, BM> {
    @Autowired
    protected BM mapper;

    /**
     * 返回mapper
     */
    @Override
    public BM mapper(){
        return mapper;
    }

    @Override
    public final int insert(E entity) {
        return mapper.insert(entity);
    }

    @Override
    public final int deleteById(Serializable id) {
        return mapper.deleteById(id);
    }

    @Override
    public final int deleteByMap(Map<String, Object> columnMap) {
        return mapper.deleteByMap(columnMap);
    }

    @Override
    public final int delete(Wrapper<E> queryWrapper) {
        return mapper.delete(queryWrapper);
    }

    @Override
    public final int deleteBatchIds(Collection<? extends Serializable> idList) {
        return mapper.deleteBatchIds(idList);
    }

    @Override
    public final int updateById(E entity) {
        return mapper.updateById(entity);
    }

    @Override
    public final int update(E entity, Wrapper<E> updateWrapper) {
        return mapper.update(entity, updateWrapper);
    }

    @Override
    public final E selectById(Serializable id) {
        return mapper.selectById(id);
    }

    @Override
    public final List<E> selectBatchIds(Collection<? extends Serializable> idList) {
        return mapper.selectBatchIds(idList);
    }

    @Override
    public final List<E> selectByMap(Map<String, Object> columnMap) {
        return mapper.selectByMap(columnMap);
    }

    @Override
    public final E selectOne(Wrapper<E> queryWrapper) {
        return mapper.selectOne(queryWrapper);
    }

    @Override
    public final Integer selectCount(Wrapper<E> queryWrapper) {
        return mapper.selectCount(queryWrapper);
    }

    @Override
    public final List<E> selectList(Wrapper<E> queryWrapper) {
        return mapper.selectList(queryWrapper);
    }

    @Override
    public final List<Map<String, Object>> selectMaps(Wrapper<E> queryWrapper) {
        return mapper.selectMaps(queryWrapper);
    }

    @Override
    public final List<Object> selectObjs(Wrapper<E> queryWrapper) {
        return mapper.selectObjs(queryWrapper);
    }

    @Override
    public final <E1 extends IPage<E>> E1 selectPage(E1 page, Wrapper<E> queryWrapper) {
        return mapper.selectPage(page, queryWrapper);
    }

    @Override
    public final <E1 extends IPage<Map<String, Object>>> E1 selectMapsPage(E1 page, Wrapper<E> queryWrapper) {
        return mapper.selectMapsPage(page, queryWrapper);
    }
}
