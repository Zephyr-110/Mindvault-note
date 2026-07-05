package org.example.community.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.community.favorite.entity.Favorite;


@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
