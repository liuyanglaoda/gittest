package com.szyciov.driver.mapper;

import org.apache.ibatis.annotations.Param;

import com.szyciov.lease.entity.LeDriverpaymanagement;
import com.szyciov.op.entity.OpDriverpaymanagement;
import com.szyciov.op.entity.OpOrderSurcharge;
import com.szyciov.op.entity.OpTaxiOrderSurcharge;
import com.szyciov.org.entity.OrgOrderSurcharge;

/**
 * @ClassName OrgOrderSurchargeMapper 
 * @author Efy Shu
 * @Description TODO(这里用一句话描述这个类的作用) 
 * @date 2017年10月13日 下午5:17:22 
 */
public interface OrderSurchargeMapper {
	public OrgOrderSurcharge getOrgOrderSurcharge(@Param("orderno") String orderno);
	public OpOrderSurcharge getOpOrderSurcharge(@Param("orderno") String orderno);
	public OpTaxiOrderSurcharge getOpTaxiOrderSurcharge(@Param("orderno") String orderno);
	public OpDriverpaymanagement getOpDriverPayManagement(@Param("orderno") String orderno);
	public LeDriverpaymanagement getLeDriverPayManagement(@Param("orderno") String orderno);
	
	public void saveOrgOrderSurcharge(OrgOrderSurcharge surcharge);
	public void saveOpOrderSurcharge(OpOrderSurcharge surcharge);
	public void saveOpTaxiOrderSurcharge(OpTaxiOrderSurcharge surcharge);
	
	public void saveOpDriverPayManagement(OpDriverpaymanagement payManageMent);
	public void saveLeDriverPayManagement(LeDriverpaymanagement payManageMent);
}

