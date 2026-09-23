package com.example.ordermanage.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

public record PageResult<T>(long total, List<T> list) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}
