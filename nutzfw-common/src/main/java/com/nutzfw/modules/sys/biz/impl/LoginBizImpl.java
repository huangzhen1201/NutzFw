package com.nutzfw.modules.sys.biz.impl;

import com.nutzfw.core.common.cons.Cons;
import com.nutzfw.core.common.util.WebUtil;
import com.nutzfw.core.plugin.shiro.LoginTypeEnum;
import com.nutzfw.modules.organize.entity.UserAccount;
import com.nutzfw.modules.sys.biz.LoginBiz;
import com.nutzfw.modules.sys.entity.UserLoginHistory;
import com.nutzfw.modules.sys.service.UserLoginHistoryService;
import eu.bitwalker.useragentutils.UserAgent;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.util.NutMap;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.Mvcs;

/**
 * @author 黄川 huchuc@vip.qq.com
 * @date: 2018/7/19
 * 描述此类：
 */
@IocBean
public class LoginBizImpl implements LoginBiz {

    static final Log log = Logs.get();

    @Inject
    UserLoginHistoryService userLoginHistoryService;

    @Override
    public NutMap doLogin(UserAccount userAccount, LoginTypeEnum loginType) {
        NutMap data = new NutMap();
        data.put(LoginBiz.NEED_CHANGE_PASS, false);
        data.put("userId", userAccount.getUserid());
        data.put("userName", userAccount.getUserName());
        data.put("realName", userAccount.getRealName());
        data.put("avatar", userAccount.getAvatar());
        data.put("deptId", userAccount.getDeptId());
        if (null != userAccount.getDept()) {
            data.put("deptName", userAccount.getDept().getName());
        } else {
            data.put("deptName", "");
        }
        //第一次登陆需要修改密码
        if (Cons.optionsCach.isFristLoginNeedChangePass() && !userLoginHistoryService.hasHistory(userAccount.getId())) {
            data.put(LoginBiz.NEED_CHANGE_PASS, true);
        } else {
            UserLoginHistory userLoginHistory = new UserLoginHistory();
            userLoginHistory.setUid(userAccount.getId());
            userLoginHistory.setIp(WebUtil.ip(Mvcs.getReq()));
            userLoginHistory.setType(loginType);
            try {
                UserAgent userAgent = UserAgent.parseUserAgentString(Mvcs.getReq().getHeader("User-Agent"));
                userLoginHistory.setOs(userAgent.getOperatingSystem().getName());
                userLoginHistory.setBrowser(userAgent.getBrowser().getName());
            } catch (Exception e) {
                log.error("UserAgent:", e);
            }
            userLoginHistoryService.async(userLoginHistory);
        }
        return data;
    }
}