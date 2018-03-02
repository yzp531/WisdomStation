package com.winsion.dispatch.modules.operation.activity.issue;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.bigkoo.pickerview.OptionsPickerView;
import com.bigkoo.pickerview.TimePickerView;
import com.winsion.dispatch.R;
import com.winsion.dispatch.base.BaseActivity;
import com.winsion.dispatch.common.biz.CommonBiz;
import com.winsion.dispatch.data.CacheDataSource;
import com.winsion.dispatch.data.NetDataSource;
import com.winsion.dispatch.data.constants.OpeCode;
import com.winsion.dispatch.data.constants.Urls;
import com.winsion.dispatch.data.listener.ResponseListener;
import com.winsion.dispatch.data.listener.UploadListener;
import com.winsion.dispatch.media.activity.RecordAudioActivity;
import com.winsion.dispatch.media.activity.RecordVideoActivity;
import com.winsion.dispatch.media.activity.TakePhotoActivity;
import com.winsion.dispatch.media.adapter.RecordAdapter;
import com.winsion.dispatch.media.constants.FileStatus;
import com.winsion.dispatch.media.constants.FileType;
import com.winsion.dispatch.media.entity.LocalRecordEntity;
import com.winsion.dispatch.modules.operation.constants.TaskType;
import com.winsion.dispatch.modules.operation.entity.FileEntity;
import com.winsion.dispatch.modules.operation.entity.PublishParameter;
import com.winsion.dispatch.modules.operation.entity.RunEntity;
import com.winsion.dispatch.modules.operation.entity.TeamEntity;
import com.winsion.dispatch.utils.ConvertUtils;
import com.winsion.dispatch.utils.DirAndFileUtils;
import com.winsion.dispatch.utils.ViewUtils;
import com.winsion.dispatch.utils.constants.Formatter;
import com.winsion.dispatch.view.CustomDialog;
import com.winsion.dispatch.view.TitleView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.winsion.dispatch.common.constants.Intents.Media.MEDIA_FILE;
import static com.winsion.dispatch.modules.operation.constants.Intents.Issue.ISSUE_TYPE;
import static com.winsion.dispatch.modules.operation.constants.Intents.Issue.SELECT_TEAM;
import static com.winsion.dispatch.modules.operation.constants.Intents.Issue.TO_TEAM_ENTITY;

/**
 * 发布命令/协作界面
 * Created by wyl on 2016/8/1.
 */
public class IssueActivity extends BaseActivity implements UploadListener {
    private TitleView tvTitle;
    private TextView tvPerformerGroupHint;
    private EditText etContent;
    private TextView tvStation;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private TextView tvTeamList;
    private EditText etTitle;
    private TextView tvTrainNumber;
    private ListView listView;

    // 选择班组
    public static final int CODE_SELECT_TEAM = 0;
    // 选择车次
    public static final int CODE_SELECT_TRAIN = 1;
    // 拍照
    public static final int CODE_TAKE_PHOTO = 2;
    // 录像
    public static final int CODE_RECORD_VIDEO = 3;
    // 录音
    public static final int CODE_RECORD_AUDIO = 4;

    // 记录选中的车站，用于回显
    private int selectStationIndex;
    // 候选车次数据
    private List<String> stationList = new ArrayList<>();

    // 选中的班组ID
    private String teamIds;
    // 选中的班组名
    private String teamNames;
    // 车次ID
    private String runsId = "";
    // 发布类型 命令/协作 从上个页面带过来
    private int issueType;

    // 记录 拍照/录像/录音
    private RecordAdapter recordAdapter;
    private ArrayList<LocalRecordEntity> localRecordEntities = new ArrayList<>();
    private File photoFile;
    private File videoFile;
    private File audioFile;
    // 发布中显示dialog
    private CustomDialog customDialog;

    /**
     * @param context   上下文
     * @param issueType 发布类型(命令<COMMAND>/协作<COOPERATE>)
     *                  {@link TaskType}
     */
    public static void startIssueActivity(Context context, @TaskType int issueType) {
        startIssueActivity(context, issueType, null);
    }

    /**
     * @param context    上下文
     * @param issueType  发布类型(命令<COMMAND>/协作<COOPERATE>)
     *                   {@link TaskType}
     * @param teamEntity 发布给班组的班组对象，可以为空
     */
    public static void startIssueActivity(Context context, @TaskType int issueType, TeamEntity teamEntity) {
        Intent intent = new Intent(context, IssueActivity.class);
        intent.putExtra(ISSUE_TYPE, issueType);
        if (teamEntity != null) {
            intent.putExtra(TO_TEAM_ENTITY, teamEntity);
        }
        context.startActivity(intent);
    }

    @Override
    protected int setContentView() {
        return R.layout.activity_issue;
    }

