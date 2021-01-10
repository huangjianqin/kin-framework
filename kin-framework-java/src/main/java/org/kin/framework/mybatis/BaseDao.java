package org.kin.framework.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * entity dao需要继承该接口
 *
 * @author huangjianqin
 * @date 2021/1/10
 */
public interface BaseDao<E, BM extends BaseMapper<E>> extends BaseMapper<E>{
    /**
     * @return mapper实现
     */
    BM mapper();
}
