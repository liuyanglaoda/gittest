package com.szyciov.driver.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.supervision.enums.CommandEnum;
import com.supervision.enums.InterfaceType;
import com.supervision.service.OperationMqService;
import com.supervision.service.OrderMqService;
import com.szyciov.annotation.NeedRelease;
import com.szyciov.annotation.ValidateRule;
import com.szyciov.base.BaseBO;
import com.szyciov.driver.base.BaseService;
import com.szyciov.driver.bo.DriverCollectionCashBo;
import com.szyciov.driver.bo.GetCertificateBO;
import com.szyciov.driver.dao.AccountDao;
import com.szyciov.driver.dao.OrderDao;
import com.szyciov.driver.dao.OrderSurchargeDao;
import com.szyciov.driver.dao.TaxiMainFuncDao;
import com.szyciov.driver.entity.DriverMessage;
import com.szyciov.driver.entity.OrderInfoDetail;
import com.szyciov.driver.entity.OrderStatistics;
import com.szyciov.driver.entity.PubDriverNews;
import com.szyciov.driver.enums.DriverMessageEnum;
import com.szyciov.driver.enums.DriverState;
import com.szyciov.driver.enums.OrderListEnum;
import com.szyciov.driver.enums.OrderState;
import com.szyciov.driver.enums.PayState;
import com.szyciov.driver.enums.PayUtilEnum;
import com.szyciov.driver.enums.PurseEnum;
import com.szyciov.driver.enums.ReviewState;
import com.szyciov.driver.param.CertificateUploadParam;
import com.szyciov.driver.param.ChangeOrderStateParam;
import com.szyciov.driver.param.DeleteOrgOrderfileParam;
import com.szyciov.driver.param.DriverCollectionCashParam;
import com.szyciov.driver.param.DriverMessageParam;
import com.szyciov.driver.param.GetCertificateListParam;
import com.szyciov.driver.param.NewsParam;
import com.szyciov.driver.param.OrderCostParam;
import com.szyciov.driver.param.OrderLineParam;
import com.szyciov.driver.param.OrderListParam;
import com.szyciov.driver.param.OrderStatisticsParam;
import com.szyciov.driver.param.OrgOrderfileParam;
import com.szyciov.driver.util.PayUtil;
import com.szyciov.dto.OrderCacheInfoDTO;
import com.szyciov.dto.PayInfoDto;
import com.szyciov.dto.driver.ConfirmCostDto;
import com.szyciov.dto.driver.DriverCollectionCashDto;
import com.szyciov.entity.AbstractOrder;
import com.szyciov.entity.DataStatus;
import com.szyciov.entity.NewsState;
import com.szyciov.entity.OrderCost;
import com.szyciov.entity.OrderReviewParam;
import com.szyciov.entity.PayMethod;
import com.szyciov.entity.PubDriver;
import com.szyciov.entity.PubDriverTradingrecord;
import com.szyciov.entity.PubJpushlog;
import com.szyciov.entity.Retcode;
import com.szyciov.enums.OrderEnum;
import com.szyciov.enums.OrderReviewEnum;
import com.szyciov.enums.PubJpushLogEnum;
import com.szyciov.enums.RedisKeyEnum;
import com.szyciov.lease.entity.LeDriverpaymanagement;
import com.szyciov.lease.entity.LeLeasescompany;
import com.szyciov.lease.entity.OrgOrganCompanyRef;
import com.szyciov.lease.entity.PubCityAddr;
import com.szyciov.message.OrderMessage;
import com.szyciov.message.TaxiOrderMessage;
import com.szyciov.op.entity.OpDriverpaymanagement;
import com.szyciov.op.entity.OpOrder;
import com.szyciov.op.entity.OpOrderSurcharge;
import com.szyciov.op.entity.OpPlatformInfo;
import com.szyciov.op.entity.OpTaxiOrder;
import com.szyciov.op.entity.OpTaxiOrderSurcharge;
import com.szyciov.org.entity.OrgOrder;
import com.szyciov.org.entity.OrgOrderSurcharge;
import com.szyciov.param.BaiduApiQueryParam;
import com.szyciov.param.OrderApiParam;
import com.szyciov.param.PubPushLogParam;
import com.szyciov.passenger.util.MessageUtil;
import com.szyciov.util.DriverCacheUtil;
import com.szyciov.util.FileUtil;
import com.szyciov.util.GUIDGenerator;
import com.szyciov.util.JedisUtil;
import com.szyciov.util.StringUtil;
import com.szyciov.util.SupervisionMessageUtil;
import com.szyciov.util.SystemConfig;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


@Service("OrderService")
public class OrderService extends BaseService{
	/**
	 *依赖
	 */
	private OrderDao dao;
	private OrderSurchargeDao osDao;
	private TaxiMainFuncDao taximainDao;
	private String fileserver = SystemConfig.getSystemProperty("fileserver") + "/";
	
	//使用ThreadLocal,避免并发时出现数据被篡改的问题
	/**当前订单*/
	@NeedRelease
	private ThreadLocal<AbstractOrder> order = new ThreadLocal<AbstractOrder>();
	/**当前订单用车类型*/
	@NeedRelease
	private ThreadLocal<OrderEnum> usetype = new ThreadLocal<OrderEnum>();
	/**当前订单订单类型*/
	@NeedRelease
	private ThreadLocal<OrderEnum> ordertype = new ThreadLocal<OrderEnum>();
	/**运管平台信息*/
	@NeedRelease
	private ThreadLocal<OpPlatformInfo> platformInfo = new ThreadLocal<OpPlatformInfo>();
	/**租赁公司信息*/
	@NeedRelease
	private ThreadLocal<LeLeasescompany> leasescompany = new ThreadLocal<LeLeasescompany>();
	
	@Resource(name = "OrderDao")
	public void setDao(OrderDao dao) {
		this.dao = dao;
	}
	
	@Resource(name = "AccountDao")
	public void setDao(AccountDao accdao) {
		this.accdao = accdao;
	}
    @Resource(name="OrderSurchargeDao")
    public void setDao(OrderSurchargeDao osDao) {
    	this.osDao = osDao;
    }
    @Resource(name="TaxiMainFuncDao")
    public void setDao(TaxiMainFuncDao taximainDao) {
    	this.taximainDao = taximainDao;
    }
	@Autowired
	private OperationMqService operationMqService;

	@Autowired
	private OrderMqService orderMqService;
	
	@Autowired
	private SupervisionMessageUtil supervisionMessageUtil;

	
	/**
	 * 获取司机消息
	 * @param param
	 * @return
	 */
	public JSONObject pollMessage(NewsParam param){
		String[] require = new String[]{ };
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		
		//保存司机ID,方便日志输出
		param.setDriverid(driver.get().getId());
		JSONObject result = doPollMessage(param);
		
		return result;
	}
	
	/**
	 * 获取订单列表
	 * @param param
	 * @return
	 */
	public JSONObject getOrderList(OrderListParam param){
		String[] require = new String[]{"type"};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		JSONObject result = new JSONObject();
		PubDriver pd = driver.get();
		param.setDriverid(pd.getId());
		List<OrderInfoDetail> list = dao.getOrderList(param);
		JSONArray orderList = new JSONArray();
		OrderInfoDetail serviceOrder = null;
		for(OrderInfoDetail oid : list){
			if(OrderState.INSERVICE.state.equals(oid.getStatus())){
				serviceOrder = oid;
				continue;
			}
			orderList.add(convertOID(oid,false));
		}
		if(serviceOrder != null){
			orderList.add(0, convertOID(serviceOrder,false));
		}
		result.put("count", list.size());
		result.put("orders", orderList);
		return result;
	}
	
	/**
	 * 获取今日订单
	 * @param param
	 * @return
	 */
	public JSONObject getTodayOrders(OrderListParam param){
		String[] require = new String[]{"todaytype","starttime","endtime"};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		JSONObject result = new JSONObject();
		PubDriver pd = driver.get();
		param.setDriverid(pd.getId());
		List<OrderInfoDetail> list = dao.getOrderList(param);
		JSONArray orderList = new JSONArray();
		OrderInfoDetail serviceOrder = null;
		for(OrderInfoDetail oid : list){
			if(OrderState.INSERVICE.state.equals(oid.getStatus())){
				serviceOrder = oid;
				continue;
			}
			orderList.add(convertOID(oid,false));
		}
		if(serviceOrder != null){
			orderList.add(0, convertOID(serviceOrder,false));
		}
		int totalCount = dao.getOrderTotalCount(param);
		result.put("totalcount", totalCount);
		result.put("count", list.size());
		result.put("orders", orderList);
		return result;
	}
	
