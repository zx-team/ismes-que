package com.isesol.mes.ismes.que.activity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.isesol.ismes.platform.module.Bundle;
import com.isesol.ismes.platform.module.Parameters;
import com.isesol.ismes.platform.module.Sys;
import com.isesol.mes.ismes.que.constant.ScxxsscxConstant;

/**
 * 生产信息实时查询
 */
public class ScxxsscxActivity {
	
	public static final String DAY = "day";
	public static final String MONTH = "month";
	
	public static Map<String,Object> mbStaticMap = new HashMap<String, Object>();
	public static Map<String,Object> gsStaticMap = new HashMap<String, Object>();

	private Logger log4j = Logger.getLogger(ScxxsscxActivity.class);
	/**
	 * 页面初始化
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String scxxsscxIndex(Parameters parameters, Bundle bundle){
		getNumbers(parameters, bundle);
		return "scxxcx";
	}
	
	public String getNumbers(Parameters parameters, Bundle bundle){
		parameters.set("ifPage", false);
		parameters.set("time_frame_flag", DAY);
		
		//得到需要得到的设备
		Parameters sb_parameter = new Parameters();
		sb_parameter.set("ifPage", false);
		sb_parameter.set("zzjgid", parameters.getString("zzjgid"));
		sb_parameter.set("flag_query", parameters.getString("flag_query"));
		sb_parameter.set("flag_text", parameters.getString("flag_text"));
		sbList(sb_parameter, bundle);
		List<Map<String,Object>> today_rows = (List<Map<String, Object>>) bundle.get("sbxxList");
		
		
		setDateParam(parameters, bundle);
		//得到这些设备对应当天的工单数量
		Parameters gd_parameter =  new Parameters();
		gd_parameter.set("ifPage", false);
		gd_parameter.set("flag_query", parameters.getString("flag_query"));
		gd_parameter.set("flag_text", parameters.getString("flag_text"));
		gd_parameter.set("sbinfoList", today_rows);
		gd_parameter.set("time_frame_flag",parameters.getString("time_frame_flag"));
		List<Map<String,Object>> today_gdList = gdList(gd_parameter, bundle);
		
		
		Date beginScop = parameters.getDate("begin");
		Date endScop = parameters.getDate("end");
		
		int today_plan = 0 ;
		int today_finish = 0 ;
		for(Map<String,Object> map : today_rows){
			//计划数量
			Integer planNum = 0;
			Integer finishNum = 0 ;
			String sbid = map.get("sbid").toString();
			//工单循环
			A : for(Map<String,Object> m : today_gdList){
				String sid =  m.get("sbid").toString();
				if(!sbid.equals(sid)){
					continue A;
				}
				planNum = planNum + getGdjhsl( m, beginScop, endScop);
				//完成数量
				parameters.set("gdid", m.get("gdid"));
				List<Map<String,Object>> wcList = wcList(parameters, sbid, beginScop, endScop);
				if(CollectionUtils.isNotEmpty(wcList)){
					for(Map<String,Object> m1 : wcList){
						if(m1.get("bgsl") == null){
							continue;
						}
						finishNum = finishNum +(Integer) m1.get("bgsl");
					}
				}
			}
			today_plan = today_plan + planNum;
			today_finish = today_finish + finishNum;
			
		}
		//今日计划
		bundle.put("today_plan", today_plan);
		//今日完成
		bundle.put("today_finish", today_finish);
		//今日比例
		String today_wcbl = "";
		if(today_plan != 0){
			BigDecimal ratio = new BigDecimal(today_finish).multiply(new BigDecimal(100))
					.divide(new BigDecimal(today_plan),2,BigDecimal.ROUND_HALF_UP);
			today_wcbl = String.valueOf(ratio) + "%";
		}
		bundle.put("today_ratio", today_wcbl);
		
		
		
		
		parameters.set("ifPage", false);
		parameters.set("time_frame_flag", MONTH);
		//得到需要得到的设备
		sb_parameter = new Parameters();
		sb_parameter.set("ifPage", false);
		sb_parameter.set("zzjgid", parameters.getString("zzjgid"));
		sb_parameter.set("flag_query", parameters.getString("flag_query"));
		sb_parameter.set("flag_text", parameters.getString("flag_text"));
		sbList(sb_parameter, bundle);
		List<Map<String,Object>> month_rows = (List<Map<String, Object>>) bundle.get("sbxxList");
		
		
		setDateParam(parameters, bundle);
		//得到这些设备对应当天的工单数量
		gd_parameter =  new Parameters();
		gd_parameter.set("ifPage", false);
		gd_parameter.set("flag_query", parameters.getString("flag_query"));
		gd_parameter.set("flag_text", parameters.getString("flag_text"));
		gd_parameter.set("sbinfoList", month_rows);
		gd_parameter.set("time_frame_flag",parameters.getString("time_frame_flag"));
		List<Map<String,Object>> month_gdList = gdList(gd_parameter, bundle);
		
		beginScop = parameters.getDate("begin");
		endScop = parameters.getDate("end");
		
		int month_plan = 0 ;
		int month_finish = 0 ;
		for(Map<String,Object> map : month_rows){
			Integer month_planNum = 0;
			Integer month_finishNum = 0;
			String sbid = map.get("sbid").toString();
			//计划数量
			//工单循环
			A : for(Map<String,Object> m : month_gdList){
				String sid =  m.get("sbid").toString();
				if(!sbid.equals(sid)){
					continue A;
				}
				month_planNum = month_planNum + getGdjhsl( m, beginScop, endScop);
				//完成数量
				parameters.set("gdid", m.get("gdid"));
				List<Map<String,Object>> wcList = wcList(parameters, sbid, beginScop, endScop);
				if(CollectionUtils.isNotEmpty(wcList)){
					B : for(Map<String,Object> m1 : wcList){
						if(m1.get("bgsl") == null){
							continue B;
						}
						month_finishNum = month_finishNum +(Integer) m1.get("bgsl");
					}
				}
			}
			month_plan = month_plan + month_planNum;
			month_finish = month_finish + month_finishNum;
		}
		
		//本月计划
		bundle.put("month_plan", month_plan);
		//本月完成
		bundle.put("month_finish", month_finish);
		//本月比例
		String month_wcbl = "";
		if(month_plan != 0){
			BigDecimal ratio = new BigDecimal(month_finish).multiply(new BigDecimal(100))
					.divide(new BigDecimal(month_plan),2,BigDecimal.ROUND_HALF_UP);
			month_wcbl = String.valueOf(ratio) + "%";
		}
		bundle.put("month_ratio", month_wcbl);
		
		return "json:";
	}
	
	/**
	 * 选择车间
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String cjSelect(Parameters parameters,Bundle bundle){
		Bundle b = Sys.callModuleService("org", "cjService", parameters);
		if(b == null){
			log4j.info("查询所属车间出现异常，没有返回结果集");
			bundle.put("select_cj", new Object[]{});
			return "json:select_cj";
		}
		List<Map<String,Object>> cjList = (List<Map<String, Object>>) b.get("data");
		List<Map<String,Object>> returnList = new ArrayList<Map<String,Object>>();
		for(Map<String,Object> map : cjList){
			Map<String,Object> m = new HashMap<String, Object>();
			m.put("label", map.get("zzjgmc"));
			m.put("value", map.get("zzjgid"));
			returnList.add(m);
		}
		bundle.put("select_cj", returnList.toArray());
		return "json:select_cj";
	}
	
	/**
	 * 表格
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	public String table(Parameters parameters,Bundle bundle){
		if(parameters.get("time_frame_flag") == null){
			parameters.set("time_frame_flag", DAY);
		}
		if(parameters.get("ifPage") != null){
			parameters.set("ifPage", true);
		}
		Parameters sb_parameters = new Parameters();
		sb_parameters.set("zzjgid", parameters.getString("zzjgid"));
		sb_parameters.set("flag_query", parameters.getString("flag_query"));
		sb_parameters.set("flag_text", parameters.getString("flag_text"));
		sb_parameters.set("ifPage", parameters.getString("ifPage"));
		sb_parameters.set("page", parameters.getString("page"));
		sb_parameters.set("pageSize", parameters.getString("pageSize"));
		sbList(sb_parameters, bundle);
		List<Map<String,Object>> rows = (List<Map<String, Object>>) bundle.get("sbxxList");
		
		
		setDateParam(parameters, bundle);
		Parameters gd_parameters = new Parameters();
		gd_parameters.set("time_frame_flag",parameters.getString("time_frame_flag"));
		gd_parameters.set("jhsj_param_value", parameters.getString("jhsj_param_value"));
		gd_parameters.set("sbinfoList", rows);
		gd_parameters.set("flag_query", parameters.getString("flag_query"));
		gd_parameters.set("flag_text", parameters.getString("flag_text"));
		gd_parameters.set("zzjgid", parameters.getString("zzjgid"));
		List<Map<String,Object>> gdList = gdList(gd_parameters, bundle);
		
		Date beginScop = parameters.getDate("begin");
		Date endScop = parameters.getDate("end");
		//最外层  设备循环
		for(Map<String,Object> map : rows){
			String sbid = map.get("sbid").toString();
			String sbbh = map.get("sbbh").toString();
			map.put("id", sbid);
			//计划数量
			Integer planNum = 0;
			//完成数量
			Integer finishNum = 0 ;
			//工单循环
			A : for(Map<String,Object> m : gdList){
				String sid =  m.get("sbid").toString();
				if(!sbid.equals(sid)){
					continue A;
				}
				int thisNum = getGdjhsl( m, beginScop, endScop);
				
				log4j.info("========= 数量统计=========");
				log4j.info("time_flag :" + parameters.getString("time_frame_flag").toString() );
				log4j.info("sbbh :" + sbbh );
				log4j.info("gdbh :" + m.get("gdbh") );
				log4j.info("plan_num :" + thisNum );
				
				planNum = planNum + thisNum;
				
				parameters.set("gdid", m.get("gdid"));
				List<Map<String,Object>> wcList = wcList(parameters, sbid, beginScop, endScop);
				if(CollectionUtils.isNotEmpty(wcList)){
					for(Map<String,Object> m1 : wcList){
						if(m1.get("bgsl") == null){
							continue;
						}
						finishNum = finishNum +(Integer) m1.get("bgsl");
					}
					log4j.info("finish_num :" + m.get(finishNum) );
				}
			}
			map.put("jh", planNum);
			
			map.put("wc", finishNum);
			
			map.put("cy", planNum - finishNum);
		}
		
		
		
		bundle.put("rows", rows);
//		bundle.put("totalPage", bundle.get("totalPage"));
//		bundle.put("currentPage", bundle.get("currentPage"));
//		bundle.put("totalRecord", bundle.get("totalRecord"));
//		bundle.put("records", bundle.get("records"));
		
		return "json:";
	}
	
	/**
	 * 得到工单 在时间范围内 的计划数量 
	 * 4种情况
	 * 1.范围(前) 工单(前) 工单(后) 范围(后)
	 * 2.工单(前) 范围(前) 范围(后) 工单(后) 
	 * 3.范围(前) 工单(前) 范围(后) 工单(后)
	 * 4.工单(前) 范围(前) 工单(后) 范围(后)
	 * 计划数量 * (范围内的工时（范围工单取小）/ 工单工时)
	 * @param gdMap
	 * @param begin
	 * @param end
	 * @param thisDate`
	 */
	private Integer getGdjhsl(Map<String,Object> gdMap,Date beginScop,Date endScop){
		String gdid = gdMap.get("gdid").toString();
		Integer jgsl = Integer.valueOf(gdMap.get("jgsl").toString());
		Date jhkssj = (Date) gdMap.get("jhkssj");
		Date jhjssj =  (Date) gdMap.get("jhjssj");
		if(beginScop.compareTo(jhkssj) < 1 && jhjssj.compareTo(endScop) < 0){
			return jgsl;
		}
		//这个时间范围内  根据设备日历，找到对应的工时模版
		String sbid = gdMap.get("sbid").toString();
		
		Calendar dd = Calendar.getInstance();//定义日期实例
		Calendar dd_d = Calendar.getInstance();//定义日期实例
		dd.setTime(string2Date(date2String(jhkssj, "yyyy-MM-dd")));//设置日期起始时间(用0点的时间)
		dd_d.setTime(string2Date(date2String(jhkssj, "yyyy-MM-dd HH:mm:ss")));//设置日期起始时间(用0点的时间)
		Parameters sbrl_parameters = new Parameters();
		Parameters sbgs_parameters = new Parameters();
		sbrl_parameters.set("sbid", sbid);
		sbgs_parameters.set("sbid", sbid);
		//工单总共的工时
		BigDecimal allGdgs = BigDecimal.ZERO;
		//范围内工时
		BigDecimal fwngs = BigDecimal.ZERO;
		while(dd.getTime().compareTo(jhjssj) <1 ){//判断是否到结束日期
			//得到设备日历
			//TODO
			String thisDay =  date2String(dd.getTime(), "yyyy-MM-dd");
			sbrl_parameters.set("start", thisDay);
			sbrl_parameters.set("end", thisDay);
			Bundle sbrl_bundle = Sys.callModuleService("em", "emservice_sbrl", sbrl_parameters);
			
			String gsmbid ="" ;
			boolean zero_flag = false;
			boolean mr_flag = false;
			//判断是否有设备日历，如果有，设备日历 判断当天是否休息，计算工时
			if(sbrl_bundle != null && sbrl_bundle.get("sbrl") != null 
					&&  MapUtils.isNotEmpty((Map<String, Object>) sbrl_bundle.get("sbrl"))){
				//设备日历存在，判断这天是不是休息
				Map<String, Object> sbrl_map = (Map<String, Object>) sbrl_bundle.get("sbrl");
				String rlzt = sbrl_map.get("rlzt").toString();
				//如果这天是工作日，查找对应的工作模版,计算工时
				if("10".equals(rlzt)){
					sbgs_parameters.set("rq", dd.getTime());
					Bundle sbgs_bundle = Sys.callModuleService("em", "emservice_sbgs", sbgs_parameters);
					//如果 设备模版不为空，使用设备模版
					if(sbgs_bundle != null && sbgs_bundle.get("sbgs") != null){
						gsmbid = ((Map<String,Object>)sbgs_bundle.get("sbgs")).get("gsmbid").toString();
					}else{
						//为空使用工厂通用模版
						Parameters gcrl_p = new Parameters();
						gcrl_p.set("start", thisDay);
						gcrl_p.set("end", thisDay);
						Bundle gcrl_bundle = Sys.callModuleService("fm", "fmService_getGcCalendarDays", gcrl_p);
						//判断工厂日历中，是否休息
						if(gcrl_bundle != null && gcrl_bundle.get("weekendsList") != null){
							List<String> weekList = (List<String>) gcrl_bundle.get("weekendsList");
							if(weekList.contains(thisDay)){
								zero_flag = true;
							}
						}
						if(gcrl_bundle == null || gcrl_bundle.get("gcrlmb") == null  ){
							zero_flag = true;
						}
						mr_flag = true;
					}
				}else{
					//如果这天是休息日，打个标识，工时计算，工时为0
					zero_flag = true;
				}
			}
			//如果没有，查看工厂日历，判断当天是否休息，计算对应的工时
			else{
				Parameters gcrl_p = new Parameters();
				gcrl_p.set("start", thisDay);
				gcrl_p.set("end", thisDay);
				Bundle gcrl_bundle = Sys.callModuleService("fm", "fmService_getGcCalendarDays", gcrl_p);
				//判断工厂日历中，是否休息
				if(gcrl_bundle != null && gcrl_bundle.get("weekendsList") != null){
					List<String> weekList = (List<String>) gcrl_bundle.get("weekendsList");
					if(weekList.contains(thisDay)){
						zero_flag = true;
					}
				}
				if(gcrl_bundle == null || gcrl_bundle.get("gcrlmb") == null  ){
					zero_flag = true;
				}
				mr_flag = true;
			}
			
			if(zero_flag){
				dd.add(Calendar.DAY_OF_MONTH, 1);//进行当前日期加1
				dd_d.add(Calendar.DAY_OF_MONTH, 1);
				continue;
			}
			
			Map<String,Object> mbMap = null;
			if(StringUtils.isNotBlank(gsmbid) && mbStaticMap.get(gsmbid) != null){
				mbMap = (Map<String, Object>) mbStaticMap.get("gsmbid");
			}
			else if(mr_flag){
				Parameters gcgsmbb_parameters = new Parameters();
				gcgsmbb_parameters.set("sfmrmb", "10");
				Bundle b = Sys.callModuleService("fm", "fmService_query_gcgsmbb", gcgsmbb_parameters);
				mbMap = (Map<String, Object>) b.get("gcgsmbb");
				mbStaticMap.put("gsmbid", mbMap);
			}
			else{
				Parameters gcgsmbb_parameters = new Parameters();
				gcgsmbb_parameters.set("gsmbid", gsmbid);
				Bundle b = Sys.callModuleService("fm", "fmService_query_gcgsmbb", gcgsmbb_parameters);
				mbMap = (Map<String, Object>) b.get("gcgsmbb");
				mbStaticMap.put("gsmbid", mbMap);
			}
			Map<String,Object> gsMap = handleGs(dd, mbMap);

			//TODO
			BigDecimal thisGdgs = handleAllGdgs(gsMap, dd, jhkssj, jhjssj);
			allGdgs = allGdgs.add(thisGdgs);
			
			BigDecimal thisFwngs = handleFwngs(gsMap, dd_d, beginScop, endScop, jhkssj, jhjssj);
			fwngs = fwngs.add(thisFwngs);
			
			dd.add(Calendar.DAY_OF_MONTH, 1);//进行当前日期加1
			dd_d.add(Calendar.DAY_OF_MONTH, 1);
		}
		if(allGdgs.compareTo(BigDecimal.ZERO) == 0){
			return BigDecimal.ZERO.intValue();
		}
		return fwngs.divide(allGdgs,2,BigDecimal.ROUND_HALF_UP)
				.multiply(new BigDecimal(jgsl)).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
	}
	
