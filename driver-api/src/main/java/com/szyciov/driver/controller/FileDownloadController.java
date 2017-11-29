package com.szyciov.driver.controller;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.szyciov.entity.Excel;
import com.szyciov.entity.PubAirPortAddr;
import com.szyciov.util.ApiExceptionHandle;
import com.szyciov.util.ExcelExport;
import com.szyciov.util.GUIDGenerator;
import com.szyciov.util.ReadExcel;
import com.szyciov.util.StringUtil;

import net.sf.json.JSONObject;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Api(value = "文件下载相关的请求 ",description = " ")
@Slf4j
public class FileDownloadController extends ApiExceptionHandle {
	
	@RequestMapping(value="/exportExcel")
	@ApiOperation(value = "导出excel", notes = "导出excel")
	public void exportExcel(HttpServletRequest request,HttpServletResponse response) throws Exception{
		// excel文件
		File tempFile = new File("全国机场数据.xls");
		ReadExcel re = new ReadExcel();
		String path = "E:\\工作文档\\工作簿1.xls";
		List<PubAirPortAddr> airportaddrs = re.readExcel(path);
		HttpClient http = new DefaultHttpClient();
		String url = "http://api.map.baidu.com/geocoder/v2/?output=json&ak=5WMQoMkG1MBaizuSdXiT3o1xYRSokdGs&";
		for(PubAirPortAddr a : airportaddrs){
			a.setId(GUIDGenerator.newGUID());
			String param = "city=" + a.getCity().trim() +"&" + "address="+a.getName().trim();
			HttpGet get = new HttpGet(url + param);
			get.setHeader("referer", url);
			HttpResponse resp = http.execute(get);
			String res = EntityUtils.toString(resp.getEntity(),"UTF-8");
			JSONObject locationJson = JSONObject.fromObject(res);
			if(locationJson.isNullObject()){
				continue;
			}else{
				locationJson = locationJson.getJSONObject("result");
				if(locationJson.isNullObject()){
					continue;
				}else{
					locationJson = locationJson.getJSONObject("location");
				}
			}
			double lng = locationJson.getDouble("lng");
			double lat = locationJson.getDouble("lat");
			a.setLng(lng);
			a.setLat(lat);
//			System.out.println(res);
		}
	
		//填充数据
		List<String> colNames = new ArrayList<>();
		Map<String, List<Object>> colDatas = new HashMap<>();
		Field[] fields = PubAirPortAddr.class.getDeclaredFields();
		for(Field f : fields){
			colNames.add(f.getName());
			List<Object> colData = new ArrayList<>();
			for(PubAirPortAddr a : airportaddrs){
				colData.add(a.getClass().getMethod("get"+StringUtil.upperHeader(f.getName())).invoke(a));
			}
			colDatas.put(f.getName(), colData);
		}
		Excel excel = new Excel();
		excel.setColName(colNames);
		excel.setColData(colDatas);

		ExcelExport ee = new ExcelExport(request,response,excel);
		ee.createExcel(tempFile);
	}
}
