package com.example.ordermanage.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageParam {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private int page = DEFAULT_PAGE;
    private int pageSize = DEFAULT_PAGE_SIZE;

    public void validate() {
        if (page < 1) {
            throw new BizException(Err.BAD_REQUEST, Err.PAGE_INVALID);
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BizException(Err.BAD_REQUEST, Err.PAGE_INVALID);
        }
    }
}
