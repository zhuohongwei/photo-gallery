package com.hongweizhuo.photogallery;

import com.google.gson.annotations.SerializedName;

public class Photo {

    @SerializedName("id")
    private String mId;

    @SerializedName("url_s")
    private String mUrl;

    @SerializedName("title")
    private String mCaption;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    @Override
    public String toString() {
        return mCaption;
    }

}
