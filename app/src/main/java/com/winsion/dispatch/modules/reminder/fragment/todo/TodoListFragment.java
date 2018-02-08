package com.winsion.dispatch.modules.reminder.fragment.todo;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.winsion.dispatch.R;
import com.winsion.dispatch.base.BaseFragment;
import com.winsion.dispatch.main.activity.MainActivity;
import com.winsion.dispatch.modules.reminder.ReminderRootFragment;
import com.winsion.dispatch.modules.reminder.activity.todo.AddTodoActivity;
import com.winsion.dispatch.modules.reminder.adapter.TodoAdapter;
import com.winsion.dispatch.modules.reminder.entity.TodoEntity;
import com.winsion.dispatch.view.SpinnerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;
import static com.winsion.dispatch.modules.reminder.constants.Intents.Todo.TODO_ID;

/**
 * Created by wyl on 2017/6/2
 */
public class TodoListFragment extends BaseFragment implements TodoListContract.View, AdapterView.OnItemClickListener {
    @BindView(R.id.sv_spinner)
    SpinnerView svSpinner;
    @BindView(R.id.lv_reminders_list)
    ListView lvRemindersList;
    @BindView(R.id.iv_shade)
    ImageView ivShade;

    /**
     * 状态筛选-全部
     */
    private static final int STATE_ALL = 0;
    /**
     * 状态筛选-未完成
     */
    private static final int STATE_UNFINISHED = 1;
    /**
     * 状态筛选-已完成
     */
    private static final int STATE_FINISHED = 2;

    private List<TodoEntity> listData = new ArrayList<>();
    private TodoAdapter mAdapter;
    private TodoListContract.Presenter mPresenter;
    private int currentState = STATE_ALL;   // 当前筛选状态，默认全部

    @SuppressLint("InflateParams")
    @Override
    protected View setContentView() {
        return getLayoutInflater().inflate(R.layout.fragment_todo, null);
    }

    @Override
    protected void init() {
        initPresenter();
        initSpinner();
        initAdapter();
        initListener();
        recoverAlarm();
        initData(true);
    }

    private void initPresenter() {
        mPresenter = new TodoListPresenter(this);
        mPresenter.start();
    }

    private void initSpinner() {
        List<String> statusList = Arrays.asList(getResources().getStringArray(R.array.todoStatusArray));
        svSpinner.setFirstOptionData(statusList);
        svSpinner.setFirstOptionItemClickListener(position -> {
            currentState = position;
            initData(false);
        });

        // 根据Spinner显示状态显隐透明背景
        svSpinner.setPopupDisplayChangeListener(status -> {
            switch (status) {
                case SpinnerView.POPUP_SHOW:
                    ivShade.setVisibility(View.VISIBLE);
                    break;
                case SpinnerView.POPUP_HIDE:
                    ivShade.setVisibility(View.GONE);
                    break;
            }
        });
    }

    private void initAdapter() {
        mAdapter = new TodoAdapter(mContext, listData);
        lvRemindersList.setAdapter(mAdapter);
    }

    private void initListener() {
        EventBus.getDefault().register(this);
        mAdapter.setDeleteBtnClickListener(todoEntity -> new AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.dialog_sure_to_delete))
                .setNegativeButton(getString(R.string.btn_cancel), (DialogInterface dialog, int which) -> dialog.cancel())
                .setPositiveButton(getString(R.string.btn_confirm), (DialogInterface dialog, int which) -> mPresenter.deleteTodo(todoEntity))
                .show());
        lvRemindersList.setOnItemClickListener(this);
    }

    /**
     * 获取代办事项数据
     *
     * @param isUpdateBadge 是否需要更新角标
     */
    private void initData(boolean isUpdateBadge) {
        listData.clear();
        switch (currentState) {
            case STATE_ALL:
                listData.addAll(mPresenter.queryTodo(false));
                listData.addAll(mPresenter.queryTodo(true));
                break;
            case STATE_UNFINISHED:
                listData.addAll(mPresenter.queryTodo(false));
                break;
            case STATE_FINISHED:
                listData.addAll(mPresenter.queryTodo(true));
                break;
        }
        mAdapter.notifyDataSetChanged();

        if (isUpdateBadge) {
            // 更新角标
            int unreadCount = 0;
            for (TodoEntity todoEntity : mPresenter.queryTodo(false)) {
                if (todoEntity.getPlanDate() < System.currentTimeMillis()) {
                    unreadCount++;
                }
            }
            ReminderRootFragment parentFragment = (ReminderRootFragment) getParentFragment();
            parentFragment.getBrbView(1).showNumber(unreadCount);
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.notifyUnreadTodoCountChanged(unreadCount);
        }
    }

    /**
     * 避免退出程序后之前设置的闹钟不会提醒，需要将之前的提醒进行重新设置
     */
    private void recoverAlarm() {
        mPresenter.recoverAlarm();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!listData.get(position).getFinished()) {
            Intent intent = new Intent(mContext, AddTodoActivity.class);
            intent.putExtra(TODO_ID, listData.get(position).getId());
            startActivityForResult(intent);
        }
    }

    @OnClick(R.id.btn_add)
    public void onViewClicked() {
        startActivityForResult(AddTodoActivity.class);
    }

    /**
     * 已读了一条代办事项，该事件由TodoReceiver发出
     *
     * @param event 提醒置为已读状态，刷新界面
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TodoEntity event) {
        initData(true);
    }

    /**
     * 更新了一条代办事项
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            initData(false);
        }
    }

    /**
     * 删除了一条代办事项
     */
    @Override
    public void notifyLocalDataChange() {
        initData(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPresenter.exit();
        EventBus.getDefault().unregister(this);
    }
}