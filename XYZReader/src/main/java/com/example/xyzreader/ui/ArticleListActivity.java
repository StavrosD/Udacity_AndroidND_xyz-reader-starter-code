package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.Transition;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.toolbox.NetworkImageView;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.databinding.ActivityArticleListBinding;
import com.example.xyzreader.databinding.ListItemArticleBinding;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener  {
    private final String SAVED_TAG = "saved_tag";
    private static final String TAG = ArticleListActivity.class.toString();
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private ActivityArticleListBinding binding;
    private static final String KEY_CURRENT_POSITION = "com.example.xyzreader.key.currentPosition";
    private int currentPosition;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityCompat.setExitSharedElementCallback(this, ExitTransitionCallback);
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            getWindow().setSharedElementEnterTransition(new ChangeImageTransform());
        }
        super.onCreate(savedInstanceState);

        binding = ActivityArticleListBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        mToolbar = binding.toolbar;
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("");

        binding.mainAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }

            }
        });
        mSwipeRefreshLayout = binding.swipeRefreshLayout;

        mRecyclerView = binding.recyclerView;
        binding.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                                                       @Override
                                                       public boolean onMenuItemClick(MenuItem menuItem) {
                                                           // I use switch instead of if to make it easier to add more menu options later.
                                                           switch (menuItem.getItemId()){
                                                               case R.id.refresh:refresh();
                                                               default: return false;
                                                           }
                                                       }
                                                   }

        );
        LoaderManager.getInstance(this).initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();

        }

        if (!isNetworkAvailable(getApplication())) {
            Snackbar.make(binding.getRoot(), R.string.error_text_label, Snackbar.LENGTH_LONG).show();
        }

    }

    private final SharedElementCallback ExitTransitionCallback = new SharedElementCallback() {
        @SuppressLint("NewApi")
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {

                // Locate the ViewHolder for the clicked position.
                RecyclerView.ViewHolder selectedViewHolder = mRecyclerView.findViewHolderForAdapterPosition(currentPosition);
                if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
                    return;
                }

                // Map the first shared element name to the child ImageView.
                sharedElements.put(names.get(0),selectedViewHolder.itemView.findViewById(R.id.thumbnail));
            }

    };

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    private boolean mIsRefreshing = false;
    private BroadcastReceiver mRefreshingReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        if (mRefreshingReceiver == null) {
            mIsRefreshing = true;
             mRefreshingReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                        mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                        updateRefreshingUI();
                    }
                }
            };
            registerReceiver(mRefreshingReceiver,
                    new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));

            updateRefreshingUI();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRefreshingReceiver != null) {
            try {
                unregisterReceiver(mRefreshingReceiver);
            } catch (Exception ex) {
                Log.e("mRefreshingReceiver", ex.getMessage());
            }

            mRefreshingReceiver = null;
        }
    }



    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRecyclerView.getLayoutManager() != null) {
            Parcelable recyclerviewData = mRecyclerView.getLayoutManager().onSaveInstanceState();
            if (recyclerviewData != null) {
                outState.putParcelable(SAVED_TAG, recyclerviewData);
            }
        }
        outState.putInt(KEY_CURRENT_POSITION, currentPosition);
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        mRecyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(SAVED_TAG));
        currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
    }

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        mSwipeRefreshLayout.setEnabled(verticalOffset == 0);
    }


    // ------------------------- Adapter -----------------------------------------------------------

    private class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public RecyclerViewAdapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    currentPosition = vh.getAdapterPosition();
                    ListItemArticleBinding binding = ListItemArticleBinding.bind(view);
                    Intent intent = new Intent(Intent.ACTION_VIEW, ItemsContract.Items.buildItemUri(getItemId(currentPosition)));
                    intent.putExtra(ArticleDetailActivity.EXTRA_POSITION, currentPosition);
                    //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(ArticleListActivity.this, binding.thumbnail, binding.thumbnail.getTransitionName());
                        startActivity( intent,options.toBundle());
                    } else {
                        startActivity( intent);
                    }
                }
            });
            return vh;

        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.thumbnailView.setTransitionName(getString(R.string.shared_image_transition, getItemId(position),position));   // set a unique Transition Name, using the item ID. I prepend a string to be sure that there will not be any other elements with the same transition name in case I use this code in another larger project
            }
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public NetworkImageView thumbnailView;
        public MaterialTextView titleView;
        public MaterialTextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            ListItemArticleBinding binding = ListItemArticleBinding.bind(view);
            thumbnailView = binding.thumbnail;
            titleView = binding.articleTitle;
            subtitleView = binding.articleSubtitle;
        }
    }
    // ---------------------------------- Adapter end ----------------------------------------------



    // credits: https://stackoverflow.com/questions/57277759/getactivenetworkinfo-is-deprecated-in-api-29
    private Boolean isNetworkAvailable(Application application) {
        ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }

}
