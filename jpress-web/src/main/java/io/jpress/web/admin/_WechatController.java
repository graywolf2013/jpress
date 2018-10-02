package io.jpress.web.admin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.weixin.sdk.api.ApiResult;
import io.jboot.utils.StrUtils;
import io.jboot.web.controller.annotation.RequestMapping;
import io.jboot.wechat.WechatApis;
import io.jpress.JPressConstants;
import io.jpress.core.menu.annotation.AdminMenu;
import io.jpress.core.wechat.WechatAddonInfo;
import io.jpress.core.wechat.WechatAddonManager;
import io.jpress.model.WechatMenu;
import io.jpress.model.WechatReplay;
import io.jpress.service.OptionService;
import io.jpress.service.WechatMenuService;
import io.jpress.service.WechatReplayService;
import io.jpress.web.base.AdminControllerBase;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 * @Title: 首页
 * @Package io.jpress.web.admin
 */
@RequestMapping("/admin/wechat")
public class _WechatController extends AdminControllerBase {

    @Inject
    private WechatReplayService replayService;

    @Inject
    private OptionService optionService;

    @Inject
    private WechatMenuService wechatMenuService;


    @AdminMenu(text = "基础设置", groupId = JPressConstants.SYSTEM_MENU_WECHAT_PUBULIC_ACCOUNT, order = 1)
    public void base() {
        render("wechat/setting_base.html");
    }


    @AdminMenu(text = "菜单设置", groupId = JPressConstants.SYSTEM_MENU_WECHAT_PUBULIC_ACCOUNT, order = 2)
    public void menu() {
        render("wechat/menu.html");
    }


    @AdminMenu(text = "默认回复", groupId = JPressConstants.SYSTEM_MENU_WECHAT_PUBULIC_ACCOUNT, order = 10)
    public void replay() {
        render("wechat/replay_base.html");
    }


    @AdminMenu(text = "自动回复", groupId = JPressConstants.SYSTEM_MENU_WECHAT_PUBULIC_ACCOUNT, order = 11)
    public void keyword() {
        Page<WechatReplay> page = replayService._paginate(getPagePara(), 10, getPara("keyword"), getPara("content"));
        setAttr("page", page);
        render("wechat/replay_list.html");
    }


    public void doDelReplay() {
        Long id = getIdPara();
        replayService.deleteById(id);
        renderJson(Ret.ok());
    }

    public void doDelReplayByIds() {
        String ids = getPara("ids");
        if (StrUtils.isBlank(ids)) {
            renderJson(Ret.fail());
            return;
        }

        Set<String> idsSet = StrUtils.splitToSet(ids, ",");
        if (idsSet == null || idsSet.isEmpty()) {
            renderJson(Ret.fail());
            return;
        }
        render(replayService.deleteByIds(idsSet.toArray()) ? Ret.ok() : Ret.fail());
    }


    @AdminMenu(text = "运营插件", groupId = JPressConstants.SYSTEM_MENU_WECHAT_PUBULIC_ACCOUNT, order = 99)
    public void addons() {
        List<WechatAddonInfo> wechatAddons = WechatAddonManager.me().getWechatAddons();
        setAttr("wechatAddons", wechatAddons);
        render("wechat/addons.html");
    }

    public void doEnableAddon(String id) {
        WechatAddonManager.me().doEnableAddon(id);
        renderJson(Ret.ok());
    }

    public void doCloseAddon(String id) {
        WechatAddonManager.me().doCloseAddon(id);
        renderJson(Ret.ok());
    }


    public void replayWrite() {
        int id = getParaToInt(0, 0);
        if (id > 0) {
            WechatReplay wechatReplay = replayService.findById(id);
            setAttr("replay", wechatReplay);
        }
        render("wechat/replay_write.html");
    }

    public void doReplaySave() {
        WechatReplay replay = getBean(WechatReplay.class, "");
        replayService.saveOrUpdate(replay);
        redirect("/admin/wechat/keyword");
    }

    /**
     * 微信菜单同步
     */
    public void doMenuSync() {
        List<WechatMenu> wechatMenus = wechatMenuService.findAll();
//        ModelSorter.tree(wechatMenus);

        if (wechatMenus == null || wechatMenus.isEmpty()) {
            renderJson(Ret.fail().set("message", "微信菜单为空"));
            return;
        }

        JSONArray button = new JSONArray();
        for (WechatMenu wechatMenu : wechatMenus) {
            if (wechatMenu.hasChild()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", wechatMenu.getText());
                List<WechatMenu> childMenus = wechatMenu.getChilds();
                JSONArray sub_buttons = new JSONArray();
                for (WechatMenu child : childMenus) {
                    createJsonObjectButton(sub_buttons, child);
                }
                jsonObject.put("sub_button", sub_buttons);
                button.add(jsonObject);
            } else {
                createJsonObjectButton(button, wechatMenu);
            }
        }

        JSONObject wechatMenuJson = new JSONObject();
        wechatMenuJson.put("button", button);
        String jsonString = wechatMenuJson.toJSONString();

        ApiResult result = WechatApis.createMenu(jsonString);
        if (result.isSucceed()) {
            renderJson(Ret.ok());
        } else {
            renderJson(Ret.ok().set("message", "同步微信菜单出错，错误码：" + result.getErrorCode()));
        }

    }

    private void createJsonObjectButton(JSONArray button, WechatMenu content) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", content.getType());
        jsonObject.put("name", content.getText());

        if ("view".equals(content.getType())) {
            jsonObject.put("url", content.getText());
        } else {
            jsonObject.put("key", content.getText());
        }

        button.add(jsonObject);
    }

}
