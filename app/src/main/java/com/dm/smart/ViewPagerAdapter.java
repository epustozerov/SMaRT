package com.dm.smart;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;


public class ViewPagerAdapter extends FragmentStateAdapter {

    public final ArrayList<Bundle> bundles = new ArrayList<>();
    public final ArrayList<Integer> fragmentIds = new ArrayList<>();
    final FragmentManager fragmentManager;

    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        CanvasFragment cf = new CanvasFragment(bundles.get(position));
        int id = cf.getId();
        fragmentIds.add(id);
        return cf;
    }

    @Override
    public int getItemCount() {
        return bundles.size();
    }

    public void add(Bundle b) {
        bundles.add(b);
    }

}
