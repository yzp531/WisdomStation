package com.winsion.dispatch.modules.grid.activity.submitproblem;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bigkoo.pickerview.OptionsPickerView;
import com.winsion.dispatch.R;
import com.winsion.dispatch.base.BaseActivity;
import com.winsion.dispatch.capture.activity.CaptureActivity;
import com.winsion.dispatch.common.biz.CommonBiz;
import com.winsion.dispatch.data.listener.UploadListener;
import com.winsion.dispatch.media.activity.TakePhotoActivity;
import com.winsion.dispatch.media.adapter.RecordAdapter;
import com.winsion.dispatch.media.constants.FileStatus;
import com.winsion.dispatch.media.constants.FileType;
import com.winsion.dispatch.media.entity.LocalRecordEntity;
import com.winsion.dispatch.modules.grid.biz.SubmitBiz;
import com.winsion.dispatch.modules.grid.constants.DeviceState;
import com.winsion.dispatch.modules.grid.entity.PatrolItemEntity;
import com.winsion.dispatch.modules.grid.entity.SubclassEntity;
import com.winsion.dispatch.modules.operation.entity.FileEntity;
import com.winsion.dispatch.utils.DirAndFileUtils;
import com.winsion.dispatch.utils.ViewUtils;
import com.winsion.dispatch.view.TipDialog;
import com.winsion.dispatch.view.TitleView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import static com.winsion.dispatch.capture.activity.CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN;
import static com.winsion.dispatch.common.constants.Intents.Media.MEDIA_FILE;
import static com.winsion.dispatch.modules.grid.constants.Intents.SubmitProblem.DEVICE_DEPENDENT;
import static com.winsion.dispatch.modules.grid.constants.Intents.SubmitProblem.PATROL_ITEM_ENTITY;
import static com.winsion.dispatch.modules.grid.constants.Intents.SubmitProblem.SITE_NAME;

/**
 * Created by 10295 on 2018/2/2.
 * 上报问题界面
 */

public class SubmitProblemActivity extends BaseActivity implements SubmitProblemContact.View, UploadListener, SubmitBiz.SubmitListener {
    @BindView(R.id.tv_title)
    TitleView tvTitle;
    @BindView(R.id.rl_device_info)
    RelativeLayout rlDeviceInfo;
    @BindView(R.id.tv_site)
    TextView tvSite;
    @BindView(R.id.tv_device_name)
    TextView tvDeviceName;
    @BindView(R.id.tv_subclass)
    TextView tvSubclass;
    @BindView(R.id.tv_grade)
    TextView tvGrade;
    @BindView(R.id.tv_time_limit)
    TextView tvTimeLimit;
    @BindView(R.id.et_word_content)
    EditText etWordContent;
    @BindView(R.id.lv_record)
    ListView lvRecordList;

    private static final int CODE_TAKE_PHOTO = 0;   // 拍照
    private static final int CODE_CAPTURE_QR = 1;   // 扫描二维码

