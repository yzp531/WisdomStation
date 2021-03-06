package com.winsion.component.task.activity.taskoperator;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsion.component.basic.base.BaseActivity;
import com.winsion.component.basic.constants.Formatter;
import com.winsion.component.basic.constants.OpeType;
import com.winsion.component.basic.data.CacheDataSource;
import com.winsion.component.basic.listener.MyDownloadListener;
import com.winsion.component.basic.listener.MyUploadListener;
import com.winsion.component.basic.listener.StateListener;
import com.winsion.component.basic.utils.ConvertUtils;
import com.winsion.component.basic.utils.DirAndFileUtils;
import com.winsion.component.basic.utils.FileUtils;
import com.winsion.component.basic.utils.ImageLoader;
import com.winsion.component.basic.utils.ViewUtils;
import com.winsion.component.basic.view.CustomDialog;
import com.winsion.component.basic.view.DrawableCenterTextView;
import com.winsion.component.basic.view.TitleView;
import com.winsion.component.media.activity.RecordAudioActivity;
import com.winsion.component.media.activity.RecordVideoActivity;
import com.winsion.component.media.activity.TakePhotoActivity;
import com.winsion.component.media.adapter.RecordAdapter;
import com.winsion.component.media.biz.MediaBiz;
import com.winsion.component.media.constants.FileStatus;
import com.winsion.component.media.constants.FileType;
import com.winsion.component.media.entity.LocalRecordEntity;
import com.winsion.component.media.entity.ServerRecordEntity;
import com.winsion.component.task.R;
import com.winsion.component.task.activity.addnote.AddNoteActivity;
import com.winsion.component.task.biz.TaskBiz;
import com.winsion.component.task.constants.RunState;
import com.winsion.component.task.constants.TaskState;
import com.winsion.component.task.constants.TaskType;
import com.winsion.component.task.constants.TrainState;
import com.winsion.component.task.entity.JobEntity;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.winsion.component.media.constants.Intents.Media.MEDIA_FILE;
import static com.winsion.component.task.constants.Intents.OperatorTaskDetail.JOB_ENTITY;

/**
 * Created by 10295 on 2018/1/19.
 * 任务执行人作业详情Activity
 * 协作/命令/任务/网格/预案
 */

public class OperatorTaskDetailActivity extends BaseActivity implements OperatorTaskDetailContract.View {
    private TitleView tvTitle;
    private TextView tvNumber;
    private TextView tvStartStationName;
    private TextView tvTrainStatus;
    private TextView tvEndStationName;
    private TextView tvTrack;
    private TextView tvPlatform;
    private TextView tvPlanArrive;
    private TextView tvPlanDepart;
    private TextView tvWaitRoom;
    private TextView tvRealArrive;
    private TextView tvRealDepart;
    private TextView tvCheckPort;
    private LinearLayout llTrainModule;
    private ImageView divTrainOperation;
    private ImageView ivStatus;
    private ImageView ivTypeIcon;
    private TextView taskTypeName;
    private ImageView ivJobIcon;
    private TextView tvTaskName;
    private TextView tvLastTime;
    private TextView tvLocation;
    private DrawableCenterTextView btnNote;
    private Button btnStatus;
    private DrawableCenterTextView btnBroadcast;
    private TextView tvPlanTime;
    private TextView tvRealTime;
    private LinearLayout llBgColor;
    private ImageView divOperationPublisher;
    private TextView tvPublisherTitle;
    private ListView lvRecordPublisherGrid;
    private TextView tvPerformerTitle;
    private ListView lvRecordPerformer;
    private ImageView divPublisherOperator;
    private RelativeLayout rlOrderModule;
    private ListView lvRecordPublisher;
    private TextView tvMonitorGroupHint;
    private TextView tvMonitorTeam;
    private TextView tvPerformerGroupHint;
    private TextView tvPerformerTeam;
    private TextView tvTitle1;
    private TextView tvTrainNumber;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private EditText etContent;

    private static final int CODE_NOTE = 0;  // 备注
    private static final int CODE_TAKE_PHOTO = 1;    // 拍照
    private static final int CODE_RECORD_VIDEO = 2;  // 录像
    private static final int CODE_RECORD_AUDIO = 3;  // 录音

    private OperatorTaskDetailContract.Presenter mPresenter;

    private JobEntity mJobEntity;   // 上个页面带过来的
    private List<LocalRecordEntity> performerRecordEntities = new ArrayList<>();    // 作业执行人上传附件集合
    private RecordAdapter performerRecordAdapter;   // 作业执行人上传附件列表Adapter
    private List<LocalRecordEntity> publisherRecordEntities = new ArrayList<>();    // 命令/协作发布人上传附件集合
    private RecordAdapter publisherRecordAdapter;   // 命令/协作发布人上传附件列表Adapter(用于命令/协作)

    private File noteFile;
    private File photoFile;
    private File videoFile;
    private File audioFile;

    // 定时刷新器(刷新任务执行时间)
    private Disposable timer;

    @Override
    protected int setContentView() {
        return R.layout.task_activity_operator_task_detail;
    }

    @Override
    protected void start() {
        initView();
        initPresenter();
        initAdapter();
        initData();
        initListener();
        updateLastTime();
    }