	/**
	 * 工单 每一天的工时
	 * @param gsMap 工时Map
	 * @param dd	传入的日期
	 * @param beginScop 范围开始
	 * @param endScop 范围结束
	 * @param jgkssj 工单计划开始日期
	 * @param jhjssj 工单计划完成日期
	 */
	private BigDecimal handleAllGdgs(Map<String,Object> gsMap,Calendar dd ,Date jgkssj,Date jhjssj){
		BigDecimal returnValue = BigDecimal.ZERO;
		
		if(gsMap.get("sbmbks") == null ||gsMap.get("sbmbjs") ==null ){
			returnValue = BigDecimal.ZERO;
		}
		//第一天  和 最后一天需要额外处理
		else if(isSameDate(dd.getTime(), jgkssj)){
			//工单开始时间 早于 设备的开始时间  返回 设备日历的工时时间
			if(jgkssj.compareTo((Date) gsMap.get("sbmbks")) <= 0){
				returnValue = (BigDecimal) gsMap.get("gs");
			}
			//工单开始时间 早于 设备的结束时间  晚于设备的开始时间    返回差值
			else if(jgkssj.compareTo((Date) gsMap.get("sbmbks")) > 0 
					&& jgkssj.compareTo((Date) gsMap.get("sbmbjs")) <= 0 ){
				long difference = ((Date) gsMap.get("sbmbjs")).getTime() - jgkssj.getTime() ;
				if(difference < 0){
					returnValue = BigDecimal.ZERO;
				}else{
					returnValue = new BigDecimal(difference)
							.divide(new BigDecimal(1000*60*60),2,BigDecimal. ROUND_HALF_UP)	;	
				}
			}
			//工单的开始时间晚于 设备的结束时间，返回0
			else{
				returnValue = BigDecimal.ZERO;
			}
		}
		else if(isSameDate(dd.getTime(), jhjssj)){
			//工单 结束时间 晚于设备日历的结束时间  返回日历的工时
			if(jhjssj.compareTo((Date) gsMap.get("sbmbjs")) >= 0){
				returnValue = (BigDecimal)gsMap.get("gs");
			}
			//工单 结束时间 早于设备日历的结束时间，晚于设备日历的开始时间
			else if(jhjssj.compareTo((Date) gsMap.get("sbmbjs")) < 0 &&
					jhjssj.compareTo((Date) gsMap.get("sbmbks")) >=0){
				long difference = jhjssj.getTime() - ((Date)(gsMap.get("sbmbks"))).getTime();
				if(difference< 0){
					return BigDecimal.ZERO;
				}else{
					returnValue = new BigDecimal(difference)
							.divide(new BigDecimal(1000*60*60),2,BigDecimal. ROUND_HALF_UP)	;	
				}
			}
		}
		else{
			returnValue = (BigDecimal)gsMap.get("gs");
		}
		return returnValue;
	}
	
