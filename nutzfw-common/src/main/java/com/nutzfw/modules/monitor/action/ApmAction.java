package com.nutzfw.modules.monitor.action;

import com.nutzfw.core.common.annotation.AutoCreateMenuAuth;
import com.nutzfw.core.common.cons.Cons;
import com.nutzfw.core.common.filter.CheckRoleAndSession;
import com.nutzfw.core.common.vo.AjaxResult;
import com.nutzfw.modules.common.action.BaseAction;
import com.nutzfw.modules.monitor.entity.AlarmOption;
import com.nutzfw.modules.monitor.quartz.job.ApmJob;
import com.nutzfw.modules.monitor.service.AlarmOptionService;
import com.nutzfw.modules.organize.service.DepartmentService;
import com.nutzfw.modules.organize.service.UserAccountService;
import com.nutzfw.modules.sys.service.DataTableService;
import com.nutzfw.modules.sys.service.RoleService;
import com.nutzfw.modules.sys.service.UserLoginHistoryService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Encoding;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.annotation.*;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: huchuc@vip.qq.com
 * Date: 2016/11/14 0014
 * To change this template use File | Settings | File Templates.
 */
@IocBean
@At("/monitor/apm")
@Filters(@By(type = CheckRoleAndSession.class, args = {Cons.SESSION_USER_KEY, Cons.SESSION_USER_ROLE}))
public class ApmAction extends BaseAction {

    @Inject
    AlarmOptionService alarmOptionService;

    @Inject
    UserLoginHistoryService userLoginHistoryService;
    @Inject
    DataTableService dataTableService;
    @Inject
    UserAccountService userAccountService;
    @Inject
    DepartmentService departmentService;
    @Inject
    RoleService roleService;
    @Inject
    ApmJob apmJob;

    /**
     * 用户设备访问情况
     *
     * @return
     */
    @Ok("json")
    @POST
    @At("/showUserDeviceBar")
    @RequiresPermissions("sysMonitor.showUserDeviceBar")
    @AutoCreateMenuAuth(name = "用户设备访问情况", icon = "fa-eye", type = AutoCreateMenuAuth.RESOURCE, parentPermission = "sysMonitor.index")
    public AjaxResult showUserDeviceBar() {
        Sql sql = Sqls.create("SELECT os,count(*) AS count FROM sys_user_login_history GROUP BY os ORDER BY count DESC");
        sql.setCallback(Sqls.callback.maps());
        userLoginHistoryService.execute(sql);
        List<NutMap> list = sql.getList(NutMap.class);
        String[] names = new String[list.size()];
        String[] datas = new String[list.size()];
        for (int i = list.size() - 1; i >= 0; i--) {
            names[i] = list.get(i).getString("os", "未知设备");
            datas[i] = list.get(i).getString("count");
        }
        NutMap nutMap = new NutMap();
        nutMap.put("names", names);
        nutMap.put("datas", datas);
        return AjaxResult.sucess(nutMap);
    }

    /**
     * 用户使用浏览器情况
     *
     * @return
     */
    @Ok("json")
    @POST
    @At("/showUserBrowserPie")
    @RequiresPermissions("sysMonitor.showUserBrowserPie")
    @AutoCreateMenuAuth(name = "用户使用浏览器情况", icon = "fa-eye", type = AutoCreateMenuAuth.RESOURCE, parentPermission = "sysMonitor.index")
    public AjaxResult showUserBrowserPie() {
        Sql sql = Sqls.create("SELECT browser,count(*) AS count FROM sys_user_login_history WHERE type='web' GROUP BY browser ORDER BY count DESC");
        sql.setCallback(Sqls.callback.maps());
        userLoginHistoryService.execute(sql);
        List<NutMap> list = sql.getList(NutMap.class);
        String[] names = new String[list.size()];
        List<NutMap> mapList = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            String name = list.get(i).getString("browser", "未知设备");
            names[i] = name;
            String value = list.get(i).getString("count");
            mapList.add(NutMap.NEW().addv("value", value).addv("name", name));
        }
        NutMap nutMap = new NutMap();
        nutMap.put("names", names);
        nutMap.put("list", mapList);
        return AjaxResult.sucess(nutMap);
    }


    @Ok("btl:WEB-INF/view/sys/monitor/apm/index.html")
    @GET
    @At("/dashboard")
    @RequiresPermissions("sysMonitor.index")
    @AutoCreateMenuAuth(name = "服务器状态监控", icon = "fa-eye", parentPermission = "sys.monitor")
    public NutMap dashboard() {
        NutMap map = new NutMap();
        map.put("dateTime", new Date());
        map.put("dataTableTotal", dataTableService.count());
        map.put("userTotal", userAccountService.count());
        map.put("deptTotal", departmentService.count());
        map.put("roleTotal", roleService.count());
        ServletContext context = Mvcs.getServletContext();
        Properties sys = System.getProperties();
        TreeMap<String, String> data = new TreeMap<>();
        data.put("Default Charset", Encoding.defaultEncoding());
        data.put("Current Path", new File(".").getAbsolutePath());
        data.put("Java Version", sys.get("java.version").toString());
        data.put("File separator", sys.get("file.separator").toString());
        data.put("Timezone", sys.get("user.timezone").toString());
        data.put("Java Version", context.getServerInfo());
        data.put("ContextPath", context.getContextPath());
        data.put("OS", sys.get("os.name").toString() + " " + sys.get("os.arch"));
        data.put("Servlet API ", context.getMajorVersion() + "." + context.getMinorVersion());
        data.put("context.tempdir", context.getAttribute("javax.servlet.context.tempdir").toString());
        sys.forEach((key, val) -> data.put(Strings.sNull(key), Strings.sNull(val)));
        setRequestAttribute("sysdata", data);
        setRequestAttribute("files", apmJob.getFileSystemInfo());
        setRequestAttribute("cpuInfo", apmJob.getCpuInfo());
        return map;
    }

    @Ok("json")
    @POST
    @At("/lineDashboard")
    @RequiresPermissions("sysMonitor.index")
    public AjaxResult lineDashboard() {
        return AjaxResult.sucess(apmJob.getMoreTempAll());
    }

    @Ok("json")
    @POST
    @At("/tableDashboard")
    @RequiresPermissions("sysMonitor.index")
    public AjaxResult tableDashboard() {
        return AjaxResult.sucess(apmJob.getOneTempAll());
    }

    @Ok("json")
    @POST
    @At("/alarmOptions")
    @RequiresPermissions("sysMonitor.index")
    public AjaxResult alarmOption() {
        return AjaxResult.sucess(alarmOptionService.query());
    }


    @Ok("json")
    @POST
    @At("/updateAlarmOption")
    @RequiresPermissions("sysMonitor.update")
    @AutoCreateMenuAuth(name = "修改配置", icon = "fa-eye", type = AutoCreateMenuAuth.RESOURCE, parentPermission = "sysMonitor.index")
    public AjaxResult updateAlarmOptions(@Param("alarmType") String alarmType,
                                         @Param("percent") double percent,
                                         @Param("email") boolean email,
                                         @Param("sms") boolean sms
    ) {
        try {
            AlarmOption option = new AlarmOption();
            option.setAlarmType(alarmType);
            option.setPercent(percent);
            option.setEmail(email);
            option.setSms(sms);
            if (alarmOptionService.updateIgnoreNull(option) > 0) {
                apmJob.setAlarmOptions(alarmOptionService.query());
                return AjaxResult.sucess("更新成功");
            } else {
                return AjaxResult.error("更新失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }
}