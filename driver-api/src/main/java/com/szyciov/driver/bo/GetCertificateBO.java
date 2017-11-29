package com.szyciov.driver.bo;

import com.szyciov.base.BaseBO;

import java.util.List;

/**
 * Created by lishuai on 2017/10/19.
 */
public class GetCertificateBO extends BaseBO {
    private List<String> urls; // 凭证列表

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
