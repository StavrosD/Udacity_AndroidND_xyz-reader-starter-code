package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.app.SharedElementCallback;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.databinding.FragmentArticleDetailBinding;
import com.example.xyzreader.databinding.ParagraphBinding;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textview.MaterialTextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> , AppBarLayout.OnOffsetChangedListener {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_POSITION = "item_position";

    private Cursor mCursor;
    private long mItemId;
    private long mItemPosition;
    private View mRootView;

    private NetworkImageView mPhotoView;
    private RecyclerView mRecyclerView;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());
    // Use default locale format
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private FragmentArticleDetailBinding binding;
    private ParagraphsAdapter paragraphsAdapter;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityCompat.setEnterSharedElementCallback(getActivity(), EnterTransitionCallback);

        }
        getActivity().getWindow().setSharedElementExitTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.shared_image_transition));
        Transition transition = TransitionInflater.from(this.getContext()).inflateTransition(R.transition.shared_image_transition);
        setSharedElementEnterTransition(transition);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            mItemPosition = getArguments().getLong(ARG_ITEM_POSITION);
        }
        setHasOptionsMenu(true);
    }

    private final SharedElementCallback EnterTransitionCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
               // sharedElements.put(names.get(0), binding.photo);

        }
    };

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setExitTransition(TransitionInflater.from(getContext()).inflateTransition(R.transition.recyclerview_exit_transition));
        binding = FragmentArticleDetailBinding.inflate(inflater, container, false);
        mRootView = binding.getRoot();


        mPhotoView = binding.photo;
        mRecyclerView = binding.recyclerView;
        binding.shareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Share book")
                        .getIntent(), getString(R.string.action_share)));
            }
        });



        bindViews();
/*
        // if the current fragment displays the selected item, start the transition after it is loaded
        if (mItemId == ((ArticleDetailActivity)getActivity()).mSelectedItemId) {
            mRootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserveimager.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                    getActivity().startPostponedEnterTransition();
                    return true;
                }
            });
        }
*/

        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPhotoView.setTransitionName(getString(R.string.shared_image_transition, mItemId,mItemPosition));
      //      ActivityCompat.startPostponedEnterTransition(getActivity());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.fragmentAppBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.fragmentAppBarLayout.removeOnOffsetChangedListener(this);
    }

    private boolean isHideToolbarView = false;
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(verticalOffset) / (float) maxScroll;

        if (percentage == 1) {
            binding.fragmentToolbarHeaderView.getRoot().setVisibility(View.VISIBLE);

        } else {
            binding.fragmentToolbarHeaderView.getRoot().setVisibility(View.GONE);
            if (percentage >= 0.7f && isHideToolbarView) {
                binding.floatHeaderView.articleByline.setVisibility(View.INVISIBLE);
                isHideToolbarView = !isHideToolbarView;
            } else if (percentage < 0.7f && !isHideToolbarView) {
                binding.floatHeaderView.articleByline.setVisibility(View.VISIBLE);
                isHideToolbarView = !isHideToolbarView;
            }
        }
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

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        MaterialTextView titleView = binding.floatHeaderView.articleTitle;
        MaterialTextView fixedTitleView = binding.fragmentToolbarHeaderView.articleTitle;
        MaterialTextView bylineView = binding.floatHeaderView.articleByline;
        MaterialTextView fixedByLineBiew = binding.fragmentToolbarHeaderView.articleByline;
        fixedByLineBiew.setVisibility(View.INVISIBLE);
        bylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            fixedTitleView.setText(titleView.getText());

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }
            paragraphsAdapter = new ParagraphsAdapter(mCursor.getString(ArticleLoader.Query.BODY));
            paragraphsAdapter.setHasStableIds(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(paragraphsAdapter);


            mPhotoView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.PHOTO_URL),
                    ImageLoaderHelper.getInstance(getActivity()).getImageLoader());
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.from(bitmap).maximumColorCount(6).generate();
                                binding.collapsingToolbar.setStatusBarScrimColor(p.getDominantColor(getResources().getColor( R.color.theme_primary)));
                                binding.collapsingToolbar.setContentScrimColor(p.getDominantColor(getResources().getColor( R.color.theme_primary)));
                                binding.collapsingToolbar.setBackgroundColor(p.getDominantColor(getResources().getColor( R.color.theme_primary)));

                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            fixedTitleView.setText("N/A");
            bylineView.setText("N/A" );

        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (!isAdded()) {
            if (data != null) {
                data.close();
            }
            return;
        }

        mCursor = data;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }


    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    // ------------------------- Adapter -----------------------------------------------------------

    private class ParagraphsAdapter extends RecyclerView.Adapter<ParagraphViewHolder> {
        private String[] paragraphs;

        public ParagraphsAdapter(String bodyText) {
            paragraphs = bodyText.split("\\r\\n\\r\\n");
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public ParagraphViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view =  getLayoutInflater().inflate(R.layout.paragraph, parent, false);
            final ParagraphViewHolder vh = new ParagraphViewHolder(view);
            return vh;
        }

        @Override
        public void onBindViewHolder(ParagraphViewHolder holder, int position) {
            holder.textView.setText(paragraphs[position].replace("\r\n"," "));
        }

        @Override
        public int getItemCount() {
            return paragraphs.length;
        }
    }

    public static class ParagraphViewHolder extends RecyclerView.ViewHolder {
        public MaterialTextView textView;

        public ParagraphViewHolder(View view) {
            super(view);
            ParagraphBinding binding = ParagraphBinding.bind(view);
            textView = binding.paragraph;
        }
    }
    // ---------------------------------- Adapter end ----------------------------------------------




}