	/**
	 * 改变订单状态
	 * @param param
	 * @return
	 */
	public JSONObject changeOrderState(ChangeOrderStateParam param){
		String[] require = new String[]{"usetype","ordertype","orderno","orderstatus","orderstate"};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		logger.info("订单"+param.getOrderno()+"本次请求参数:" + param.getOrderstatus() + ",时间:" + StringUtil.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		JSONObject result = new JSONObject();
		PubDriver pd = driver.get();
		OrderApiParam oap = new OrderApiParam();
		oap.setOrderno(param.getOrderno());
		oap.setUsetype(param.getUsetype());
		oap.setOrdertype(param.getOrdertype());
		OrderInfoDetail oid = dao.getOrderInfoById(oap);
		oap.setToken(param.getToken());
		oap.setOrderstate(param.getOrderstatus());
		oap.setOrderprop(oid.getOrderprop());
		OrderState paramState = OrderState.getByCode(param.getOrderstatus());
		result  =  OrderState.WAITSTART.equals(paramState) ? takingOrder(oid, pd,oap) : //抢单
						OrderState.START.equals(paramState) ? orderStart(oid, pd,param) :          //已出发
						OrderState.ARRIVAL.equals(paramState) ? orderArrival(oid, pd,param) :   //已抵达
//						OrderState.PICKUP.equals(paramState) ? orderPickup(oid) :                        //接到乘客(不存在接到乘客了)
						OrderState.INSERVICE.equals(paramState) ? orderInservice(oid,pd, param) :              //服务中
						OrderState.WAITMONEY.equals(paramState) ? orderEnd(oid, pd, oap, param) : null;  //服务结束:默认返回null
		//异常状态码则直接返回
		if(result != null && result.has("status") && result.getInt("status") != Retcode.OK.code) return result;
		//如果是抢单使用另外的通知
		if(OrderState.WAITSTART.equals(paramState)) return result;
		//司机变更状态需要通知(完成时不重新调用完成推送)
		if(oid.getOrderprop() == 0 && !OrderState.WAITMONEY.equals(paramState)){
			OrgOrder orgOrder = dao.getOrgOrder(oid.getOrderno());
			OrderMessage ordermessage = new OrderMessage(orgOrder,OrderMessage.ORDERHINT);
			MessageUtil.sendMessage(ordermessage);
		}else if(oid.getOrderprop() == 1 && !OrderState.WAITMONEY.equals(paramState)){
			OpOrder opOrder = dao.getOpOrder(oid.getOrderno());
			OrderMessage ordermessage = new OrderMessage(opOrder,OrderMessage.ORDERHINT);
			MessageUtil.sendMessage(ordermessage);
		}
		result.put("orderstatus", oid.getStatus());
		result.put("orderno", oid.getOrderno());
		result.put("usetype", oid.getUsetype());
		result.put("ordertype", oid.getType());
		
		removeOrderMessage(oap); //无论改变什么状态都做一次提醒任务删除
		return result;
	}
	
	/**
	 * 保存时间节点司机所在地(先调用此方法再更新订单)
	 * @param oid
	 * @param cosp
	 */
	private void saveLocInTime(OrderInfoDetail oid,ChangeOrderStateParam cosp){
		try {
			//如果上传了经纬度,则记录地址信息
			if(cosp.getLng() == 0 || cosp.getLat() == 0) return;
			JSONObject result = new JSONObject();
			BaiduApiQueryParam baqp = new BaiduApiQueryParam();
			baqp.setOrderStartLng(cosp.getLng());
			baqp.setOrderStartLat(cosp.getLat());
			String carserviceApiUrl = SystemConfig.getSystemProperty("carserviceApiUrl");
			result = templateHelper.dealRequestWithFullUrlToken(
					carserviceApiUrl+"/BaiduApi/GetAddress", 
					HttpMethod.POST, 
					null, 
					baqp, 
					JSONObject.class);
			if(result.getInt("status") != Retcode.OK.code) {
				logger.info("经纬度反查失败,仅保存APP上传经纬度");
				oid.setLng(cosp.getLng());
				oid.setLat(cosp.getLat());
				dao.updateOrder(oid);
				return;
			}
			PubCityAddr pca = dao.getPubCity(result.getString("city"));
			oid.setLng(result.getDouble("lng"));
			oid.setLat(result.getDouble("lat"));
			oid.setCityintime(pca == null ? null : pca.getId());
			oid.setAddressintime(result.getString("address").isEmpty()?null:result.getString("address"));
			dao.updateOrder(oid);
		} catch (Exception e) {
			logger.error("保存改变订单状态节点位置出错",e);
		}
	}
	
	/**
	 * 运营平台追加流程
	 * @param oid
	 * @param pd
	 */
	private void orderEndFollowUp(OrderInfoDetail oid,PubDriver pd){
//		营运结束
		operationMqService.operationArrival(oid.getOrderno(),oid.getUsetype(),oid.getType());
//		如果是机构订单，并且是因公，触发营运支付
		if(!oid.getType().equals("4")&&oid.getUsetype().equals("0")){
			operationMqService.operationPay(oid.getOrderno(),oid.getUsetype(),oid.getType());
		}
		//司机定位->乘客下车
		Map<String, String> map = new HashMap<String, String>();
		map.put("driverId",pd.getId());
		map.put("longitude", String.valueOf(pd.getLng()));
		map.put("latitude", String.valueOf(pd.getLat()));
		map.put("orderId", oid.getOrderno());
		map.put("positionType", "CKXC");
		supervisionMessageUtil.sendToSupervision(InterfaceType.GPS, CommandEnum.DriverLocationInfo, map);
	}
	
	/**
	 *  司机订单数增加&重置司机状态(订单完成时调用)
	 * @param pd
	 */
	private void addDriverOrderCount(PubDriver pd){
		pd.setOrdercount(pd.getOrdercount()+1);
		pd.setUpdateTime(new Date());
		pd.setWorkstatus(DriverState.IDLE.code);
		accdao.updatePubDriver(pd);
	}
	
	/**
	 * 保存司机服务中订单
	 * @param oid
	 * @param pd
	 */
	private void saveOrUpdateInserviceOrder(OrderInfoDetail oid,PubDriver pd,boolean isupdate){
		String key = "OBD_MILGES_CURRENT_ORDER_"+pd.getId() + "_" + oid.getStatus();
		String inserviceOrderKey = "OBD_MILGES_CURRENT_ORDER_"+pd.getId()+"_"+OrderState.INSERVICE.state;
		String inserviceDriver = pd.getId() + "#";
		String driverKey = "DRIVER_INSERVICE_ORDER";
		try{
			JSONObject json = new JSONObject();
			json.put("orderno", oid.getOrderno());
			json.put("driverid", pd.getId());
			json.put("vehicleid", pd.getVehicleid());
			json.put("starttime", StringUtil.formatDate(oid.getStarttime(), StringUtil.DATE_TIME_FORMAT));
			json.put("orderstatus", oid.getStatus());
			//删除之前所有的服务中缓存,只保存当前一条(防止set时出错导致缓存没有清除)
			for(String tempKey : JedisUtil.getKeys(inserviceOrderKey)){
				JedisUtil.delKey(tempKey);
			}
			//删除之前所有的服务中缓存
			for (String value : JedisUtil.sMembers(driverKey)) {
				if(value.startsWith(inserviceDriver)) {
					JedisUtil.sRem(driverKey, value);
				}
			}
			if(isupdate){
				int second = 60 * 10; //十分钟
				json.put("endtime", StringUtil.formatDate(oid.getEndtime(), StringUtil.DATE_TIME_FORMAT));
				JedisUtil.setString(key, json.toString(),second);
			}else{
				JedisUtil.setString(key, json.toString());
				JedisUtil.sAdd(driverKey, pd.getId() + "#" + oid.getOrderno());
			}
		}catch(Exception e){
			logger.error("保存OBD轨迹失败",e);
			JedisUtil.delKey(key);
		}
	}
	
	/**
	 * 司机已出发处理逻辑
	 * @param oid
	 * @param pd
	 * @param cosp
	 * @return
	 */
	public JSONObject orderStart(OrderInfoDetail oid,PubDriver pd,ChangeOrderStateParam cosp){
		JSONObject result = new JSONObject();
		OrderListParam olp = new OrderListParam();
		olp.setType(1);
		olp.setDriverid(pd.getId());
		//如果存在需要更早出发的订单,则返回(不能跳单执行)
		List<OrderInfoDetail> list = dao.getOrderList(olp);
		if(list != null && !list.isEmpty()){
			for(OrderInfoDetail o : list){
				if(!o.getOrderno().equals(oid.getOrderno()) && o.getUsetime().before(oid.getUsetime())){
					result.put("status", Retcode.HASEARLIERORDER.code);
					result.put("message", Retcode.HASEARLIERORDER.msg);
					result.put("orderno", o.getOrderno());
					result.put("usetype", o.getUsetype());
					result.put("ordertype", o.getType());
					return result;
				}
			}
		}
		//如果出发接人时,司机已经处于服务中则提示2004
		if(DriverState.INSERVICE.code.equals(pd.getWorkstatus())){
			result.put("status", Retcode.ORDERNOTDONE.code);
			result.put("message", Retcode.ORDERNOTDONE.msg);
			return result;
		}
		if(!OrderState.WAITSTART.state.equals(oid.getStatus())){
			result.put("status", Retcode.INVALIDORDERSTATUS.code);
			result.put("message", Retcode.INVALIDORDERSTATUS.msg);
			return result;
		}
		oid.setStatus(OrderState.START.state);
		oid.setDeparturetime(new Date());
		//无论是否预约用车的单,司机只要出发了就属于服务中
		pd.setWorkstatus(DriverState.INSERVICE.code);
		pd.setUpdateTime(new Date());
		accdao.updatePubDriver(pd);
		saveLocInTime(oid,cosp);
		//司机状态缓存
		DriverCacheUtil.startOrder(pd.getId(), oid.getOrderno());
		//更新订单状态
		dao.updateOrder(oid);
		return new JSONObject();
	}
	
	/**
	 * 司机已抵达处理逻辑
	 * @param oid
	 * @param pd
	 * @param cosp
	 * @return
	 */
	public JSONObject orderArrival(OrderInfoDetail oid,PubDriver pd,ChangeOrderStateParam cosp){
		JSONObject result = new JSONObject();
		if(!OrderState.START.state.equals(oid.getStatus())){
			result.put("status", Retcode.INVALIDORDERSTATUS.code);
			result.put("message", Retcode.INVALIDORDERSTATUS.msg);
			return result;
		}
		oid.setStatus(OrderState.ARRIVAL.state);
		oid.setArrivaltime(new Date());
		saveLocInTime(oid,cosp);
		//更新订单状态
		dao.updateOrder(oid);
		return new JSONObject();
	}
	
	/**
	 * 接到乘客处理逻辑(已废弃)
	 * @param oid
	 * @deprecated
	 * @return
	 */
	public JSONObject orderPickup(OrderInfoDetail oid){
		oid.setStatus(OrderState.PICKUP.state);
		//更新订单状态
		dao.updateOrder(oid);
		return new JSONObject();
	}
	
	/**
	 * 开始服务处理逻辑
	 * @param oid
	 * @param cosp
	 * @return
	 */
	public JSONObject orderInservice(OrderInfoDetail oid, PubDriver pd,ChangeOrderStateParam cosp){
		oid.setStarttime(new Date());
		oid.setStatus(OrderState.INSERVICE.state);
		saveLocInTime(oid, cosp);
		//更新订单状态
		dao.updateOrder(oid);
		saveOrUpdateInserviceOrder(oid,pd,false);
		//司机状态缓存
		DriverCacheUtil.updateOrderStartTime(pd.getId(), oid.getOrderno(), oid.getStarttime());
		
		//营运出发
		operationMqService.operationDeparture(oid.getOrderno(),oid.getUsetype(),oid.getType());
		
		//司机定位->乘客上车
		Map<String, String> map = new HashMap<String, String>();
		map.put("driverId",pd.getId());
		map.put("longitude", String.valueOf(pd.getLng()));
		map.put("latitude", String.valueOf(pd.getLat()));
		map.put("orderId", oid.getOrderno());
		map.put("positionType", "CKSC");
		supervisionMessageUtil.sendToSupervision(InterfaceType.GPS, CommandEnum.DriverLocationInfo, map);
		return new JSONObject();
	}

	/**
	 * 订单确费接口
	 * @param dto
	 * @return
	 */
	public JSONObject confirmCost(ConfirmCostDto param){
		String[] require = new String[]{
				"usetype","ordertype","orderno","orderstatus",
				"rangefee","expresswayfee","bridgefee","parkingfee","otherfee"
				};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		//保存司机ID,方便日志输出
		param.setDriverid(driver.get().getId());
		doConfirmCost(param);
		doSaveSurcharge(param);
		//如果是个人垫付(机构因私)订单,确费时不保存垫付记录
		if(!(order.get() instanceof OrgOrder)
			|| !PayMethod.ADVANCE.code.equals(((OrgOrder)order.get()).getPaymethod())){
			doSaveDriverPayManagement(param);
			addDriverOrderCount(driver.get());
			//订单结束时,需要删除司机状态缓存
			DriverCacheUtil.endOrder(driver.get().getId(), param.getOrderno());
		}
		return new JSONObject();
	}
	
	/**
	 * 结束订单处理逻辑
	 * @param oid
	 * @param pd
	 * @param oap
	 * @param cosp
	 * @return
	 */
	public JSONObject orderEnd(OrderInfoDetail oid,PubDriver pd,OrderApiParam oap, ChangeOrderStateParam cosp){
		oid.setStatus(OrderState.WAITMONEY.state);   //一定不能删除,用来判断更新哪个坐标字段用的
		saveLocInTime(oid,cosp);
		JSONObject result = new JSONObject();
		//订单完成需要进行支付动作,由carservice完成
		result = templateHelper.dealRequestWithFullUrlToken(
				SystemConfig.getSystemProperty("carserviceApiUrl")+"/OrderApi/ChangeOrderState", 
				HttpMethod.POST, 
				null, 
				oap, 
				JSONObject.class);
		if(Retcode.OK.code != result.getInt("status")) return result;
		oid = dao.getOrderInfoById(oap);
		//保存|删除司机服务中订单(redis缓存,用来核查obd数据)
		saveOrUpdateInserviceOrder(oid,pd,true);
		//订单结束后续流程
		orderEndFollowUp(oid,pd);
		return result;
	}
	
	/**
	 * 订单已取消处理逻辑
	 * @return
	 */
	public JSONObject orderCancel(OrderInfoDetail oid, PubDriver pd){
		saveDriverHansUp(oid,pd);
		JSONObject result = new JSONObject();
		result.put("status", Retcode.ORDERISCANCEL.code);
		result.put("message", Retcode.ORDERISCANCEL.msg);
		return result;
	}
	
	/**
	 * 抢单时订单已进入人工处理逻辑
	 * @return
	 */
	public JSONObject orderMantic(OrderInfoDetail oid, PubDriver pd){
		saveDriverHansUp(oid, pd);
		JSONObject result = new JSONObject();
		result.put("status", Retcode.FAILED.code);
		result.put("message", "抢单失败.");
		return result;
	}
	
	/**
	 * 订单已被接走处理逻辑
	 * @return
	 */
	public JSONObject orderGone(OrderInfoDetail oid,PubDriver pd){
		saveDriverHansUp(oid, pd);
		JSONObject result = new JSONObject();
		result.put("status", Retcode.ORDERISGONE.code);
		result.put("message", Retcode.ORDERISGONE.msg);
		return result;
	}
	
	/**
	 * 更新订单信息为抢单司机
	 * @param oid
	 * @param pd
	 * @param param
	 * @return
	 */
	public JSONObject orderTaking(OrderInfoDetail oid,PubDriver pd,OrderApiParam param){
		pd.setLeasescompanyid(oid.getCompanyid());   //将司机的公司id设置为订单归属方,获取司机的实际车型
		pd = accdao.getPubDriverFactModel(pd);
		JSONObject result = new JSONObject();
		if(oid.getOrderprop() == 0){
			OrgOrder order = dao.getOrgOrder(oid.getOrderno());
			order.setFactmodel(pd.getOrgcartypeid());  //更新实际车型
			order.setOrdertime(new Date());
			order.setDriverid(pd.getId());
			order.setOrderstatus(OrderState.WAITSTART.state);
			order.setVehicleid(pd.getVehicleid());
			order.setPlateno(pd.getPlateno());
			order.setVehcbrandname(pd.getVehcbrandname());
			order.setVehclinename(pd.getVehclinename());
			order.setBelongleasecompany(pd.getLeasescompanyid());
			dao.updateOrgOrder(order);
		}else{
			OpOrder order = dao.getOpOrder(oid.getOrderno());
			order.setFactmodel(pd.getOpcartypeid());  //更新实际车型
			order.setOrdertime(new Date());
			order.setDriverid(pd.getId());
			order.setOrderstatus(OrderState.WAITSTART.state);
			order.setVehicleid(pd.getVehicleid());
			order.setPlateno(pd.getPlateno());
			order.setVehcbrandname(pd.getVehcbrandname());
			order.setVehclinename(pd.getVehclinename());
			order.setBelongleasecompany(pd.getLeasescompanyid());
			dao.updateOpOrder(order);
		}
		oid.setDriverid(pd.getId());  //设置司机ID,表示该订单抢单成功
		saveDriverHansUp(oid,pd);
		removeDriverMessage(oid,pd,param);
		sendTakingOrderMessage(oid,param);
		saveTakingOrderCache(oid,pd);
		result.put("order", convertOID(oid,true));
		try{
			orderMqService.orderSuccess(oid.getOrderno(),oid.getType(),oid.getUsetype());
    	}catch(Exception ex){
    		logger.error("订单成功推送失败:error{}|orderno:{},ordertype:{},usetype:{}",ex.getMessage(),oid.getOrderno(),oid.getType(),oid.getUsetype());
    	}

		return result;
	}
	
	/**
	 * 执行抢单流程
	 * @param oid
	 * @param pd
	 * @param param
	 * @return
	 */
	public JSONObject takingOrder(OrderInfoDetail oid,PubDriver pd,OrderApiParam param){
		JSONObject result = new JSONObject();
		boolean flag = JedisUtil.lock(oid.getOrderno()); //锁定该订单
		if(!flag) return orderGone(oid,pd); //抢单时首先判断是否订单被锁定,已被锁定
		OrderState orderState = OrderState.getByCode(oid.getStatus());
		result  =  OrderState.MANTICSEND.equals(orderState) ? orderMantic(oid,pd) :             //已进入人工派单环节
						OrderState.WAITTAKE.equals(orderState) ? orderTaking(oid, pd, param) :     //允许接单
						OrderState.CANCEL.equals(orderState) ? orderCancel(oid, pd) : orderGone(oid,pd);  //订单已取消 | 订单被抢
		oid = dao.getOrderInfoById(param);
		JedisUtil.unLock(oid.getOrderno()); //流程执行完成后解锁
		return result;
	}
	
	/**
	 * 保存司机举手记录
	 * @param oid
	 * @param pd
	 */
	public void saveDriverHansUp(OrderInfoDetail oid,PubDriver pd){
		logger.info("保存司机举手记录...");
		logger.info("使用参数:" + JSONObject.fromObject(oid));
		logger.info("使用参数:" + JSONObject.fromObject(pd));
		PubPushLogParam param = new PubPushLogParam();
		param.setOrderno(oid.getOrderno());
		param.setDriverid(pd.getId());
		PubJpushlog jpushlog = dao.getPubJpushlog(param);
		if(jpushlog == null){
			logger.warn("没有找到相应推送记录");
			return;
		}else{
			jpushlog.setHandstate(PubJpushLogEnum.SUCCESS_HANDSTATE_PUBJPUSHLOG.icode);
			//司机抢单成功才更改此字段,否则只保存举手记录
			if(oid.getDriverid() != null && oid.getDriverid().equals(pd.getId())){
				jpushlog.setTakeorderstate(PubJpushLogEnum.SUCCESS_TAKEORDERSTATE_PUBJPUSHLOG.icode);
			}
			dao.saveDriverHansUp(jpushlog);
		}
		logger.info("保存司机举手记录完成");
	}
	
	/**
	 * 获取订单状态
	 * @param param
	 * @return
	 */
	public JSONObject getOrderState(OrderApiParam param){
		String[] require = new String[]{"usetype","ordertype","orderno"};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		JSONObject result = new JSONObject();
		OrderInfoDetail oid = dao.getOrderInfoById(param);
		//如果订单已完成,则计算完成时间
		if(OrderState.SERVICEDONE.state.equals(oid.getStatus())){
			oid.setTimes((int) ((oid.getEndtime().getTime() - oid.getStarttime().getTime())/1000));
		//如果订单未完成,则计算累计时间
		}else if(OrderState.INSERVICE.state.equals(oid.getStatus())){
			oid.setTimes((int) ((System.currentTimeMillis() - oid.getStarttime().getTime())/1000));
		}
		GetCertificateListParam gclp = new GetCertificateListParam();
		gclp.setOrderno(param.getOrderno());
		gclp.setOrdertype(param.getOrdertype());
		gclp.setUsetype(param.getUsetype());
		GetCertificateBO gcbo = getCertificateList(gclp);
		oid.setIsuploadcert(!gcbo.getUrls().isEmpty());
		result.put("order", convertOID(oid,false));
		result.put("surcharge", getOrderSurcharge(param));
		return result;
	}
	
	/**
	 * 发送抢单消息
	 * @param oid
	 * @param oap
	 */
	public void sendTakingOrderMessage(OrderInfoDetail oid,OrderApiParam oap){
		//司机抢单成功推送抢单成功消息,不在派单环节中推送
		if(oid.getOrderprop() == 0){
			OrgOrder orderinfo = dao.getOrgOrder(oid.getOrderno());
			OrderMessage om = new OrderMessage(orderinfo,OrderMessage.TAKEORDER);
			MessageUtil.sendMessage(om);
		}else{
			OpOrder orderinfo = dao.getOpOrder(oid.getOrderno());
			OrderMessage om = new OrderMessage(orderinfo,OrderMessage.TAKEORDER);
			MessageUtil.sendMessage(om);
		}
		logger.info("抢单成功已推送至短信和消息平台...");
		oap.setOrderstate(OrderState.WAITSTART.state);
		JSONObject result = new JSONObject();
		result = templateHelper.dealRequestWithFullUrlToken(
				SystemConfig.getSystemProperty("carserviceApiUrl")+"/OrderApi/ChangeOrderState", 
				HttpMethod.POST, 
				oap.getToken(), 
				oap, 
				JSONObject.class);
		if(result.getInt("status") == Retcode.OK.code){
			logger.info("抢单成功已推送至web平台...");
		}else{
			logger.info("抢单成功推送web平台失败...");
		}
	}
	
	/**
	 * 保存司机接单缓存
	 * @param oid
	 * @param pd
	 */
	private void saveTakingOrderCache(OrderInfoDetail oid,PubDriver pd){
		OrderCacheInfoDTO cacheInfoDTO = new OrderCacheInfoDTO();
		cacheInfoDTO.setOrderNo(oid.getOrderno());
		cacheInfoDTO.setUseNow(oid.isusenow());
		cacheInfoDTO.setUseTime(StringUtil.formatDate(oid.getUsetime(), StringUtil.DATE_TIME_FORMAT));
		cacheInfoDTO.setEstimatedtime((long)oid.getEstimatedtime());
		DriverCacheUtil.setOrderCache(pd.getId(), cacheInfoDTO);
	}
	
	/**
	 * 订单统计
	 * @param param
	 * @return
	 */
	public JSONObject orderStatistics(OrderStatisticsParam param){
		String[] require = new String[]{};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		JSONObject result = new JSONObject();
		PubDriver pd = driver.get();
		param.setDriverid(pd.getId());
		param.setStatus(OrderState.SERVICEDONE.state);
		List<OrderStatistics> list = dao.orderStatistics(param);
		result.put("statistics", list);
		return result;
	}
	
	/**
	 * 获取订单轨迹线
	 * @param param
	 * @return
	 */
	public JSONObject getOrderLine(OrderLineParam param){
		String[] require = new String[]{"usetype","ordertype","orderno"};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		
		JSONObject result = new JSONObject();
		OrderInfoDetail oid = dao.getOrderInfoById(param);
		param.setStartDate(oid.getStarttime());
		param.setEndDate(oid.getEndtime());
		result = templateHelper.dealRequestWithToken(
				"/BaiduApi/GetTraceData/?orderno={orderno}&ordertype={ordertype}&usetype={usetype}",
				HttpMethod.GET, 
				param.getToken(), 
				null, 
				JSONObject.class,
				param.getOrderno(),
				param.getOrdertype(),
				param.getUsetype());
		if(result.getInt("status") != Retcode.OK.code) return result;
		result.put("message",Retcode.OK.msg);
		result.remove("duration");
		return result;
	}
	
	/**
	 * 申请复核
	 * @param param
	 * @return
	 */
	public JSONObject applyForReview(OrderReviewParam param){
		String[] require = new String[]{"usetype","ordertype","orderno"};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		
		if(param.getOrderprop() == 0){
			OrgOrder order = dao.getOrgOrder(param.getOrderno());
			order.setReviewstatus(ReviewState.WAITFORREVIEW.state);
			order.setReviewperson(param.getReviewperson());
			order.setOrderreason(param.getReason());
			dao.updateOrgOrder(order);
		}else{
			OpOrder order = dao.getOpOrder(param.getOrderno());
			order.setReviewstatus(ReviewState.WAITFORREVIEW.state);
			order.setReviewperson(param.getReviewperson());
			order.setOrderreason(param.getReason());
			dao.updateOpOrder(order);
		}

		JSONObject result = new JSONObject();
		result.put("status", Retcode.OK.code);
		result.put("message", "申请复议成功.");
		return result;
	}
	
	/**
	 * 获取订单实时费用
	 * @param param
	 * @return
	 */
	public JSONObject getCurrentCost(OrderCostParam param){
		String[] require = new String[]{"usetype","ordertype","orderno"};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		JSONObject result = templateHelper.dealRequestWithFullUrlToken(
				SystemConfig.getSystemProperty("carserviceApiUrl")+"/OrderApi/GetOrderCost", 
				HttpMethod.POST, 
				param.getToken(), 
				param, 
				JSONObject.class);
		return result;
	}
	
	/**
	 * 获取订单实时费用(不做token校验)
	 * @param param
	 * @return
	 */
	public JSONObject getCurrentCostNoCheck(OrderCostParam param){
		//已取消订单也获取费用
		JSONObject result = templateHelper.dealRequestWithFullUrlToken(
				SystemConfig.getSystemProperty("carserviceApiUrl")+"/OrderApi/GetOrderCost", 
				HttpMethod.POST, 
				param.getToken(), 
				param, 
				JSONObject.class);
		return result;
	}
	
	/**
	 * 获取司机服务中订单
	 * @param param
	 * @return
	 */
	public JSONObject getCurrentOrder(OrderListParam param){
		String[] require = new String[]{};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		
		JSONObject result = new JSONObject();
		PubDriver pd = driver.get();
		param.setDriverid(pd.getId());
		List<OrderInfoDetail> list = dao.getOrderList(param);
		if(list !=null && !list.isEmpty()){
			result.put("order", convertOID(list.get(0),false));
		}else{
			result.put("order", new JSONObject());
		}
		return result;
	}
	
	/**
	 * 删除消息
	 * @param param
	 * @return
	 */
	public JSONObject delNews(NewsParam param){
		String[] require = new String[]{};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		
		PubDriver pd = driver.get();
		param.setDriverid(pd.getId());
		JSONObject result = new JSONObject();
		PubDriverNews pdn = dao.getNewsById(param);
		if(pdn == null) return null;
		pdn.setStatus(DataStatus.DELETE.code);
		dao.updateNews(pdn);
		return result;
	}
	
	/**
	 * 消息标记为已读
	 * @param param
	 * @return
	 */
	public JSONObject readNews(NewsParam param){
		String[] require = new String[]{};
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		
		PubDriver pd = driver.get();
		JSONObject result = new JSONObject();
		PubDriverNews pdn;
		if(param.isAllread() || param.getNewsid() == null || param.getNewsid().trim().isEmpty()){
			pdn = new PubDriverNews();
			pdn.setNewsstate("1");
			pdn.setUserid(pd.getId());
			dao.updateNewsAllRead(pdn);
		}else{
			param.setDriverid(pd.getId());
			pdn = dao.getNewsById(param);
			if(pdn == null) return null;
			pdn.setNewsstate("1");
			dao.updateNews(pdn);
		}
		return result;
	}
	
	/**
	 * 行程提醒(只做取消提醒)
	 * @param param
	 * @return
	 */
	public JSONObject orderRemind(OrderApiParam param){
		JSONObject result = new JSONObject();
		if(!param.isRemind()){
			param.setOrderno(param.getOrderid());
			result = templateHelper.dealRequestWithFullUrlToken(
					SystemConfig.getSystemProperty("carserviceApiUrl")+"/OrderApi/CancelOrderRemind", 
					HttpMethod.POST, 
					null, 
					param, 
					JSONObject.class);
		}
		removeOrderMessage(param);
		return result;
	}
	
	/**
	 * 网约车获取订单消息
	 * @param param
	 * @return
	 */
	public JSONObject getOrderMessageList(DriverMessageParam param){
		String[] require = new String[]{ };
		if(!checkeParam(param,getExceptElement(param, require))) {
			return errorResult.get();
		}
		
		JSONObject result = new JSONObject();
		List<DriverMessage> messages = new ArrayList<>();
		messages = doGetOrderMessageList();
		if(messages == null){
			messages = new ArrayList<>(); 
		}
		result.put("count", messages.size());
		result.put("news", messages);
		return result;
	}
	
	/**
	 * 司机代收现金
	 * @param param
	 * @return
	 */
	public BaseBO driverCollectionCash(DriverCollectionCashParam param){
		DriverCollectionCashBo bo = new DriverCollectionCashBo();
		param.setOrderstatus(OrderState.SERVICEDONE.state);
		String[] require = new String[]{"usetype","ordertype","orderno","orderstatus","paymethod"};
		if(!checkeParam(param,getExceptElement(param, require))) {
            bo.setStatus(errorResult.get().getInt("status"));
            bo.setMessage(errorResult.get().getString("message"));
            return bo;
		}
		//保存司机ID,方便日志输出
		param.setDriverid(driver.get().getId());
		DriverCollectionCashDto dto = StringUtil.castObject(param, DriverCollectionCashDto.class);
		dto.setIpaddr(param.getIpaddr());
		dto.setDriver(driver.get());
		dto.setOrder(order.get());
		if(!doDriverCollectionCash(dto)){
			bo.setStatus(errorResult.get().getInt("status"));
			bo.setMessage(errorResult.get().getString("message"));
		}else{
			bo.setUrl(dto.getPayurl());
		}
		return bo;
	}

    /**
     * 网约车获取订单消息
     *
     * @param param
     * @return
     */
    public BaseBO certificateUpload(CertificateUploadParam param) {
        String[] require = new String[]{"usetype","ordertype","orderno"};
        if (!checkeParam(param, getExceptElement(param, require))) {
            BaseBO bo = new BaseBO();
            bo.setStatus(errorResult.get().getInt("status"));
            bo.setMessage(errorResult.get().getString("message"));
            return bo;
        }


        if (OrderState.SERVICEDONE.state.equals(order.get().getOrderstatus())
                || OrderState.CANCEL.state.equals(order.get().getOrderstatus())) {
            // 已支付、已结算、结算中、已付结、已关闭
            if (PayState.PAYED.state.equals(order.get().getPaymentstatus())
                    || PayState.STATEMENTED.state.equals(order.get().getPaymentstatus())
                    || PayState.STATEMENTING.state.equals(order.get().getPaymentstatus())
                    || PayState.PAYOVER.state.equals(order.get().getPaymentstatus())
                    || PayState.CLOSE.state.equals(order.get().getPaymentstatus())
                    // 出租车，行程费已支付，无论调度费是否支付，都不允许再次修改
                    || PayState.PASSENGERNOPAY.state.equals(order.get().getPaymentstatus())
                    || PayState.DRIVERNOPAY.state.equals(order.get().getPaymentstatus())) {
                BaseBO bo = new BaseBO();
                bo.setStatus(Retcode.CERTIFICATE_UPLOAD_UNUSE.code);
                bo.setMessage(Retcode.CERTIFICATE_UPLOAD_UNUSE.msg);
                return bo;
            }
        }

        List<OrgOrderfileParam> list = new ArrayList<>();
        OrgOrderfileParam file;
        Map<String, Object> fileResult;
        ByteArrayInputStream inputstream = null;
        for (String image : param.getImages()) {
            try {
                byte[] imgbyte = org.apache.commons.codec.binary.Base64.decodeBase64(image);
                inputstream = new ByteArrayInputStream(imgbyte);
                fileResult = FileUtil.upload2FileServer(inputstream, "driverimg.jpg");
            } finally {
                if (inputstream != null) {
                    try {
                        inputstream.close();
                    } catch (Exception e) {
                    }
                }
            }

            file = new OrgOrderfileParam();
            if (fileResult != null && fileResult.get("message") != null) {
                String path = (String) ((List<?>) fileResult.get("message")).get(0);
                file.setFilepath(path);
            } else {
                BaseBO bo = new BaseBO();
                bo.setStatus(Retcode.EXCEPTION.code);
                bo.setMessage(Retcode.EXCEPTION.msg);
                return bo;
            }

            file.setCreatetime(new Date());
            file.setUpdatetime(new Date());
            file.setFilesuffix("jpg");
            file.setId(GUIDGenerator.newGUID());
            file.setOrderno(param.getOrderno());
            file.setStatus(1);
            file.setType(0); // 0-附加费
            list.add(file);
        }

        int cat;
        if (OrderEnum.ORDERTYPE_TAXI.code.equals(param.getOrdertype())) {
            cat = 2;
        } else if (OrderEnum.USETYPE_PERSONAL.code.equals(param.getUsetype())) {
            // 个人 - 运管
            cat = 0;
        } else {
            cat = 1;
        }

        DeleteOrgOrderfileParam delete = new DeleteOrgOrderfileParam();
        delete.setCat(cat);
        delete.setOrderno(param.getOrderno());
        this.dao.deleteOrgOrderfile(delete);
        if (!list.isEmpty()) {
            this.dao.addOrgOrderfile(list, cat);
        }

        BaseBO bo = new BaseBO();
        bo.setMessage(Retcode.OK.msg);
        bo.setStatus(Retcode.OK.code);
        return bo;
    }

    public GetCertificateBO getCertificateList(GetCertificateListParam param){
        String[] require = new String[]{"usetype","ordertype","orderno"};
        if (!checkeParam(param, getExceptElement(param, require))) {
            GetCertificateBO bo = new GetCertificateBO();
            bo.setStatus(errorResult.get().getInt("status"));
            bo.setMessage(errorResult.get().getString("message"));
            return bo;
        }

        int cat;
        if (OrderEnum.ORDERTYPE_TAXI.code.equals(param.getOrdertype())) {
            cat = 2;
        } else if (OrderEnum.USETYPE_PERSONAL.code.equals(param.getUsetype())) {
            // 个人 - 运管
            cat = 0;
        } else {
            cat = 1;
        }
        List<String> filepaths = this.dao.getCertificateList(param.getOrderno(),cat);
        List<String> urls = new ArrayList<>();
        for(String filepath: filepaths){
            urls.add( SystemConfig.getSystemProperty("fileserver")+"/" + filepath);
        }

        GetCertificateBO bo = new GetCertificateBO();
        bo.setUrls(urls);
        bo.setMessage(Retcode.OK.msg);
        bo.setStatus(Retcode.OK.code);
        return bo;
    }


	/**********************************************************内部方法***************************************************************/
	/**
	 * 司机代收现金逻辑
	 * 扫码支付使用乘客渠道(没有driver开头)
	 * @param dto
	 */
	private boolean doDriverCollectionCash(DriverCollectionCashDto dto){
		logger.info("司机代收现金逻辑开始...");
		logger.info("使用参数:" + StringUtil.parseBeanToJSON(dto));
		if(PayMethod.BALANCE.code.equals(dto.getPaymethod())){
			doBalancePay(dto);
			doConfirmCost4Advance(dto);
			//司机累计接单量+1(完成时才算)
			addDriverOrderCount(driver.get());
			//订单结束时,需要删除司机状态缓存(个人垫付订单,在支付完成时才算订单结束)
			DriverCacheUtil.endOrder(driver.get().getId(), dto.getOrderno());
		}else if(PayMethod.ALIPAY.code.equals(dto.getPaymethod())){
			if(!doAliPay(dto)){
				return false;
			}
		}else{
			if(!doWeChatPay(dto)){
				return false;
			}
		}
		logger.info("司机代收现金逻辑完成");
		return true;
	}
	
	/**
	 * 个人垫付需要单独保存垫付明细
	 * @param dto
	 */
	private void doConfirmCost4Advance(DriverCollectionCashDto dto){
		OrderApiParam oap = new OrderApiParam();
		ConfirmCostDto ccdto = new ConfirmCostDto();
		oap.setOrderno(dto.getOrderno());
		oap.setOrdertype(dto.getOrdertype());
		oap.setUsetype(dto.getUsetype());
		JSONObject surcharge = getOrderSurcharge(oap);
		if(surcharge.isEmpty()) return;
		ccdto.setOrderno(dto.getOrderno());
		ccdto.setOrdertype(dto.getOrdertype());
		ccdto.setUsetype(dto.getUsetype());
		if(surcharge.has("expresswayfee")){
			double fee = Double.parseDouble(surcharge.getString("expresswayfee").replace("元", ""));
			ccdto.setExpresswayfee(fee);
		}
		if(surcharge.has("bridgefee")){
			double fee = Double.parseDouble(surcharge.getString("bridgefee").replace("元", ""));
			ccdto.setBridgefee(fee);
		}
		if(surcharge.has("parkingfee")){
			double fee = Double.parseDouble(surcharge.getString("parkingfee").replace("元", ""));
			ccdto.setParkingfee(fee);
		}
		if(surcharge.has("otherfee")){
			double fee = Double.parseDouble(surcharge.getString("otherfee").replace("元", ""));
			ccdto.setOtherfee(fee);
		}
		doSaveDriverPayManagement(ccdto);
	}
	
	/**
	 * 如果使用收现方式,则改变订单状态为未结算(司机还需打款给平台)
	 * @param dto
	 */
	private void doBalancePay(DriverCollectionCashDto dto){
		logger.info("使用现金支付...");
		dto.getOrder().setOrderstatus(OrderState.SERVICEDONE.state);
		dto.getOrder().setPaymentstatus(PayState.MENTED.state);
		dto.getOrder().setPaytype(OrderEnum.PAYTYPE_CASH.code);
		dto.getOrder().setPaymenttime(new Date());
		if(OrderEnum.USETYPE_PERSONAL.code.equals(dto.getUsetype())){
			dao.updateOpOrder((OpOrder)dto.getOrder());
		}else{
			dao.updateOrgOrder((OrgOrder)dto.getOrder());
		}
		logger.info("使用现金支付完成");
	}
	
	/**
	 * 使用支付宝扫码付款
	 * @param dto
	 */
	private boolean doAliPay(DriverCollectionCashDto dto){
		logger.info("使用支付宝二维码支付...");
		PayUtil pu = new PayUtil(dto.getIpaddr(),PayUtilEnum.PAYORDER,order.get().getOrderno());
//		String partner = "2088521158240961",privateKey = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAOjV55GV7qTtZnD76N/I8wSz8RtLUtFT9k3RISi6SwVtMQQvwHNr/Pe+gN0MW5eOqzikRFbQ3B16dW1QUVu5cY7XkYlIldAK1JQn4sio5sqJgxA0Club2cxKGgUNioj2y4kPYhN4ZIrhTdz9pPuDhuOpbxXQ1QsD1WWADwYakwHXAgMBAAECgYAxnvy2Ez0D2zBc3eL4ZmwcUXkN9xSUVg+E8A/gDSvV4Tp0CPU75ATKi8gM1AhlGVu2O5Pw6JwwkBuci2R7Zt8jSQqyLL6z4jdmuWluZCTaKhaVAu1kGwVIPR1l5ranOvajKQzQ5WNoqOJVzRUEAqTXPeOSmn5q+zzCZ6WOKAUbaQJBAPjgl2J1KSkQN4PMbn/ZYVAkAn0rvFjDaHlqnNpD2nJc6pZ85ulSDnQqt8w/h9qoVWF+GdnDv5VhWSSBPSqgBBMCQQDvf8hk6TIEqPrEmZdCKJZQeGtBe92Ekc/kvmGnouCP8fGGu+X4cIflFPI2a/mmeJNmVYcUk6QmVXIOrt9JvNutAkAn0hemw0RAs72OMwmDH074uapESNksAqgWtT4/lhe/sKpARd/UeTKi16rs3UVpcQGoRbrxIubmidrvglY9GblNAkEAyDuFRxjQAKVmQshGdcGJKm4C/hSY9yURMqUY8BZ0uOQGkia19ife9d+1QVq0tkFIut32uXVWX9ZALZZ2iCelYQJBAJ/WU9KgCvsfNuhNFIiBOxNhE/0QddTV+32bgwCSOHnWTksFJMpgnTIoEfFMhXkxYmVBGxLquJzDEQecPIe5ifg=";
		String partner = null,privateKey = null;
		//根据订单归属平台不同获取不同的收款信息
		if(usetype.get() != null && !OrderEnum.USETYPE_PERSONAL.equals(usetype.get())){
			partner = leasescompany.get().getAlipaypartnerid();
			privateKey = leasescompany.get().getAlipaypartnerprivatekey();
		}else if(usetype.get() != null && OrderEnum.USETYPE_PERSONAL.equals(usetype.get())){
			partner = platformInfo.get().getAliPayPartnerId();
			privateKey = platformInfo.get().getAliPayPartnerPrivateKey();
		}
		PayInfoDto payInfo = pu.createAliPayQRCode(dto.getOrder().getOrderamount(), privateKey, partner);
		if(payInfo == null){
			logger.info("使用支付宝二维码支付失败");
			errorResult.get().put("status", Retcode.FAILED.code);
			errorResult.get().put("message", "支付失败");
			return false;
		}
		dto.setPayurl(payInfo.getCodeurl());
		//保存第三方支付记录,回调时使用
		doSaveTradeRecord(payInfo,dto);
		logger.info("使用支付宝二维码支付完成");
		return true;
	}
	
	/**
	 * 使用微信扫码付款
	 * @param dto
	 */
	private boolean doWeChatPay(DriverCollectionCashDto dto){
		logger.info("使用微信二维码支付...");
		PayUtil pu = new PayUtil(dto.getIpaddr(),PayUtilEnum.PAYORDER,order.get().getOrderno());
		String appid = null,privateKey = null,shno = null;
		//根据订单归属平台不同获取不同的收款信息
		if(usetype.get() != null && !OrderEnum.USETYPE_PERSONAL.equals(usetype.get())){
			appid = leasescompany.get().getWechatappid();
			privateKey = leasescompany.get().getWechatsecretkey();
			shno = leasescompany.get().getWechatmerchantno();
		}else if(usetype.get() != null && OrderEnum.USETYPE_PERSONAL.equals(usetype.get())){
			appid = platformInfo.get().getWechatAppId();
			privateKey = platformInfo.get().getWechatSecretKey();
			shno = platformInfo.get().getWechatMerchantNo();
		}
		PayInfoDto payInfo = pu.createWeChatQRCode(dto.getOrder().getOrderamount(), appid, privateKey, shno);
		if(payInfo == null){
			logger.info("使用微信二维码支付失败");
			errorResult.get().put("status", Retcode.FAILED.code);
			errorResult.get().put("message", "支付失败");
			return false;
		}
		dto.setPayurl(payInfo.getCodeurl());
		//保存第三方支付记录,回调时使用
		doSaveTradeRecord(payInfo,dto);
		logger.info("使用微信二维码支付完成");
		return true;
	}
	
	/**
	 * 保存第三方支付记录,回调时使用
	 * @param publicKey 支付宝验证公钥
	 * @param param
	 * @return
	 */
	private boolean doSaveTradeRecord(PayInfoDto payInfo,DriverCollectionCashDto dto){
		logger.info("保存第三方交易记录开始...");
		logger.info("使用参数:" + StringUtil.parseBeanToJSON(payInfo));
		PubDriverTradingrecord tradingrecord = new PubDriverTradingrecord();
		tradingrecord.setOuttradeno(payInfo.getOuttradeno());
		tradingrecord.setLeasescompanyid(driver.get().getLeasescompanyid());
		tradingrecord.setDriverid(driver.get().getId());
		tradingrecord.setType(Integer.valueOf(PayUtilEnum.PAYORDER.code));
		tradingrecord.setPaymenttype(Integer.valueOf(dto.getPaymethod()));
//		tradingrecord.setValidatekey(publicKey);
		tradingrecord.setTradingstatus(Integer.valueOf(PayUtilEnum.TRADING_PROCESSING.code));
		tradingrecord.setOrderno(dto.getOrderno());
		tradingrecord.setOrdertype(dto.getOrdertype());
		tradingrecord.setUsetype(dto.getUsetype());
		tradingrecord.setAmount(dto.getOrder().getOrderamount());
		tradingrecord.setStatus(DataStatus.OK.code);
		taximainDao.saveDriverTradeRecord(tradingrecord);
		logger.info("保存第三方交易记录完成");
		return true;
	}
	
	/**
	 * 获取订单附加费明细
	 * @param param
	 * @return
	 */
	private JSONObject getOrderSurcharge(OrderApiParam param){
		JSONObject surchargeJson = new JSONObject();
		if(OrderEnum.ORDERTYPE_TAXI.code.equals(param.getOrdertype())){
			OpTaxiOrderSurcharge surcharge = getOpTaxiOrderSurcharge(param.getOrderno());
			if(surcharge == null){
				return surchargeJson;
			}
			if(surcharge.getPayexpressway() > 0){
				surchargeJson.put("expresswayfee", surcharge.getPayexpressway() + "元");
			}
			if(surcharge.getPaytoll() > 0){
				surchargeJson.put("bridgefee", surcharge.getPaytoll() + "元");
			}
			if(surcharge.getPayparking() > 0){
				surchargeJson.put("parkingfee", surcharge.getPayparking() + "元");
			}
			if(surcharge.getPayother() > 0){
				surchargeJson.put("otherfee", surcharge.getPayother() + "元");
			}
		}else if (OrderEnum.USETYPE_PERSONAL.code.equals(param.getUsetype())) {
			OpOrderSurcharge surcharge = getOpOrderSurcharge(param.getOrderno());
			if(surcharge == null){
				return surchargeJson;
			}
			if(surcharge.getPayexpressway() > 0){
				surchargeJson.put("expresswayfee", surcharge.getPayexpressway() + "元");
			}
			if(surcharge.getPaytoll() > 0){
				surchargeJson.put("bridgefee", surcharge.getPaytoll() + "元");
			}
			if(surcharge.getPayparking() > 0){
				surchargeJson.put("parkingfee", surcharge.getPayparking() + "元");
			}
			if(surcharge.getPayother() > 0){
				surchargeJson.put("otherfee", surcharge.getPayother() + "元");
			}
		}else{
			OrgOrderSurcharge surcharge = getOrgOrderSurcharge(param.getOrderno());
			if(surcharge == null){
				return surchargeJson;
			}
			if(surcharge.getPayexpressway() > 0){
				surchargeJson.put("expresswayfee", surcharge.getPayexpressway() + "元");
			}
			if(surcharge.getPaytoll() > 0){
				surchargeJson.put("bridgefee", surcharge.getPaytoll() + "元");
			}
			if(surcharge.getPayparking() > 0){
				surchargeJson.put("parkingfee", surcharge.getPayparking() + "元");
			}
			if(surcharge.getPayother() > 0){
				surchargeJson.put("otherfee", surcharge.getPayother() + "元");
			}
		}
		return surchargeJson;
	}
	/**
	 * 获取机构订单附加费明细
	 * @param orderno
	 * @return
	 */
	private OrgOrderSurcharge getOrgOrderSurcharge(String orderno){
		return osDao.getOrgOrderSurcharge(orderno);
	}
	/**
	 * 获取个人订单附加费明细
	 * @param orderno
	 * @return
	 */
	private OpOrderSurcharge getOpOrderSurcharge(String orderno){
		return osDao.getOpOrderSurcharge(orderno);
	}
	/**
	 * 获取机构订单附加费明细
	 * @param orderno
	 * @return
	 */
	private OpTaxiOrderSurcharge getOpTaxiOrderSurcharge(String orderno){
		return osDao.getOpTaxiOrderSurcharge(orderno);
	}
	/**
	 * 订单确费逻辑(更新订单状态为已完成,并更新订单费用)
	 * @param param
	 */
	private void doConfirmCost(ConfirmCostDto param){
		logger.info("订单确费逻辑开始...");
		logger.info("使用参数:" + StringUtil.parseBeanToJSON(param));
		if(param.getSurchargefee() == 0){
			order.get().setReviewdetailstatus(OrderReviewEnum.REVIEWDETAILSTATUS_ROUTE.icode);
		}else if(param.getSurchargefee() != 0 
				&& !OrderEnum.USETYPE_PERSONAL.code.equals(param.getUsetype())){
			//如果是机构订单,并且附加费不为0,还需更改机构账户的附加费总额
			OrderCostParam ocp = new OrderCostParam();
			ocp.setUserid(order.get().getUserid());
			ocp.setCompanyid(order.get().getCompanyid());
			OrgOrganCompanyRef balance = dao.getOrgBalance(ocp);
			balance.setSurcharge(balance.getSurcharge() + param.getSurchargefee());
			dao.updateOrgBalance(balance);
		}
		//每次确费都需要取原始值(因私订单可以重复确费,如果取orderamount,会重复添加附加费)
		OrderCost oc = StringUtil.parseJSONToBean(order.get().getPricecopy(), OrderCost.class);
		double routemoney = StringUtil.formatNum(oc.getCost(), 0);  //行程费需要取整
		double allcost = routemoney + param.getSurchargefee();
		order.get().setShouldpayamount(allcost);
		order.get().setActualpayamount(allcost);
		order.get().setOriginalorderamount(allcost);
		order.get().setOrderamount(allcost);
		order.get().setPayroutemoney(routemoney);
		order.get().setShouldroutemoney(routemoney);
		order.get().setPaysurcharge(param.getSurchargefee());
		order.get().setShouldsurcharge(param.getSurchargefee());
		order.get().setOrderstatus(OrderState.SERVICEDONE.state); //确费流程结束后改为完成
		order.get().setExpensetype(Integer.parseInt(OrderEnum.EXPENSETYPE_SERVICE_DONE.code));
		if (order.get() instanceof OpOrder) {
			OpOrder opOrder = (OpOrder)order.get();
			dao.updateOpOrder(opOrder);
		}else{
			OrgOrder orgOrder = (OrgOrder)order.get();
			//如果是个人垫付(机构因私)订单,确费时不做订单状态变更,支付时变更
			if(PayMethod.ADVANCE.code.equals(orgOrder.getPaymethod())){
				order.get().setOrderstatus(OrderState.WAITMONEY.state);
			}
			dao.updateOrgOrder(orgOrder);
		}
		logger.info("订单确费逻辑完成");
	}

	/**
	 * 保存订单附加费明细
	 * @param param
	 */
	private void doSaveSurcharge(ConfirmCostDto param){
		logger.info("保存订单附加费明细...");
		logger.info("使用参数:" + StringUtil.parseBeanToJSON(param));
		try {
			OrgOrderSurcharge orgSurcharge = new OrgOrderSurcharge();
			orgSurcharge.setId(GUIDGenerator.newGUID());
			orgSurcharge.setOrderno(param.getOrderno());
			orgSurcharge.setPayexpressway(param.getExpresswayfee());
			orgSurcharge.setPaytoll(param.getBridgefee());
			orgSurcharge.setPayparking(param.getParkingfee());
			orgSurcharge.setPayother(param.getOtherfee());
			orgSurcharge.setShouldexpressway(param.getExpresswayfee());
			orgSurcharge.setShouldtoll(param.getBridgefee());
			orgSurcharge.setShouldparking(param.getParkingfee());
			orgSurcharge.setShouldother(param.getOtherfee());
			orgSurcharge.setStatus(DataStatus.OK.code);
			if(OrderEnum.ORDERTYPE_TAXI.code.equals(param.getOrdertype())){
				OpTaxiOrderSurcharge opTaxiSurcharge = osDao.getOpTaxiOrderSurcharge(param.getOrderno());
				String id = opTaxiSurcharge != null ? opTaxiSurcharge.getId() : orgSurcharge.getId();
				opTaxiSurcharge = StringUtil.castObject(orgSurcharge, OpTaxiOrderSurcharge.class);
				opTaxiSurcharge.setId(id);
				osDao.saveOpTaxiOrderSurcharge(opTaxiSurcharge);
			}else if (OrderEnum.USETYPE_PERSONAL.code.equals(param.getUsetype())) {
				OpOrderSurcharge opSurcharge = osDao.getOpOrderSurcharge(param.getOrderno());
				String id = opSurcharge != null ? opSurcharge.getId() : orgSurcharge.getId();
				opSurcharge = StringUtil.castObject(orgSurcharge, OpOrderSurcharge.class);
				opSurcharge.setId(id);
				osDao.saveOpOrderSurcharge(opSurcharge);
			}else{
				OrgOrderSurcharge orgSurchargeTemp = osDao.getOrgOrderSurcharge(param.getOrderno());
				String id = orgSurchargeTemp != null ? orgSurchargeTemp.getId() : orgSurcharge.getId();
				orgSurcharge.setId(id);
				osDao.saveOrgOrderSurcharge(orgSurcharge);
			}
		} catch (Exception e) {
			logger.error("保存订单附加费明细异常,当前参数" + StringUtil.parseBeanToJSON(param),e);
		}
		logger.info("保存订单附加费明细完成");
	}
	
	/**
	 * 保存司机垫付记录信息
	 * @param param
	 */
	private void doSaveDriverPayManagement(ConfirmCostDto param){
		//附加费为0时不保存司机垫付记录信息
		if(param.getSurchargefee() <= 0){
			return;
		}
		//出租车线下支付不保存司机垫付记录
		if(PayMethod.OFFLINE.code.equals(param.getPaymethod())){
			return;
		}
		logger.info("保存司机垫付记录...");
		logger.info("使用参数:" + StringUtil.parseBeanToJSON(param));
		try{
			LeDriverpaymanagement leDriverPay = new LeDriverpaymanagement();
			leDriverPay.setId(GUIDGenerator.newGUID());
			leDriverPay.setOrderno(param.getOrderno());
			leDriverPay.setOrdertype(param.getOrdertype());
			leDriverPay.setUsetype(param.getUsetype());
			leDriverPay.setLecompanyid(order.get().getCompanyid());
			leDriverPay.setPaybackstate(0);
			leDriverPay.setStatus(DataStatus.OK.code);
			if (OrderEnum.USETYPE_PERSONAL.code.equals(param.getUsetype())) {
				OpDriverpaymanagement opDriverPay = osDao.getOpDriverPayManagement(param.getOrderno());
				String id = opDriverPay != null ? opDriverPay.getId() : leDriverPay.getId();
				opDriverPay = StringUtil.castObject(leDriverPay, OpDriverpaymanagement.class);
				opDriverPay.setId(id);
				osDao.saveOpDriverPayManagement(opDriverPay);
			}else{
				LeDriverpaymanagement tempLeDriverPay = osDao.getLeDriverPayManagement(param.getOrderno());
				String id = tempLeDriverPay != null ? tempLeDriverPay.getId() : leDriverPay.getId();
				leDriverPay.setId(id);
				osDao.saveLeDriverPayManagement(leDriverPay);
			}
		} catch (Exception e) {
			logger.error("保存司机垫付记录异常,当前参数" + StringUtil.parseBeanToJSON(param),e);
		}
		logger.info("保存司机垫付记录完成");
	}
	
	/**
	 * 取消订单提醒时删除redis中的订单提醒信息
	 * @param param
	 * @return
	 */
	private boolean removeOrderMessage(OrderApiParam param){
		String key = RedisKeyEnum.ORDER_FORCESEND_REMINDER.code + param.getOrderno() + "_" + param.getUsetype();
		JedisUtil.delKey(key);
		return true;
	}
	/**
	 * 网约车获取订单消息
	 * @param param
	 * @return
	 */
	private List<DriverMessage> doGetOrderMessageList(){
		logger.info("网约车获取订单消息...");
		List<DriverMessage> messages = new ArrayList<>();
		String key = "DriverGrabMessage_" + driver.get().getId() + "_"+driver.get().getPhone()+"_*";
		Set<String> keys = JedisUtil.getKeys(key);
		if (keys.isEmpty()) return messages;
		for(String k : keys){
			String value = JedisUtil.getString(k);
			if(value == null) continue;
			DriverMessage message = StringUtil.parseJSONToBean(value, DriverMessage.class);
			messages.add(message);
        }
    	sortMessageList(messages);
		logger.info("网约车获取订单消息完成");
		return messages;
	}
	
	/**
	 * 网约车获取系统消息
	 * @param param
	 * @return
	 */
	private List<DriverMessage> doGetSystemMessageList(NewsParam param){
		logger.info("网约车获取系统消息...");
		List<PubDriverNews> news = dao.getNewsByType(param);
		List<DriverMessage> messages = new ArrayList<>();
		for(PubDriverNews n : news){
			DriverMessage dm = convertDriverNewsToMessage(n);
			messages.add(dm);
		}
		logger.info("网约车获取系统消息完成");
		return messages;
	}
	
	/**
	 * 获取司机消息
	 * @param param
	 * @return
	 */
	private JSONObject doPollMessage(NewsParam param){
		logger.info("网约车获取消息开始...");
		logger.info("使用参数:" + JSONObject.fromObject(param));
		List<DriverMessage> messages = new ArrayList<>();
		JSONObject result = new JSONObject();
		if(DriverMessageEnum.TYPE_ORDER.code.equals(param.getType()+"")){
			messages = doGetOrderMessageList();
		}else{
			messages = doGetSystemMessageList(param);
		}
		
		if(messages == null){
			messages = new ArrayList<>(); 
		}
		result.put("count", messages.size());
		result.put("comment", messages);
		logger.info("网约车获取消息完成");
		return result;
	}
	
	/**
	 * 订单信息单位换算,费用和时长都需要换算成String
	 * @param oid
	 * @return
	 */
	private JSONObject convertOID(OrderInfoDetail oid,boolean isTakeOrder){
		OrderCost oc = StringUtil.parseJSONToBean(oid.getPricecopy(), OrderCost.class);
		if(oid.getCartypelogo() != null && !oid.getCartypelogo().isEmpty()){
			oid.setCartypelogo(fileserver + oid.getCartypelogo());
		}
		if(oid.getPassengericonmin() != null && !oid.getPassengericonmin().isEmpty()){
			oid.setPassengericonmin(fileserver + oid.getPassengericonmin());
		}
		if(oid.getPassengericonmax() != null && !oid.getPassengericonmax().isEmpty()){
			oid.setPassengericonmax(fileserver + oid.getPassengericonmax());
		}
		JSONObject pricecopy = JSONObject.fromObject(oid.getPricecopy());

		int times;
		if(pricecopy.get("timetype")==null){
			times = oid.getTimes();
		}else{
			times = pricecopy.getInt("timetype")==0?oid.getTimes():pricecopy.getInt("slowtimes");
		}

		double rangecost = StringUtil.formatNum(oid.getMileage()/1000, 1)*oid.getRangeprice();
		double timecost = StringUtil.formatNum(oid.getTimes(), 1)*oid.getTimeprice();
		String passengers = (oid.getPassengers() == null || oid.getPassengers().trim().isEmpty()) ? "佚名" : oid.getPassengers();
		//获取订单费用
		JSONObject order = JSONObject.fromObject(oid);
		order.put("passengers", passengers);
		order.put("orderamount", StringUtil.formatNum(oid.getOrderamount(), 1));
		order.put("mileage", StringUtil.formatNum(oid.getMileage()/1000, 1)+"公里");
		order.put("startprice", StringUtil.formatNum(oid.getStartprice(), 1)+"元");
		order.put("rangeprice", StringUtil.formatNum(oid.getRangeprice(), 1)+"元/公里");
		order.put("timeprice", StringUtil.formatNum(oid.getTimeprice(), 1)+"元/分钟");
		order.put("deadheadprice", oc.getDeadheadprice()+"元/公里");
		order.put("deadheadmileage", oc.getDeadheadmileage()+"公里");
		order.put("realdeadheadmileage", oc.getRealdeadheadmileage()+"公里");
		order.put("nighteprice", oc.getNighteprice()+"元/公里");
		order.put("nightstarttime", oc.getNightstarttime());
		order.put("nightendtime", oc.getNightendtime());
		order.put("rangecost", StringUtil.formatNum(rangecost, 1)+"元");
		order.put("timecost", StringUtil.formatNum(timecost, 1)+"元");
		order.put("deadheadcost", oc.getDeadheadcost()+"元");
		order.put("surchargecost", oid.getSurcharge());
		order.put("nightcost", oc.getNightcost()+"元");
		order.put("nightrange", (oc.getNightcost() == 0 ? "0" : oc.getMileage()/1000)+"公里");
		order.put("organshort", oid.getOrganid());
		order.put("organfull", oid.getOrganfull());
		order.put("organshort", oid.getOrganshort());
		order.put("times",   StringUtil.formatCostTime(times));
		if(isTakeOrder){  //剩余时间,抢单成功返回总剩余时长,列表页返回待出发
			int  lasttime = (int) ((oid.getUsetime().getTime()-System.currentTimeMillis())/1000/60);
			lasttime = lasttime < 0 ? 0 : lasttime; // 如果超过了剩余时间则显示0分钟
			oid.setLasttime("剩余"+StringUtil.formatCostTime(lasttime).replace("钟", ""));
		}else{
			order.put("lasttime", StringUtil.formatOrderStatus(oid.getUsetime(),oid.getStatus()));
		}
		int cancelAmount = (oid.getCancelamount() == null ? 0 : oid.getCancelamount());
		int cancelNature = (oid.getCancelnature() == null ? 2 : oid.getCancelnature());
		order.put("paystatus", StringUtil.formatPayStatus(oid.getPaystatus(),oid.getStatus(),cancelAmount,oid.getPaymethod()));
		order.put("ordertype", oid.getType());
		order.put("cancelamount", cancelAmount + "元");
		order.put("cancelnature", (1 == cancelNature ? "有责" : "免责"));
		order.put("paymethod", oid.getPaymethod());
		//去掉不必要字段
		order.remove("pricecopy");
		order.remove("organid");
		order.remove("companyid");
		order.remove("cityid");
		order.remove("driverid");
		order.remove("cancelparty");
		order.remove("vehicleid");
		order.remove("cityintime");
		order.remove("addressintime");
		order.remove("lng");
		order.remove("lat");
		return order;
	}
	
    /**
	 * 消息排序(抢单时限越少的在上面)
	 * @param list
	 * @return
	 */
	private void sortMessageList(List<DriverMessage> list){
		if(list == null || list.isEmpty()) return;
		list.sort(new Comparator<DriverMessage>() {
            @Override
            public int compare(DriverMessage o1, DriverMessage o2) {
                if (o1.getOrderinfo().getLong("grabendtime") > o2.getOrderinfo().getLong("grabendtime")) {
                    return 1;
                }
                if (o1.getOrderinfo().getLong("grabendtime") < o2.getOrderinfo().getLong("grabendtime")) {
                    return -1;
                }
                return 0;
            }
        });
	}
	
	/**
	 * 将PubdriverNews转换为DriverMessage
	 * @param news
	 * @return
	 */
	private DriverMessage convertDriverNewsToMessage(PubDriverNews news){
		JSONObject newsContent = JSONObject.fromObject(news.getContent());
		int newsType = newsContent.getInt("type");
		DriverMessage dm = new DriverMessage();
		dm.setNewsid(news.getId());
		dm.setCreatetime(news.getCreatetime());
		dm.setReaded(NewsState.isReaded(news.getNewsstate()));
		dm.setTitle(newsContent.get("title") == null ? "" : newsContent.getString("title"));
		dm.setContent(newsContent.get("content") == null ? "" : newsContent.getString("content"));
		dm.setNewstype(NewsState.convertNewsType(newsType));
		//如果存在订单信息
		JSONObject orderinfo = new JSONObject();
		if(newsContent.has("orderid")){
			orderinfo.put("orderno", newsContent.getString("orderid"));
			orderinfo.put("usetype", newsContent.getString("usetype"));
			orderinfo.put("ordertype", newsContent.getString("ordertype"));
			orderinfo.put("type",OrderEnum.getOrderType(newsContent.getString("ordertype")).msg);
			orderinfo.put("onaddr", newsContent.getString("onaddr"));
			orderinfo.put("offaddr", newsContent.getString("offaddr"));
			orderinfo.put("usetime", StringUtil.parseDate(newsContent.getString("usetime"), "yyyy-MM-dd HH:mm"));
			orderinfo.put("usenow", newsContent.getString("usenow")+"用车");
			orderinfo.put("lasttime", newsContent.getString("lasttime"));//剩余时间
			//下面的字段都是在任务消息里才会有
			orderinfo.put("grabendtime", 0);
			orderinfo.put("headimage", "");
			orderinfo.put("remark", "");
		}else{
			orderinfo.put("orderno", "");
			orderinfo.put("usetype", "");
			orderinfo.put("ordertype", "");
			orderinfo.put("type", "");
			orderinfo.put("onaddr", "");
			orderinfo.put("offaddr", "");
			orderinfo.put("usetime", 0);
			orderinfo.put("usenow", "");
			orderinfo.put("lasttime", "");//剩余时间
			//下面的字段都是在任务消息里才会有
			orderinfo.put("grabendtime", 0);
			orderinfo.put("headimage", "");
			orderinfo.put("remark", "");
		}
		//如果是提现消息
		JSONObject takecashinfo = new JSONObject();
		if(DriverMessageEnum.NEWS_TYPE_WITHDRAW.code.equals(NewsState.convertNewsType(newsType))){
			takecashinfo.put("remark", newsContent.getString("remark"));
			takecashinfo.put("amount", newsContent.getDouble("amount"));
			takecashinfo.put("bank", newsContent.getString("bankname"));
			takecashinfo.put("applytime", StringUtil.parseDate(newsContent.getString("applytime"), "yyyy-MM-dd HH:mm"));
		}else{
			takecashinfo.put("remark", "");
			takecashinfo.put("bank", "");
			takecashinfo.put("amount", 0);
			takecashinfo.put("applytime", 0);
		}
		dm.setOrderinfo(orderinfo);
		dm.setTakecashinfo(takecashinfo);
		return dm;
	}
	
	/**
	 * 抢单成功清除redis中的抢单信息
	 * @return
	 */
	private boolean removeDriverMessage(OrderInfoDetail oid,PubDriver driver,OrderApiParam param){
		String useday = StringUtil.formatDate(StringUtil.getToday(oid.getUsetime()), StringUtil.TIME_WITH_DAY);
		String key = "DriverGrabMessage_*_*_" + param.getOrderno()+"_*";
		String sameTime = "DriverGrabMessage_"+ driver.getId() +"_"+ driver.getPhone() +"_*_" + useday;
		Set<String> keys = JedisUtil.getKeys(key);
		Set<String> sameKeys = JedisUtil.getKeys(sameTime);
		List<String> phones = new ArrayList<>();
		for(String k : keys){
			JedisUtil.delKey(k);
			//剔除当前司机
			if(driver.getPhone().equals(k.split("_")[2])) continue;
			phones.add(k.split("_")[2]);
		}
		//删除该司机同一天的订单
		for(String k : sameKeys){
			JedisUtil.delKey(k);
		}
		if(!phones.isEmpty()){
			//给被删除消息的司机发送通知
			sendMessage4Order(phones);
		}
		return true;
	}
	
	/**
	 * 给司机推送订单失效(可以共用出租车发送逻辑,type一样即可)
	 */
	private boolean sendMessage4Order(List<String> phones){
		String messagetype = TaxiOrderMessage.TAXI_DRIVERMESSAGE;
		TaxiOrderMessage om = null;
		//推送给其他之前推送过抢单消息的司机
		om = new TaxiOrderMessage(messagetype,phones);
		MessageUtil.sendMessage(om);
		return true;
	}
	/**********************************************************校验方法***************************************************************/
	/**
	 * 检查司机状态是否能够接单
	 * @param driver
	 * @param order
	 * @param sow
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean checkDriverState(PubDriver driver){
		String workstatus = driver.getWorkstatus();
		 //司机状态不是空闲
		if(!DriverState.IDLE.code.equals(workstatus)){
			return false;
		} 
		return true;
	}
	
	/**
	 * 校验订单列表类型
	 * @param type
	 * @return
	 */
	@ValidateRule("订单列表类型不正确")
	private boolean checkOrderListType(int type){
		return OrderListEnum.DEFAULT.state == type ? true :
					OrderListEnum.CURRENT.state == type ? true : 
					OrderListEnum.WAITPAY.state == type ? true : 
					OrderListEnum.COMPLETE.state == type ? true : false;
	}
	
	/**
	 * 校验今日订单列表请求类型
	 * @param todaytype
	 * @return
	 */
	@ValidateRule("今日订单列表请求类型错误")
	private boolean checkTodayOrderListType(int todaytype){
		return OrderListEnum.CURRENT.state == todaytype ? true :
					OrderListEnum.WAITPAY.state == todaytype ? true : false; 
	}
	
	/**
	 * 校验司机是否可以抢单(已经有即刻单不能再抢,预约订单不能抢同一天的)
	 * @param orderstate
	 * @return
	 */
	@ValidateRule("司机存在与抢单规则相斥的订单")
	private boolean checkDriverCanTakeOrder(String orderstate){
		//如果当前请求不是抢单,则不校验此规则
		if(!OrderState.WAITSTART.state.equals(orderstate)){
			return true;
		}
		//检查司机的未出行订单和是否有服务中订单
		int estimated = 0;
		if(order.get() instanceof OrgOrder){
			estimated = ((OrgOrder)order.get()).getEstimatedtime();
		}else if(order.get() instanceof OpOrder){
			estimated = ((OpOrder)order.get()).getEstimatedtime();
		}else if (order.get() instanceof OpTaxiOrder) {
			estimated = ((OpTaxiOrder)order.get()).getEstimatedtime();
		}
		errorResult.get().put("status", Retcode.ORDERNOTDONE.code);
		errorResult.get().put("message", Retcode.ORDERNOTDONE.msg);
		OrderListParam olp = new OrderListParam();
		olp.setType(OrderListEnum.CURRENT.state);
		olp.setDriverid(driver.get().getId());
		List<OrderInfoDetail> currentOrder = dao.getOrderList(olp);
		if(order.get().isIsusenow()){   //当前是即刻单
			for(OrderInfoDetail o : currentOrder){
				//当前订单预估时长(秒) + 1小时
				int estimatedSecond = estimated*60 + 3600;
				//预估结束时间
				Date noTakeStart = StringUtil.addDate(order.get().getUsetime(), estimatedSecond);
				//存在未开始的即刻单或正在服务的订单
				if(o.isIsusenow() || o.getStarttime() != null){
					return false;
				//当前订单的预估结束时间(用车时间+预估时长+1小时)不在已存在的预约单用车时间之前是不可以接的
				}else if(!noTakeStart.before(o.getUsetime())){
					return false;
				}
			}
		}else{   //当前是预约单
			for(OrderInfoDetail o : currentOrder){
				if(o.getStarttime() != null){  //存在正在服务的订单
					int minute = (int)o.getEstimatedtime();  //预估时间
					//调度时间(当前订单的用车时间,必须晚于服务中订单的预估时间的2倍)
	                Date temptime = StringUtil.addDate(o.getStarttime(), minute*60*2);
	                if(temptime.after(order.get().getUsetime())){
	                	return false;
	                }
	            //当前订单用车时间不在已存在的即刻单一个小时之后是不可以接的
				}else if(o.isIsusenow() && order.get().getUsetime().before(StringUtil.addDate(o.getUsetime(), 3600))){
					return false;
				//当前订单的用车时间与已存在的预约单是同一天,不可以接
				}else if (!o.isIsusenow() && StringUtil.getToday(order.get().getUsetime()).equals(StringUtil.getToday(o.getUsetime()))) {
					return false;
				}
			}
		}
		errorResult.get().clear();
		return true;
	}
	
    /**
     * 校验usetype
     * @param usetype
     * @return
     */
    @ValidateRule("用车类型错误")
    private boolean checkUseType(String usetype){
    	this.usetype.set(OrderEnum.getUseType(usetype));
    	return this.usetype.get() != null;
    }
    
    /**
     * 校验ordertype
     * @param ordertype
     * @return
     */
    @ValidateRule("订单类型错误")
    private boolean checkOrderType(String ordertype){
    	this.ordertype.set(OrderEnum.getOrderType(ordertype));
    	return this.ordertype.get() != null;
    }
    
	/**
	 * 校验订单号格式
	 * @param orderno
	 * @return
	 */
	@ValidateRule("订单号格式不正确")
	private boolean checkOrderNO(String orderno){
		order.set(null);
		OrderApiParam oap = new OrderApiParam();
		oap.setOrderno(orderno);
		oap.setDriverid(driver.get().getId());
		//根据平台不同获取不同平台订单
		if(OrderEnum.ORDERTYPE_TAXI.equals(ordertype.get())){
			if(OrderEnum.USETYPE_PERSONAL.equals(usetype.get())){
				order.set(dao.getOpTaxiOrder(orderno));	  //目前只有运管端有出租车
//			}else{
//				order.set(taxiorderfuncdao.getOrgTaxiOrder(oap));	//目前只有运管端有出租车
			}
		}else {
			if(OrderEnum.USETYPE_PERSONAL.equals(usetype.get())){
				order.set(dao.getOpOrder(orderno));
			}else{
				order.set(dao.getOrgOrder(orderno));
			}
		}
		if(order.get() == null 
			|| (!StringUtil.isEmpty(order.get().getDriverid()) && !order.get().getDriverid().equals(driver.get().getId()))){
			errorResult.get().put("status", Retcode.ORDERNOTEXIT.code);
			errorResult.get().put("message", Retcode.ORDERNOTEXIT.msg);
			return false;
		}
		return true;
	}
	
	/**
	 * 检测订单状态是否正确
	 * @param orderstatus
	 * @return
	 */
	@ValidateRule("订单状态不正确")
	private boolean checkOrderStatus(String orderstatus){
		boolean flag = false;
		OrderState paramState = OrderState.getByCode(orderstatus);
		OrderState orderState = OrderState.getByCode(order.get().getOrderstatus());
		flag  = OrderState.WAITSTART.equals(paramState) ? OrderState.WAITTAKE.equals(orderState) : 
					OrderState.START.equals(paramState) ? OrderState.WAITSTART.equals(orderState) : 
					OrderState.ARRIVAL.equals(paramState) ? OrderState.START.equals(orderState) : 
//					OrderState.PICKUP.equals(paramState) ? OrderState.ARRIVAL.equals(orderState) :  //不存在接到乘客了
					OrderState.INSERVICE.equals(paramState) ? OrderState.ARRIVAL.equals(orderState) : 
					OrderState.WAITMONEY.equals(paramState) ? OrderState.INSERVICE.equals(orderState) : 
					OrderState.SERVICEDONE.equals(paramState) ? OrderState.WAITMONEY.equals(orderState) : false;
		//订单已取消的情况下操作
		if(!flag && OrderState.CANCEL.equals(orderState)){
			//如果是抢单操作,还需记录举手日志
			if(OrderState.WAITSTART.equals(paramState)){
				OrderApiParam oap = new OrderApiParam();
				oap.setOrderno(order.get().getOrderno());
				oap.setUsetype(order.get().getUsetype());
				oap.setOrdertype(order.get().getOrdertype());
				OrderInfoDetail oid = dao.getOrderInfoById(oap);
				orderCancel(oid, driver.get());
			}
			errorResult.get().put("status", Retcode.ORDERISCANCEL.code);
			errorResult.get().put("message", Retcode.ORDERISCANCEL.msg);
		//订单已完成的情况下调用结束
		}else if(!flag && OrderState.SERVICEDONE.equals(orderState) 
			&& (OrderState.WAITMONEY.equals(paramState) 
			||OrderState.SERVICEDONE.equals(paramState))){
			errorResult.get().put("status", Retcode.ORDER_ALREADY_END.code);
			errorResult.get().put("message", Retcode.ORDER_ALREADY_END.msg);
		}else if(!flag){
			errorResult.get().put("status", Retcode.INVALIDORDERSTATUS.code);
			errorResult.get().put("message", Retcode.INVALIDORDERSTATUS.msg);
		}
		return flag;
	}
	
	/**
	 * 校验支付方式是否正确
	 * @param paymethod
	 * @return
	 */
	@ValidateRule("支付渠道不正确")
	private boolean checkPayMethod(String paymethod){
		boolean flag;
		//检查是否在支付渠道范围内
		flag =  PayMethod.BALANCE.code.equals(paymethod) ? true : 
					PayMethod.WECHAT.code.equals(paymethod) ? true : 
					PayMethod.ALIPAY.code.equals(paymethod) ? true : false;
		//余额支付或不在范围内直接返回
		if(PayMethod.BALANCE.code.equals(paymethod) || !flag) return flag;
		//第三方支付还需检查商户是否开通(订单支付根据订单归属而非司机归属)
		if(usetype.get() != null && !OrderEnum.USETYPE_PERSONAL.equals(usetype.get())){
			leasescompany.set(accdao.getCompanyById(order.get().getCompanyid()));
			if(leasescompany.get() == null) return false;
			flag =  PayMethod.WECHAT.code.equals(paymethod) ? 
						PurseEnum.ACCOUNT_ON.code.equals(leasescompany.get().getDriverwechatstatus()) : 
						PayMethod.ALIPAY.code.equals(paymethod) ? 
						PurseEnum.ACCOUNT_ON.code.equals(leasescompany.get().getDriveralipaystatus()) : false;
			
		}else if (usetype.get() != null && OrderEnum.USETYPE_PERSONAL.equals(usetype.get())) {
			platformInfo.set(accdao.getPlatformInfo());
			if(platformInfo.get() == null) return false;
			flag =  PayMethod.WECHAT.code.equals(paymethod) ? 
						PurseEnum.ACCOUNT_ON.code.equals(platformInfo.get().getDriverwechatstatus()) : 
						PayMethod.ALIPAY.code.equals(paymethod) ? 
						PurseEnum.ACCOUNT_ON.code.equals(platformInfo.get().getDriveralipaystatus()) : false;
		}
		if(!flag){ //如果这里失败表示,第三方支付账户没有开通
			String pm = PayMethod.WECHAT.code.equals(paymethod) ? "微信" : "支付宝";
			errorResult.get().put("status", Retcode.NOPAYCHANNEL.code);
			errorResult.get().put("message", "暂不支持"+ pm +"支付");
		}
		return flag;
	}
}
