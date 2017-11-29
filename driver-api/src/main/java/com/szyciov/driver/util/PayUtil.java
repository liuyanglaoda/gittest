package com.szyciov.driver.util;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.config.AlipayConfig;
import com.alipay.util.OrderInfoUtil;
import com.szyciov.driver.enums.PayUtilEnum;
import com.szyciov.dto.PayInfoDto;
import com.szyciov.util.StringUtil;
import com.szyciov.util.SystemConfig;
import com.szyciov.util.UNID;
import com.wx.DocFunc;
import com.wx.WXOrderUtil;

import net.sf.json.JSONObject;

/**
 * @ClassName PayUtil
 * @author Efy Shu
 * @Description 第三方支付工具类
 * @date 2017年3月31日 下午8:08:29
 */
public class PayUtil {
	/**项目名*/
	private final static String PAYBODY = SystemConfig.getSystemProperty("pay_body","如约的士");
	/**项目地址*/
	private final static String PROJECT_URL = SystemConfig.getSystemProperty("project_url","http://szycwh.f3322.net:8004");
	/**微信回调接口*/
	private final static String WECHAT_NOTIFY_URL = PROJECT_URL + "/driver-api/WeChatCallBack";
	/**支付宝回调接口*/
	private final static String ALIPAY_NOTIFY_URL = PROJECT_URL + "/driver-api/AliPayCallBack";
	/**是否真实付款*/
	private final static boolean PAY_MODE = "on".equals(SystemConfig.getSystemProperty("pay_mode","off"));
	/**超时时间*/
	private final static String TIMEOUT = "30m";
	
	private String ipaddr;
	private String orderno;
	private PayUtilEnum payType;

	
	public static void main(String[] args) {
		PayUtil pu = new PayUtil(":",PayUtilEnum.PAYORDER,"BSI1710240900003");
		String partner = "2088521158240961";
		String aliPrivateKey = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAOjV55GV7qTtZnD76N/I8wSz8RtLUtFT9k3RISi6SwVtMQQvwHNr/Pe+gN0MW5eOqzikRFbQ3B16dW1QUVu5cY7XkYlIldAK1JQn4sio5sqJgxA0Club2cxKGgUNioj2y4kPYhN4ZIrhTdz9pPuDhuOpbxXQ1QsD1WWADwYakwHXAgMBAAECgYAxnvy2Ez0D2zBc3eL4ZmwcUXkN9xSUVg+E8A/gDSvV4Tp0CPU75ATKi8gM1AhlGVu2O5Pw6JwwkBuci2R7Zt8jSQqyLL6z4jdmuWluZCTaKhaVAu1kGwVIPR1l5ranOvajKQzQ5WNoqOJVzRUEAqTXPeOSmn5q+zzCZ6WOKAUbaQJBAPjgl2J1KSkQN4PMbn/ZYVAkAn0rvFjDaHlqnNpD2nJc6pZ85ulSDnQqt8w/h9qoVWF+GdnDv5VhWSSBPSqgBBMCQQDvf8hk6TIEqPrEmZdCKJZQeGtBe92Ekc/kvmGnouCP8fGGu+X4cIflFPI2a/mmeJNmVYcUk6QmVXIOrt9JvNutAkAn0hemw0RAs72OMwmDH074uapESNksAqgWtT4/lhe/sKpARd/UeTKi16rs3UVpcQGoRbrxIubmidrvglY9GblNAkEAyDuFRxjQAKVmQshGdcGJKm4C/hSY9yURMqUY8BZ0uOQGkia19ife9d+1QVq0tkFIut32uXVWX9ZALZZ2iCelYQJBAJ/WU9KgCvsfNuhNFIiBOxNhE/0QddTV+32bgwCSOHnWTksFJMpgnTIoEfFMhXkxYmVBGxLquJzDEQecPIe5ifg=";
//		String wecPrivateKey = "Guangzhou1234xunxinbugaosunimima";
//		String wechatAppid = "wx1fd8f930707cd040";
//		String shno = "1397089802";
		pu.createAliPayQRCode(100.00, aliPrivateKey, partner);
//		pu.createWeChatQRCode(1000, wechatAppid, wecPrivateKey, shno);
	}
	
	public PayUtil() {
		
	}
	
	/**
	 * 构造
	 * @param ipaddr               来源IP
	 * @param payType            支付类型(充值|支付订单)
	 */
	public PayUtil(String ipaddr,PayUtilEnum payType) {
		this.ipaddr = ipaddr.indexOf(":")>=0?"0.0.0.0":ipaddr;
		this.payType = payType;
	}
	
