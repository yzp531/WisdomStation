package com.winsion.component.basic.biz;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.RequiresPermission;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSON;
import com.bigkoo.pickerview.OptionsPickerView;
import com.bigkoo.pickerview.TimePickerView;
import com.bigkoo.pickerview.view.BasePickerView;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okserver.download.DownloadTask;
import com.winsion.component.basic.R;
import com.winsion.component.basic.constants.Urls;
import com.winsion.component.basic.data.CacheDataSource;
import com.winsion.component.basic.data.NetDataSource;
import com.winsion.component.basic.entity.MyObjectBox;
import com.winsion.component.basic.listener.MyDownloadListener;
import com.winsion.component.basic.listener.ResponseListener;
import com.winsion.component.basic.utils.AppUtils;
import com.winsion.component.basic.utils.ConvertUtils;
import com.winsion.component.basic.utils.DirAndFileUtils;
import com.winsion.component.basic.utils.ToastUtils;
import com.winsion.component.basic.view.CustomDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.objectbox.BoxStore;

/**
 * Created by 10295 on 2017/12/21 0021.
 * 通用业务层
 */

public class BasicBiz {
    /**
     * 检查版本更新
     *
     * @param context  上下文
     * @param showHint 没有更新/检查更新失败是否显示提示信息
     */
    public static void checkVersionUpdate(Context context, Object tag, boolean showHint) {
        if (CacheDataSource.getTestMode()) {
            UpdateEntity updateEntity = new UpdateEntity();
            updateEntity.setFilePath("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk");
            updateEntity.setVersionContent("新年快乐，大吉大利。");
            showUpdateDialog(context, updateEntity, tag);
            return;
        }
        HttpParams httpParams = new HttpParams();
        httpParams.put("key", "gridApp");
        NetDataSource.post(tag, Urls.CHECK_UPDATE, httpParams, new ResponseListener<UpdateEntity>() {
            @Override
            public UpdateEntity convert(String jsonStr) {
                return JSON.parseObject(jsonStr, UpdateEntity.class);
            }

            @Override
            public void onSuccess(UpdateEntity updateEntity) {
                if (AppUtils.getPackageInfo(context).versionCode < updateEntity.getVersionNumber()) {
                    // 需要更新
                    showUpdateDialog(context, updateEntity, tag);
                } else if (showHint) {
                    // 不需要更新
                    ToastUtils.showToast(context, R.string.toast_current_version_is_the_latest);
                }
            }

            @Override
            public void onFailed(int errorCode, String errorInfo) {
                if (showHint) ToastUtils.showToast(context, R.string.toast_check_update_failed);
            }
        });
    }

    /**
     * 显示更新版本对话框
     *
     * @param context      上下文
     * @param updateEntity 包含下载地址、更新信息
     */
    private static void showUpdateDialog(Context context, UpdateEntity updateEntity, Object tag) {
        // 需要更新,弹出对话框
        new CustomDialog.NormalBuilder(context)
                .setTitle(R.string.dialog_version_update)
                .setMessage(updateEntity.getVersionContent())
                .setPositiveButtonText(R.string.btn_update_now)
                .setPositiveButton((dialog, which) -> downloadNewVersion(context, updateEntity.getFilePath(), tag))
                .setNegativeButtonText(R.string.btn_update_later)
                .setIrrevocable()
                .show();
    }

    /**
     * 下载更新包
     *
     * @param context     上下文
     * @param downloadUrl 文件下载地址
     */
    private static void downloadNewVersion(Context context, String downloadUrl, Object tag) {
        try {
            // 下载文件目标存储目录
            String targetDir = DirAndFileUtils.getUpdateDir().getAbsolutePath();
            String[] split = downloadUrl.split("/");
            // 更新包文件名
            String fileName = split[split.length - 1];
            File updateFile = new File(targetDir, fileName);

            // 显示下载进度对话框
            CustomDialog.ProgressBuilder progressBuilder = (CustomDialog.ProgressBuilder)
                    new CustomDialog.ProgressBuilder(context)
                            .setMessage(R.string.dialog_downloading_installation_package)
                            .setIrrevocable();
            CustomDialog customDialog = progressBuilder.create();
            customDialog.show();

            // 下载更新包
            DownloadTask downloadTask = NetDataSource.downloadFile(tag, downloadUrl, targetDir,
                    new MyDownloadListener() {
                        @Override
                        public void downloadProgress(String serverUri, int progress) {
                            progressBuilder.setProgress(progress);
                        }

                        @Override
                        public void downloadSuccess(File file, String serverUri) {
                            customDialog.dismiss();
                            Uri downloadFileUri = Uri.fromFile(updateFile);
                            if (downloadFileUri != null) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            }
                        }

                        @Override
                        public void downloadFailed(String serverUri) {
                            progressBuilder.updateMessage(context.getString(R.string.toast_download_failed));
                        }
                    });
            downloadTask.start();

            // 取消下载
            progressBuilder.setNegativeButton((dialog, which) -> {
                downloadTask.remove();
                downloadTask.unRegister(downloadUrl);
                dialog.dismiss();
            });
        } catch (IOException e) {
            ToastUtils.showToast(context, R.string.toast_check_sdcard);
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    public static String getBSSID(Context context) {
        String bssid = "";
        String ssid = "";
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().
                getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            bssid = connectionInfo.getBSSID();
            ssid = connectionInfo.getSSID();
        }
        ssid = ssid.substring(1, ssid.length() - 1);
        // 根据配置的SSID对BSSID过滤
        if (!TextUtils.isEmpty(bssid) &&
                !TextUtils.isEmpty(CacheDataSource.getSsid()) &&
                TextUtils.equals(ssid, CacheDataSource.getSsid())) {
            bssid = bssid.replace(":", "").replace("-", "");
        } else {
            bssid = "";
        }
        return bssid;
    }

