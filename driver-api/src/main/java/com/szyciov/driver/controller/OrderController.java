package com.szyciov.driver.controller;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.szyciov.base.BaseBO;
import com.szyciov.driver.bo.DriverCollectionCashBo;
import com.szyciov.driver.bo.GetCertificateBO;
import com.szyciov.driver.enums.OrderListEnum;
import com.szyciov.driver.enums.OrderState;
import com.szyciov.driver.param.CertificateUploadParam;
import com.szyciov.driver.param.ChangeOrderStateParam;
import com.szyciov.driver.param.DriverCollectionCashParam;
import com.szyciov.driver.param.DriverMessageParam;
import com.szyciov.driver.param.GetCertificateListParam;
import com.szyciov.driver.param.NewsParam;
import com.szyciov.driver.param.OrderCostParam;
import com.szyciov.driver.param.OrderLineParam;
import com.szyciov.driver.param.OrderListParam;
import com.szyciov.driver.param.OrderStatisticsParam;
import com.szyciov.driver.service.OrderService;
import com.szyciov.dto.driver.ConfirmCostDto;
import com.szyciov.entity.OrderReviewParam;
import com.szyciov.entity.PayMethod;
import com.szyciov.entity.Retcode;
import com.szyciov.param.OrderApiParam;
import com.szyciov.util.ApiExceptionHandle;
import com.szyciov.util.StringUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;

/**
 * 订单控制器
 * @ClassName OrderController 
 * @author Efy Shu
 * @Description 处理订单相关的请求
 * @date 2016年8月24日 上午9:18:54
 */
@RestController
@RequestMapping("/")
@Api(value = "处理订单相关的请求",description = " ")
@Slf4j
public class OrderController extends ApiExceptionHandle {
	private OrderService os;
	
	public OrderController() {
	}
	
	@Resource(name="OrderService")
	public void setOs(OrderService os) {
		this.os = os;
	}
	