	/**
	 * 构造
	 * @param ipaddr               来源IP
	 * @param payType            支付类型(充值|支付订单)
	 * @param orderno             订单号
	 */
	public PayUtil(String ipaddr,PayUtilEnum payType,String orderno) {
		this.ipaddr = ipaddr.indexOf(":")>=0?"0.0.0.0":ipaddr;
		this.orderno = orderno;
		this.payType = payType;
	}
	
	/**
	 * 创建微信支付流程
	 * @param amount
	 * @param appid
	 * @param privateKey
	 * @param shno
	 * @return
	 */
	public JSONObject createWeChatPay(double amount,String appid,String privateKey,String shno) {
		try {
			String out_trade_no = getOutTradeNO();
			Map<String, String> prepayorderparam = WXOrderUtil.createPreOrderParam(appid, shno, PAYBODY, out_trade_no,
					PAY_MODE ? (long)(amount*100)+"" : "1",
					WECHAT_NOTIFY_URL, "APP",ipaddr);
			Document doc = WXOrderUtil.createPreOrderInfo(prepayorderparam, WXOrderUtil.getSign(prepayorderparam, privateKey));
			Map<String, String> preres = WXOrderUtil.getPreOrderId(DocFunc.doc2String(doc), "https://api.mch.weixin.qq.com/pay/unifiedorder");
			if(preres != null && "0".equalsIgnoreCase(preres.get("status"))){
				String prepay_id = preres.get("prepay_id");
				Map<String,String> payorderparam = WXOrderUtil.createPayOrderParam(appid, shno, prepay_id);
				String sign = WXOrderUtil.getSign(payorderparam, privateKey);
				payorderparam.put("sign", sign);
				
				JSONObject result = new JSONObject();
				result.put("out_trade_no", out_trade_no);
				result.put("payorderinfo", payorderparam);
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 创建支付宝支付流程
	 * @param amount   需要支付的金额
	 * @param appid                支付宝appid
	 * @param privateKey        支付宝私钥
	 * @return
	 */
	public JSONObject createAliPay(double amount,String appid,String privateKey) {
		try{
			String out_trade_no = getOutTradeNO();
			String title =  PAYBODY + (PayUtilEnum.PAYORDER.equals(payType) ? payType.msg + orderno : payType.msg);
			Map<String, String> payorderparam = OrderInfoUtil.buildOrderParamMap(appid, TIMEOUT, PAY_MODE ? amount : 0.01D,
					title, PAYBODY, out_trade_no, ALIPAY_NOTIFY_URL);
			String payorderstr = OrderInfoUtil.buildOrderParam(payorderparam);
			String sign = OrderInfoUtil.getSign(payorderparam, privateKey);
			String payorderinfo = payorderstr + "&" + sign;
			JSONObject result = new JSONObject();
			result.put("out_trade_no", out_trade_no);
			result.put("payorderinfo", payorderinfo);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 创建微信二维码支付
	 * @param amount
	 * @param appid
	 * @param privateKey
	 * @param shno
	 * @return
	 */
	public PayInfoDto createWeChatQRCode(double amount,String appid,String privateKey,String shno){
		try{
			String out_trade_no = getOutTradeNO();
			String code_url = null;
			Map<String, String> prepayorderparam = WXOrderUtil.createPreOrderParam(
					appid, shno, PAYBODY, out_trade_no,
					PAY_MODE ? (long)(amount*100)+"" : "1",
					WECHAT_NOTIFY_URL, "NATIVE",ipaddr);
			Document doc = WXOrderUtil.createPreOrderInfo(prepayorderparam, WXOrderUtil.getSign(prepayorderparam, privateKey));
			Map<String,String> res = WXOrderUtil.getPreOrderCodeUrl(DocFunc.doc2String(doc), "https://api.mch.weixin.qq.com/pay/unifiedorder");
			if(res!=null && "0".equalsIgnoreCase(res.get("status"))){
				code_url = res.get("code_url");
			}
			PayInfoDto result = new PayInfoDto();
			result.setOuttradeno(out_trade_no);
			result.setCodeurl(code_url.trim());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 创建支付宝二维码支付
	 * @param amount
	 * @param appid
	 * @param privateKey
	 * @param shno
	 * @return
	 */
	public PayInfoDto createAliPayQRCode(double amount,String privateKey,String partner){
		try{
			String out_trade_no = getOutTradeNO();
	        //订单名称，必填
			String subject =  PAYBODY + (PayUtilEnum.PAYORDER.equals(payType) ? payType.msg + orderno : payType.msg);
	        //付款金额，必填
	        String total_fee = PAY_MODE ? (amount)+"" : "0.01";
	        //商品描述，可空
	        String body = "sdgsdg123123";
	        
			//////////////////////////////////////////////////////////////////////////////////
			//把请求参数打包成数组
			Map<String, String> sParaTemp = new HashMap<String, String>();
			sParaTemp.put("service", "create_direct_pay_by_user");
	        sParaTemp.put("partner", partner);
	        sParaTemp.put("seller_id", partner);
	        sParaTemp.put("_input_charset", "UTF-8");
			sParaTemp.put("payment_type", "1");
			sParaTemp.put("notify_url", ALIPAY_NOTIFY_URL);
			/* sParaTemp.put("return_url", basePath+"resource/zfbreturn?id="+orderno); */
			sParaTemp.put("anti_phishing_key", "");
			sParaTemp.put("exter_invoke_ip", "");
			sParaTemp.put("out_trade_no", out_trade_no);
			sParaTemp.put("subject", subject);
			sParaTemp.put("total_fee", total_fee);
			sParaTemp.put("body", body);
	        sParaTemp.put("qr_pay_mode", "4");
	        sParaTemp.put("qrcode_width", "105");
			//建立请求
			String sHtmlText = OrderInfoUtil.buildRequest(sParaTemp,"get","确认",privateKey);
			StringBuffer sb = new StringBuffer();
			sb.append("<!DOCTYPE html>\r\n");
			sb.append("<html>\r\n");
			sb.append("<head>\r\n");
			sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\r\n");
			sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\r\n");
			sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n");
			sb.append("</head>\r\n");
			sb.append("<body>\r\n");
			sb.append(sHtmlText + "\r\n");
			sb.append("</body>\r\n");
			sb.append("</html>");
			System.out.println(sb.toString());
			PayInfoDto result = new PayInfoDto();
			result.setOuttradeno(out_trade_no);
			result.setCodeurl(sb.toString());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 获取微信支付结果
	 * @param request
	 * @return 成功时,返回out_trade_no和tradeno,失败时返回null
	 */
	public Map<String, Object> checkWeChatStatus(HttpServletRequest request){
		Map<String, Object> result = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(request.getInputStream());
			Node return_code = doc.getElementsByTagName("return_code").item(0);
			if(return_code == null) return result;
			if ("SUCCESS".equalsIgnoreCase(return_code.getFirstChild().getNodeValue())) {
				Node result_code = doc.getElementsByTagName("result_code").item(0);
				if(result_code == null) return result;
				if ("SUCCESS".equalsIgnoreCase(result_code.getFirstChild().getNodeValue())) {
					// attach存储订单号，根据订单号修改订单状态
					Node out_trade_no = doc.getElementsByTagName("out_trade_no").item(0);
					String outtradeno = out_trade_no.getFirstChild().getNodeValue();
					String tradeno = doc.getElementsByTagName("transaction_id").item(0).getFirstChild().getNodeValue();
					//成功时,返回out_trade_no和tradeno
					result = new HashMap<>();
					result.put("out_trade_no", outtradeno);
					result.put("tradeno", tradeno);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 获取支付宝支付结果
	 * @param request
	 * @return 成功时,返回out_trade_no和tradeno,失败时返回null
	 */
	public Map<String, Object> checkAliPayStatus(HttpServletRequest request){
		Map<String, Object> result = null;
		//根据支付结果，更改订单状态，并且返回支付宝“success”
		String result_code = request.getParameter("trade_status");
		if("TRADE_SUCCESS".equalsIgnoreCase(result_code)){
			String outtradeno = request.getParameter("out_trade_no");
			String tradeno = request.getParameter("trade_no");
			//成功时,返回out_trade_no和tradeno
			result = new HashMap<>();
			result.put("out_trade_no", outtradeno);
			result.put("tradeno", tradeno);
		}
		return result;
	}
	
	/**
	 * 支付宝验签方法
	 * @param request
	 * @param alipubkey
	 * @return
	 * @throws AlipayApiException
	 */
	public boolean isZFBSignValid(HttpServletRequest request, String alipubkey) throws AlipayApiException {
		Map<String,String> pp = new HashMap<String,String>();
		Enumeration<String> pnames = request.getParameterNames();
		while(pnames.hasMoreElements()){
			String pname = pnames.nextElement();
			pp.put(pname, request.getParameter(pname));
		}
		return AlipaySignature.rsaCheckV1(pp, alipubkey, AlipayConfig.input_charset);
	}
	
	/**
	 * 生成交易号
	 * @return
	 */
	private String getOutTradeNO(){
		// 获取订单的交易号 (时间加上5位随机码)
		return StringUtil.formatDate(new Date(), "yyyyMMddHHmmssSSS") + UNID.getUNID();
	}
	
	/***************************************************Getter & Setter***********************************************************/
	/**  
	 * 设置ipaddr  
	 * @param ipaddr ipaddr  
	 */
	public void setIpaddr(String ipaddr) {
		this.ipaddr = ipaddr;
	}
	

	/**  
	 * 设置payType  
	 * @param payType payType  
	 */
	public void setPayType(PayUtilEnum payType) {
		this.payType = payType;
	}
	
}