    private void initView() {
        tvTitle = findViewById(R.id.tv_title);
        tvNumber = findViewById(R.id.tv_number);
        tvStartStationName = findViewById(R.id.tv_startStationName);
        tvTrainStatus = findViewById(R.id.tv_train_status);
        tvEndStationName = findViewById(R.id.tv_endStationName);
        tvTrack = findViewById(R.id.tv_track);
        tvPlatform = findViewById(R.id.tv_platform);
        tvPlanArrive = findViewById(R.id.tv_plan_arrive);
        tvPlanDepart = findViewById(R.id.tv_plan_depart);
        tvWaitRoom = findViewById(R.id.tv_wait_room);
        tvRealArrive = findViewById(R.id.tv_real_arrive);
        tvRealDepart = findViewById(R.id.tv_real_depart);
        tvCheckPort = findViewById(R.id.tv_check_port);
        llTrainModule = findViewById(R.id.ll_train_module);
        divTrainOperation = findViewById(R.id.div_train_operation);
        ivStatus = findViewById(R.id.iv_status);
        ivTypeIcon = findViewById(R.id.iv_type_icon);
        taskTypeName = findViewById(R.id.task_type_name);
        ivJobIcon = findViewById(R.id.iv_job_icon);
        tvTaskName = findViewById(R.id.tv_task_name);
        tvLastTime = findViewById(R.id.tv_last_time);
        tvLocation = findViewById(R.id.tv_location);
        btnNote = findViewById(R.id.btn_note);
        btnStatus = findViewById(R.id.btn_status);
        btnBroadcast = findViewById(R.id.btn_broadcast);
        tvPlanTime = findViewById(R.id.tv_plan_time);
        tvRealTime = findViewById(R.id.tv_real_time);
        llBgColor = findViewById(R.id.ll_bg_color);
        tvPublisherTitle = findViewById(R.id.tv_publisher_title);
        lvRecordPublisherGrid = findViewById(R.id.lv_record_publisher_grid);
        tvPerformerTitle = findViewById(R.id.tv_performer_title);
        lvRecordPerformer = findViewById(R.id.lv_record_performer);
        divPublisherOperator = findViewById(R.id.div_publisher_operator);
        rlOrderModule = findViewById(R.id.rl_order_module);
        lvRecordPublisher = findViewById(R.id.lv_record_publisher);
        tvMonitorGroupHint = findViewById(R.id.tv_monitor_group_hint);
        tvMonitorTeam = findViewById(R.id.tv_monitor_team);
        tvPerformerGroupHint = findViewById(R.id.tv_performer_group_hint);
        tvPerformerTeam = findViewById(R.id.tv_performer_team);
        tvTitle1 = findViewById(R.id.tv_title1);
        tvTrainNumber = findViewById(R.id.tv_train_number);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        etContent = findViewById(R.id.et_content);
        divOperationPublisher = findViewById(R.id.div_operation_publisher);
    }

    private void initPresenter() {
        mPresenter = new OperatorTaskDetailPresenter(this);
        mJobEntity = (JobEntity) getIntent().getSerializableExtra(JOB_ENTITY);
    }

    private void initAdapter() {
        initPerformerRecordAdapter();
        int taskType = mJobEntity.getTaktype();
        if (taskType == TaskType.COMMAND || taskType == TaskType.COOPERATE || taskType == TaskType.GRID)
            initMonitorRecordAdapter();
    }

    private void initData() {
        initViewModule();

        // 获取作业执行人本地保存的和已经上传到服务器的附件记录
        String jobOperatorsId = mJobEntity.getJoboperatorsid();
        ArrayList<LocalRecordEntity> localFile = ((TaskBiz) mPresenter).getPerformerLocalFile(jobOperatorsId);
        performerRecordEntities.addAll(localFile);
        notifyPerformerRecordDataSetChanged(true);
        mPresenter.getPerformerUploadedFile(jobOperatorsId);

        int taskType = mJobEntity.getTaktype();
        if (taskType == TaskType.COMMAND || taskType == TaskType.COOPERATE || taskType == TaskType.GRID) {
            // 获取命令/协作/网格任务发布人本地保存的和已经上传到服务器的附件记录
            String jobsId = mJobEntity.getJobsid();
            localFile = ((TaskBiz) mPresenter).getPublisherLocalFile(jobsId);
            publisherRecordEntities.addAll(localFile);
            notifyPublisherRecordDataSetChanged(true);
            mPresenter.getPublisherUploadedFile(mJobEntity.getJobsid());
        }
    }

    /**
     * 任务执行人上传的附件数发生改变，刷新界面
     * 判断是否应该显示任务执行人上传附件的标题
     * 1.没有附件隐藏标题  2.有附件显示标题
     *
     * @param needRecalculateHeight 是否需要重新计算ListView高度
     */
    private void notifyPerformerRecordDataSetChanged(boolean needRecalculateHeight) {
        performerRecordAdapter.notifyDataSetChanged();
        if (performerRecordAdapter.getCount() != 0 && mJobEntity.getTaktype() == TaskType.GRID) {
            divPublisherOperator.setVisibility(View.VISIBLE);
            tvPerformerTitle.setVisibility(View.VISIBLE);
        } else {
            divPublisherOperator.setVisibility(View.GONE);
            tvPerformerTitle.setVisibility(View.GONE);
        }

        if (needRecalculateHeight) {
            ViewUtils.setListViewHeightBasedOnChildren(lvRecordPerformer);
        }
    }

