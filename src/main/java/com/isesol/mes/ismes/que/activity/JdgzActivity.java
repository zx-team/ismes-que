package com.isesol.mes.ismes.que.activity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.isesol.ismes.platform.core.service.bean.Dataset;
import com.isesol.ismes.platform.module.Bundle;
import com.isesol.ismes.platform.module.Parameters;
import com.isesol.ismes.platform.module.Sys;

import net.sf.json.JSONArray;

public class JdgzActivity {
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	DecimalFormat fnum = new DecimalFormat("##0.00"); 
	

	/**根据批次ID查询工单进度
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String query_gdjd(Parameters parameters, Bundle bundle) {
		String pcid= parameters.getString("pcid"); 
		if(StringUtils.isBlank(pcid))
		{
			return "json:";
		}
		Bundle b_gdxx = Sys.callModuleService("pl", "plservice_gdxxfy", parameters);
		if(null!=b_gdxx)
		{
			List<Map<String, Object>> gdjd = (List<Map<String, Object>>) b_gdxx.get("rows");
			for(int i=0;i<gdjd.size();i++){
				parameters.set("gxzid", gdjd.get(i).get("gxid"));
				Bundle b_gxzxx = Sys.callModuleService("pm", "queryGxzxxByGxid_new", parameters);
				Map<String, Object> gxzxx = (Map<String, Object>) b_gxzxx.get("gxxx");
				gdjd.get(i).put("gxmc", gxzxx.get("gxzmc"));
			}
			//计算生产任务百分比
			for (int i = 0; i < gdjd.size(); i++) {
				gdjd.get(i).put("wcjd",""+(Math.round((Integer.parseInt(gdjd.get(i).get("gdywcsl").toString())*10000)/Integer.parseInt(gdjd.get(i).get("jgsl").toString()))/100.0));
			}
			bundle.put("rows", gdjd);
			bundle.put("totalPage",  b_gdxx.get("totalPage"));
			bundle.put("currentPage",  b_gdxx.get("currentPage"));
			bundle.put("totalRecord",  b_gdxx.get("totalRecord"));
		}
		return "json:";
	}
	
	/**加工任务列表
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String table_jgrw(Parameters parameters, Bundle bundle) {
		//查询零件信息
		Bundle b_ljxx = Sys.callModuleService("pm", "pmservice_ljxxbybhmc", parameters);
		if (null==b_ljxx) {
			return "json:";
		}
		List<Map<String, Object>> ljxx = (List<Map<String, Object>>) b_ljxx.get("ljxx");
		if (ljxx.size()<=0) {
			return "json:";
		}
		String val_lj = "(";
		for (int i = 0; i < ljxx.size(); i++) {
			if(i!=0)
			{
				val_lj = val_lj +",";
			}
			val_lj += "'" +ljxx.get(i).get("ljid")+"'";
		} 
		val_lj = val_lj +")";
		parameters.set("val_lj", val_lj);
		
		String querySign = parameters.getString("query_sign");
		if (querySign == null && StringUtils.isEmpty(parameters.getString("scrwbh"))) {
			// 非查询时默认显示状态为“执行中”
			parameters.set("query_rwzt", "20");
		} else {
			// 通过其他页跳转过来的并且指定了"生产任务编号"的不加执行中状态
			if (StringUtils.isNotEmpty(parameters.getString("query_rwzt"))) {
				parameters.set("query_rwzt", parameters.getString("query_rwzt"));
			}
		}
		parameters.set("sortName", "scrwlrsj");
		parameters.set("sortOrder", "desc");
		Bundle b_scrw = Sys.callModuleService("pro", "proService_scrw", parameters);
		List<Map<String, Object>> scrw = (List<Map<String, Object>>) b_scrw.get("rows");
		bundle.put("totalPage", b_scrw.get("totalPage"));
		bundle.put("currentPage",b_scrw.get("currentPage"));
		bundle.put("totalRecord", b_scrw.get("totalRecord"));
		if (scrw.size()<=0) {
			return "json:";
		}
		String val_scrw = "(";
		for (int i = 0; i < scrw.size(); i++) {
			scrw.get(i).put("scrwzt", scrwzt(scrw.get(i).get("scrwztdm")));
			for (int j = 0; j < ljxx.size(); j++) {
				if (scrw.get(i).get("ljid").toString().equals(ljxx.get(j).get("ljid").toString())) {
					scrw.get(i).put("ljbh", ljxx.get(j).get("ljbh"));
					scrw.get(i).put("ljmc", ljxx.get(j).get("ljmc"));
					break;
				}
			}
			String scrwid = scrw.get(i).get("scrwid") + "";
			parameters.set("scrwid", scrwid);
			Bundle ydclxx_bundle = Sys.callModuleService("pl", 
					"plservice_query_EquivalentYieldByScrw", parameters);
			
			if(ydclxx_bundle == null){
				scrw.get(i).put("ywcsl", 0);
				scrw.get(i).put("wxwcsl", 0);
				scrw.get(i).put("wcjd", 0.0);
				scrw.get(i).put("wxwcjd", 0.0);
				continue;
			}
			
			Map<String, Object> ydcl_map = (Map<String, Object>)ydclxx_bundle.get("map");
			
			scrw.get(i).put("ywcsl",ydcl_map.get("scrw_ydclsl"));
			scrw.get(i).put("wcjd", new BigDecimal(ydcl_map.get("scrw_ydcljd") + "").
					multiply(new BigDecimal(100)));
			scrw.get(i).put("wxwcsl", 0);
			scrw.get(i).put("wxwcjd", 0.0);
//			//初始化完成数量
//			scrw.get(i).put("ywcsl", 0);
//			scrw.get(i).put("wxwcsl", 0);
//			scrw.get(i).put("wcjd", 0.0);
//			scrw.get(i).put("wxwcjd", 0.0);
//			
//			if(i!=0)
//			{
//				val_scrw = val_scrw +",";
//			}
//			val_scrw += scrw.get(i).get("scrwid");
		}
//		val_scrw = val_scrw +")";
//		parameters.set("val_scrw", val_scrw);
//		
//		//根据生产任务ID，查询生产任务批次信息
//		Bundle b_pcxx = Sys.callModuleService("pro", "proService_pcxx", parameters);
//		List<Map<String, Object>> pcxx = (List<Map<String, Object>>) b_pcxx.get("pcxx");
//		
//		String val_pc = "";
//		for(int i = 0, len = pcxx.size(); i < len; i++){
//			if(i==0)
//			{
//				val_pc += "(";
//			}
//			val_pc += "'"+pcxx.get(i).get("scrwpcid")+"'";
//			if(i < (len - 1)){
//				val_pc += ",";
//			} else{
//				val_pc += ")";
//			}
//			pcxx.get(i).put("ywcsl", 0);
//		}
//		parameters.set("val_pc", val_pc.toString());
//		
//		Bundle b_gdxx = Sys.callModuleService("pl", "proService_pcxx", parameters);
//		if(null!=b_gdxx)
//		{
//			List<Map<String, Object>> pcgdxx = (List<Map<String, Object>>) b_gdxx.get("pcgdxx");
//			
//			for (int i = 0; i < pcxx.size(); i++) {
//				for (int j = 0; j < pcgdxx.size(); j++) {
//					if (pcxx.get(i).get("scrwpcid").toString().equals(pcgdxx.get(j).get("pcid").toString())) {
//						pcxx.get(i).put("ywcsl",Float.parseFloat(pcgdxx.get(j).get("gdywcsl").toString()));
//					}
//				}
//			}
//
//			//计算生产任务已完成数量
//			for (int i = 0; i < scrw.size(); i++) {
//				for (int j = 0; j < pcxx.size(); j++) {
//					if (scrw.get(i).get("scrwid").toString().equals(pcxx.get(j).get("scrwid").toString())) {
//						scrw.get(i).put("ywcsl",Float.parseFloat(scrw.get(i).get("ywcsl").toString())+ Float.parseFloat(pcxx.get(j).get("ywcsl").toString()));
//					}
//				}
//			}
//			//计算生产任务百分比
//			for (int i = 0; i < scrw.size(); i++) {
//				scrw.get(i).put("wcjd",""+(Math.round((Float.parseFloat(scrw.get(i).get("ywcsl").toString())*10000)/Float.parseFloat(scrw.get(i).get("jgsl").toString()))/100.0));
//			}
//		}
		
		bundle.put("rows", scrw);
		return "json:";
	}
	
	/**工单约当产量的计算,递归
	 * @param parameters
	 * @param bundle
	 */
	public Map<String, Object> gx_ydcl(List<Map<String, Object>> gxxx ,Map<String, Object> gxMap) {
		Map<String, Object> gxxxMap = new HashMap<String, Object>();
		gxxxMap.put("ydcl", 0);//约当产量
		gxxxMap.put("wxbz", "1");//尾序标识
		for (int i = 0; i < gxxx.size(); i++) {
			if(gxMap.get("gxid").equals(gxxx.get(i).get("qxid")))
			{
				
				gxxx.get(i).put("qxjgsj", Integer.parseInt(gxxx.get(i).get("jgfs").toString()) + Integer.parseInt(gxMap.get("qxjgsj").toString()));
				gxxxMap = gx_ydcl(gxxx,gxxx.get(i));   
				
				if("1".equals(gxxxMap.get("wxbz")))
				{
					gxxxMap.put("wxwcsl", gxxx.get(i).get("wcsl"));
					gxxx.get(i).put("zzpsl", Integer.parseInt(gxMap.get("wcsl").toString()));
				}else{
					gxxxMap.put("wxwcsl", gxxxMap.get("wxwcsl"));
					gxxx.get(i).put("zzpsl", Integer.parseInt(gxMap.get("wcsl").toString()) - Integer.parseInt(gxxx.get(i).get("wcsl").toString()));
				}
				if(0!=Integer.parseInt(gxxx.get(i).get("jgsl").toString()))
				{
					float zzpsl = Float.parseFloat(gxxx.get(i).get("zzpsl").toString());
					float wcsl = Float.parseFloat(gxxx.get(i).get("wcsl").toString());
					float jgsl = Float.parseFloat(gxxx.get(i).get("jgsl").toString());
					float jgfs = Float.parseFloat(gxxx.get(i).get("jgfs").toString());
					float zjgsj = Float.parseFloat(gxxx.get(i).get("zjgsj").toString());
					float qxjgsj = Float.parseFloat(gxMap.get("qxjgsj").toString());
					gxxxMap.put("ydcl", Float.parseFloat(gxxxMap.get("ydcl").toString())+(zzpsl*((wcsl*100/jgsl)*jgfs+qxjgsj*100)/zjgsj)/100.0);
				}
				gxxxMap.put("wxbz", "0");
				break;
			}
		}
		
		return gxxxMap;
	}
	
