package com.rental.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/** 对外采用从 1 开始的分页，避免直接绑定不可实例化的 PageRequest。 */
@Data
@AllArgsConstructor
public class Page<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
    public static int offset(int page, int size) {
        if (page < 1 || page > 100000 || size < 1 || size > 100) throw BusinessException.bad("page 为 1—100000，size 为 1—100");
        return (page - 1) * size;
    }
}
