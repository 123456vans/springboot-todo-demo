package com.example.todo.mapper;

import com.example.todo.entity.Todo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TodoMapper {

    int insert(Todo todo);

    List<Todo> findByUserId(@Param("userId") Long userId);

    Todo findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countByUserId(@Param("userId") Long userId);

    long countByUserIdAndTitle(@Param("userId") Long userId, @Param("title") String title);

    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("status") Integer status);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