	/**
	 * 范围内 的工时
	 * @param gsMap 工时Map
	 * @param dd	传入的日期
	 * @param beginScop 范围开始
	 * @param endScop 范围结束
	 * @param jgkssj 工单计划开始日期
	 * @param jhjssj 工单计划完成日期
	 */
	private BigDecimal handleFwngs(Map<String,Object> gsMap,Calendar dd ,Date beginScop,Date endScop,Date jgkssj,Date jhjssj){
		BigDecimal returnValue = null;
		//开始日期 
		Date compareBeginDate = beginScop.compareTo(jgkssj) > 0 ? beginScop : jgkssj;
		//结束日期
		Date compareEndDate = endScop.compareTo(jhjssj) < 0 ? endScop : jhjssj;
		
		if(string2Date(date2String(compareBeginDate, "yyyy-MM-dd")).compareTo(dd.getTime()) > 0
				|| string2Date(date2String(compareEndDate, "yyyy-MM-dd")).compareTo(dd.getTime()) < 0){
			returnValue = BigDecimal.ZERO;
			return returnValue;
		}
		
		if(gsMap.get("sbmbks") == null ||gsMap.get("sbmbjs") ==null ){
			returnValue = BigDecimal.ZERO;
		}
		//第一天  和 最后一天需要额外处理
		else if(isSameDate(dd.getTime(), compareBeginDate)){
			if(dd.getTime().compareTo(compareBeginDate) > 0){
				long difference = ((Date) gsMap.get("sbmbjs") ).getTime() - dd.getTime() .getTime();
				if(difference < 0 ){
					returnValue =  BigDecimal.ZERO;
				}else{
					returnValue = new BigDecimal(difference)
							.divide(new BigDecimal(1000*60*60),2,BigDecimal. ROUND_HALF_UP)	;	
				}
			}else{
				returnValue = (BigDecimal) gsMap.get("gs");
			}
		}
		else if(isSameDate(dd.getTime(), compareEndDate)){
			if(dd.getTime().compareTo(compareEndDate) > 0){
				returnValue = (BigDecimal) gsMap.get("gs");
			}else{
				long difference = dd.getTime().getTime()- ((Date) gsMap.get("sbmbks") ).getTime() ;
				if(difference < 0){
					returnValue =  BigDecimal.ZERO;
				}else{
					returnValue = new BigDecimal(difference)
							.divide(new BigDecimal(1000*60*60),2,BigDecimal. ROUND_HALF_UP)	;	
				}
			}
		}
		else{
			returnValue = (BigDecimal) gsMap.get("gs");
		}
		
		return returnValue;
	}
	
