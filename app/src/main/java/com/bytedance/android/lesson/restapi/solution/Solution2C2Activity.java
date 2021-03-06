package com.bytedance.android.lesson.restapi.solution;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bytedance.android.lesson.restapi.solution.bean.Feed;
import com.bytedance.android.lesson.restapi.solution.bean.FeedResponse;
import com.bytedance.android.lesson.restapi.solution.bean.PostVideoResponse;
import com.bytedance.android.lesson.restapi.solution.newtork.IMiniDouyinService;
import com.bytedance.android.lesson.restapi.solution.newtork.RetrofitManager;
import com.bytedance.android.lesson.restapi.solution.utils.ResourceUtils;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Solution2C2Activity extends AppCompatActivity{

    private static final int PICK_IMAGE = 1;
    private static final int PICK_VIDEO = 2;
    private static final String TAG = "Solution2C2Activity";
    private static final String BASE_URL = "http://10.108.10.39:8080/";
    private static final String STU_NAME = "liuYang";
    private static final String STU_ID = "3220180830";

    private RecyclerView mRv;
    private List<Feed> mFeeds = new ArrayList<>();
    public Uri mSelectedImage;
    private Uri mSelectedVideo;
    public Button mBtn;
    private Button mBtnRefresh;
    private OrientationUtils orientationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solution2_c2);
        initRecyclerView();
        initBtns();
    }

    private void initBtns() {
        mBtn = findViewById(R.id.btn);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String s = mBtn.getText().toString();
                if (getString(R.string.select_an_image).equals(s)) {
                    chooseImage();
                } else if (getString(R.string.select_a_video).equals(s)) {
                    chooseVideo();
                } else if (getString(R.string.post_it).equals(s)) {
                    if (mSelectedVideo != null && mSelectedImage != null) {
                        postVideo();
                    } else {
                        throw new IllegalArgumentException("error data uri, mSelectedVideo = " + mSelectedVideo + ", mSelectedImage = " + mSelectedImage);
                    }
                } else if ((getString(R.string.success_try_refresh).equals(s))) {
                    mBtn.setText(R.string.select_an_image);
                }
            }
        });

        mBtnRefresh = findViewById(R.id.btn_refresh);
    }

    private void initRecyclerView() {
        mRv = findViewById(R.id.rv);
        mRv.setLayoutManager(new LinearLayoutManager(this));
        //设置滑动监听器，播放第一个完整可见的player
        mRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                /**
                 * 自定义的自动播放策略：列表填充数据完成后，若存在第一个视频，则自动播放
                 * 当滑动列表静止后，自动播放第一个完整可见的视频
                 */

                if(newState == RecyclerView.SCROLL_STATE_IDLE){
                    autoPlay();
                }

            }
        });
        mRv.setAdapter(new RecyclerView.Adapter() {
            @NonNull @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                //填充一个列表播放器布局供使用
                View inflate = LayoutInflater.from(Solution2C2Activity.this)
                        .inflate(R.layout.item_list_autoplay, null, false);
                return new FeedViewHolder(inflate);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {


                // TODO-C2 (10) Uncomment these 2 lines, assign image url of Feed to this url variable
                String url = mFeeds.get(i).getImgUrl();
                //Glide.with(iv.getContext()).load(url).into(iv);
                //初始化播放器及封面
                FeedViewHolder holder = (FeedViewHolder) viewHolder;
                initPlayer(holder.player,mFeeds.get(i));

            }

            @Override public int getItemCount() {
                return mFeeds.size();
            }


        });
    }

    /**
     * 自动播放列表第一个完整可见的视频逻辑
     */
    private void autoPlay() {
        RecyclerView.LayoutManager layoutManager = mRv.getLayoutManager();
        if(layoutManager instanceof LinearLayoutManager){
            LinearLayoutManager manager = (LinearLayoutManager) layoutManager;

            int firstPosition;
            FeedViewHolder holder = null;

            firstPosition = manager.findFirstCompletelyVisibleItemPosition();

            holder = (FeedViewHolder) mRv.findViewHolderForLayoutPosition(firstPosition);

            if(holder!=null){
                Log.i(TAG, "onScrollStateChanged: 自动播放,视频位于列表位置为："+firstPosition);
                holder.player.startPlayLogic();
            }
        }
    }


    /**
     * 视频列表的ViewHolder
     */
    class FeedViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public StandardGSYVideoPlayer player;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);

            //设置点击事件
            itemView.setOnClickListener(this);
            player = itemView.findViewById(R.id.list_player);
        }

        @Override
        public void onClick(View v) {

            //每个播放布局预留了简介的位置，当简介位置被点击时跳转视频详情页面
            onFeedItemClick(getAdapterPosition());
        }
    }

    /**
     * 拿到数据后初始化播放器
     * @param player 播放器
     * @param feed 单个视频数据
     */
    private void initPlayer(StandardGSYVideoPlayer player, Feed feed) {
        player.setUp(feed.getVideoUrl(), true, "feed");

        //增加封面
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.ic_launcher);
        Glide.with(this.getApplicationContext())
                .setDefaultRequestOptions(
                        new RequestOptions()
                                .frame(3000000)
                                .centerCrop()
                                .error(R.mipmap.xxx2))
                .load(feed.getImgUrl())
                .into(imageView);
        player.setThumbImageView(imageView);
        //增加title
        player.getTitleTextView().setVisibility(View.VISIBLE);
        //设置返回键
        player.getBackButton().setVisibility(View.VISIBLE);
        //设置旋转
        orientationUtils = new OrientationUtils(this, player);
        //设置全屏按键功能,这是使用的是选择屏幕，而不是全屏
        player.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orientationUtils.resolveByClick();
            }
        });
        //是否可以滑动调整
        player.setIsTouchWiget(true);
        //设置返回按键功能
        player.getBackButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    public void chooseImage() {
        // TODO-C2 (4) Start Activity to select an image
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE);
    }


    public void chooseVideo() {
        // TODO-C2 (5) Start Activity to select a video

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent,"Select Video"), PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() called with: requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");

        if (resultCode == RESULT_OK && null != data) {

            if (requestCode == PICK_IMAGE) {
                mSelectedImage = data.getData();
                Log.d(TAG, "selectedImage = " + mSelectedImage);
                mBtn.setText(R.string.select_a_video);
            } else if (requestCode == PICK_VIDEO) {
                mSelectedVideo = data.getData();
                Log.d(TAG, "mSelectedVideo = " + mSelectedVideo);
                mBtn.setText(R.string.post_it);
            }
        }
    }

    private MultipartBody.Part getMultipartFromUri(String name, Uri uri) {
        // if NullPointerException thrown, try to allow storage permission in system settings
        File f = new File(ResourceUtils.getRealPath(Solution2C2Activity.this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), f);
        return MultipartBody.Part.createFormData(name, f.getName(), requestFile);
    }

    private void postVideo() {
        mBtn.setText("POSTING...");
        mBtn.setEnabled(false);

        // TODO-C2 (6) Send Request to post a video with its cover image
        // if success, make a text Toast and show
        Retrofit retrofit = RetrofitManager.get(BASE_URL);

        MultipartBody.Part img = getMultipartFromUri("cover_image", mSelectedImage);
        MultipartBody.Part video = getMultipartFromUri("video", mSelectedVideo);


        Call<PostVideoResponse> call = retrofit.create(IMiniDouyinService.class)
                .createVideo(STU_ID, STU_NAME, img, video);

        call.enqueue(new Callback<PostVideoResponse>() {
            @Override
            public void onResponse(Call<PostVideoResponse> call, Response<PostVideoResponse> response) {
                PostVideoResponse body= response.body();

                if(body!=null && body.isResult()){
                    Toast.makeText(Solution2C2Activity.this,"success",Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<PostVideoResponse> call, Throwable t) {
                Toast.makeText(Solution2C2Activity.this,"failure",Toast.LENGTH_SHORT).show();
                resetRefreshBtn();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GSYVideoManager.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }

    public void fetchFeed(View view) {
        mBtnRefresh.setText("requesting...");
        mBtnRefresh.setEnabled(false);

        // TODO-C2 (9) Send Request to fetch feed
        // if success, assign data to mFeeds and call mRv.getAdapter().notifyDataSetChanged()
        // don't forget to call resetRefreshBtn() after response received
        Retrofit retrofit = RetrofitManager.get(BASE_URL);
        Call<FeedResponse> call = retrofit.create(IMiniDouyinService.class).fetchFeeds();
        call.enqueue(new Callback<FeedResponse>() {
            @Override
            public void onResponse(Call<FeedResponse> call, Response<FeedResponse> response) {
                mFeeds = response.body().getFeeds();
                mRv.getAdapter().notifyDataSetChanged();
                mRv.scrollToPosition(0);
                resetRefreshBtn();
            }

            @Override
            public void onFailure(Call<FeedResponse> call, Throwable t) {
                resetRefreshBtn();
            }
        });

    }

    private void resetRefreshBtn() {
        mBtnRefresh.setText(R.string.refresh_feed);
        mBtnRefresh.setEnabled(true);
    }

    /**
     * 点击item时触发点击事件
     * @param index 点击的item位置
     */
    public void onFeedItemClick(int index) {

        //点击cover时跳转视频详情页面
        Intent intent = new Intent(this, DetailPlayerActivity.class);
        Feed feed = mFeeds.get(index);
        intent.putExtra(DetailPlayerActivity.FEED_VIDEO_URL,feed.getVideoUrl());
        intent.putExtra(DetailPlayerActivity.FEED_COVER_URL,feed.getImgUrl());
        startActivity(intent);
    }

}