    /**
     * 任务发布人上传的附件数发生改变，刷新界面
     * 判断是否应该显示任务执行人上传附件的标题
     * 1.没有附件隐藏标题  2.有附件显示标题
     *
     * @param needRecalculateHeight 是否需要重新计算ListView高度
     */
    private void notifyPublisherRecordDataSetChanged(boolean needRecalculateHeight) {
        publisherRecordAdapter.notifyDataSetChanged();
        if (publisherRecordAdapter.getCount() != 0 && mJobEntity.getTaktype() == TaskType.GRID) {
            divOperationPublisher.setVisibility(View.VISIBLE);
            tvPublisherTitle.setVisibility(View.VISIBLE);
        } else {
            divOperationPublisher.setVisibility(View.GONE);
            tvPublisherTitle.setVisibility(View.GONE);
        }

        if (needRecalculateHeight) {
            ListView listView = mJobEntity.getTaktype() == TaskType.GRID ?
                    lvRecordPublisherGrid : lvRecordPublisher;
            ViewUtils.setListViewHeightBasedOnChildren(listView);
        }
    }

    /**
     * 根据任务类型显示不同的VIEW模块
     */
    private void initViewModule() {
        int taskType = mJobEntity.getTaktype();
        switch (taskType) {
            case TaskType.GRID:
                rlOrderModule.setVisibility(View.GONE);
                llTrainModule.setVisibility(View.GONE);
                divTrainOperation.setVisibility(View.GONE);
                tvPerformerTitle.setVisibility(View.VISIBLE);
                tvPublisherTitle.setVisibility(View.VISIBLE);
                divPublisherOperator.setVisibility(View.VISIBLE);
                break;
            case TaskType.PLAN:
                rlOrderModule.setVisibility(View.GONE);
                llTrainModule.setVisibility(View.GONE);
                divTrainOperation.setVisibility(View.GONE);
                tvPerformerTitle.setVisibility(View.GONE);
                tvPublisherTitle.setVisibility(View.GONE);
                divPublisherOperator.setVisibility(View.GONE);
                break;
            case TaskType.COOPERATE:
            case TaskType.COMMAND:
                rlOrderModule.setVisibility(View.VISIBLE);
                llTrainModule.setVisibility(View.GONE);
                divTrainOperation.setVisibility(View.VISIBLE);
                tvPerformerTitle.setVisibility(View.GONE);
                tvPublisherTitle.setVisibility(View.GONE);
                divPublisherOperator.setVisibility(View.GONE);
                initOrderModuleView(taskType);
                break;
            default:
                rlOrderModule.setVisibility(View.GONE);
                llTrainModule.setVisibility(View.VISIBLE);
                divTrainOperation.setVisibility(View.VISIBLE);
                tvPerformerTitle.setVisibility(View.GONE);
                tvPublisherTitle.setVisibility(View.GONE);
                divPublisherOperator.setVisibility(View.GONE);
                initTrainModuleView();
                break;
        }
        initTaskModuleView();
    }

    private void initListener() {
        tvTitle.setOnBackClickListener(v -> finish());
        addOnClickListeners(R.id.btn_status, R.id.btn_note, R.id.btn_broadcast,
                R.id.btn_take_photo, R.id.btn_video, R.id.btn_record);
    }

