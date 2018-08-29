package com.hongweizhuo.photogallery;

import com.google.gson.annotations.SerializedName;

public class PhotoList {

    @SerializedName("photo")
    private Photo[] mPhotos;

    public Photo[] getPhotos() {
        return mPhotos;
    }

}