    private SubmitProblemContact.Presenter mPresenter;
    private PatrolItemEntity patrolItemEntity;
    private String siteName;
    private boolean deviceDependent;
    private TipDialog mLoadingDialog;
    private File photoFile; // 拍摄的照片文件
    private List<LocalRecordEntity> localRecordEntities = new ArrayList<>(); // 上传的附件
    private RecordAdapter recordAdapter;
    private String mDeviceId;   // 设备ID
    private String mClassificationId;    // 设备对应的类别ID
    private String mSelectSubclassId;    // 所选子类ID
    private int selectSubclassPosition; // 选中的子项在列表中的位置

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.exit();
    }

    @Override
    protected int setContentView() {
        return R.layout.activity_submit_problem;
    }

    @Override
    protected void start() {
        initPresenter();
        getIntentData();
        initView();
        initAdapter();
    }

    private void initPresenter() {
        mPresenter = new SubmitProblemPresenter(this);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        patrolItemEntity = (PatrolItemEntity) intent.getSerializableExtra(PATROL_ITEM_ENTITY);
        siteName = intent.getStringExtra(SITE_NAME);
        deviceDependent = intent.getBooleanExtra(DEVICE_DEPENDENT, false);
    }

    private void initView() {
        tvTitle.setOnBackClickListener(v -> showHintDialog());
        tvTitle.setOnConfirmClickListener(v -> submit());

        if (!deviceDependent) {
            rlDeviceInfo.setVisibility(View.GONE);
        } else {
            tvSite.setText(siteName);
        }
    }

    private void initAdapter() {
        recordAdapter = new RecordAdapter(mContext, localRecordEntities);
        lvRecordList.setAdapter(recordAdapter);
    }

    /**
     * 上报问题
     */
    private void submit() {
        // 检查数据是否填写完整
        if (checkDataIsComplete()) {
            // 隐藏软键盘
            CommonBiz.hideKeyboard(tvTitle);
            // 上报中，显示对话框
            showDoingDialog(R.string.dialog_on_submit);
            // 获取上传的附件
            ArrayList<FileEntity> fileList = new ArrayList<>();
            for (LocalRecordEntity localRecordEntity : localRecordEntities) {
                FileEntity fileEntity = new FileEntity();
                fileEntity.setFileName(localRecordEntity.getFile().getName());
                fileEntity.setFileType(localRecordEntity.getFileType());
                fileList.add(fileEntity);
            }

            // 上报问题
            if (deviceDependent) {
                // 设备相关问题
                ((SubmitBiz) mPresenter).submitWithDevice(patrolItemEntity, mSelectSubclassId,
                        fileList, getText(etWordContent), mDeviceId, this);
            } else {
                // 设备无关问题
                ((SubmitBiz) mPresenter).submitWithoutDevice(patrolItemEntity, DeviceState.FAILURE,
                        fileList, getText(etWordContent), this);
            }
        }
    }

    /**
     * 检查数据是否填写完整
     *
     * @return
     */
    private boolean checkDataIsComplete() {
        if (deviceDependent) {
            String deviceName = getText(tvDeviceName);
            if (isEmpty(mSelectSubclassId) || isEmpty(deviceName)) {
                showToast(R.string.toast_complete_info);
                return false;
            }
        }
        for (LocalRecordEntity localRecordEntity : localRecordEntities) {
            if (localRecordEntity.getFileStatus() != FileStatus.SYNCHRONIZED) {
                showToast(getString(R.string.toast_wait_for_files_upload_complete));
                return false;
            }
        }
        return true;
    }

    /**
     * 显示进行中对话框
     *
     * @param tipWord 提示状态文字
     */
    private void showDoingDialog(@StringRes int tipWord) {
        if (mLoadingDialog == null) {
            mLoadingDialog = new TipDialog.Builder(mContext)
                    .setIconType(TipDialog.Builder.ICON_TYPE_LOADING)
                    .setTipWord(getString(tipWord))
                    .create();
        } else {
            mLoadingDialog.updateTipWord(getString(tipWord));
        }
        mLoadingDialog.show();
    }

    @OnClick({R.id.tv_device_name, R.id.iv_scan, R.id.tv_subclass, R.id.iv_take_photo})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_device_name:
                // 创建对话框
                EditText editText = new EditText(this);
                new AlertDialog.Builder(mContext)
                        .setView(editText)
                        .setMessage(R.string.title_input_device_id)
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                            String deviceId = getText(editText);
                            if (!isEmpty(deviceId)) {
                                dialog.dismiss();
                                checkDeviceId(deviceId);
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                        .show();

                // 调整EditText的margin值
                int margin = getResources().getDimensionPixelSize(R.dimen.d10);
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) editText.getLayoutParams();
                layoutParams.setMargins(margin, 0, margin, 0);
                editText.setLayoutParams(layoutParams);
                break;
            case R.id.iv_scan:
                // 跳转扫描二维码界面
                startActivityForResult(CaptureActivity.class, CODE_CAPTURE_QR);
                break;
            case R.id.tv_subclass:
                // 选择子类(应先添加设备)
                if (isEmpty(mClassificationId)) {
                    showToast(R.string.toast_add_device_first);
                } else {
                    // 隐藏软键盘
                    CommonBiz.hideKeyboard(etWordContent);
                    // 显示查询中对话框
                    showDoingDialog(R.string.dialog_on_search);
                    // 获取子类数据
                    mPresenter.getSubclass(mClassificationId);
                }
                break;
            case R.id.iv_take_photo:
                // 跳转拍照界面
                try {
                    photoFile = CommonBiz.getMediaFile(DirAndFileUtils.getIssueDir(), FileType.PICTURE);
                    Intent intent = new Intent(mContext, TakePhotoActivity.class);
                    intent.putExtra(MEDIA_FILE, photoFile);
                    startActivityForResult(intent, CODE_TAKE_PHOTO);
                } catch (IOException e) {
                    showToast(R.string.toast_check_sdcard);
                }
                break;
        }
    }

    /**
     * 根据输入或扫描到的设备ID查找设备对应的问题类别
     *
     * @param deviceId 设备ID
     */
    private void checkDeviceId(String deviceId) {
        // 查询设备编号中，显示dialog
        showDoingDialog(R.string.dialog_on_search);
        mPresenter.checkDeviceId(deviceId);
    }

    @Override
    public void checkDeviceIdSuccess(String deviceName, String classificationId, String deviceId) {
        mLoadingDialog.dismiss();
        tvDeviceName.setText(deviceName);
        mClassificationId = classificationId;
        mDeviceId = deviceId;

        // 清空上一次得到的数据
        mSelectSubclassId = null;
        tvSubclass.setText("");
        tvGrade.setText("");
        tvTimeLimit.setText("");
        selectSubclassPosition = 0;
    }

    @Override
    public void checkDeviceIdFailed(@StringRes int errorInfo) {
        mLoadingDialog.dismiss();
        showToast(errorInfo);
    }

    @Override
    public void getSubclassSuccess(List<SubclassEntity> list) {
        mLoadingDialog.dismiss();
        // 创建选择器
        List<String> nameList = new ArrayList<>();
        for (SubclassEntity subclassDto : list) {
            nameList.add(subclassDto.getTypename());
        }
        OptionsPickerView.Builder pickerBuilder = CommonBiz.getMyOptionPickerBuilder(mContext,
                (int options1, int options2, int options3, View v1) -> {
                    SubclassEntity subclassDto = list.get(options1);
                    mSelectSubclassId = subclassDto.getId();
                    tvSubclass.setText(subclassDto.getTypename());
                    tvGrade.setText(String.valueOf(subclassDto.getPriority()));
                    int planCostTime = subclassDto.getPlancosttime();
                    tvTimeLimit.setText(String.format("%s%s", planCostTime, getString(R.string.suffix_minute)));
                    selectSubclassPosition = options1;
                });
        OptionsPickerView<String> pickerView = new OptionsPickerView<>(pickerBuilder);
        pickerView.setPicker(nameList);
        pickerView.setSelectOptions(selectSubclassPosition);
        CommonBiz.selfAdaptionTopBar(pickerView);
        pickerView.show();
    }

    @Override
    public void getSubclassFailed() {
        mLoadingDialog.dismiss();
        showToast(R.string.toast_get_subclass_failed);
    }

    @Override
    public void submitSuccess(PatrolItemEntity patrolItemEntity, String deviceState) {
        mLoadingDialog.dismiss();
        Intent intent = new Intent();
        intent.putExtra(PATROL_ITEM_ENTITY, patrolItemEntity);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void submitFailed() {
        showToast(R.string.toast_submit_failed);
        mLoadingDialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CODE_TAKE_PHOTO:
                    // 拍照成功
                    LocalRecordEntity localRecordEntity = new LocalRecordEntity();
                    localRecordEntity.setFileType(FileType.PICTURE);
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    localRecordEntity.setFile(photoFile);
                    localRecordEntities.add(localRecordEntity);
                    recordAdapter.notifyDataSetChanged();
                    ViewUtils.setListViewHeightBasedOnChildren(lvRecordList);
                    // 上传
                    mPresenter.uploadFile(photoFile, this);
                    break;
                case CODE_CAPTURE_QR:
                    String result = data.getStringExtra(INTENT_EXTRA_KEY_QR_SCAN);
                    checkDeviceId(result);
                    break;
            }
        }
    }

    @Override
    public void uploadProgress(File uploadFile, int progress) {
        for (LocalRecordEntity localRecordEntity : localRecordEntities) {
            if (localRecordEntity.getFile() == uploadFile) {
                localRecordEntity.setFileStatus(FileStatus.UPLOADING);
                localRecordEntity.setProgress(progress);
                recordAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void uploadSuccess(File uploadFile) {
        for (LocalRecordEntity localRecordEntity : localRecordEntities) {
            if (localRecordEntity.getFile() == uploadFile) {
                localRecordEntity.setFileStatus(FileStatus.SYNCHRONIZED);
                recordAdapter.notifyDataSetChanged();
                showToast(R.string.toast_upload_success);
                break;
            }
        }
    }

    @Override
    public void uploadFailed(File uploadFile) {
        for (LocalRecordEntity localRecordEntity : localRecordEntities) {
            if (localRecordEntity.getFile() == uploadFile) {
                localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                recordAdapter.notifyDataSetChanged();
                showToast(R.string.toast_upload_failed);
                break;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showHintDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showHintDialog() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.dialog_after_exiting_data_will_be_cleared_are_you_sure)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    // 删除附件
                    deleteRecordFiles();
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * 没有上报而退出需要删除本地已经保存的附件
     */
    private void deleteRecordFiles() {
        if (localRecordEntities.size() != 0) {
            int deleteFileSize = 0;
            for (LocalRecordEntity localRecordEntity : localRecordEntities) {
                File file = localRecordEntity.getFile();
                if (file.delete()) deleteFileSize++;
            }
            if (deleteFileSize != localRecordEntities.size()) {
                showToast(R.string.toast_local_file_clear_failed);
            } else {
                showToast(R.string.toast_local_file_clear_success);
            }
        }
    }

    @Override
    public Context getContext() {
        return mContext;
    }

}