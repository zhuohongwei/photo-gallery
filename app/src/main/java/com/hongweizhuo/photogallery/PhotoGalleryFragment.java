package com.hongweizhuo.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static String TAG = "PhotoGalleryFragment";

    private RecyclerView mRecyclerView;
    private PhotoAdapter mPhotoAdapter;
    private SearchView mSearchView;
    private ProgressBar mProgressBar;

    private ThumbnailDownloader<PhotoViewHolder> mThumbnailDownloader;

    private int mPage = 1;

    private boolean mIsLoading = false;
    private FetchPhotosTask mFetchTask = null;

    public PhotoGalleryFragment() {
    }

    public static PhotoGalleryFragment newInstance() {
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        mThumbnailDownloader = new ThumbnailDownloader<>(new Handler());

        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoViewHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoViewHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }

        });

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        Log.i(TAG, "Background thread started...");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed...");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.loading_view_photo_gallery);
        mProgressBar.setVisibility(View.GONE);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_photo_gallery);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));


        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    if (!mIsLoading) {
                        updatePhotos();
                    }
                }

            }
        });

        setupAdapter();

        updatePhotos();

        return view;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        mSearchView = (SearchView) searchItem.getActionView();

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                mSearchView.setQuery(query, false);
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                mPage = 1;
                mPhotoAdapter.setPhotos(new ArrayList<Photo>());
                mPhotoAdapter.notifyDataSetChanged();

                QueryPreferences.setStoredQuery(getActivity(), query);

                updatePhotos();

                mSearchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:

                mPage = 1;
                mPhotoAdapter.setPhotos(new ArrayList<Photo>());
                mPhotoAdapter.notifyDataSetChanged();

                QueryPreferences.setStoredQuery(getActivity(), null);
                mSearchView.setQuery(null, false);

                updatePhotos();

                mSearchView.clearFocus();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoAdapter = new PhotoAdapter(new ArrayList<Photo>());
            mRecyclerView.setAdapter(mPhotoAdapter);
        }
    }

    private void updatePhotos() {

        if (mFetchTask != null && mFetchTask.getStatus() != AsyncTask.Status.FINISHED) {
            mFetchTask.cancel(true);
        }

        String query = QueryPreferences.getStoredQuery(getActivity());
        mFetchTask = new FetchPhotosTask(query);
        mFetchTask.execute();

    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> {

        private List<Photo> mPhotos;

        public PhotoAdapter(List<Photo> photos) {
            mPhotos = photos;
        }

        public void setPhotos(List<Photo> photos) {
            mPhotos = photos;
        }

        public void appendPhotos(List<Photo> photos) {
            mPhotos.addAll(photos);
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_photo, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {

            Photo photo = mPhotos.get(position);
            mThumbnailDownloader.queueThumnbnail(holder, photo.getUrl());

            //preload previous 10 photos and next 10 photos

            for (int i = position + 1; i < position + 11 && i < mPhotos.size(); i++) {
                Photo nexPhoto = mPhotos.get(position);
                mThumbnailDownloader.preloadThumbnail(nexPhoto.getUrl());
            }

            for (int i = position - 1; i > position - 11 && i >= 0; i--) {
                Photo previousPhoto = mPhotos.get(position);
                mThumbnailDownloader.preloadThumbnail(previousPhoto.getUrl());
            }

            holder.bindDrawable(null);
        }

        @Override
        public int getItemCount() {
            return mPhotos.size();
        }

    }

    private class PhotoViewHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image_view_photo);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }

    }

    private class FetchPhotosTask extends AsyncTask<Void, Void, List<Photo>> {

        private String mQuery;

        public FetchPhotosTask(String query) {
            mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            mIsLoading = true;
            if (mPhotoAdapter.getItemCount() == 0) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(List<Photo> photos) {

            if (!isAdded()) {
                return;
            }

            if (mPage == 1) {
                mPhotoAdapter.setPhotos(photos);

            } else {
                mPhotoAdapter.appendPhotos(photos);
            }

            mPage++;
            mIsLoading = false;

            mProgressBar.setVisibility(View.GONE);
            mPhotoAdapter.notifyDataSetChanged();

        }

        @Override
        protected List<Photo> doInBackground(Void... args) {
            if (mQuery == null) {
                return new FlickrFetcher().fetchRecentPhotos(mPage);
            } else {
                return new FlickrFetcher().search(mQuery, mPage);
            }
        }

    }


}