	/**加工任务批次
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String table_jgrwpcxx(Parameters parameters, Bundle bundle) {
		String val_scrw = parameters.getString("parentRowid");
		if (StringUtils.isBlank(val_scrw)) {
			return "json:";
		}
		parameters.set("val_scrw", "('"+val_scrw + "')"); 
		//根据生产任务ID，查询生产任务批次信息
		Bundle b_pcxx = Sys.callModuleService("pro", "proService_pcxxfy", parameters);
		if (null==b_pcxx) {
			return "json:";
		} 
		List<Map<String, Object>> pcxx = (List<Map<String, Object>>) b_pcxx.get("pcxx");
		if (null==pcxx||pcxx.size()<=0){
			return "json:";
		}
		bundle.put("totalPage", b_pcxx.get("totalPage"));
		bundle.put("currentPage",b_pcxx.get("currentPage"));
		bundle.put("totalRecord", b_pcxx.get("totalRecord"));
		
		String val_pc = "";
		for (int i = 0, len = pcxx.size(); i < len; i++) {
			pcxx.get(i).put("pcjhzt", pcjhzt(pcxx.get(i).get("pcjhztdm")));
			parameters.set("pcid", pcxx.get(i).get("scrwpcid"));
			Bundle ydcl_bundle = Sys.callModuleService("pl", "plservice_query_EquivalentYieldByPc", parameters);
			
			Map<String, Object> ydcl_map = (Map<String, Object>)ydcl_bundle.get("map");
			pcxx.get(i).put("wxwcsl", ydcl_map.get("wcsl"));
			pcxx.get(i).put("ywcsl", ydcl_map.get("pc_ydclsl"));
			pcxx.get(i).put("wcjd", new BigDecimal(ydcl_map.get("pc_ydcljd") + "").
					multiply(new BigDecimal(100)));
			
		}
		
//		parameters.set("val_pc", val_pc.toString());
//		Bundle b_gdxx = Sys.callModuleService("pl", "plservice_gdjgjd", parameters);
//		if (null != b_gdxx) {
//			List<Map<String, Object>> pcgdxx = (List<Map<String, Object>>) b_gdxx.get("pcgdxx");
//			for (int i = 0; i < pcxx.size(); i++) {
//				for (int j = 0; j < pcgdxx.size(); j++) {
//					if (pcxx.get(i).get("scrwpcid").toString().equals(pcgdxx.get(j).get("pcid").toString())) {
//						pcxx.get(i).put("ywcsl",Float.parseFloat(pcgdxx.get(j).get("gdywcsl").toString()));
//						pcxx.get(i).put("wcjd",""+(Math.round((Float.parseFloat(pcxx.get(i).get("ywcsl").toString())*10000)/Float.parseFloat(pcxx.get(i).get("pcsl").toString()))/100.0));
//					}
//				}
//			}
//		}
		bundle.put("rows",pcxx);
		return "json:";
	}
	
	/**跳转质量确认界面
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String query_jdgz(Parameters parameters, Bundle bundle) {
		Map<String, Object> scrw = new HashMap<String, Object>();
		if(null!=parameters.get("scrwbh"))
		{
			scrw.put("scrwbh", parameters.get("scrwbh"));
		}else{
			scrw.put("scrwbh", "");
		}
		bundle.put("scrw", scrw);
		return "que_jdgz";
	}
	
	/**保存报工数量
	 * @param parameters
	 * @param bundle
	 */
	public void save_bgsl(Parameters parameters, Bundle bundle) {
		Sys.callModuleService("pl", "plservice_save_bgsl", parameters);
	}
	