	//=====================================================================//
	/**
	 * 获取新消息
	 * @param param
	 * @return
	 * @see {@link PollMessageParam}
	 */
	@ResponseBody
	@RequestMapping(value="Driver/PollMessage")
	@ApiOperation(value = "获取新消息", notes = "获取新消息")
	public JSONObject pollMessage(@RequestBody NewsParam param) {
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.pollMessage(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 获取订单列表接口
	 * @param param
	 * @return
	 * @see {@link OrderListParam}
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetOrderList")
	@ApiOperation(value = "获取订单列表接口", notes = "获取订单列表接口")
	public JSONObject getOrderList(@RequestBody OrderListParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.getOrderList(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 获取今日订单
	 * @param param
	 * @return
	 * @see {@link OrderListParam}
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetTodayOrders")
	@ApiOperation(value = "获取今日订单", notes = "获取今日订单")
	public JSONObject getTodayOrders(@RequestBody OrderListParam param){
		starttime.set(System.currentTimeMillis());
		Date start = new Date();
		Date end = new Date(start.getTime() + (1000*60*60*24));
		param.setToday(true);
		param.setStarttime(StringUtil.formatDate(start,"yyyy-MM-dd"));
		param.setEndtime(StringUtil.formatDate(end,"yyyy-MM-dd"));
		JSONObject result = os.getTodayOrders(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 变更订单状态
	 * @param param
	 * @return
	 * @see {@link OrderService.changeOrderState()}
	 */
	@ResponseBody
	@RequestMapping(value="Driver/ChangeOrderState")
	@ApiOperation(value = "变更订单状态", notes = "变更订单状态")
	public JSONObject changeOrderState(@RequestBody ChangeOrderStateParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.changeOrderState(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 网约车订单确费
	 * @param dto
	 * @return
	 * @see {@link OrderService.confirmCost()}
	 */
	@ResponseBody
	@RequestMapping(value="Driver/NetCarConfirmCost")
	@ApiOperation(value = "网约车订单确费", notes = "网约车订单确费")
	public JSONObject confirmCost(@RequestBody ConfirmCostDto dto){
		starttime.set(System.currentTimeMillis());
		dto.setOrderstatus(OrderState.SERVICEDONE.state);
		JSONObject result = os.confirmCost(dto);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 获取订单状态
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetOrderState")
	@ApiOperation(value = "获取订单状态", notes = "获取订单状态")
	public JSONObject getOrderState(@RequestBody OrderApiParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.getOrderState(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 获取轨迹线
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetOrderLine")
	@ApiOperation(value = "获取轨迹线", notes = "获取轨迹线")
	public JSONObject getOrderLine(@RequestBody OrderLineParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.getOrderLine(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 抢单
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/TakingOrder")
	@ApiOperation(value = "抢单", notes = "抢单")
	public JSONObject takingOrder(@RequestBody OrderApiParam param){
		starttime.set(System.currentTimeMillis());
		ChangeOrderStateParam cosp = new ChangeOrderStateParam();
		cosp.setToken(param.getToken());
		cosp.setOrderno(param.getOrderno());
		cosp.setOrderstate(OrderState.WAITSTART.state);
		cosp.setUsetype(param.getUsetype());
		cosp.setOrdertype(param.getOrdertype());
		JSONObject result = os.changeOrderState(cosp);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 我的贡献
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/OrderStatistics")
	@ApiOperation(value = "我的贡献", notes = "我的贡献")
	public JSONObject orderStatistics(@RequestBody OrderStatisticsParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.orderStatistics(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 申请复核
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/ApplyForReview")
	@ApiOperation(value = "申请复核", notes = "申请复核")
	public JSONObject applyForReview(@RequestBody OrderReviewParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.applyForReview(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 获取实时车费
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetCurrentCost")
	@ApiOperation(value = "获取实时车费", notes = "获取实时车费")
	public JSONObject getCurrentCost(@RequestBody OrderCostParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = os.getCurrentCost(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 获取当前订单
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetCurrentOrder")
	@ApiOperation(value = "获取当前订单", notes = "获取当前订单")
	public JSONObject getCurrentOrder(@RequestBody OrderListParam param){
		starttime.set(System.currentTimeMillis());
		param.setType(OrderListEnum.DEFAULT.state); //只取服务中订单
		JSONObject result = os.getCurrentOrder(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 司机阅读消息
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/Driver/ReadNews")
	@ApiOperation(value = "司机阅读消息", notes = "司机阅读消息")
	public JSONObject readNews(@RequestBody NewsParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result =   os.readNews(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 司机删除消息
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/Driver/DelNews")
	@ApiOperation(value = "司机删除消息", notes = "司机删除消息")
	public JSONObject delNews(@RequestBody NewsParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result =   os.delNews(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 司机订单提醒
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/Driver/OrderRemind")
	@ApiOperation(value = "司机订单提醒", notes = "司机订单提醒")
	public JSONObject orderRemind(@RequestBody OrderApiParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result =   os.orderRemind(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 司机订单提醒
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/Driver/GetOrderMessageList")
	@ApiOperation(value = "司机订单提醒", notes = "司机订单提醒")
	public JSONObject getOrderMessageList(@RequestBody DriverMessageParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result =   os.getOrderMessageList(param);
		releaseResource(os);
		return checkResult(result);
	}
	
	/**
	 * 司机代收现金
	 * @param param
	 * @return
	 * @throws IOException 
	 */
	@ResponseBody
	@RequestMapping(value="/Driver/DriverCollectionCash", method = RequestMethod.GET)
	@ApiOperation(value = "司机代收现金", notes = "司机代收现金")
	public DriverCollectionCashBo driverCollectionCash(DriverCollectionCashParam param,HttpServletRequest request,HttpServletResponse response) throws IOException{
		starttime.set(System.currentTimeMillis());
		//保存ip地址
		param.setIpaddr(request.getRemoteAddr());
		DriverCollectionCashBo result =   (DriverCollectionCashBo) os.driverCollectionCash(param);
		releaseResource(os);
		if(PayMethod.ALIPAY.code.equals(param.getPaymethod()) 
			&& result.getStatus() == Retcode.OK.code){
			response.setHeader("Content-Type", "text/html;charset=UTF-8");
			response.getWriter().write(result.getUrl());
			return null;
		}
		return checkResult(result);
	}
	
    @ResponseBody
    @RequestMapping(value = "/Driver/CertificateUpload", method = RequestMethod.POST)
	@ApiOperation(value = "证书上传", notes = "证书上传")
	public BaseBO certificateUpload(@RequestBody CertificateUploadParam param) {
        try {
            BaseBO bo = os.certificateUpload(param);
            bo.setServertime(System.currentTimeMillis());
            return bo;
        } catch (Exception ex) {
            BaseBO bo = new BaseBO();
            bo.setStatus(Retcode.EXCEPTION.code);
            bo.setMessage(Retcode.EXCEPTION.msg);
            bo.setServertime(System.currentTimeMillis());
            return bo;
        }
    }


    @ResponseBody
    @RequestMapping(value = "/Driver/GetCertificateList", method = RequestMethod.POST)
	@ApiOperation(value = "获取证书列表", notes = "获取证书列表")
	public GetCertificateBO getCertificateList(@RequestBody GetCertificateListParam param) {
        try {
            GetCertificateBO result = os.getCertificateList(param);
            result.setServertime(System.currentTimeMillis());
            return result;
        } catch (Exception ex) {
            GetCertificateBO bo = new GetCertificateBO();
            bo.setStatus(Retcode.EXCEPTION.code);
            bo.setMessage(Retcode.EXCEPTION.msg);
            return bo;
        }
    }
}