    /**
     * 初始化命令/协作模块数据
     *
     * @param taskType 作业类型(命令/协作)
     */
    private void initOrderModuleView(int taskType) {
        tvMonitorGroupHint.setText(taskType == TaskType.COMMAND ? R.string.name_issue_order_group
                : R.string.name_issue_cooperation_group);
        tvPerformerGroupHint.setText(taskType == TaskType.COMMAND ? R.string.name_order_group
                : R.string.name_cooperation_group);
        tvMonitorTeam.setText(mJobEntity.getMonitorteamname());
        tvPerformerTeam.setText(mJobEntity.getOpteamname());
        tvTitle1.setText(mJobEntity.getTaskname());
        String trainNumber = mJobEntity.getTrainnumber();
        trainNumber = TextUtils.isEmpty(trainNumber) ? getString(R.string.value_nothing) : trainNumber;
        tvTrainNumber.setText(trainNumber);
        tvStartTime.setText(mJobEntity.getPlanstarttime());
        tvEndTime.setText(mJobEntity.getPlanendtime());

        String prefix = taskType == TaskType.COMMAND ? getString(R.string.name_command_content) + "  " : getString(R.string.name_cooperation_content) + "  ";
        ForegroundColorSpan gray = new ForegroundColorSpan(0xFF69696D);
        SpannableStringBuilder builder = new SpannableStringBuilder()
                .append(prefix)
                .append(mJobEntity.getWorkcontent());
        builder.setSpan(gray, 0, prefix.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        etContent.setText(builder);
    }

    /**
     * 初始化车次模块数据
     */
    private void initTrainModuleView() {
        String trainNumber = mJobEntity.getTrainnumber();
        int color = 0xFFFFFFFF;
        switch (mJobEntity.getTrainlate()) {
            case RunState.LATE:
                trainNumber = trainNumber + getString(R.string.value_late);
                color = 0xFFE24D46;
                break;
            case RunState.LATE_UNSURE:
                trainNumber = trainNumber + getString(R.string.value_late_unsure);
                color = 0xFFE24D46;
                break;
            case RunState.STOP:
                trainNumber = trainNumber + getString(R.string.value_stop_run);
                color = 0xFFE24D46;
                break;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(trainNumber);
        ForegroundColorSpan statusColor = new ForegroundColorSpan(color);
        builder.setSpan(statusColor, mJobEntity.getTrainnumber().length(), trainNumber.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvNumber.setText(builder);

        int trainStatus = mJobEntity.getTrainstatus();
        switch (trainStatus) {
            case TrainState.IN_PROGRESS:
                tvTrainStatus.setText(R.string.value_check_in_progress);
                break;
            case TrainState.FINISH:
                tvTrainStatus.setText(R.string.value_check_finished);
                break;
            case TrainState.STOP:
                tvTrainStatus.setText(R.string.value_check_stopped);
                break;
            default:
                tvTrainStatus.setText(R.string.default_check_state);
                break;
        }

        String planArrive = ConvertUtils.splitToHM(mJobEntity.getArrivetime());
        tvPlanArrive.setText(planArrive);
        String planDepart = ConvertUtils.splitToHM(mJobEntity.getDeparttime());
        tvPlanDepart.setText(planDepart);
        String realArrive = ConvertUtils.splitToHM(mJobEntity.getRealarrivetime());
        if (equals(realArrive, planArrive) || isEmpty(realArrive)) {
            tvRealArrive.setTextColor(0xFF46DBE2);
        } else {
            tvRealArrive.setTextColor(0xFFE24D46);
        }
        tvRealArrive.setText(realArrive);
        String realDepart = ConvertUtils.splitToHM(mJobEntity.getRealdeparttime());
        if (equals(realDepart, planDepart) || isEmpty(realDepart)) {
            tvRealDepart.setTextColor(0xFF46DBE2);
        } else {
            tvRealDepart.setTextColor(0xFFE24D46);
        }
        tvRealDepart.setText(realDepart);

        String[] areaType = mJobEntity.getAreatypeno().split(",");
        String[] name = mJobEntity.getRunareaname().split(",");
        String[] strings = ((TaskBiz) mPresenter).formatTrainData(areaType, name);
        tvStartStationName.setText(mJobEntity.getSstname());
        tvEndStationName.setText(mJobEntity.getEstname());
        tvTrack.setText(strings[0]);
        tvPlatform.setText(strings[1]);
        tvWaitRoom.setText(strings[2]);
        tvCheckPort.setText(strings[3]);
    }

    /**
     * 初始化任务模块数据
     */
    private void initTaskModuleView() {
        // 设置任务名
        tvTaskName.setText(mJobEntity.getTaskname());
        tvTaskName.setSelected(true);

        // 根据任务类型显示任务类型名称和任务类型图标
        int taskType = mJobEntity.getTaktype();
        switch (taskType) {
            // 任务
            case TaskType.TASK:
                tvTitle.setTitleText(R.string.title_operation_detail);
                taskTypeName.setText(R.string.name_operation_name);
                ivTypeIcon.setVisibility(View.GONE);
                break;
            // 命令
            case TaskType.COMMAND:
                tvTitle.setTitleText(R.string.title_command_detail);
                taskTypeName.setText(R.string.name_command_name);
                ivTypeIcon.setImageResource(R.drawable.task_ic_command);
                ivTypeIcon.setVisibility(View.VISIBLE);
                break;
            // 协作
            case TaskType.COOPERATE:
                tvTitle.setTitleText(R.string.title_cooperation_detail);
                taskTypeName.setText(R.string.name_cooperation_name);
                ivTypeIcon.setImageResource(R.drawable.task_ic_cooperation);
                ivTypeIcon.setVisibility(View.VISIBLE);
                break;
            // 网格
            case TaskType.GRID:
                tvTitle.setTitleText(R.string.title_grid_detail);
                taskTypeName.setText(R.string.value_grid_task);
                ivTypeIcon.setImageResource(R.drawable.task_ic_type_grid);
                ivTypeIcon.setVisibility(View.VISIBLE);
                break;
            // 预案
            case TaskType.PLAN:
                tvTitle.setTitleText(R.string.title_alarm_detail);
                taskTypeName.setText(R.string.value_alarm_task);
                ivTypeIcon.setImageResource(R.drawable.task_ic_type_alarm);
                ivTypeIcon.setVisibility(View.VISIBLE);
                break;
        }

        // 设置位置信息
        tvLocation.setText(mJobEntity.getTaskareaname());

        // 把需要用到的时间转换好
        int lastTime = 0;
        long currentTime = System.currentTimeMillis();

        String planStartTimeStr = mJobEntity.getPlanstarttime();
        String realStartTimeStr = mJobEntity.getRealstarttime();
        long planStartTime = ConvertUtils.parseDate(planStartTimeStr, Formatter.DATE_FORMAT1);
        long realStartTime = ConvertUtils.parseDate(realStartTimeStr, Formatter.DATE_FORMAT1);

        String planEndTimeStr = mJobEntity.getPlanendtime();
        String realEndTimeStr = mJobEntity.getRealendtime();
        long planEndTime = ConvertUtils.parseDate(planEndTimeStr, Formatter.DATE_FORMAT1);
        long realEndTime = ConvertUtils.parseDate(realEndTimeStr, Formatter.DATE_FORMAT1);

        // 设置任务计划和实际开始结束时间
        String splitPlanStartTime = ConvertUtils.splitToHM(planStartTimeStr);
        String splitPlanEndTime = ConvertUtils.splitToHM(planEndTimeStr);
        tvPlanTime.setText(String.format("%s ~ %s", splitPlanStartTime, splitPlanEndTime));
        String splitRealStartTime = ConvertUtils.splitToHM(realStartTimeStr);
        String splitRealEndTime = ConvertUtils.splitToHM(realEndTimeStr);
        tvRealTime.setText(String.format("%s ~ %s", splitRealStartTime, splitRealEndTime));

        boolean isTimeOut = false;
        int workStatus = mJobEntity.getWorkstatus();
        switch (workStatus) {
            case TaskState.NOT_STARTED:
                // 未开始
                // 设置按钮背景和文字
                btnStatus.setBackgroundResource(R.drawable.task_btn_bg_start);
                btnStatus.setText(getString(R.string.btn_click_to_start));
                // 设置持续时间
                tvLastTime.setText(String.format("%s%s", String.valueOf(lastTime), getString(R.string.suffix_minute)));
                // 判断是否超时
                isTimeOut = planStartTime < currentTime;
                if (isTimeOut) {
                    ivStatus.setImageResource(R.drawable.task_ic_timeout_unstart);
                } else {
                    ivStatus.setImageResource(R.drawable.task_ic_not_start);
                }
                break;
            case TaskState.RUN:
                // 进行中
                // 设置按钮背景和文字
                btnStatus.setBackgroundResource(R.drawable.task_btn_bg_finish);
                btnStatus.setText(getString(R.string.btn_click_to_finish));
                // 设置持续时间
                lastTime = (int) ((System.currentTimeMillis() - realStartTime) / (60 * 1000));
                tvLastTime.setText(String.format("%s%s", String.valueOf(lastTime), getString(R.string.suffix_minute)));
                // 判断是否超时
                isTimeOut = planEndTime < currentTime;
                if (isTimeOut) {
                    ImageLoader.loadGif(ivStatus, R.drawable.task_gif_doing_timeout);
                } else {
                    ImageLoader.loadGif(ivStatus, R.drawable.task_gif_doing);
                }
                break;
            case TaskState.DONE:
                // 已完成
                // 设置按钮背景和文字
                btnStatus.setBackgroundResource(R.drawable.task_btn_bg_done);
                btnStatus.setText(getString(R.string.spinner_finished));
                // 设置持续时间
                lastTime = (int) ((realEndTime - realStartTime) / (60 * 1000));
                tvLastTime.setText(String.format("%s%s", String.valueOf(lastTime), getString(R.string.suffix_minute)));
                // 判断是否超时
                isTimeOut = planEndTime < realEndTime;
                if (isTimeOut) {
                    ivStatus.setImageResource(R.drawable.task_ic_timeout_done);
                } else {
                    ivStatus.setImageResource(R.drawable.task_ic_done);
                }
                // 如果是网格任务已完成状态则显示为待验收
                if (taskType == TaskType.GRID) {
                    ivStatus.setImageResource(R.drawable.task_ic_wait_pass);
                    btnStatus.setText(R.string.btn_wait_pass);
                }
                break;
            case TaskState.GRID_NOT_PASS:
                // 验收未通过
                // 设置按钮背景和文字
                btnStatus.setBackgroundResource(R.drawable.task_btn_bg_restart);
                btnStatus.setText(getString(R.string.btn_restart));
                // 设置持续时间
                lastTime = (int) ((realEndTime - realStartTime) / (60 * 1000));
                tvLastTime.setText(String.format("%s%s", String.valueOf(lastTime), getString(R.string.suffix_minute)));
                // 判断是否超时
                isTimeOut = planEndTime < realEndTime;
                if (isTimeOut) {
                    ivStatus.setImageResource(R.drawable.task_ic_not_pass);
                } else {
                    ivStatus.setImageResource(R.drawable.task_ic_not_pass);
                }
                break;
        }

        // 根据是否超时设置任务模块背景色
        if (isTimeOut) {
            llBgColor.setBackgroundResource(R.color.basic_yellow1);
            tvLastTime.setTextColor(getMyColor(R.color.basic_red2));
        } else {
            llBgColor.setBackgroundResource(R.color.basic_gray8);
            tvLastTime.setTextColor(getMyColor(R.color.basic_blue1));
        }
    }

    /**
     * 初始化作业监控人附件列表Adapter
     */
    private void initMonitorRecordAdapter() {
        // 上传附件列表adapter
        publisherRecordAdapter = new RecordAdapter(mContext, publisherRecordEntities);
        // 设置下载文件具体操作
        publisherRecordAdapter.setDownloadPerformer(localRecordEntity -> {
            try {
                String userId = CacheDataSource.getUserId();
                String jobsId = mJobEntity.getJobsid();
                File publisherDir = DirAndFileUtils.getMonitorDir(userId, jobsId);
                ((TaskBiz) mPresenter).downloadFile(localRecordEntity.getServerUri(),
                        publisherDir.getAbsolutePath(), myDownloadListener);
            } catch (IOException e) {
                showToast(R.string.toast_check_sdcard);
            }
        });
        int taskType = mJobEntity.getTaktype();
        ListView listView = taskType == TaskType.COMMAND || taskType == TaskType.COOPERATE ?
                lvRecordPublisher : lvRecordPublisherGrid;
        listView.setAdapter(publisherRecordAdapter);
    }

    /**
     * 初始化作业执行人附件列表Adapter
     */
    private void initPerformerRecordAdapter() {
        // 上传附件列表adapter
        performerRecordAdapter = new RecordAdapter(mContext, performerRecordEntities);
        // 设置上传文件具体操作
        performerRecordAdapter.setUploadPerformer(localRecordEntity ->
                mPresenter.uploadFile(mJobEntity, localRecordEntity.getFile(), myUploadListener));
        // 设置下载文件具体操作
        performerRecordAdapter.setDownloadPerformer(localRecordEntity -> {
            try {
                String userId = CacheDataSource.getUserId();
                String jobOperatorsId = mJobEntity.getJoboperatorsid();
                File performerDir = DirAndFileUtils.getPerformerDir(userId, jobOperatorsId);
                ((TaskBiz) mPresenter).downloadFile(localRecordEntity.getServerUri(),
                        performerDir.getAbsolutePath(), myDownloadListener);
            } catch (IOException e) {
                showToast(R.string.toast_check_sdcard);
            }
        });
        lvRecordPerformer.setAdapter(performerRecordAdapter);
    }

    private MyDownloadListener myDownloadListener = new MyDownloadListener() {
        @Override
        public void downloadProgress(String serverUri, int progress) {
            for (LocalRecordEntity localRecordEntity : performerRecordEntities) {
                if (TextUtils.equals(localRecordEntity.getServerUri(), serverUri)) {
                    localRecordEntity.setFileStatus(FileStatus.DOWNLOADING);
                    localRecordEntity.setProgress(progress);
                    notifyPerformerRecordDataSetChanged(false);
                    break;
                }
            }
            for (LocalRecordEntity localRecordEntity : publisherRecordEntities) {
                if (TextUtils.equals(localRecordEntity.getServerUri(), serverUri)) {
                    localRecordEntity.setFileStatus(FileStatus.DOWNLOADING);
                    localRecordEntity.setProgress(progress);
                    notifyPublisherRecordDataSetChanged(false);
                    break;
                }
            }
        }

        @Override
        public void downloadSuccess(File file, String serverUri) {
            for (LocalRecordEntity localRecordEntity : performerRecordEntities) {
                if (TextUtils.equals(localRecordEntity.getServerUri(), serverUri)) {
                    localRecordEntity.setFileStatus(FileStatus.SYNCHRONIZED);
                    localRecordEntity.setFile(file);
                    notifyPerformerRecordDataSetChanged(false);
                    showToast(R.string.toast_download_success);
                    break;
                }
            }
            for (LocalRecordEntity localRecordEntity : publisherRecordEntities) {
                if (TextUtils.equals(localRecordEntity.getServerUri(), serverUri)) {
                    localRecordEntity.setFileStatus(FileStatus.SYNCHRONIZED);
                    localRecordEntity.setFile(file);
                    notifyPublisherRecordDataSetChanged(false);
                    showToast(R.string.toast_download_success);
                    break;
                }
            }
        }

        @Override
        public void downloadFailed(String serverUri) {
            for (LocalRecordEntity localRecordEntity : performerRecordEntities) {
                if (TextUtils.equals(localRecordEntity.getServerUri(), serverUri)) {
                    localRecordEntity.setFileStatus(FileStatus.NO_DOWNLOAD);
                    notifyPerformerRecordDataSetChanged(false);
                    showToast(R.string.toast_download_failed);
                    break;
                }
            }
            for (LocalRecordEntity localRecordEntity : publisherRecordEntities) {
                if (TextUtils.equals(localRecordEntity.getServerUri(), serverUri)) {
                    localRecordEntity.setFileStatus(FileStatus.NO_DOWNLOAD);
                    notifyPublisherRecordDataSetChanged(false);
                    showToast(R.string.toast_download_failed);
                    break;
                }
            }
        }
    };

    private MyUploadListener myUploadListener = new MyUploadListener() {
        @Override
        public void uploadProgress(File uploadFile, int progress) {
            for (LocalRecordEntity localRecordEntity : performerRecordEntities) {
                if (localRecordEntity.getFile() == uploadFile) {
                    localRecordEntity.setFileStatus(FileStatus.UPLOADING);
                    localRecordEntity.setProgress(progress);
                    notifyPerformerRecordDataSetChanged(false);
                    break;
                }
            }
        }

        @Override
        public void uploadSuccess(File uploadFile) {
            for (LocalRecordEntity localRecordEntity : performerRecordEntities) {
                if (localRecordEntity.getFile() == uploadFile) {
                    localRecordEntity.setFileStatus(FileStatus.SYNCHRONIZED);
                    notifyPerformerRecordDataSetChanged(false);
                    showToast(R.string.toast_upload_success);
                    break;
                }
            }
        }

        @Override
        public void uploadFailed(File uploadFile) {
            for (LocalRecordEntity localRecordEntity : performerRecordEntities) {
                if (localRecordEntity.getFile() == uploadFile) {
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    notifyPerformerRecordDataSetChanged(false);
                    showToast(R.string.toast_upload_failed);
                    break;
                }
            }
        }
    };

    /**
     * 获取执行人上传到服务器的附件记录成功
     *
     * @param serverRecordFileList 上传到服务器的附件列表
     */
    @Override
    public void onPerformerUploadFileGetSuccess(List<ServerRecordEntity> serverRecordFileList) {
        boolean needRecalculateHeight = false;
        for (ServerRecordEntity entity : serverRecordFileList) {
            String[] split = entity.getFilepath().split("/");
            String fileName = split[split.length - 1];
            int position = checkFileExist(fileName, performerRecordEntities);
            if (position == -1) {
                // 本地没有
                LocalRecordEntity localRecordEntity = new LocalRecordEntity();
                localRecordEntity.setFileType(Integer.valueOf(entity.getType()));
                localRecordEntity.setFileStatus(FileStatus.NO_DOWNLOAD);
                localRecordEntity.setServerUri(entity.getFilepath());
                localRecordEntity.setFileName(entity.getFilepath().split("/")[split.length - 1]);
                performerRecordEntities.add(localRecordEntity);
                needRecalculateHeight = true;
            } else {
                // 本地存在
                LocalRecordEntity localRecordEntity = performerRecordEntities.get(position);
                localRecordEntity.setFileStatus(FileStatus.SYNCHRONIZED);
            }
        }
        notifyPerformerRecordDataSetChanged(needRecalculateHeight);

        // 自动上传没有上传成功的文件,自动下载没有下载成功的文件
        for (LocalRecordEntity performerRecordEntity : performerRecordEntities) {
            // 备注不上传
            if (performerRecordEntity.getFileType() == FileType.TEXT) {
                continue;
            }
            if (performerRecordEntity.getFileStatus() == FileStatus.NO_UPLOAD) {
                mPresenter.uploadFile(mJobEntity, performerRecordEntity.getFile(), myUploadListener);
            }
            if (performerRecordEntity.getFileStatus() == FileStatus.NO_DOWNLOAD) {
                try {
                    String userId = CacheDataSource.getUserId();
                    String jobOperatorsId = mJobEntity.getJoboperatorsid();
                    File performerDir = DirAndFileUtils.getPerformerDir(userId, jobOperatorsId);
                    ((TaskBiz) mPresenter).downloadFile(performerRecordEntity.getServerUri(),
                            performerDir.getAbsolutePath(), myDownloadListener);
                } catch (IOException e) {
                    showToast(R.string.toast_check_sdcard);
                }
            }
        }
    }

    /**
     * 获取命令/协作/网格任务发布人上传到服务器的附件记录成功
     *
     * @param serverRecordFileList 上传到服务器的附件列表
     */
    @Override
    public void onPublisherUploadFileGetSuccess(List<ServerRecordEntity> serverRecordFileList) {
        boolean needRecalculateHeight = false;
        for (ServerRecordEntity entity : serverRecordFileList) {
            String[] split = entity.getFilepath().split("/");
            String fileName = split[split.length - 1];
            int position = checkFileExist(fileName, publisherRecordEntities);
            if (position == -1) {
                // 本地没有
                LocalRecordEntity localRecordEntity = new LocalRecordEntity();
                localRecordEntity.setFileType(Integer.valueOf(entity.getType()));
                localRecordEntity.setFileStatus(FileStatus.NO_DOWNLOAD);
                localRecordEntity.setServerUri(entity.getFilepath());
                localRecordEntity.setFileName(entity.getFilepath().split("/")[split.length - 1]);
                publisherRecordEntities.add(localRecordEntity);
                needRecalculateHeight = true;
            } else {
                // 本地存在
                LocalRecordEntity localRecordEntity = publisherRecordEntities.get(position);
                localRecordEntity.setFileStatus(FileStatus.SYNCHRONIZED);
            }
        }
        notifyPublisherRecordDataSetChanged(needRecalculateHeight);

        // 自动下载没有下载成功的文件
        for (LocalRecordEntity publisherRecordEntity : publisherRecordEntities) {
            if (publisherRecordEntity.getFileStatus() == FileStatus.NO_DOWNLOAD) {
                try {
                    String userId = CacheDataSource.getUserId();
                    String jobsId = mJobEntity.getJobsid();
                    File publisherDir = DirAndFileUtils.getMonitorDir(userId, jobsId);
                    ((TaskBiz) mPresenter).downloadFile(publisherRecordEntity.getServerUri(),
                            publisherDir.getAbsolutePath(), myDownloadListener);
                } catch (IOException e) {
                    showToast(R.string.toast_check_sdcard);
                }
            }
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 不存在返回-1，存在返回该文件在集合中的position
     */
    private int checkFileExist(String fileName, List<LocalRecordEntity> recordEntities) {
        int position = -1;
        for (int i = 0; i < recordEntities.size(); i++) {
            if (recordEntities.get(i).getFileName().equals(fileName)) {
                position = i;
                break;
            }
        }
        return position;
    }

    @Override
    public void onClick(View view) {
        String userId = CacheDataSource.getUserId();
        String jobOperatorsId = mJobEntity.getJoboperatorsid();
        Intent intent;
        int id = view.getId();
        if (id == R.id.btn_status) {
            // 更改任务状态按钮点击事件
            int workStatus = mJobEntity.getWorkstatus();
            if (workStatus == TaskState.RUN || workStatus == TaskState.NOT_STARTED || workStatus == TaskState.GRID_NOT_PASS) {
                showDialog(workStatus == TaskState.RUN, view);
            }
        } else if (id == R.id.btn_broadcast) {
            showToast("暂未开放");
        } else if (id == R.id.btn_note) {
            try {
                noteFile = MediaBiz.getMediaFile(DirAndFileUtils.getPerformerDir(userId, jobOperatorsId), FileType.TEXT);
                intent = new Intent(mContext, AddNoteActivity.class);
                intent.putExtra(MEDIA_FILE, noteFile);
                startActivityForResult(intent, CODE_NOTE);
            } catch (IOException e) {
                showToast(R.string.toast_check_sdcard);
            }
        } else if (id == R.id.btn_take_photo) {
            try {
                photoFile = MediaBiz.getMediaFile(DirAndFileUtils.getPerformerDir(userId, jobOperatorsId), FileType.PICTURE);
                intent = new Intent(mContext, TakePhotoActivity.class);
                intent.putExtra(MEDIA_FILE, photoFile);
                startActivityForResult(intent, CODE_TAKE_PHOTO);
            } catch (IOException e) {
                showToast(R.string.toast_check_sdcard);
            }
        } else if (id == R.id.btn_video) {
            try {
                videoFile = MediaBiz.getMediaFile(DirAndFileUtils.getPerformerDir(userId, jobOperatorsId), FileType.VIDEO);
                intent = new Intent(mContext, RecordVideoActivity.class);
                intent.putExtra(MEDIA_FILE, videoFile);
                startActivityForResult(intent, CODE_RECORD_VIDEO);
            } catch (IOException e) {
                showToast(R.string.toast_check_sdcard);
            }
        } else if (id == R.id.btn_record) {
            try {
                audioFile = MediaBiz.getMediaFile(DirAndFileUtils.getPerformerDir(userId, jobOperatorsId), FileType.AUDIO);
                intent = new Intent(mContext, RecordAudioActivity.class);
                intent.putExtra(MEDIA_FILE, audioFile);
                startActivityForResult(intent, CODE_RECORD_AUDIO);
            } catch (IOException e) {
                showToast(R.string.toast_check_sdcard);
            }
        }
    }

    /**
     * 更改任务状态前弹出对话框
     *
     * @param isRunning 当前任务是否正在进行中
     * @param btn       按钮Button
     */
    private void showDialog(boolean isRunning, View btn) {
        new CustomDialog.NormalBuilder(mContext)
                .setMessage(getString(isRunning ? R.string.dialog_sure_to_finish : R.string.dialog_sure_to_start))
                .setPositiveButton((dialog, which) -> changeStatus(isRunning, btn))
                .show();
    }

    private void changeStatus(boolean isRunning, View btn) {
        btn.setEnabled(false);
        int opeType = isRunning ? OpeType.COMPLETE : OpeType.BEGIN;
        ((TaskBiz) mPresenter).changeJobStatus(mContext, mJobEntity, opeType, new StateListener() {
            @Override
            public void onSuccess() {
                btn.setEnabled(true);
                String currentTime = ConvertUtils.formatDate(System.currentTimeMillis(), Formatter.DATE_FORMAT1);
                if (isRunning) {
                    mJobEntity.setWorkstatus(TaskState.DONE);
                    mJobEntity.setRealendtime(currentTime);
                } else {
                    mJobEntity.setWorkstatus(TaskState.RUN);
                    mJobEntity.setRealstarttime(currentTime);
                }
                // 状态发生改变，重新初始化任务模块
                initTaskModuleView();
                // 通知上个界面(OperatorTaskListFragment)同步数据
                EventBus.getDefault().post(mJobEntity);
            }

            @Override
            public void onFailed() {
                btn.setEnabled(true);
                showToast(R.string.toast_change_state_failed);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            LocalRecordEntity localRecordEntity;
            switch (requestCode) {
                case CODE_TAKE_PHOTO:
                    // 拍照成功
                    localRecordEntity = new LocalRecordEntity();
                    localRecordEntity.setFileType(FileType.PICTURE);
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    localRecordEntity.setFile(photoFile);
                    localRecordEntity.setFileName(photoFile.getName());
                    performerRecordEntities.add(localRecordEntity);
                    notifyPerformerRecordDataSetChanged(true);
                    // 上传
                    mPresenter.uploadFile(mJobEntity, photoFile, myUploadListener);
                    break;
                case CODE_RECORD_VIDEO:
                    // 录像成功
                    localRecordEntity = new LocalRecordEntity();
                    localRecordEntity.setFileType(FileType.VIDEO);
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    localRecordEntity.setFile(videoFile);
                    localRecordEntity.setFileName(videoFile.getName());
                    performerRecordEntities.add(localRecordEntity);
                    notifyPerformerRecordDataSetChanged(true);
                    // 上传
                    mPresenter.uploadFile(mJobEntity, videoFile, myUploadListener);
                    break;
                case CODE_RECORD_AUDIO:
                    // 录音成功
                    localRecordEntity = new LocalRecordEntity();
                    localRecordEntity.setFileType(FileType.AUDIO);
                    localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                    localRecordEntity.setFile(audioFile);
                    localRecordEntity.setFileName(audioFile.getName());
                    performerRecordEntities.add(localRecordEntity);
                    notifyPerformerRecordDataSetChanged(true);
                    // 上传
                    mPresenter.uploadFile(mJobEntity, audioFile, myUploadListener);
                    break;
                case CODE_NOTE:
                    // 添加备注成功
                    if (performerRecordEntities.size() == 0 || performerRecordEntities.get(0).getFileType() != FileType.TEXT) {
                        localRecordEntity = new LocalRecordEntity();
                        localRecordEntity.setFileType(FileType.TEXT);
                        localRecordEntity.setFileStatus(FileStatus.NO_UPLOAD);
                        localRecordEntity.setFile(noteFile);
                        localRecordEntity.setFileName(noteFile.getName());
                        performerRecordEntities.add(0, localRecordEntity);
                    }
                    // 如果备注内容为空不显示
                    if (TextUtils.isEmpty(FileUtils.readFile2String(noteFile, "UTF-8"))) {
                        performerRecordEntities.remove(0);
                    }
                    notifyPerformerRecordDataSetChanged(true);
                    break;
            }
        }
    }

    /**
     * 定时更新任务持续时间
     */
    private void updateLastTime() {
        timer = Observable.interval(30, 30, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Long aLong) -> initTaskModuleView());
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.exit();
        timer.dispose();
    }
}
