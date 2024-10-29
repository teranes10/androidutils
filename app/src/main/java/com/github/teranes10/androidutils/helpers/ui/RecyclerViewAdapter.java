package com.github.teranes10.androidutils.helpers.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter<T> extends RecyclerView.Adapter<RecyclerViewAdapter<T>.ViewHolder> {
    private final Context _context;
    private OnItemViewTypeListener<T> _onViewType;
    private OnViewBinding _onViewBinding;
    private OnDataBinding<T> _onDataBinding;
    private OnItemClickListener<T> _onClick;
    private OnItemLongClickListener<T> _onLongClick;
    private OnLoadMoreDataListener<T> _onLoadMoreData;
    private List<T> _items = new ArrayList<>();
    private int _page = 1;
    private int _itemsPerPage = 10;
    private int _totalPages = 0;

    @SuppressLint("NotifyDataSetChanged")
    private final LoadMoreData<T> _loadMore = (items, totalItems) -> new Handler(Looper.getMainLooper()).post(() -> {
        _items.addAll(items);
        setTotalItems(totalItems);
        notifyDataSetChanged();
    });

    public RecyclerViewAdapter(Context context) {
        this._context = context;
    }

    public void setTotalItems(int totalItems) {
        _totalPages = (int) Math.ceil((double) totalItems / _itemsPerPage);
    }

    public RecyclerViewAdapter<T> bindViewType(OnItemViewTypeListener<T> listener) {
        _onViewType = listener;
        return this;
    }

    public RecyclerViewAdapter<T> bindView(OnViewBinding binding) {
        _onViewBinding = binding;
        return this;
    }

    public RecyclerViewAdapter<T> bindData(OnDataBinding<T> binding) {
        _onDataBinding = binding;
        return this;
    }

    public RecyclerViewAdapter<T> setOnLoadMoreData(OnLoadMoreDataListener<T> listener) {
        _onLoadMoreData = listener;
        return this;
    }

    public RecyclerViewAdapter<T> setAdapter(RecyclerView recyclerView) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_context);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(this);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = linearLayoutManager.getChildCount();
                int totalItemCount = linearLayoutManager.getItemCount();
                int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();

                if (totalItemCount >= _itemsPerPage && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                    _page++;
                    if (_page <= _totalPages) {
                        if (_onLoadMoreData != null) {
                            _onLoadMoreData.OnLoadMoreData(_page, _itemsPerPage, _loadMore);
                        }
                    }
                }
            }
        });

        if (_onLoadMoreData != null) {
            _onLoadMoreData.OnLoadMoreData(_page, _itemsPerPage, _loadMore);
        }
        return this;
    }

    public RecyclerViewAdapter<T> setOnClickListener(OnItemClickListener<T> listener) {
        _onClick = listener;
        return this;
    }

    public RecyclerViewAdapter<T> setOnLongClickListener(OnItemLongClickListener<T> listener) {
        _onLongClick = listener;
        return this;
    }

    public int get_page() {
        return _page;
    }

    public int get_itemsPerPage() {
        return _itemsPerPage;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<T> items) {
        new Handler(Looper.getMainLooper()).post(() -> {
            _page = 1;
            _items = items;
            notifyDataSetChanged();
        });
    }

    public RecyclerViewAdapter<T> setItemsPerPage(int itemsPerPage) {
        _itemsPerPage = itemsPerPage;
        return this;
    }

    public List<T> getItems() {
        return _items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                _onViewBinding.onViewCreate(
                        LayoutInflater.from(parent.getContext()), parent, viewType));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (_items != null && position < _items.size()) {
            T item = _items.get(position);
            holder.bind(item);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (_onViewType != null && (_items != null && position < _items.size())) {
            return _onViewType.OnItemViewType(_items.get(position));
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        return _items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ViewBinding _binding;

        public ViewHolder(@NonNull ViewBinding binding) {
            super(binding.getRoot());
            _binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (_onClick != null && position != RecyclerView.NO_POSITION) {
                    if (_items != null && position < _items.size()) {
                        _onClick.onItemClick(_items.get(position));
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (_onLongClick != null && position != RecyclerView.NO_POSITION) {
                    if (_items != null && position < _items.size()) {
                        _onLongClick.onItemLongClick(_items.get(position));
                    }
                }
                return false;
            });
        }

        public void bind(T item) {
            _onDataBinding.onDataBind(_binding, item);
        }
    }

    public interface OnItemViewTypeListener<T> {
        int OnItemViewType(T item);
    }

    public interface OnViewBinding {
        ViewBinding onViewCreate(LayoutInflater inflater, ViewGroup parent, int viewType);
    }

    public interface OnDataBinding<T> {
        void onDataBind(ViewBinding binding, T item);
    }

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    public interface OnItemLongClickListener<T> {
        void onItemLongClick(T item);
    }

    public interface OnLoadMoreDataListener<T> {
        void OnLoadMoreData(int page, int itemsPerPage, LoadMoreData<T> callback);
    }

    public interface LoadMoreData<T> {
        void LoadMore(List<T> items, Integer totalItems);
    }
}
