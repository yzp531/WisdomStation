package com.winsion.component.contact.fragment;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;

import com.winsion.component.basic.PlaceHolderFragment;
import com.winsion.component.basic.base.BaseFragment;
import com.winsion.component.basic.view.MyIndicator;
import com.winsion.component.basic.view.NoScrollViewPager;
import com.winsion.component.contact.R;
import com.winsion.component.contact.activity.contacts.ContactsFragment;

/**
 * Created by 10295 on 2017/12/10 0010.
 * 联系人一级界面
 */

public class ContactRootFragment extends BaseFragment {
    private NoScrollViewPager vpContent;
    private MyIndicator mIndicator;

    private final Fragment[] mFragments = {new ContactsFragment(), new PlaceHolderFragment(), new PlaceHolderFragment()};
    private final int[] mTitles = {R.string.tab_contacts, R.string.tab_team_group, R.string.tab_contact_group};

    @SuppressLint("InflateParams")
    @Override
    public View setContentView() {
        return LayoutInflater.from(mContext).inflate(R.layout.basic_fragment_three_pager, null);
    }

    @Override
    protected void init() {
        initView();
        initAdapter();
    }

    private void initView() {
        vpContent = findViewById(R.id.vp_content);
        mIndicator = findViewById(R.id.mi_container);
    }

    private void initAdapter() {
        vpContent.setAdapter(new MyPagerAdapter(getChildFragmentManager()));
        mIndicator.setViewPager(vpContent);
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments[position];
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(mTitles[position]);
        }
    }
}