	private boolean isSameDate(Date date1,Date date2){
		  Calendar cal1 = Calendar.getInstance();
		  cal1.setTime(date1);
		  Calendar cal2 = Calendar.getInstance();
		  cal2.setTime(date2);
		  return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) 
				  && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
				  && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
	}
	
	private Map<String,Object> handleGs(Calendar dd,Map<String,Object> mbMap){
		int weekday = dd.get(Calendar.DAY_OF_WEEK);
		Date ks = null;
		Date js = null;
		Map<String,Object> returnMap = new HashMap<String, Object>();
		if(weekday == Calendar.MONDAY){
			if(mbMap.get("zhouyiks") != null){
				ks = (Date) mbMap.get("zhouyiks");
			}
			if((Date) mbMap.get("zhouyijs") != null){
				js = (Date) mbMap.get("zhouyijs");
			}
		}
		if(weekday == Calendar.TUESDAY){
			if(mbMap.get("zhouerks") != null){
				ks = (Date) mbMap.get("zhouerks");
			}
			if((Date) mbMap.get("zhouerjs") != null){
				js = (Date) mbMap.get("zhouerjs");
			}
		}
		if(weekday == Calendar.WEDNESDAY){
			if(mbMap.get("zhousanks") != null){
				ks = (Date) mbMap.get("zhousanks");
			}
			if((Date) mbMap.get("zhousanjs") != null){
				js = (Date) mbMap.get("zhousanjs");
			}
		}
		if(weekday == Calendar.THURSDAY){
			if(mbMap.get("zhousiks") != null){
				ks = (Date) mbMap.get("zhousiks");
			}
			if((Date) mbMap.get("zhousijs") != null){
				js = (Date) mbMap.get("zhousijs");
			}
		}
		if(weekday == Calendar.FRIDAY){
			if(mbMap.get("zhouwuks") != null){
				ks = (Date) mbMap.get("zhouwuks");
			}
			if((Date) mbMap.get("zhouwujs") != null){
				js = (Date) mbMap.get("zhouwujs");
			}
		}
		if(weekday == Calendar.SATURDAY){
			if(mbMap.get("zhouliuks") != null){
				ks = (Date) mbMap.get("zhouliuks");
			}
			if((Date) mbMap.get("zhouliujs") != null){
				js = (Date) mbMap.get("zhouliujs");
			}
		}
		if(weekday == Calendar.SUNDAY){
			if(mbMap.get("zhouriks") != null){
				ks = (Date) mbMap.get("zhouriks");
			}
			if((Date) mbMap.get("zhourijs") != null){
				js = (Date) mbMap.get("zhourijs");
			}
		}
		if(ks!=null){
			ks = string2Date(deal_calendar(dd, ks));
		}
		if(js!=null){
			js = string2Date(deal_calendar(dd, js));
		}
		returnMap.put("sbmbks", ks);
		returnMap.put("sbmbjs", js);
		BigDecimal gs = BigDecimal.ZERO;
		if(ks != null && js != null){
			 gs = new BigDecimal(js.getTime() - ks.getTime())
						.divide(new BigDecimal(1000*60*60),2,BigDecimal. ROUND_HALF_UP)	;	
		}		
		returnMap.put("gs", gs);
		return returnMap;
	}
	
	private String deal_calendar(Calendar dd,Date date){
		Calendar deal_calendar = Calendar.getInstance();
		deal_calendar.setTime(date);
		return String.valueOf(dd.get(Calendar.YEAR)) + "-" +
			String.valueOf(dd.get(Calendar.MONTH) + 1) + "-" +
			String.valueOf(dd.get(Calendar.DAY_OF_MONTH)) + " " +
			String.valueOf(deal_calendar.get(Calendar.HOUR_OF_DAY)) +":" +
			String.valueOf(deal_calendar.get(Calendar.MINUTE)) +":" +
			String.valueOf(deal_calendar.get(Calendar.SECOND));
	}
	
	/**
	 * 得到机构(所有)的设备集合
	 * 任务得到批次   批次得到工单   工单得到设备
	 * @param parameters
	 * @param bundle
	 */
	private void sbList(Parameters parameters,Bundle bundle){
		String zzjgid = parameters.getString("zzjgid");
		parameters.set("zzjgid", zzjgid);
		
		String flag_query = parameters.getString("flag_query");
		String flag_text = parameters.getString("flag_text");
		boolean flag = false;
		
		List<String> pcIdList = new ArrayList<String>();
		//生产任务编号   
		if(ScxxsscxConstant.PRODUCE_TASK_CODE.equals(flag_query)){
			parameters.set("scrwbh", flag_text);
			Bundle pc_bundle = Sys.callModuleService("pro", "scrwAndPcInfoByPcidService", parameters);
			if(pc_bundle != null && pc_bundle.get("scrwandpcList") != null){
				List<Map<String,Object>> scrwandpcList = (List<Map<String, Object>>) pc_bundle.get("scrwandpcList");
				for(Map<String,Object> m: scrwandpcList){
					String pcid = m.get("scrwpcid").toString();
					pcIdList.add(pcid);
				}
			}
			flag = true;
		}
		//批次编号
		if(ScxxsscxConstant.BATCH_CODE.equals(flag_query)){
			parameters.set("pcbh", flag_text);
			Bundle pc_bundle = Sys.callModuleService("pro", "scrwAndPcInfoByPcidService", parameters);
			if(pc_bundle != null && pc_bundle.get("scrwandpcList") != null){
				List<Map<String,Object>> scrwandpcList = (List<Map<String, Object>>) pc_bundle.get("scrwandpcList");
				for(Map<String,Object> m: scrwandpcList){
					String pcid =  m.get("scrwpcid").toString();
					pcIdList.add(pcid);
				}
			}
			flag = true;
		}
		//工单号
		String gdbh = "";
		if(ScxxsscxConstant.WORK_ORDER_CODE.equals(flag_query)){
			gdbh = flag_text;
		}
		parameters.set("gdbh", gdbh);
		parameters.set("pcIdList", pcIdList);
		
		parameters.set("pcidNull_returnNull", flag);
		Bundle gd_bundle = Sys.callModuleService("pl", "plservice_gdxx_param", parameters);
		List<Map<String,Object>> list = (List<Map<String, Object>>) gd_bundle.get("gdxxList");
		List<String> sbidList = new ArrayList<String>();
		for(Map<String,Object> m : list){
			String sbid = m.get("sbid").toString();
			sbidList.add(sbid);
		}
		if(flag && CollectionUtils.isEmpty(sbidList)){
			bundle.put("sbxxList", new ArrayList<Map<String,Object>>());
			bundle.put("rows",  new ArrayList<Map<String,Object>>());
		}else{
			parameters.set("sbidList", sbidList);
			Bundle sbxx_bundle = Sys.callModuleService("em", "emservice_sbxxInfo", parameters);
			
			bundle.put("sbxxList", sbxx_bundle.get("sbxxList"));
			bundle.put("rows", sbxx_bundle.get("sbxxList"));
			bundle.put("totalPage", sbxx_bundle.get("totalPage"));
			bundle.put("currentPage", sbxx_bundle.get("currentPage"));
			bundle.put("totalRecord", sbxx_bundle.get("totalRecord"));
			bundle.put("records", sbxx_bundle.get("records"));
		}
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
	
	private void setDateParam(Parameters parameters,Bundle bundle){
		String time_frame_flag = parameters.getString("time_frame_flag");
		if(StringUtils.isBlank(time_frame_flag)){
			return;
		}
		Date today = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(today);
		String todayStr = String.valueOf(calendar.get(Calendar.YEAR)) + "-" +
				String.valueOf(calendar.get(Calendar.MONTH) + 1) + "-" +
				String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
		
		Calendar c = Calendar.getInstance();
		c.setTime(today);
		c.add(Calendar.DAY_OF_MONTH, 1);
		String tomorrowStr = String.valueOf(c.get(Calendar.YEAR)) + "-" +
				String.valueOf(c.get(Calendar.MONTH) + 1) + "-" +
				String.valueOf(c.get(Calendar.DAY_OF_MONTH));
		
		String monthFirstDay = String.valueOf(calendar.get(Calendar.YEAR)) + "-" +
				String.valueOf(calendar.get(Calendar.MONTH) + 1) + "-01" ;
		String nextMonthFirstDay = String.valueOf(calendar.get(Calendar.YEAR)) + "-" +
				String.valueOf(calendar.get(Calendar.MONTH) + 2) + "-01" ;
		
		Date begin = null;
		Date end = null;
		
//		String jhsj_param = " and ((jhkssj >= ? and  jhkssj < ? )"
//				+ "or(jhjssj >= ? and jhjssj < ? )"
//				+ "or(jhkssj <= ? and jhjssj >= ? ))";
		String jhsj_param = " and (jhkssj < ? and jhjssj >= ? ) ";
		List<Object> jhsj_param_value = new ArrayList<Object>();
		if(DAY.equals(time_frame_flag)){
			//时间范围
			begin = string2Date(todayStr);
			end = string2Date(tomorrowStr);
		}
		if(MONTH.equals(time_frame_flag)){
			//时间范围
			begin = string2Date(monthFirstDay);
			end = string2Date(nextMonthFirstDay);
		}
		parameters.set("begin", begin);
		parameters.set("end", end);
		

		jhsj_param_value.add(end);
		jhsj_param_value.add(begin);
		
		parameters.set("jhsj_param", jhsj_param);
		parameters.set("jhsj_param_value", jhsj_param_value);
	}
	
	/**
	 * 通过一堆参数，得到工单的 list
	 * @param parameters
	 * @param bundle
	 * @return
	 */
	private List<Map<String,Object>> gdList(Parameters parameters,Bundle bundle){
		String zzjgid = parameters.getString("zzjgid");
		String flag_query = parameters.getString("flag_query");
		String flag_text = parameters.getString("flag_text");
		List<Map<String, Object>> sbinfoList = (List<Map<String, Object>>) parameters.get("sbinfoList");
		boolean flag = false;
		List<String> sbIdList = new ArrayList<String>();
		if(CollectionUtils.isEmpty(sbinfoList) && StringUtils.isNotBlank(zzjgid)){
			//使用部门 查询设备
			sbList(parameters, bundle);
			sbinfoList = (List<Map<String, Object>>) bundle.get("sbxxList");
		}
		//使用设备--> 工单范围
		if(CollectionUtils.isNotEmpty(sbinfoList)){
			for(Map<String,Object> m : sbinfoList ){
				sbIdList.add(m.get("sbid").toString());
			}
		}
		
		//不限·
		if(ScxxsscxConstant.ALL.equals(flag_query)){
			
		}
		
		List<String> pcIdList = new ArrayList<String>();
		//生产任务编号   
		if(ScxxsscxConstant.PRODUCE_TASK_CODE.equals(flag_query)){
			parameters.set("scrwbh", flag_text);
			Bundle pc_bundle = Sys.callModuleService("pro", "scrwAndPcInfoByPcidService", parameters);
			if(pc_bundle != null && pc_bundle.get("scrwandpcList") != null){
				List<Map<String,Object>> scrwandpcList = (List<Map<String, Object>>) pc_bundle.get("scrwandpcList");
				for(Map<String,Object> m: scrwandpcList){
					String pcid = m.get("scrwpcid").toString();
					pcIdList.add(pcid);
				}
			}
			flag = true;
		}
		//批次编号
		if(ScxxsscxConstant.BATCH_CODE.equals(flag_query)){
			parameters.set("pcbh", flag_text);
			Bundle pc_bundle = Sys.callModuleService("pro", "scrwAndPcInfoByPcidService", parameters);
			if(pc_bundle != null && pc_bundle.get("scrwandpcList") != null){
				List<Map<String,Object>> scrwandpcList = (List<Map<String, Object>>) pc_bundle.get("scrwandpcList");
				for(Map<String,Object> m: scrwandpcList){
					String pcid = m.get("scrwpcid").toString();
					pcIdList.add(pcid);
				}
			}
			flag = true;
		}
		//工单号
		String gdbh = "";
		if(ScxxsscxConstant.WORK_ORDER_CODE.equals(flag_query)){
			gdbh = flag_text;
		}
		
		parameters.set("gdbh", gdbh);
		parameters.set("pcIdList", pcIdList);
		
		parameters.set("pcidNull_returnNull", flag);
		parameters.set("sbIdList", sbIdList);
		//设置工单时间范围
		setDateParam(parameters, bundle);
		
		parameters.set("gdztdm_s", "'20','30','40','50'");
		Bundle gd_bundle = Sys.callModuleService("pl", "plservice_gdxx_param", parameters);
		
		List<Map<String,Object>> list = (List<Map<String, Object>>) gd_bundle.get("gdxxList");
		return list;
	}
	
	public List<Map<String,Object>> wcList(Parameters parameters,String sbid,Date beginScop,Date endScop){
		parameters.set("sbid", sbid);
		parameters.set("bgrq_start", beginScop);
		parameters.set("bgrq_end", endScop);
		Bundle pc_bundle = Sys.callModuleService("pc", "queryservice_bgls", parameters);
		if(pc_bundle != null ){
			return (List<Map<String, Object>>) pc_bundle.get("bglsList");
		}
		return null;
	}
	
	public String query_search(Parameters parameters, Bundle bundle){
		String type = parameters.getString("type");
		String term = parameters.getString("term");
		term = StringUtils.isNotEmpty(term) ? term.trim() : "";
		
		List<Map<String,Object>> returnList = new ArrayList<Map<String,Object>>();
		List<String> distinct_list = new ArrayList<String>();
		if(ScxxsscxConstant.PRODUCE_TASK_CODE.equals(type)){
			Parameters task_parameters = new Parameters();
			task_parameters.set("scrwbh", term);
			Bundle rw_bundle = Sys.callModuleService("pro", "scrwAndPcInfoByPcidService", task_parameters);
			List<Map<String,Object>> scrwandpcList =  (List<Map<String, Object>>) rw_bundle.get("scrwandpcList");
			for(Map<String,Object> map : scrwandpcList){
				if(distinct_list.contains(map.get("scrwbh").toString())){
					continue;
				}
				Map<String,Object> m = new HashMap<String, Object>();
				m.put("label", map.get("scrwbh"));
				m.put("value", map.get("scrwbh"));
				returnList.add(m);
				distinct_list.add(map.get("scrwbh").toString());
			}
		}
		
		if(ScxxsscxConstant.BATCH_CODE.equals(type)){
			Parameters task_batch_parameters = new Parameters();
			task_batch_parameters.set("pcbh", term);
			Bundle rwpc_bundle = Sys.callModuleService("pro", "scrwAndPcInfoByPcidService", task_batch_parameters);
			List<Map<String,Object>> scrwandpcList =  (List<Map<String, Object>>) rwpc_bundle.get("scrwandpcList");
			for(Map<String,Object> map : scrwandpcList){
				if(distinct_list.contains(map.get("pcbh").toString())){
					continue;
				}
				Map<String,Object> m = new HashMap<String, Object>();
				m.put("label", map.get("pcbh"));
				m.put("value", map.get("pcbh"));
				returnList.add(m);
				distinct_list.add(map.get("pcbh").toString());
			}
		}

		if(ScxxsscxConstant.WORK_ORDER_CODE.equals(type)){
			Parameters workorder_parameters = new Parameters();
			workorder_parameters.set("gdbh", term);
			workorder_parameters.set("pcidNull_returnNull", false);
			Bundle gd_bundle = Sys.callModuleService("pl", "plservice_gdxx_param", workorder_parameters);
			List<Map<String,Object>> list = (List<Map<String, Object>>) gd_bundle.get("gdxxList");
			for(Map<String,Object> map : list){
				if(distinct_list.contains(map.get("gdbh").toString())){
					continue;
				}
				Map<String,Object> m = new HashMap<String, Object>();
				m.put("label", map.get("gdbh"));
				m.put("value", map.get("gdbh"));
				returnList.add(m);
				distinct_list.add(map.get("gdbh").toString());
			}
		}
		
		bundle.put("select_query", returnList.toArray());
		
		return "json:select_query";
	}
}