	/**申请质检
	 * @param parameters
	 * @param bundle
	 */
	public void save_sqzj(Parameters parameters, Bundle bundle) {
		Sys.callModuleService("pl", "plservice_save_sqzj", parameters);
	}
	
	/**
	 * 根据生产任务状态代码获取状态名称
	 * @param dm
	 * @return
	 */
	private String scrwzt(Object dm) {
		String name = "";
		if (dm != null) {
			switch (Integer.parseInt(String.valueOf(dm))) {
			case 10:
				name = "未执行";
				break;
			case 20:
				name = "执行中";
				break;
			case 30:
				name = "已完成";
				break;
			case 40:
				name = "已终止";
				break;
			}
		}
		return name;
	}
	
	private String pcjhzt(Object dm) {
		String name = "";
		if (dm != null) {
			switch (Integer.parseInt(String.valueOf(dm))) {
			case 10:
				name = "未下发";
				break;
			case 20:
				name = "已下发";
				break;
			case 30:
				name = "计划制定中";
				break;
			case 40:
				name = "工单已生成";
				break;
			case 50:
				name = "工单已下发";
				break;
			case 70:
				name = "加工中";
				break;
			case 80:
				name = "加工完成";
				break;
			case 85:
				name = "已入库";
				break;
			case 90:
				name = "已终止";
				break;
			}
		}
		return name;
	}
}




