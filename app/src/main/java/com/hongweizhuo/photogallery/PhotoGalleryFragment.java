package com.hongweizhuo.photogallery;

import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
        new FetchPhotosTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_photo_gallery);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

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
            holder.bindPhoto(photo);
        }

        @Override
        public int getItemCount() {
            return mPhotos.size();
        }

    }

    private class PhotoViewHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;
        private TextView mTextView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image_view_photo);
            mTextView = itemView.findViewById(R.id.text_view_caption);
        }

        public void bindPhoto(Photo photo) {

            if (photo.getUrl().length() % 2 == 0) {
                itemView.setBackgroundColor(Color.WHITE);

            } else {
                itemView.setBackgroundColor(Color.GRAY);
            }

            mTextView.setText(photo.getCaption());

        }

    }

    private class FetchPhotosTask extends AsyncTask<Void, Void, List<Photo>> {
        @Override
        protected void onPostExecute(List<Photo> photos) {
            if (isAdded()) {
                mPhotoAdapter.setPhotos(photos);
                mPhotoAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected List<Photo> doInBackground(Void... voids) {
            return new FlickrFetcher().fetchPhotos();
        }

    }


}