    @Override
    protected void start() {
        initView();
        initData();
        initListener();
        initAdapter();
    }

    private void initView() {
        tvTitle = findViewById(R.id.tv_title);
        tvPerformerGroupHint = findViewById(R.id.tv_performer_group_hint);
        etContent = findViewById(R.id.et_content);
        tvStation = findViewById(R.id.tv_station);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        tvTeamList = findViewById(R.id.tv_team_list);
        etTitle = findViewById(R.id.et_title);
        tvTrainNumber = findViewById(R.id.tv_train_number);
        listView = findViewById(R.id.list_view);
    }

    private void initData() {
        initIntentData();
        initStationData();
        initViewData();
    }

    private void initIntentData() {
        Intent intent = getIntent();
        issueType = intent.getIntExtra(ISSUE_TYPE, -1);
        if (issueType != TaskType.COMMAND && issueType != TaskType.COOPERATE) {
            throw new RuntimeException("The Issue type is not supported!");
        }
        switch (issueType) {
            case TaskType.COMMAND:
                tvTitle.setTitleText(getString(R.string.title_issue_command));
                break;
            case TaskType.COOPERATE:
                tvTitle.setTitleText(getString(R.string.title_issue_cooperation));
                break;
        }
        TeamEntity toTeamEntity = (TeamEntity) intent.getSerializableExtra(TO_TEAM_ENTITY);
        if (toTeamEntity != null) {
            teamIds = toTeamEntity.getTeamid();
            teamNames = toTeamEntity.getTeamsName();
        }
    }

    // 选择车站数据
    private void initStationData() {
        stationList.add(getString(R.string.spinner_station_name));
    }

    private void initViewData() {
        // 命令/协作内容  hint
        switch (issueType) {
            case TaskType.COMMAND:
                tvPerformerGroupHint.setText(R.string.name_order_group);
                etContent.setHint(R.string.name_command_content);
                break;
            case TaskType.COOPERATE:
                tvPerformerGroupHint.setText(R.string.name_cooperation_group);
                etContent.setHint(R.string.name_cooperation_content);
                break;
        }
        // 进入界面默任回写开始时间
        tvStartTime.setText(ConvertUtils.formatDate(System.currentTimeMillis(), Formatter.DATE_FORMAT1));
        // 进入界面默任回写后一天的时间
        tvEndTime.setText(ConvertUtils.formatDate(System.currentTimeMillis() + 1000 * 60 * 60 * 24, Formatter.DATE_FORMAT1));
        // 回显跳转过来时传递的班组
        if (isEmpty(teamNames)) {
            tvTeamList.setText(teamNames);
        }
    }

    private void initListener() {
        tvTitle.setOnBackClickListener(v -> showHintDialog());
        tvTitle.setOnConfirmClickListener(v -> issue());
        addOnClickListeners(R.id.tv_station, R.id.tv_start_time, R.id.tv_end_time, R.id.iv_add_performer,
                R.id.tv_train_number, R.id.btn_take_photo, R.id.btn_video, R.id.btn_record);
    }

    private void initAdapter() {
        recordAdapter = new RecordAdapter(mContext, localRecordEntities);
        recordAdapter.setUploadPerformer(localRecordEntity -> NetDataSource.uploadFileNoData(this,
                localRecordEntity.getFile(), this));
        listView.setAdapter(recordAdapter);
    }

    /**
     * 发布命令/协作
     */
    private void issue() {
        // 检查数据是否填写完整
        if (checkDataIsComplete()) {
            // 隐藏软键盘
            CommonBiz.hideKeyboard(tvTitle);
            // 发布中，显示dialog
            showOnIssueDialog();
            // 发布
            ArrayList<FileEntity> fileList = new ArrayList<>();
            for (LocalRecordEntity localRecordEntity : localRecordEntities) {
                FileEntity fileEntity = new FileEntity();
                fileEntity.setFileName(localRecordEntity.getFileName());
                fileEntity.setFileType(localRecordEntity.getFileType());
                fileList.add(fileEntity);
            }

            PublishParameter publishParameter = new PublishParameter();
            publishParameter.setRunsId(runsId);
            publishParameter.setSsId(CommonBiz.getBSSID(this));
            publishParameter.setUsersId(CacheDataSource.getUserId());
            publishParameter.setTaskName(getText(etTitle));
            publishParameter.setPlanEndTime(getText(tvEndTime));
            publishParameter.setTaskType(issueType);
            publishParameter.setNote("");
            publishParameter.setPlanStartTime(getText(tvStartTime));
            publishParameter.setWorkContent(getText(etContent));
            publishParameter.setMonitorTeamId(CacheDataSource.getTeamId());
            publishParameter.setAreaId("");
            publishParameter.setOperatorTeamId(teamIds);
            publishParameter.setFileList(fileList);

            NetDataSource.post(this, Urls.JOb, publishParameter, OpeCode.ISSUE,
                    new ResponseListener<String>() {
                        @Override
                        public String convert(String jsonStr) {
                            return jsonStr;
                        }

                        @Override
                        public void onSuccess(String result) {
                            customDialog.dismiss();
                            showToast(R.string.toast_issue_success);
                            finish();
                        }

                        @Override
                        public void onFailed(int errorCode, String errorInfo) {
                            customDialog.dismiss();
                            showToast(R.string.toast_issue_failed);
                        }
                    });
        }
    }

