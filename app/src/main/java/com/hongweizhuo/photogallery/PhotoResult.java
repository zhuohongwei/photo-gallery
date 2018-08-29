package com.hongweizhuo.photogallery;

import com.google.gson.annotations.SerializedName;

public class PhotoResult {

    public PhotoList getPhotosList() {
        return photosList;
    }

    @SerializedName("photos")
    private PhotoList photosList;

}