    /**
     * 隐藏软键盘
     * 可以和{@link #showKeyboard(EditText, boolean)}搭配使用，进行键盘的显示隐藏控制。
     *
     * @param view 当前页面上任意一个可用的view
     */
    public static void hideKeyboard(final View view) {
        if (null == view) return;
        InputMethodManager imm = (InputMethodManager) view.getContext().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 针对给定的editText显示软键盘（editText会先获得焦点）.
     * 可以和{@link #hideKeyboard(View)}搭配使用，进行键盘的显示隐藏控制。
     */
    @SuppressWarnings({"all"})
    public static void showKeyboard(final EditText editText, boolean delay) {
        if (null == editText || !editText.requestFocus()) return;
        InputMethodManager imm = (InputMethodManager) editText.getContext().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        if (delay) {
            editText.postDelayed(() -> imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT), 200);
        } else {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static TimePickerView.Builder getMyTimePickerBuilder(Context context, TimePickerView.OnTimeSelectListener listener) {
        return new TimePickerView.Builder(context, listener)
                .isCyclic(false)
                .setOutSideCancelable(true)
                // 标题背景颜色
                .setTitleBgColor(0xFF373739)
                // 滚轮背景颜色
                .setBgColor(0xFF3B3B3D)
                // 取消按钮文字颜色
                .setCancelColor(0xFF373739)
                // 确定按钮文字颜色
                .setSubmitColor(0xFF46DBE2)
                // 取消和确定文字大小
                .setSubCalSize(ConvertUtils.getDimenSp(context, R.dimen.basic_s18))
                // 滚轮文字大小
                .setContentSize(ConvertUtils.getDimenSp(context, R.dimen.basic_s20))
                // 选中的文字颜色
                .setTextColorCenter(0xFF46DBE2)
                // 是否只显示中间选中项的label文字，false则每项item全部都带有label
                .isCenterLabel(false);
    }

    public static OptionsPickerView.Builder getMyOptionPickerBuilder(
            Context context, OptionsPickerView.OnOptionsSelectListener listener) {
        //条件选择器
        return new OptionsPickerView.Builder(context, listener)
                .setCyclic(false, false, false)
                .setOutSideCancelable(true)
                // 标题背景颜色
                .setTitleBgColor(0xFF373739)
                // 滚轮背景颜色
                .setBgColor(0xFF3B3B3D)
                // 取消按钮文字颜色
                .setCancelColor(0xFF373739)
                // 确定按钮文字颜色
                .setSubmitColor(0xFF46DBE2)
                // 取消和确定文字大小
                .setSubCalSize(ConvertUtils.getDimenSp(context, R.dimen.basic_s18))
                // 滚轮文字大小
                .setContentTextSize(ConvertUtils.getDimenSp(context, R.dimen.basic_s20))
                // 选中的文字颜色
                .setTextColorCenter(0xFF46DBE2)
                // 是否只显示中间选中项的label文字，false则每项item全部都带有label
                .isCenterLabel(false);
    }

    /**
     * 适配选择器标题栏高度
     */
    public static void selfAdaptionTopBar(BasePickerView pickerView) {
        RelativeLayout tvTopBar = (RelativeLayout) pickerView.findViewById(R.id.rv_topbar);
        if (tvTopBar == null) return;
        ViewGroup.LayoutParams layoutParams = tvTopBar.getLayoutParams();
        layoutParams.height = tvTopBar.getResources().getDimensionPixelSize(R.dimen.basic_d45);
        tvTopBar.setLayoutParams(layoutParams);
    }

    /**
     * 折半查找
     * 条件：
     * 1.集合必须有序
     * 2.集合中元素必须实现{@link HalfSearchCondition}
     *
     * @param tList 在该集合中查找
     * @param findT 要查找的元素(判断相等的条件equalFieldValue())
     * @return 返回该元素在集合中的位置，不存在返回-1
     */
    public static int halfSearch(List<? extends HalfSearchCondition> tList, HalfSearchCondition findT) {
        int min = 0;
        int max = tList.size() - 1;
        while (min <= max) {
            int middle = (min + max) >>> 1;
            HalfSearchCondition halfSearchCondition = tList.get(middle);
            long time1 = halfSearchCondition.compareFieldValue();
            long time2 = findT.compareFieldValue();
            if (halfSearchCondition.equalFieldValue().equals(findT.equalFieldValue())) {
                return middle;
            } else if (time1 < time2) {
                min = middle + 1;
            } else {
                max = middle - 1;
            }
        }
        return -1;
    }

    private static BoxStore mBoxStore;

    public static BoxStore getBoxStore(Context context) {
        if (mBoxStore == null) {
            mBoxStore = MyObjectBox.builder().androidContext(context).build();
        }
        return mBoxStore;
    }

    /**
     * 折半查找条件
     */
    public interface HalfSearchCondition {
        /**
         * 用来比较是否相等的字段的值
         */
        String equalFieldValue();

        /**
         * 用来比较顺序的字段值
         */
        long compareFieldValue();
    }

    /**
     * Created by 10295 on 2018/1/30
     */

    public static class UpdateEntity {
        private int versionNumber;
        private String filePath;
        private String versionContent;

        public int getVersionNumber() {
            return versionNumber;
        }

        public void setVersionNumber(int versionNumber) {
            this.versionNumber = versionNumber;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getVersionContent() {
            return versionContent;
        }

        public void setVersionContent(String versionContent) {
            this.versionContent = versionContent;
        }
    }
}
