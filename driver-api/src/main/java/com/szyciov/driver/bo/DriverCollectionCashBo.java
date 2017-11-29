package com.szyciov.driver.bo;

import com.szyciov.base.BaseBO;

/**
 * @ClassName DriverCollectionCashBo 
 * @author Efy Shu
 * @Description TODO(这里用一句话描述这个类的作用) 
 * @date 2017年10月19日 下午2:44:31 
 */
public class DriverCollectionCashBo extends BaseBO{
	/**付款链接*/
	private String url;

	/**  
	 * 获取付款链接  
	 * @return url 付款链接  
	 */
	public String getUrl() {
		return url;
	}
	

	/**  
	 * 设置付款链接  
	 * @param url 付款链接  
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}