    private boolean checkDataIsComplete() {
        String title = getText(etTitle);
        String content = getText(etContent);
        if (isEmpty(getText(tvStation)) || isEmpty(teamIds) || isEmpty(title) || isEmpty(content)) {
            showToast(getString(R.string.toast_complete_info));
            return false;
        }
        for (LocalRecordEntity localRecordEntity : localRecordEntities) {
            if (localRecordEntity.getFileStatus() != FileStatus.SYNCHRONIZED) {
                showToast(getString(R.string.toast_wait_for_files_upload_complete));
                return false;
            }
        }
        return true;
    }

    private void showOnIssueDialog() {
        if (customDialog == null) {
            customDialog = new CustomDialog.StateBuilder(mContext)
                    .setStateText(R.string.dialog_on_issue)
                    .setIrrevocable()
                    .create();
        }
        customDialog.show();
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.tv_station:
                // 选择车站点击事件
                showStationPickerView(view);
                break;
            case R.id.tv_start_time:
                // 开始时间点击事件
                showTimePickerView((TextView) view, 0);
                break;
            case R.id.tv_end_time:
                // 结束时间点击事件
                showTimePickerView((TextView) view, 1);
                break;
            case R.id.iv_add_performer:
                // 选择班组按钮点击事件
                startActivityForResult(SelectTeamActivity.class, CODE_SELECT_TEAM);
                break;
            case R.id.tv_train_number:
                // 选择车次
                startActivityForResult(SelectTrainActivity.class, CODE_SELECT_TRAIN);
                break;
            case R.id.btn_take_photo:
                try {
                    photoFile = CommonBiz.getMediaFile(DirAndFileUtils.getIssueDir(), FileType.PICTURE);
                    intent = new Intent(mContext, TakePhotoActivity.class);
                    intent.putExtra(MEDIA_FILE, photoFile);
                    startActivityForResult(intent, CODE_TAKE_PHOTO);
                } catch (IOException e) {
                    showToast(R.string.toast_check_sdcard);
                }
                break;
            case R.id.btn_video:
                try {
                    videoFile = CommonBiz.getMediaFile(DirAndFileUtils.getIssueDir(), FileType.VIDEO);
                    intent = new Intent(mContext, RecordVideoActivity.class);
                    intent.putExtra(MEDIA_FILE, videoFile);
                    startActivityForResult(intent, CODE_RECORD_VIDEO);
                } catch (IOException e) {
                    showToast(R.string.toast_check_sdcard);
                }
                break;
            case R.id.btn_record:
                try {
                    audioFile = CommonBiz.getMediaFile(DirAndFileUtils.getIssueDir(), FileType.AUDIO);
                    intent = new Intent(mContext, RecordAudioActivity.class);
                    intent.putExtra(MEDIA_FILE, audioFile);
                    startActivityForResult(intent, CODE_RECORD_AUDIO);
                } catch (IOException e) {
                    showToast(R.string.toast_check_sdcard);
                }
                break;
        }
    }

    private void showStationPickerView(View v) {
        // 隐藏软键盘
        CommonBiz.hideKeyboard(v);
        // 创建选择器
        OptionsPickerView.Builder pickerBuilder = CommonBiz.getMyOptionPickerBuilder(mContext,
                (int options1, int options2, int options3, View v1) -> {
                    selectStationIndex = options1;
                    tvStation.setText(stationList.get(options1));
                });
        OptionsPickerView<String> pickerView = new OptionsPickerView<>(pickerBuilder);
        pickerView.setPicker(stationList);
        pickerView.setSelectOptions(selectStationIndex);
        CommonBiz.selfAdaptionTopBar(pickerView);
        pickerView.show();
    }

    /**
     * 显示时间选择器
     *
     * @param textView 点击的textView
     * @param type     开始时间/结束时间 0/1
     */
    private void showTimePickerView(TextView textView, int type) {
        // 隐藏软键盘
        CommonBiz.hideKeyboard(textView);

        // 获取回显时间
        Date currentDate;
        String str = getText(textView);
        if (isEmpty(str)) {
            currentDate = new Date(System.currentTimeMillis());
        } else {
            currentDate = new Date(ConvertUtils.parseDate(str, Formatter.DATE_FORMAT1));
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        TimePickerView.OnTimeSelectListener onTimeSelectListener = (date, v) -> {
            if (type == 0) {
                // 开始时间不能晚于结束时间
                if (date.getTime() > ConvertUtils.parseDate(getText(tvEndTime), Formatter.DATE_FORMAT1)) {
                    showToast(getString(R.string.toast_start_time_cannot_be_later_than_finish_time));
                    return;
                }
            } else if (type == 1) {
                // 结束时间不能早于开始时间
                if (date.getTime() < ConvertUtils.parseDate(getText(tvStartTime), Formatter.DATE_FORMAT1)) {
                    showToast(getString(R.string.toast_finish_time_cannot_be_earlier_than_start_time));
                    return;
                }
            }
            textView.setText(ConvertUtils.formatDate(date.getTime(), Formatter.DATE_FORMAT1));
        };

        // 创建选择器
        TimePickerView timePickerView = CommonBiz.getMyTimePickerBuilder(mContext, onTimeSelectListener)
                .setType(new boolean[]{true, true, true, true, true, false})
                .setRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
                .setDate(calendar)
                .build();
        CommonBiz.selfAdaptionTopBar(timePickerView);
        timePickerView.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            LocalRecordEntity localRecordEntity;
            switch (requestCode) {
                case CODE_SELECT_TEAM:
                    ArrayList selectData = (ArrayList) data.getSerializableExtra(SELECT_TEAM);
                    StringBuilder teamIdsSb = new StringBuilder();
                    StringBuilder teamNamesSb = new StringBuilder();
                    for (int i = 0; i < selectData.size(); i++) {
                        TeamEntity teamEntity = (TeamEntity) selectData.get(i);
                        if (i == selectData.size() - 1) {
                            teamIdsSb.append(teamEntity.getTeamid());
                            teamNamesSb.append(teamEntity.getTeamsName());
                        } else {
                            teamIdsSb.append(teamEntity.getTeamid()).append(",");
                            teamNamesSb.append(teamEntity.getTeamsName()).append(",");
                        }
                    }

                    teamNames = teamNamesSb.toString();
                    teamIds = teamIdsSb.toString();
                    tvTeamList.setText(teamNamesSb.toString());
                    break;
                case CODE_SELECT_TRAIN:
                    RunEntity runEntity = (RunEntity) data.getSerializableExtra("runEntity");
                    runsId = runEntity.getRunsid();
                    tvTrainNumber.setText(runEntity.getTrainnumber());
                    break;
                case CODE_TAKE_PHOTO:
                    // 拍照成功
                    localRecordEntity = new LocalRecordEntity();
                    localRecordEntity.setFileType(FileType.PICTURE);
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    localRecordEntity.setFile(photoFile);
                    localRecordEntity.setFileName(photoFile.getName());
                    localRecordEntities.add(localRecordEntity);
                    recordAdapter.notifyDataSetChanged();
                    ViewUtils.setListViewHeightBasedOnChildren(listView);
                    // 上传
                    NetDataSource.uploadFileNoData(this, photoFile, this);
                    break;
                case CODE_RECORD_VIDEO:
                    // 录像成功
                    localRecordEntity = new LocalRecordEntity();
                    localRecordEntity.setFileType(FileType.VIDEO);
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    localRecordEntity.setFile(videoFile);
                    localRecordEntity.setFileName(videoFile.getName());
                    localRecordEntities.add(localRecordEntity);
                    recordAdapter.notifyDataSetChanged();
                    ViewUtils.setListViewHeightBasedOnChildren(listView);
                    // 上传
                    NetDataSource.uploadFileNoData(this, videoFile, this);
                    break;
                case CODE_RECORD_AUDIO:
                    // 录音成功
                    localRecordEntity = new LocalRecordEntity();
                    localRecordEntity.setFileType(FileType.AUDIO);
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    localRecordEntity.setFile(audioFile);
                    localRecordEntity.setFileName(audioFile.getName());
                    localRecordEntities.add(localRecordEntity);
                    recordAdapter.notifyDataSetChanged();
                    ViewUtils.setListViewHeightBasedOnChildren(listView);
                    // 上传
                    NetDataSource.uploadFileNoData(this, audioFile, this);
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
        new CustomDialog.NormalBuilder(mContext)
                .setMessage(R.string.dialog_after_exiting_data_will_be_cleared_are_you_sure)
                .setPositiveButton((dialog, which) -> {
                    // 删除附件
                    deleteRecordFiles();
                    finish();
                })
                .show();
    }

    /**
     * 没有发布而退出需要删除本地已经保存的附件
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
    protected void onDestroy() {
        super.onDestroy();
        NetDataSource.unSubscribe(this);
    }
}
