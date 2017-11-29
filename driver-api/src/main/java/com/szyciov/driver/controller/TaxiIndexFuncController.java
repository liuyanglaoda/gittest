package com.szyciov.driver.controller;


import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.szyciov.driver.param.ShiftWorkParam;
import com.szyciov.driver.service.TaxiIndexFuncService;
import com.szyciov.util.ApiExceptionHandle;

import net.sf.json.JSONObject;
import org.springframework.web.bind.annotation.RestController;


/**
  * @ClassName TaxiIndexFuncController
  * @author Efy Shu
  * @Description 出租车首页功能Controller
  * @date 2017年3月20日 09:47:20
  */
@RestController("TaxiIndexFuncController")
@RequestMapping("/")
@Api(value = "出租车首页功能",description = " ")
@Slf4j
public class TaxiIndexFuncController extends ApiExceptionHandle{

	/**
	  *依赖
	  */
	private TaxiIndexFuncService taxiindexfuncservice;

	/**
	  *依赖注入
	  */
	@Resource(name="TaxiIndexFuncService")
	public void setTaxiIndexFuncService(TaxiIndexFuncService taxiindexfuncservice){
		this.taxiindexfuncservice=taxiindexfuncservice;
	}
	
	/**
	 * 获取对班司机列表
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetWorkmates")
	@ApiOperation(value = "获取对班司机列表", notes = "获取对班司机列表")
	public JSONObject getWorkmates(@RequestBody ShiftWorkParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = taxiindexfuncservice.getWorkmates(param);
		releaseResource(taxiindexfuncservice);
		return checkResult(result);
	}
	
	/**
	 * 交班申请
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/ShiftWorkOFF")
	@ApiOperation(value = "交班申请", notes = "交班申请")
	public JSONObject shiftWorkOFF(@RequestBody ShiftWorkParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = taxiindexfuncservice.shiftWorkOFF(param);
		releaseResource(taxiindexfuncservice);
		return checkResult(result);
	}
	
	/**
	 * 接班申请
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/ShiftWorkON")
	@ApiOperation(value = "接班申请", notes = "接班申请")
	public JSONObject shiftWorkON(@RequestBody ShiftWorkParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = taxiindexfuncservice.shiftWorkON(param);
		releaseResource(taxiindexfuncservice);
		return checkResult(result);
	}
	
	/**
	 * 是否存在交接班申请
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/HasShiftWorkApply")
	@ApiOperation(value = "是否存在交接班申请", notes = "是否存在交接班申请")
	public JSONObject hasShiftWorkApply(@RequestBody ShiftWorkParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = taxiindexfuncservice.hasShiftWorkApply(param);
		releaseResource(taxiindexfuncservice);
		return checkResult(result);
	}
	
	/**
	 * 取消交班申请
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/CancelShiftWork")
	@ApiOperation(value = "取消交班申请", notes = "取消交班申请")
	public JSONObject cancelShiftWork(@RequestBody ShiftWorkParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = taxiindexfuncservice.cancelShiftWork(param);
		releaseResource(taxiindexfuncservice);
		return checkResult(result);
	}
	
	/**
	 * 司机心跳包
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/HeartPack")
	@ApiOperation(value = "司机心跳包", notes = "司机心跳包")
	public JSONObject heartPack(@RequestBody ShiftWorkParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = taxiindexfuncservice.heartPack(param);
		releaseResource(taxiindexfuncservice);
		return checkResult(result);
	}
}
