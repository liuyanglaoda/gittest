package com.szyciov.driver.controller;

import java.security.NoSuchAlgorithmException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.szyciov.driver.param.AtworkParam;
import com.szyciov.driver.param.BaseParam;
import com.szyciov.driver.param.ChangeDriverIconParam;
import com.szyciov.driver.param.ChangePasswordParam;
import com.szyciov.driver.param.GetCommonInfoParam;
import com.szyciov.driver.param.LoginParam;
import com.szyciov.driver.param.LogoutParam;
import com.szyciov.driver.param.SendSMSParam;
import com.szyciov.driver.param.UploadOrdinatesParam;
import com.szyciov.driver.service.AccountService;
import com.szyciov.util.ApiExceptionHandle;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;

/**
 * 账号信息控制器
 * @ClassName AccountController 
 * @author Efy Shu
 * @Description 处理账号相关的请求 
 * @date 2016年8月24日 上午9:19:47
 */
@RestController
@RequestMapping("/")
@Api(value = "处理账号相关的请求 ",description = " ")
@Slf4j
public class AccountController extends ApiExceptionHandle {
	private AccountService as;
	
	@Resource(name="AccountService")
	public void setAs(AccountService as){
		this.as = as;
	}
	
	public AccountController() {
		
	}
	
	//=====================================================================//
	/**
	 * 登入
	 * @param param
	 * @return
	 * @throws NoSuchAlgorithmException 
	 */
	@ResponseBody
	@RequestMapping(value="Driver/Login")
	@ApiOperation(value = "登入", notes = "登入")
	public JSONObject login(@RequestBody LoginParam param,HttpServletRequest request){
		starttime.set(System.currentTimeMillis());
		param.setIpaddr(request == null ? "" : request.getRemoteAddr());
		JSONObject result = as.login(param);
		releaseResource(as);
		return checkResult(result);
	}

	/**
	 * 登出
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/Logout")
	@ApiOperation(value = "登出", notes = "登出")
	public JSONObject logout(@RequestBody LogoutParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.logout(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 上/下班
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/AtWork")
	@ApiOperation(value = "上/下班", notes = "上/下班")
	public JSONObject atwork(@RequestBody AtworkParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.atwork(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 发送短信验证码
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/SendSMS")
	@ApiOperation(value = "发送短信验证码", notes = "发送短信验证码")
	public JSONObject sendsms(@RequestBody SendSMSParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.sendSMS(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 获取司机个人信息
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetDriverInfo")
	@ApiOperation(value = "获取司机个人信息", notes = "获取司机个人信息")
	public JSONObject getDriverInfo(@RequestBody BaseParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.getDriverInfo(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 更换司机头像
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/ChangeDriverIcon")
	@ApiOperation(value = "更换司机头像", notes = "更换司机头像")
	public JSONObject changeDriverIcon(@RequestBody ChangeDriverIconParam param,HttpServletRequest request){
//		JSONObject result = new JSONObject();
//		try {
//			BufferedInputStream bis = new BufferedInputStream(request.getInputStream());
//			BufferedImage bi = ImageIO.read(bis);
//			String realPath = request.getSession().getServletContext().getRealPath("/upload/icon/");
//			File file = new File(realPath);
//			if(!file.exists())	file.mkdirs();
//			String account = UserTokenManager.getUserNameFromToken(token, UserTokenManager.DRIVERUSER);
//			file = new File(realPath+account+".png");
//			ImageIO.write(bi, "png", file);
//			json.put("status", Retcode.OK.code);
//			json.put("message", Retcode.OK.msg);
//		} catch (Exception e) {
//			json.put("status", Retcode.EXCEPTION.code);
//			json.put("message", Retcode.EXCEPTION.msg);
//		}
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.changeDriverIcon(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 更换密码
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/ChangePassword")
	@ApiOperation(value = "更换密码", notes = "更换密码")
	public JSONObject changePassword(@RequestBody ChangePasswordParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.changePassword(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 验证原始密码
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/AuthOldPass")
	@ApiOperation(value = "验证原始密码", notes = "验证原始密码")
	public JSONObject authOldPass(@RequestBody ChangePasswordParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.authOldPass(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 上传坐标
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/UploadOrdinates")
	@ApiOperation(value = "上传坐标", notes = "上传坐标")
	public JSONObject uploadOrdinates(@RequestBody UploadOrdinatesParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.uploadOrdinates(param);
		releaseResource(as);
		return checkResult(result);
	}

	/**
	 * 检查版本信息
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/CheckAPPVersion")
	@ApiOperation(value = "检查版本信息", notes = "检查版本信息")
	public JSONObject checkAPPVersion(@RequestBody GetCommonInfoParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.checkAPPVersion(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 获取广告页
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetADPage")
	@ApiOperation(value = "获取广告页", notes = "获取广告页")
	public JSONObject getADPage(@RequestBody GetCommonInfoParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.getADPage(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 获取引导页
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/GetGuidePage")
	@ApiOperation(value = "获取引导页", notes = "获取引导页")
	public JSONObject getGuidePage(@RequestBody GetCommonInfoParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.getGuidePage(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 一键报警
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/CallPolice")
	@ApiOperation(value = "一键报警", notes = "一键报警")
	public JSONObject callPolice(@RequestBody UploadOrdinatesParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.callPolice(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 保存极光推送ID
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/SaveRegID")
	@ApiOperation(value = "保存极光推送ID", notes = "保存极光推送ID")
	public JSONObject saveRegID(@RequestBody LoginParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.saveRegID(param);
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 删除极光推送ID
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/RemoveOBD")
	@ApiOperation(value = "删除极光推送ID", notes = "删除极光推送ID")
	public JSONObject removeOBD(){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.removeOBD();
		releaseResource(as);
		return checkResult(result);
	}
	
	/**
	 * 校验短信验证码
	 * @param param
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="Driver/AuthSMSCode")
	@ApiOperation(value = "校验短信验证码", notes = "校验短信验证码")
	public JSONObject authSMSCode(@RequestBody LoginParam param){
		starttime.set(System.currentTimeMillis());
		JSONObject result = as.authSMSCode(param);
		releaseResource(as);
		return checkResult(result);
	}
}
