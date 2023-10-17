package com.example.train.common.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public class PageReq {

    @NotNull(message="【页码】不能为空")
    private Integer Page;

    @NotNull(message = "【每页条数】不能为空")
    @Max(value = 100,message = "【每页条数】最多为100")
    private Integer size;

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PageReq{");
        sb.append("Page=").append(Page);
        sb.append(", size=").append(size);
        sb.append('}');
        return sb.toString();
    }

    public Integer getPage() {
        return Page;
    }

    public void setPage(Integer page) {
        Page = page;
    }
}
