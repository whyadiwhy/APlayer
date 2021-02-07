package remix.myplayer.ui.adapter;

import static remix.myplayer.helper.MusicServiceRemote.setPlayQueue;
import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.theme.ThemeStore.getHighLightTextColor;
import static remix.myplayer.theme.ThemeStore.getTextColorPrimary;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.MusicUtil.makeCmdIntent;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.promeg.pinyinhelper.Pinyin;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.databinding.ItemSongRecycleBinding;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.misc.menu.SongPopupListener;
import remix.myplayer.service.Command;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-6-24.
 */
@SuppressLint("RestrictedApi")
public class ChildHolderAdapter extends HeaderAdapter<Song, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  private int mType;
  private String mArg;

  private Song mLastPlaySong = MusicServiceRemote.getCurrentSong();

  public ChildHolderAdapter(int layoutId, int type, String arg,
      MultipleChoice multiChoice, RecyclerView recyclerView) {
    super(layoutId, multiChoice, recyclerView);
    this.mType = type;
    this.mArg = arg;
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return viewType == TYPE_HEADER ?
        new SongAdapter.HeaderHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_header_1, parent, false)) :
        new ChildHolderViewHolder(
            ItemSongRecycleBinding.inflate(LayoutInflater.from(parent.getContext()), parent,
                false));
  }


  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @Override
  protected void convert(final BaseViewHolder baseHolder, final Song song, int position) {
    final Context context = baseHolder.itemView.getContext();
    if (position == 0) {
      final SongAdapter.HeaderHolder headerHolder = (SongAdapter.HeaderHolder) baseHolder;
      //没有歌曲时隐藏
      if (mDatas == null || mDatas.size() == 0) {
        headerHolder.binding.getRoot().setVisibility(View.GONE);
        return;
      }

      headerHolder.binding.playShuffleButton.setImageDrawable(
          Theme.tintVectorDrawable(context, R.drawable.ic_shuffle_white_24dp,
              ThemeStore.getAccentColor())
      );
      headerHolder.binding.tvShuffleCount.setText(context.getString(R.string.play_random, getItemCount() - 1));

      //显示当前排序方式
      headerHolder.binding.getRoot().setOnClickListener(v -> {
        //设置正在播放列表
        if (mDatas == null || mDatas.isEmpty()) {
          ToastUtil.show(context, R.string.no_song);
          return;
        }
        setPlayQueue(mDatas, makeCmdIntent(Command.NEXT, true));
      });
      return;
    }

    final ChildHolderViewHolder holder = (ChildHolderViewHolder) baseHolder;
    if (song == null || song.getId() < 0 || song.getTitle()
        .equals(context.getString(R.string.song_lose_effect))) {
      holder.binding.songTitle.setText(R.string.song_lose_effect);
      holder.binding.songButton.setVisibility(View.INVISIBLE);
    } else {
      holder.binding.songButton.setVisibility(View.VISIBLE);

      //封面
      holder.binding.songHeadImage.setTag(
          setImage(holder.binding.songHeadImage, getSearchRequestWithAlbumType(song), SMALL_IMAGE_SIZE, position));

      //高亮
      if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
        mLastPlaySong = song;
        holder.binding.songTitle.setTextColor(getHighLightTextColor());
        holder.binding.indicator.setVisibility(View.VISIBLE);
      } else {
        holder.binding.songTitle.setTextColor(getTextColorPrimary());
        holder.binding.indicator.setVisibility(View.GONE);
      }
      holder.binding.indicator.setBackgroundColor(getHighLightTextColor());

      //设置标题
      holder.binding.songTitle.setText(song.getShowName());

      //艺术家与专辑
      holder.binding.songOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));

      if (holder.binding.songButton != null) {
        //设置按钮着色
        int tintColor = ThemeStore.getLibraryBtnColor();
        Theme.tintDrawable(holder.binding.songButton, R.drawable.icon_player_more, tintColor);

        holder.binding.songButton.setOnClickListener(v -> {
          if (mChoice.isActive()) {
            return;
          }
          final PopupMenu popupMenu = new PopupMenu(context, holder.binding.songButton, Gravity.END);
          popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
          popupMenu.setOnMenuItemClickListener(
              new SongPopupListener((AppCompatActivity) context, song, mType == Constants.PLAYLIST,
                  mArg));
          popupMenu.show();
        });
      }
    }

    if (holder.binding.getRoot() != null && mOnItemClickListener != null) {
      holder.binding.getRoot().setOnClickListener(v -> {
        if (holder.getAdapterPosition() - 1 < 0) {
          ToastUtil.show(context, R.string.illegal_arg);
          return;
        }
        if (song != null && song.getId() > 0) {
          mOnItemClickListener.onItemClick(v, holder.getAdapterPosition() - 1);
        }
      });
      holder.binding.getRoot().setOnLongClickListener(v -> {
        if (holder.getAdapterPosition() - 1 < 0) {
          ToastUtil.show(context, R.string.illegal_arg);
          return true;
        }
        mOnItemClickListener.onItemLongClick(v, holder.getAdapterPosition() - 1);
        return true;
      });
    }

    holder.binding.getRoot().setSelected(mChoice.isPositionCheck(position - 1));
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (mDatas != null && mDatas.size() > 0 && position < mDatas.size()
        && mDatas.get(position - 1) != null) {
      String title = mDatas.get(position - 1).getTitle();
      return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  public void updatePlayingSong() {
    final Song currentSong = MusicServiceRemote.getCurrentSong();
    if (currentSong.getId() == -1 || currentSong.getId() == mLastPlaySong.getId()) {
      return;
    }

    if (mDatas != null && mDatas.contains(currentSong)) {
      // 找到新的高亮歌曲
      final int index = mDatas.indexOf(currentSong) + 1;
      final int lastIndex = mDatas.indexOf(mLastPlaySong) + 1;

      ChildHolderViewHolder newHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(index) instanceof ChildHolderViewHolder) {
        newHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
      }
      ChildHolderViewHolder oldHolder = null;
      if (mRecyclerView
          .findViewHolderForAdapterPosition(lastIndex) instanceof ChildHolderViewHolder) {
        oldHolder = (ChildHolderViewHolder) mRecyclerView
            .findViewHolderForAdapterPosition(lastIndex);
      }

      if (newHolder != null) {
        newHolder.binding.songTitle.setTextColor(getHighLightTextColor());
        newHolder.binding.indicator.setVisibility(View.VISIBLE);
      }

      if (oldHolder != null) {
        oldHolder.binding.songTitle.setTextColor(getTextColorPrimary());
        oldHolder.binding.indicator.setVisibility(View.GONE);
      }
      mLastPlaySong = currentSong;
    }
  }

  static class ChildHolderViewHolder extends BaseViewHolder {

    private final ItemSongRecycleBinding binding;

    ChildHolderViewHolder(ItemSongRecycleBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }
}