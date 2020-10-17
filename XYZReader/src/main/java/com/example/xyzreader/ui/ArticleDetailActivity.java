package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.SharedElementCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.databinding.ActivityArticleDetailBinding;
import com.example.xyzreader.databinding.FragmentArticleDetailBinding;
import java.util.List;
import java.util.Map;


/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_ID = "id";

    private Cursor mCursor;
    public long mStartId;
    private int mPosition;
    public long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;
    private ActivityArticleDetailBinding binding;
    public static int SelectedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityCompat.postponeEnterTransition(this);
            ActivityCompat.setEnterSharedElementCallback(this, EnterTransitionCallback);

        }
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        binding = ActivityArticleDetailBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        LoaderManager.getInstance(this).initLoader(0, null, this);
        FragmentManager fragmentManager = getSupportFragmentManager();


        fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
        mPagerAdapter = new MyPagerAdapter(fragmentManager);
        mPager = binding.pager;
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);

                mPosition = position;
                updateUpButtonPosition();
            }
        });
        mUpButtonContainer = binding.upContainer;

        mUpButton = binding.actionUp;
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSupportNavigateUp();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }

        if (savedInstanceState == null) {
        //    if (getIntent() != null && getIntent().getData() != null) {
//                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            if (getIntent() != null){
                mStartId = getIntent().getLongExtra(EXTRA_ID,0);
                mPosition = getIntent().getIntExtra(EXTRA_POSITION,0);
                mSelectedItemId = mStartId;

            }
        }
        mPager.addOnPageChangeListener(PageChangeListener);
        mPager.getViewTreeObserver().addOnGlobalLayoutListener(PagerLayoutListener);

    }

    private ViewPager.OnPageChangeListener PageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            SelectedIndex = position;

        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };


    private final SharedElementCallback EnterTransitionCallback = new SharedElementCallback() {
        @SuppressLint("NewApi")
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            View view = null;

            if (mPager.getChildCount() > 0) {
                view = mPagerAdapter.getCurrentView(mPager);
                FragmentArticleDetailBinding fragmentArticleDetailBinding = FragmentArticleDetailBinding.bind(view);
                view = fragmentArticleDetailBinding.photo;
            }

            if (view != null) {
                sharedElements.put(names.get(0), view);
            }
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem( mPosition);
        // Select the start ID
/*        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                   // final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }    */


    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private ViewTreeObserver.OnGlobalLayoutListener PagerLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mPager.getViewTreeObserver().removeOnGlobalLayoutListener(PagerLayoutListener);
            ActivityCompat.startPostponedEnterTransition(ArticleDetailActivity.this);
        }
    };

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }

    @Override
    public void onBackPressed() {
    supportFinishAfterTransition();
      //  finish();
    }


    // --------------------------------- MyPagerAdapter start --------------------------------
    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
               // mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            container.setTag(R.id.index,position);
            return super.instantiateItem(container, position);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

        public View getCurrentView(ViewPager pager) {
            for (int i=0; i<pager.getChildCount(); i++) {
                if (pager.getChildAt(i).getTag(R.id.index).equals(pager.getCurrentItem())) {
                    return pager.getChildAt(i);
                }
            }

            return null;
        }
    }
    // --------------------------------- MyPagerAdapter end --------------------------------

}
