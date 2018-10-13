package com.hongweizhuo.photogallery;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static String TAG = "PhotoGalleryFragment";

    private RecyclerView mRecyclerView;
    private PhotoAdapter mPhotoAdapter;
    private ThumbnailDownloader<PhotoViewHolder> mThumbnailDownloader;

    private int mPage = 1;
    private boolean mIsLoading = false;

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

        new FetchPhotosTask().execute();

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

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_photo_gallery);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));


        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    if (!mIsLoading) {
                        new FetchPhotosTask().execute();
                    }
                }

            }
        });

        setupAdapter();

        return view;

    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoAdapter = new PhotoAdapter(new ArrayList<Photo>());
            mRecyclerView.setAdapter(mPhotoAdapter);
        }
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

        @Override
        protected void onPreExecute() {
            mIsLoading = true;
        }

        @Override
        protected void onPostExecute(List<Photo> photos) {

            mPage++;
            mIsLoading = false;

            if (!isAdded()) {
                return;
            }

            if (mPage == 1) {
                mPhotoAdapter.setPhotos(photos);

            } else {
                mPhotoAdapter.appendPhotos(photos);
            }

            mPhotoAdapter.notifyDataSetChanged();

        }

        @Override
        protected List<Photo> doInBackground(Void... args) {
            return new FlickrFetcher().fetchPhotos(mPage);
        }

    }


}
