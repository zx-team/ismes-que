package com.isesol.mes.ismes.que.activity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.isesol.ismes.platform.module.Bundle;
import com.isesol.ismes.platform.module.Parameters;
import com.isesol.ismes.platform.module.Sys;

/**
 * 设备能力与负载查询
 */
public class SbnlfzcxActivity {
	
	private Logger log4j = Logger.getLogger(SbnlfzcxActivity.class);
	
	/**
	 * 设备能力负载公式
	 * 加工单元 报工数量 * 节拍时间 / 加工单元内的设备日历工时之和
	 */
	
	/**
	 * 设备能力负载查询
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String sbnlfzcxIndex(Parameters parameters, Bundle bundle){
		
		bundle.put("end", date2String(new Date(), "yyyy-MM-dd"));
		
		Calendar c = Calendar.getInstance();
	    c.add(Calendar.MONTH, -1);
		bundle.put("begin",  date2String(c.getTime(), "yyyy-MM-dd"));
		
		return "sbnlfzcx";
	}
	
	/**
	 * 设备能力负载查询的表格
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String table(Parameters parameters, Bundle bundle){
		//查询的时间范围
		String begin = parameters.getString("begin");
		String end = parameters.getString("end");
		String param = " ";
		List<Object> valueList = new ArrayList<Object>();
		if(StringUtils.isBlank(begin)){
			Calendar c = Calendar.getInstance();
		    c.add(Calendar.MONTH, -1);
		    begin = date2String(c.getTime(), "yyyy-MM-dd");
		}
		param = param + "and jhjssj > ? ";
		Date beginDate= string2Date(begin);
		valueList.add(beginDate);
		
		if(StringUtils.isBlank(end)){
			end  = date2String(new Date(), "yyyy-MM-dd");
		}
		param = param + "and jhkssj < ? ";
		Date endDate= string2Date(end +" 23:59:59");
		valueList.add(endDate);
		
		String jgdybh = parameters.getString("jgdybh");
		String jgdymc = parameters.getString("jgdymc");
		List<String> jgdyList = new ArrayList<String>();
		boolean flag = false;
		if(StringUtils.isNotBlank(jgdybh) || StringUtils.isNotBlank(jgdymc)){
			flag = true;
			Parameters jgdy_parameters = new Parameters();
			jgdy_parameters.set("jgdybh", jgdybh);
			jgdy_parameters.set("jgdymc", jgdymc);
			Bundle jgdy_bundle = Sys.callModuleService("em", "emservice_jgdyByParam", jgdy_parameters);
			List<Map<String,Object>> jgdyDataList = (List<Map<String, Object>>) jgdy_bundle.get("dataList");
			if(CollectionUtils.isNotEmpty(jgdyDataList)){
				for(Map<String,Object> m : jgdyDataList){
					jgdyList.add(m.get("jgdyid").toString());
				}
			}
		}
		
		
		Parameters workorder_parameters = new Parameters();
		workorder_parameters.set("sbIdList", jgdyList);
		workorder_parameters.set("sbidNullReturnFlag", flag);
		workorder_parameters.set("jhsj_param", param);
		workorder_parameters.set("jhsj_param_value", valueList);
		workorder_parameters.set("page_flag_str", "false");
		int page = parameters.getInteger("page") == null ? 1 : parameters.getInteger("page") ;
		int pageSize = parameters.getInteger("pageSize") == null ? 1 : parameters.getInteger("pageSize") ;
		
		Bundle gd_bundle = Sys.callModuleService("pl", "plservice_gdxx_param", workorder_parameters);
		//符合的所有的工单
		List<Map<String,Object>> list = (List<Map<String, Object>>) gd_bundle.get("gdxxList");
		//key 是加工单元id   list 是工单list
		Map<String,List<Map<String,Object>>> jgdy_flag_map = new HashMap<String,List<Map<String,Object>>>();
		List<String> jgdyids = new ArrayList<String>();
		//循环工单
		if(CollectionUtils.isNotEmpty(list)){
			for(Map<String,Object> map : list){
				
				String jgdyid = map.get("sbid").toString();
				if(!jgdy_flag_map.containsKey(jgdyid)){
					List<Map<String,Object>> jgdy_gd_list = new ArrayList<Map<String,Object>>();
					jgdy_gd_list.add(map);
					jgdy_flag_map.put(jgdyid, jgdy_gd_list);
					jgdyids.add(jgdyid);
				}else{
					jgdy_flag_map.get(jgdyid).add(map);
				}
			}
		}
		Parameters jgdy_parameters = new Parameters();
		jgdy_parameters.set("jgdyids", jgdyids);
		jgdy_parameters.set("page", page);
		jgdy_parameters.set("pageSize", pageSize);
		Bundle jgdy_bundle = Sys.callModuleService("em", "emservice_jgdyByIds", jgdy_parameters);
		
		List<Map<String,Object>> returnList = (List<Map<String, Object>>) jgdy_bundle.get("jgdyList");
		
		if(CollectionUtils.isNotEmpty(returnList)){
			for(Map<String,Object> jgdyMap : returnList){
				String jgdyid = jgdyMap.get("jgdyid").toString();
				jgdyMap.put("rowKey", jgdyid + "_" + begin + "_" + end);
				//加工单元的设备集合
				Parameters jgdysbglb_parameters = new Parameters();
				jgdysbglb_parameters.set("jgdyid", jgdyid);
				Bundle jgdysbglb_bundle = Sys.callModuleService("em",
						"emservice_jgdysbglb", jgdysbglb_parameters);
				List<Map<String,Object>> sbidList = 
						(List<Map<String, Object>>) jgdysbglb_bundle.get("sbidList");
				BigDecimal jgdy_shouldwork_hours = BigDecimal.ZERO;
				//加工单元内的设备日历
				for(Map<String,Object> m : sbidList){
					String sbid = m.get("sbid").toString();
					
					Parameters sbrl_parameters = new Parameters();
					sbrl_parameters.set("sbid", sbid);
					sbrl_parameters.set("begin", beginDate);
					sbrl_parameters.set("end", endDate);
					Bundle sbrl_bundle = Sys.callModuleService("em",
							"query_sb_shouldJobTime", sbrl_parameters);
					List<Map<String,Object>> sbrlList = (List<Map<String, Object>>) sbrl_bundle.get("list");
					for(Map<String,Object> m_sbrl : sbrlList){
						jgdy_shouldwork_hours = jgdy_shouldwork_hours.add((BigDecimal)m_sbrl.get("work_hours"));
					}
				}
				
				//这个加工单元下的所有的工单
				List<Map<String, Object>> gdList = jgdy_flag_map.get(jgdyid);
				BigDecimal jgdygzsj = BigDecimal.ZERO;
				for(Map<String, Object> gd_map : gdList ){
					//工序组id
					String gxid = gd_map.get("gxid").toString();
					
					//根据加工单元 + 工序 == 工序组&加工单元关联表，得到 节拍时间
					Parameters gxz_jgdy_parameters = new Parameters();
					gxz_jgdy_parameters.set("gxzid", gxid);
					gxz_jgdy_parameters.set("jgdyid", jgdyid);
					Bundle jgdy_gxz_bundle = Sys.callModuleService("pm",
							"pmservice_query_time_by_gxzid_jgdyid", gxz_jgdy_parameters);
					//节拍时间
					int jgjpSecond = (Integer) jgdy_gxz_bundle.get("jgjp");
					
					//报工个数    应该得到时间范围内的加工个数
					int bgsl = getGdjgsl(gd_map, beginDate, endDate);
					
					//加工单元工作时间 （小时）
					BigDecimal gdgzsj = new BigDecimal(bgsl).multiply(new BigDecimal(jgjpSecond)).divide
							(new BigDecimal(3600), 2, BigDecimal.ROUND_HALF_UP);
					
					jgdygzsj = jgdygzsj.add(gdgzsj);
				}
				
				jgdyMap.put("jgdy_shouldwork_hours", jgdy_shouldwork_hours);
				jgdyMap.put("jgdygzsj", jgdygzsj);
				
				jgdyMap.put("sbfh", jgdygzsj.multiply(new BigDecimal(100)).divide(
						jgdy_shouldwork_hours, 2, BigDecimal.ROUND_HALF_UP));
				
			}
		}
		//遍历每一个加工单元
		
		bundle.put("rows", returnList);
		bundle.put("totalPage", jgdy_bundle.get("totalPage"));
		bundle.put("currentPage", jgdy_bundle.get("currentPage"));
		bundle.put("totalRecord", jgdy_bundle.get("totalRecord"));
		return "json:";
	}
	
	public String subtable(Parameters parameters, Bundle bundle){
		String rowKey = parameters.getString("parentRowid");
		String jgdyid = rowKey.split("_")[0];
		String begin = rowKey.split("_")[1];
		String end = rowKey.split("_")[2];
		Date beginDate= string2Date(begin);
		Date endDate= string2Date(end +" 23:59:59");
		Parameters workorder_parameters = new Parameters();
		List<String> jgdyidList = new ArrayList<String>();
		jgdyidList.add(jgdyid);
		workorder_parameters.set("sbIdList", jgdyidList);
		workorder_parameters.set("sbidNullReturnFlag", true);
		
		List<Object> valueList = new ArrayList<Object>();
		String param = " ";
		param = param + "and jhjssj > ? ";
		valueList.add(beginDate);
		param = param + "and jhkssj < ? ";
		valueList.add(endDate);
		workorder_parameters.set("jhsj_param", param);
		workorder_parameters.set("jhsj_param_value", valueList);
		workorder_parameters.set("page_flag_str", "false");
		
		
		Bundle gd_bundle = Sys.callModuleService("pl", "plservice_gdxx_param", workorder_parameters);
		//符合的所有的工单
		List<Map<String,Object>> list = (List<Map<String, Object>>) gd_bundle.get("gdxxList");
		for(Map<String,Object> gd_map : list){
			
			String gxid = gd_map.get("gxid").toString();
			
			//根据加工单元 + 工序 == 工序组&加工单元关联表，得到 节拍时间
			Parameters gxz_jgdy_parameters = new Parameters();
			gxz_jgdy_parameters.set("gxzid", gxid);
			gxz_jgdy_parameters.set("jgdyid", jgdyid);
			Bundle jgdy_bundle = Sys.callModuleService("pm",
					"pmservice_query_time_by_gxzid_jgdyid", gxz_jgdy_parameters);
			
			Parameters p_gxz = new Parameters();
			p_gxz.set("gxid", gxid);
			Bundle bundle_gxz = Sys.callModuleService("pm", "queryGxzxxByGxid", p_gxz);
			Map<String, Object> gxzMap = (Map<String, Object>) bundle_gxz.get("gxxx");
			gd_map.put("gxzmc", gxzMap.get("gxzmc"));
			
			//节拍时间
			int jgjpSecond = (Integer) jgdy_bundle.get("jgjp");
			
			//报工个数    应该得到时间范围内的加工个数
			int bgsl = getGdjgsl(gd_map, beginDate, endDate);
			gd_map.put("bgsl", bgsl);
			
			//加工单元工作时间 （小时）
			BigDecimal gdgzsj = new BigDecimal(bgsl).multiply(new BigDecimal(jgjpSecond)).divide
				(new BigDecimal(3600), 2, BigDecimal.ROUND_HALF_UP);
			gd_map.put("gdgzsj", gdgzsj);
			
		}
		bundle.put("rows", list);
		bundle.put("totalPage", 1);
		bundle.put("currentPage", 1);
		bundle.put("totalRecord", 200);
		
		return "json:";
	}
	
	private int getGdjgsl(Map<String,Object> gdmap,Date scopeBegin,Date scopeEnd){
		//NC 自动报工数量
		Integer ncbgsl = null;
		if(gdmap.get("ncbgsl") != null){
			ncbgsl = (Integer) gdmap.get("ncbgsl");
		}
		//人工报工数量
		Integer gdywcsl = 0;
		if(gdmap.get("gdywcsl") != null){
			gdywcsl = (Integer)gdmap.get("gdywcsl");
		}
		//报废工废
		Integer bfgf = null;
		if(gdmap.get("bfgf") != null){
			bfgf = Integer.valueOf(gdmap.get("bfgf").toString());
		}
		//报废料废
		Integer bflf = null;
		if(gdmap.get("bflf") != null){
			bflf = Integer.valueOf(gdmap.get("bflf").toString());
		}
		
		Integer bgsl = ncbgsl == null || ncbgsl == 0 ? gdywcsl + bfgf + bflf : ncbgsl;
		
		//计划开始时间
		Date jhkssj = (Date) gdmap.get("jhkssj");
		//计划结束时间
		Date jhjssj = (Date) gdmap.get("jhjssj");
		
		//如果开始时间  和  结束时间都在这个范围内，返回报工数量
		if(jhkssj.compareTo(scopeBegin) > 0 && jhjssj.compareTo(scopeEnd) < 0 ){
			return bgsl;
		}
		//如果 在范围外，返回0
		if(jhjssj.compareTo(scopeBegin) < 0 || jhkssj.compareTo(scopeEnd) > 0){
			return 0;
		}
		//如果重叠，取范围内，查找报工流水
		Parameters pc_parameters = new Parameters();
		pc_parameters.set("gdid", gdmap.get("gdid"));
		//开始时间比范围时间小，结束时间比范围时间小
		if(jhkssj.compareTo(scopeBegin)<0 &&  jhjssj.compareTo(scopeEnd) < 0) {
			pc_parameters.set("bgrq_start", scopeBegin);
			pc_parameters.set("bgrq_end", jhjssj);
		}
		//结束时间比范围时间大，开始时间比范围时间大
		if(jhkssj.compareTo(scopeBegin)>0 &&  jhjssj.compareTo(scopeEnd) > 0){
			pc_parameters.set("bgrq_start", jhkssj);
			pc_parameters.set("bgrq_end", scopeEnd);
		}
		Bundle pc_bundle = Sys.callModuleService("pc",
				"queryservice_bgls", pc_parameters);
		List<Map<String,Object>> bgls_list = (List<Map<String, Object>>) pc_bundle.get("bglsList");
		if(CollectionUtils.isEmpty(bgls_list)){
			return 0 ;
		}
		//自动报工
		Set<String> set = new HashSet<String>();
		//手动报工
		int sdbgsl = 0 ;
		int sdbfsl = 0 ;
		for(Map<String,Object> bgls : bgls_list){
			//手动报工
			if("-1".equals(bgls.get("bgbj").toString())){
				sdbgsl = sdbgsl +(Integer) bgls.get("bgsl");
				sdbfsl = sdbfsl +(Integer) bgls.get("bfsl");
			}
			//自动报工
			else{
				set.add((String) bgls.get("bgbj"));
			}
		}
		if(set.size() > 0){
			return set.size();
		}
		else{
			return sdbgsl + sdbfsl;
		}
	}
	
	
	
	private BigDecimal betweenTimesHours(Date begin,Date end){
		if(begin == null || end == null){
			return BigDecimal.ZERO;
		}
		return 	new BigDecimal(end.getTime() - begin.getTime())
				.divide(new BigDecimal(1000*60*60),2,BigDecimal.ROUND_HALF_UP);
	}
	
	private Date string2Date(String timeStr){
	    if(!timeStr.contains(":")){
	    	timeStr = timeStr + " 00:00:00";
	    }
	    String format ="";
	    if(timeStr.contains("-")){
	    	format = "yyyy-MM-dd HH:mm:ss";
	    }
	    if(timeStr.contains("/")){
	    	format = "yyyy/MM/dd HH:mm:ss";
	    }
	    SimpleDateFormat formatter=new SimpleDateFormat(format);  
	    try {
			return formatter.parse(timeStr);
		} catch (ParseException e) {
			log4j.info("时间转换出现异常;;;"+timeStr);
			log4j.error(e.getMessage());
			return null;
		} 
	}
	
	private String date2String(Date time,String format){
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(time);
	}
	
}




