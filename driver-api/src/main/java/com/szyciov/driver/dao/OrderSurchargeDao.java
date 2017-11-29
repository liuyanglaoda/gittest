package com.szyciov.driver.dao;

import javax.annotation.Resource;

import org.springframework.stereotype.Repository;

import com.szyciov.driver.mapper.OrderSurchargeMapper;
import com.szyciov.lease.entity.LeDriverpaymanagement;
import com.szyciov.op.entity.OpDriverpaymanagement;
import com.szyciov.op.entity.OpOrderSurcharge;
import com.szyciov.op.entity.OpTaxiOrderSurcharge;
import com.szyciov.org.entity.OrgOrderSurcharge;

/**
 * @ClassName OrderSurchargeDao 
 * @author Efy Shu
 * @Description 附加费操作DAO
 * @date 2017年10月13日 下午5:16:10 
 */
@Repository("OrderSurchargeDao")
public class OrderSurchargeDao {
	public OrderSurchargeDao() {
		
	}
	
	private OrderSurchargeMapper mapper;

	@Resource
	public void setMapper(OrderSurchargeMapper mapper) {
		this.mapper = mapper;
	}
	
	/**
	 * 查询机构订单附加费明细
	 * @param orderno
	 * @return
	 */
	public OrgOrderSurcharge getOrgOrderSurcharge(String orderno){
		return mapper.getOrgOrderSurcharge(orderno);
	}
	
	/**
	 * 查询个人订单附加费明细
	 * @param orderno
	 * @return
	 */
	public OpOrderSurcharge getOpOrderSurcharge(String orderno){
		return mapper.getOpOrderSurcharge(orderno);
	}
	
	/**
	 * 查询运管出租车订单附加费明细
	 * @param orderno
	 * @return
	 */
	public OpTaxiOrderSurcharge getOpTaxiOrderSurcharge(String orderno){
		return mapper.getOpTaxiOrderSurcharge(orderno);
	}
	/**
	 * 查询运管司机垫付记录
	 * @param orderno
	 * @return
	 */
	public OpDriverpaymanagement getOpDriverPayManagement(String orderno){
		return mapper.getOpDriverPayManagement(orderno);
	}
	/**
	 * 查询租赁司机垫付记录
	 * @param orderno
	 * @return
	 */
	public LeDriverpaymanagement getLeDriverPayManagement(String orderno){
		return mapper.getLeDriverPayManagement(orderno);
	}
	/**
	 * 保存机构订单附加费明细
	 * @param surcharge
	 * @return
	 * @see {@linkplain OrgOrderSurcharge}
	 */
	public boolean saveOrgOrderSurcharge(OrgOrderSurcharge surcharge){
		mapper.saveOrgOrderSurcharge(surcharge);
		return true;
	}
	
	/**
	 * 保存个人订单附加费明细
	 * @param surcharge
	 * @return
	 * @see {@linkplain OpOrderSurcharge}
	 */
	public boolean saveOpOrderSurcharge(OpOrderSurcharge surcharge){
		mapper.saveOpOrderSurcharge(surcharge);
		return true;
	}
	
	/**
	 * 保存运管出租车订单附加费明细
	 * @param surcharge
	 * @return
	 * @see {@linkplain OpTaxiOrderSurcharge}
	 */
	public boolean saveOpTaxiOrderSurcharge(OpTaxiOrderSurcharge surcharge){
		mapper.saveOpTaxiOrderSurcharge(surcharge);
		return true;
	}
	/**
	 * 保存租赁端司机垫付记录
	 * @param payManageMent
	 * @return
	 * @see {@linkplain LeDriverpaymanagement}
	 */
	public boolean saveLeDriverPayManagement(LeDriverpaymanagement payManageMent){
		mapper.saveLeDriverPayManagement(payManageMent);
		return true;
	}
	/**
	 * 保存运管端司机垫付记录
	 * @param payManageMent
	 * @return
	 * @see {@linkplain OpDriverpaymanagement}
	 */
	public boolean saveOpDriverPayManagement(OpDriverpaymanagement payManageMent){
		mapper.saveOpDriverPayManagement(payManageMent);
		return true;
	}
}
